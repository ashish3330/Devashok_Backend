package com.realestate.emi.service;

import com.realestate.emi.dto.response.DealMaterialCostResponse;
import com.realestate.emi.dto.response.InventoryValuationResponse;
import com.realestate.emi.dto.response.StockMovementResponse;
import com.realestate.emi.dto.response.StockTransactionResponse;
import com.realestate.emi.dto.response.WastageReportResponse;
import com.realestate.emi.entity.Material;
import com.realestate.emi.entity.StockTransaction;
import com.realestate.emi.enums.TransactionType;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.MaterialMapper;
import com.realestate.emi.mapper.StockTransactionMapper;
import com.realestate.emi.repository.MaterialRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.StockTransactionRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReportService {

    private final MaterialRepository materialRepository;
    private final StockTransactionRepository transactionRepository;
    private final MaterialMapper materialMapper;
    private final StockTransactionMapper transactionMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public InventoryValuationResponse getInventoryValuation() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        var orgMaterials = materialRepository.findByOrganizationIdAndIsActiveTrueOrderByNameAsc(orgId);
        return InventoryValuationResponse.builder()
                .totalInventoryValue(materialRepository.calculateTotalInventoryValueByOrg(orgId))
                .totalMaterials((long) orgMaterials.size())
                .lowStockCount((long) materialRepository.findLowStockMaterialsByOrg(orgId).size())
                .outOfStockCount((long) materialRepository.findOutOfStockMaterialsByOrg(orgId).size())
                .materials(materialMapper.toResponseList(orgMaterials))
                .build();
    }

    @Transactional(readOnly = true)
    public DealMaterialCostResponse getCostPerDeal(Long dealId) {
        BigDecimal totalCost = transactionRepository.sumMaterialCostByDealId(dealId);
        List<StockTransactionResponse> transactions = transactionMapper.toResponseList(
                transactionRepository.findByDealIdOrderByTransactionDateDesc(dealId));

        return DealMaterialCostResponse.builder()
                .dealId(dealId)
                .totalMaterialCost(totalCost)
                .transactions(transactions)
                .build();
    }

    @Transactional(readOnly = true)
    public WastageReportResponse getWastageReport(LocalDate from, LocalDate to) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        List<TransactionType> types = List.of(TransactionType.WASTAGE, TransactionType.DAMAGE, TransactionType.RETURN_TO_SUPPLIER);
        List<StockTransaction> transactions = transactionRepository.findByTypesAndDateRangeAndOrg(types, fromDt, toDt, orgId);

        BigDecimal totalWastage = BigDecimal.ZERO;
        BigDecimal totalDamage = BigDecimal.ZERO;
        BigDecimal totalReturn = BigDecimal.ZERO;

        // Group by material
        Map<Long, List<StockTransaction>> byMaterial = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getMaterial().getId()));

        List<WastageReportResponse.MaterialWastageItem> breakdown = new ArrayList<>();

        for (Map.Entry<Long, List<StockTransaction>> entry : byMaterial.entrySet()) {
            Material material = entry.getValue().get(0).getMaterial();
            BigDecimal wQty = BigDecimal.ZERO, wVal = BigDecimal.ZERO;
            BigDecimal dQty = BigDecimal.ZERO, dVal = BigDecimal.ZERO;
            BigDecimal rQty = BigDecimal.ZERO, rVal = BigDecimal.ZERO;

            for (StockTransaction t : entry.getValue()) {
                BigDecimal cost = t.getTotalCost() != null ? t.getTotalCost() : BigDecimal.ZERO;
                switch (t.getType()) {
                    case WASTAGE -> { wQty = wQty.add(t.getQuantity()); wVal = wVal.add(cost); }
                    case DAMAGE -> { dQty = dQty.add(t.getQuantity()); dVal = dVal.add(cost); }
                    case RETURN_TO_SUPPLIER -> { rQty = rQty.add(t.getQuantity()); rVal = rVal.add(cost); }
                    default -> {}
                }
            }

            totalWastage = totalWastage.add(wVal);
            totalDamage = totalDamage.add(dVal);
            totalReturn = totalReturn.add(rVal);

            breakdown.add(WastageReportResponse.MaterialWastageItem.builder()
                    .materialId(material.getId())
                    .materialName(material.getName())
                    .category(material.getCategory().name())
                    .unit(material.getUnit().name())
                    .wastageQuantity(wQty).wastageValue(wVal)
                    .damageQuantity(dQty).damageValue(dVal)
                    .returnQuantity(rQty).returnValue(rVal)
                    .build());
        }

        breakdown.sort((a, b) -> b.getWastageValue().add(b.getDamageValue())
                .compareTo(a.getWastageValue().add(a.getDamageValue())));

        return WastageReportResponse.builder()
                .totalWastageValue(totalWastage)
                .totalDamageValue(totalDamage)
                .totalReturnValue(totalReturn)
                .breakdown(breakdown)
                .build();
    }

    @Transactional(readOnly = true)
    public StockMovementResponse getStockMovementReport(Long materialId, LocalDate from, LocalDate to) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material", materialId));
        if (material.getOrganization() != null && !material.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Material", materialId);
        }

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        // Calculate opening stock: current stock - net changes after 'from' date
        // Get all transactions in the period
        List<StockTransaction> periodTxns = transactionRepository
                .findByMaterialIdAndTransactionDateBetween(materialId, fromDt, toDt);

        BigDecimal totalInward = BigDecimal.ZERO;
        BigDecimal totalOutward = BigDecimal.ZERO;
        BigDecimal totalWastage = BigDecimal.ZERO;
        BigDecimal totalDamage = BigDecimal.ZERO;
        BigDecimal totalAdjustments = BigDecimal.ZERO;
        BigDecimal totalReturns = BigDecimal.ZERO;

        for (StockTransaction t : periodTxns) {
            switch (t.getType()) {
                case INWARD -> totalInward = totalInward.add(t.getQuantity());
                case OUTWARD -> totalOutward = totalOutward.add(t.getQuantity());
                case WASTAGE -> totalWastage = totalWastage.add(t.getQuantity());
                case DAMAGE -> totalDamage = totalDamage.add(t.getQuantity());
                case ADJUSTMENT -> totalAdjustments = totalAdjustments.add(t.getQuantity());
                case RETURN_TO_SUPPLIER -> totalReturns = totalReturns.add(t.getQuantity());
            }
        }

        // Calculate opening stock: current - net period changes
        // Net changes = inward - outward - wastage - damage - returns (adjustments set absolute so skip)
        BigDecimal netChange = totalInward.subtract(totalOutward).subtract(totalWastage)
                .subtract(totalDamage).subtract(totalReturns);

        // Also account for transactions after 'to' date to get closing stock at 'to'
        // Closing stock = current stock (we approximate as current since we only have current)
        // For simplicity: closing = current stock, opening = closing - netChange
        BigDecimal closingStock = material.getCurrentQuantity();
        BigDecimal openingStock = closingStock.subtract(netChange);

        BigDecimal netMovement = totalInward.subtract(totalOutward).subtract(totalWastage)
                .subtract(totalDamage).subtract(totalReturns);

        return StockMovementResponse.builder()
                .materialId(material.getId())
                .materialName(material.getName())
                .category(material.getCategory().name())
                .unit(material.getUnit().name())
                .openingStock(openingStock)
                .totalInward(totalInward)
                .totalOutward(totalOutward)
                .totalWastage(totalWastage)
                .totalDamage(totalDamage)
                .totalAdjustments(totalAdjustments)
                .totalReturns(totalReturns)
                .closingStock(closingStock)
                .netMovement(netMovement)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] getInventoryExcel() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        var materials = materialRepository.findByOrganizationIdAndIsActiveTrueOrderByNameAsc(orgId);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Inventory Report");

            // Column widths
            int[] widths = {6000, 3500, 2500, 3000, 3500, 4000, 3500, 3000, 5000, 6000};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i]);

            // Styles
            XSSFCellStyle titleStyle = createTitleStyle(wb);
            XSSFCellStyle headerStyle = createHeaderStyle(wb);
            XSSFCellStyle textStyle = createTextStyle(wb);
            XSSFCellStyle numStyle = createNumStyle(wb);
            XSSFCellStyle lowStockStyle = createLowStockStyle(wb);
            XSSFCellStyle summaryLabelStyle = createSummaryLabelStyle(wb);
            XSSFCellStyle summaryNumStyle = createSummaryNumStyle(wb);

            int r = 0;

            // Title row
            Row titleRow = sheet.createRow(r++);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("INVENTORY REPORT  |  Generated: " +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            r++; // blank row

            // Headers
            Row hdr = sheet.createRow(r++);
            hdr.setHeightInPoints(20);
            String[] cols = {"Material", "Category", "Unit", "Current Qty", "Unit Cost",
                    "Stock Value", "Reorder Level", "Status", "Last Inward Date", "Suppliers"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = hdr.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            // Data rows
            BigDecimal totalStockValue = BigDecimal.ZERO;
            int lowStockCount = 0;
            int outOfStockCount = 0;

            for (Material m : materials) {
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(17);
                BigDecimal stockVal = m.getCurrentQuantity().multiply(m.getUnitCost());
                totalStockValue = totalStockValue.add(stockVal);
                boolean isLow = m.getCurrentQuantity().compareTo(m.getReorderLevel()) <= 0;
                boolean isOutOfStock = m.getCurrentQuantity().compareTo(BigDecimal.ZERO) == 0;
                if (isOutOfStock) outOfStockCount++;
                else if (isLow) lowStockCount++;

                XSSFCellStyle rowText = isLow ? lowStockStyle : textStyle;
                XSSFCellStyle rowNum = isLow ? lowStockStyle : numStyle;

                setStrCell(row, 0, m.getName(), rowText);
                setStrCell(row, 1, m.getCategory().name(), rowText);
                setStrCell(row, 2, m.getUnit().name(), rowText);
                setNumCell(row, 3, m.getCurrentQuantity().doubleValue(), rowNum);
                setNumCell(row, 4, m.getUnitCost().doubleValue(), rowNum);
                setNumCell(row, 5, stockVal.doubleValue(), rowNum);
                setNumCell(row, 6, m.getReorderLevel().doubleValue(), rowNum);
                setStrCell(row, 7, isOutOfStock ? "OUT OF STOCK" : isLow ? "LOW STOCK" : "OK", rowText);

                // Last inward date
                LocalDateTime lastInward = transactionRepository.findLastInwardDateByMaterialId(m.getId());
                setStrCell(row, 8, lastInward != null ?
                        lastInward.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : "-", rowText);

                // Suppliers
                String supplierNames = m.getSuppliers() != null ?
                        m.getSuppliers().stream().map(s -> s.getName()).collect(Collectors.joining(", ")) : "-";
                setStrCell(row, 9, supplierNames, rowText);
            }

            r++; // blank row

            // Summary
            Row sumRow1 = sheet.createRow(r++);
            setStrCell(sumRow1, 0, "Total Stock Value", summaryLabelStyle);
            setNumCell(sumRow1, 1, totalStockValue.doubleValue(), summaryNumStyle);

            Row sumRow2 = sheet.createRow(r++);
            setStrCell(sumRow2, 0, "Low Stock Count", summaryLabelStyle);
            setNumCell(sumRow2, 1, lowStockCount, summaryNumStyle);

            Row sumRow3 = sheet.createRow(r++);
            setStrCell(sumRow3, 0, "Out of Stock Count", summaryLabelStyle);
            setNumCell(sumRow3, 1, outOfStockCount, summaryNumStyle);

            Row sumRow4 = sheet.createRow(r);
            setStrCell(sumRow4, 0, "Total Materials", summaryLabelStyle);
            setNumCell(sumRow4, 1, materials.size(), summaryNumStyle);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Inventory Excel generation failed", e);
            throw new ServiceException("Failed to generate Excel: " + e.getMessage(), "EXCEL_GENERATION_FAILED");
        }
    }

    // ─── Excel style helpers ────────────────────────────────────────────────

    private XSSFCellStyle createTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 14); f.setFontName("Calibri");
        f.setColor(new XSSFColor(new byte[]{(byte) 25, (byte) 80, (byte) 150}, null));
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 220, (byte) 230, (byte) 245}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 11); f.setFontName("Calibri");
        f.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 25, (byte) 80, (byte) 150}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(s);
        return s;
    }

    private XSSFCellStyle createTextStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10); f.setFontName("Calibri");
        s.setFont(f);
        applyBorder(s);
        return s;
    }

    private XSSFCellStyle createNumStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10); f.setFontName("Calibri");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        applyBorder(s);
        return s;
    }

    private XSSFCellStyle createLowStockStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10); f.setFontName("Calibri");
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 255, (byte) 199, (byte) 206}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(s);
        return s;
    }

    private XSSFCellStyle createSummaryLabelStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 11); f.setFontName("Calibri");
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 189, (byte) 215, (byte) 238}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(s);
        return s;
    }

    private XSSFCellStyle createSummaryNumStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 11); f.setFontName("Calibri");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 189, (byte) 215, (byte) 238}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(s);
        return s;
    }

    private void applyBorder(XSSFCellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    private void setNumCell(Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col, CellType.NUMERIC);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private void setStrCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col, CellType.STRING);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }
}
