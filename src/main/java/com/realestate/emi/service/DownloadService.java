package com.realestate.emi.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.realestate.emi.entity.Attendance;
import com.realestate.emi.entity.Deal;
import com.realestate.emi.entity.EmiSchedule;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Payment;
import com.realestate.emi.entity.SalaryRecord;
import com.realestate.emi.entity.Staff;
import com.realestate.emi.entity.StockTransaction;
import com.realestate.emi.entity.Supplier;
import com.realestate.emi.enums.AttendanceStatus;
import com.realestate.emi.enums.SalaryStatus;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.AttendanceRepository;
import com.realestate.emi.repository.DealRepository;
import com.realestate.emi.repository.EmiScheduleRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.PaymentRepository;
import com.realestate.emi.repository.SalaryRecordRepository;
import com.realestate.emi.repository.StaffRepository;
import com.realestate.emi.repository.StockTransactionRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadService {

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

    private static final Color BLUE_DARK   = new Color(25,  80,  150);
    private static final Color GREEN_DARK  = new Color(0,   130, 50);
    private static final Color GREY_LIGHT  = new Color(245, 245, 250);
    private static final Color TEXT_DARK   = new Color(40,  40,  40);
    private static final Color TEXT_LABEL  = new Color(60,  60,  60);
    private static final Color TEXT_FOOTER = new Color(130, 130, 130);

    private final DealRepository dealRepository;
    private final PaymentRepository paymentRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final AttendanceRepository attendanceRepository;
    private final StaffRepository staffRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantContext tenantContext;

    public record FileDownload(byte[] content, String fileName) {}

    // ─── Public API ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FileDownload getEmiReceipt(Long dealId, Long paymentId) {
        Deal deal = dealRepository.findByIdWithDetails(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", dealId));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (deal.getOrganization() != null && !deal.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Deal", dealId);
        }
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        if (!payment.getDeal().getId().equals(dealId)) {
            throw new ServiceException("Payment does not belong to this deal", "PAYMENT_DEAL_MISMATCH");
        }
        String fileName = sanitize(deal.getCustomer().getFullName())
                + "_" + sanitize(deal.getPropertyType().getName())
                + "_Receipt_" + paymentId + ".pdf";
        return new FileDownload(buildReceiptPdf(deal, payment), fileName);
    }

    @Transactional(readOnly = true)
    public FileDownload getEmiScheduleExcel(Long dealId) {
        Deal deal = dealRepository.findByIdWithDetails(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", dealId));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (deal.getOrganization() != null && !deal.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Deal", dealId);
        }
        List<EmiSchedule> schedules = emiScheduleRepository.findByDealOrderByDueDateAsc(deal);
        String fileName = sanitize(deal.getCustomer().getFullName())
                + "_" + sanitize(deal.getPropertyType().getName())
                + "_EMI_Schedule.xlsx";
        return new FileDownload(buildScheduleExcel(deal, schedules), fileName);
    }

    @Transactional(readOnly = true)
    public FileDownload getSalarySlip(Long salaryId) {
        SalaryRecord record = salaryRecordRepository.findById(salaryId)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryRecord", salaryId));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (record.getStaff().getOrganization() != null
                && !record.getStaff().getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("SalaryRecord", salaryId);
        }
        if (record.getStatus() == SalaryStatus.PENDING || record.getStatus() == SalaryStatus.HOLD) {
            throw new ServiceException("Salary slip can only be downloaded after payment has been made", "SALARY_NOT_PAID");
        }
        String monthName = Month.of(record.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String fileName = sanitize(record.getStaff().getFullName())
                + "_Salary_Slip_" + monthName + "_" + record.getYear() + ".pdf";
        return new FileDownload(buildSalarySlipPdf(record), fileName);
    }

    @Transactional(readOnly = true)
    public FileDownload getStockTransactionReceipt(Long transactionId) {
        StockTransaction txn = stockTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransaction", transactionId));
        Organization org = txn.getMaterial().getOrganization();
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (org != null && !org.getId().equals(orgId)) {
            throw new ResourceNotFoundException("StockTransaction", transactionId);
        }
        String fileName = "Stock_Transaction_Receipt_" + transactionId + ".pdf";
        return new FileDownload(buildStockTransactionReceiptPdf(txn, org), fileName);
    }

    @Transactional(readOnly = true)
    public FileDownload getAttendanceReport(int year, int month) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        List<Staff> activeStaff = staffRepository.findByOrganizationIdAndIsActiveTrueOrderByFullNameAsc(orgId);

        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String fileName = "Attendance_Report_" + monthName + "_" + year + ".xlsx";
        return new FileDownload(buildAttendanceExcel(org, activeStaff, year, month), fileName);
    }

    // ─── Payroll Excel ────────────────────────────────────────────────────────

    public FileDownload getPayrollExcel(int year, int month, List<SalaryRecord> records) {
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String fileName = "Payroll_" + monthName + "_" + year + ".xlsx";
        return new FileDownload(buildPayrollExcel(year, month, records), fileName);
    }

    private byte[] buildPayrollExcel(int year, int month, List<SalaryRecord> records) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            XSSFSheet sheet = wb.createSheet("Payroll " + monthName + " " + year);

            int[] colWidths = {
                3000, 6000, 4000, 4000, 3000, 3000, 3000, 3000, 3000,
                4000, 4000, 3500, 4000, 4000, 4500,
                3500, 3500, 3500, 4000, 4000, 4000,
                5000, 4500, 4500, 4000
            };
            for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i]);

            XSSFCellStyle titleStyle = makeTitleStyle(wb);
            XSSFCellStyle headerStyle = makeHeaderStyle(wb);
            XSSFCellStyle sumLabelStyle = makeSumLabelStyle(wb);
            XSSFCellStyle sumNumStyle = makeSumNumStyle(wb);

            byte[] rgbPaid    = {(byte) 198, (byte) 239, (byte) 206};
            byte[] rgbPartial = {(byte) 255, (byte) 235, (byte) 156};
            byte[] rgbPending = {(byte) 255, (byte) 199, (byte) 206};
            XSSFCellStyle numPaid    = makeNumStyle(wb, rgbPaid);
            XSSFCellStyle numPartial = makeNumStyle(wb, rgbPartial);
            XSSFCellStyle numPending = makeNumStyle(wb, rgbPending);
            XSSFCellStyle txtPaid    = makeTxtStyle(wb, rgbPaid);
            XSSFCellStyle txtPartial = makeTxtStyle(wb, rgbPartial);
            XSSFCellStyle txtPending = makeTxtStyle(wb, rgbPending);

            int r = 0;

            String orgName = getOrgName();
            Row titleRow = sheet.createRow(r++);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(orgName + "  |  PAYROLL REGISTER  |  " + monthName + " " + year);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 24));

            r++; // blank row

            Row hdr = sheet.createRow(r++);
            hdr.setHeightInPoints(22);
            String[] cols = {
                "Emp Code", "Name", "Department", "Base Salary",
                "Working Days", "Present", "Absent", "Half", "OT Hrs",
                "Basic (Earned)", "HRA", "Conv", "Special", "OT Pay", "Gross",
                "PF", "ESI", "PT", "Advance Ded", "Other Ded", "Total Ded",
                "Net Salary", "Paid", "Balance", "Status"
            };
            for (int i = 0; i < cols.length; i++) {
                Cell c = hdr.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            double[] totals = new double[21];
            for (SalaryRecord rec : records) {
                String status = rec.getStatus().name();
                XSSFCellStyle ns = switch (status) {
                    case "PAID"    -> numPaid;
                    case "PARTIAL" -> numPartial;
                    default        -> numPending;
                };
                XSSFCellStyle ts = switch (status) {
                    case "PAID"    -> txtPaid;
                    case "PARTIAL" -> txtPartial;
                    default        -> txtPending;
                };

                BigDecimal balanceDue = rec.getNetSalary().subtract(rec.getAmountPaid()).max(BigDecimal.ZERO);
                BigDecimal basicPay = rec.getBasicPay() != null ? rec.getBasicPay() : BigDecimal.ZERO;
                BigDecimal hraVal = rec.getHra() != null ? rec.getHra() : BigDecimal.ZERO;
                BigDecimal convVal = rec.getConveyanceAllowance() != null ? rec.getConveyanceAllowance() : BigDecimal.ZERO;
                BigDecimal specVal = rec.getSpecialAllowance() != null ? rec.getSpecialAllowance() : BigDecimal.ZERO;
                BigDecimal grossVal = rec.getGrossEarnings() != null ? rec.getGrossEarnings() : BigDecimal.ZERO;
                BigDecimal pfVal = rec.getPfDeduction() != null ? rec.getPfDeduction() : BigDecimal.ZERO;
                BigDecimal esiVal = rec.getEsiDeduction() != null ? rec.getEsiDeduction() : BigDecimal.ZERO;
                BigDecimal ptVal = rec.getProfessionalTax() != null ? rec.getProfessionalTax() : BigDecimal.ZERO;
                BigDecimal advVal = rec.getAdvanceDeduction() != null ? rec.getAdvanceDeduction() : BigDecimal.ZERO;
                BigDecimal totalDedVal = rec.getTotalDeductions() != null ? rec.getTotalDeductions() : BigDecimal.ZERO;

                Row row = sheet.createRow(r++);
                row.setHeightInPoints(17);
                int c = 0;
                String empCode = rec.getStaff().getEmployeeCode() != null ? rec.getStaff().getEmployeeCode() : "EMP-" + String.format("%03d", rec.getStaff().getId());
                setStrCell(row, c++, empCode, ts);
                setStrCell(row, c++, rec.getStaff().getFullName(), ts);
                setStrCell(row, c++, rec.getStaff().getStaffRole() != null ? rec.getStaff().getStaffRole().getName() : "-", ts);
                setNumCell(row, c++, rec.getBaseSalary().doubleValue(), ns);
                setNumCell(row, c++, rec.getWorkingDays(), ns);
                setNumCell(row, c++, rec.getPresentDays(), ns);
                setNumCell(row, c++, rec.getAbsentDays(), ns);
                setNumCell(row, c++, rec.getHalfDays(), ns);
                setNumCell(row, c++, rec.getOvertimeHours().doubleValue(), ns);
                setNumCell(row, c++, basicPay.doubleValue(), ns);
                setNumCell(row, c++, hraVal.doubleValue(), ns);
                setNumCell(row, c++, convVal.doubleValue(), ns);
                setNumCell(row, c++, specVal.doubleValue(), ns);
                setNumCell(row, c++, rec.getOvertimePay().doubleValue(), ns);
                setNumCell(row, c++, grossVal.doubleValue(), ns);
                setNumCell(row, c++, pfVal.doubleValue(), ns);
                setNumCell(row, c++, esiVal.doubleValue(), ns);
                setNumCell(row, c++, ptVal.doubleValue(), ns);
                setNumCell(row, c++, advVal.doubleValue(), ns);
                setNumCell(row, c++, rec.getDeductions().doubleValue(), ns);
                setNumCell(row, c++, totalDedVal.add(rec.getDeductions()).doubleValue(), ns);
                setNumCell(row, c++, rec.getNetSalary().doubleValue(), ns);
                setNumCell(row, c++, rec.getAmountPaid().doubleValue(), ns);
                setNumCell(row, c++, balanceDue.doubleValue(), ns);
                setStrCell(row, c, status, ts);

                totals[0]  += rec.getBaseSalary().doubleValue();
                totals[6]  += basicPay.doubleValue();
                totals[7]  += hraVal.doubleValue();
                totals[8]  += convVal.doubleValue();
                totals[9]  += specVal.doubleValue();
                totals[10] += rec.getOvertimePay().doubleValue();
                totals[11] += grossVal.doubleValue();
                totals[12] += pfVal.doubleValue();
                totals[13] += esiVal.doubleValue();
                totals[14] += ptVal.doubleValue();
                totals[15] += advVal.doubleValue();
                totals[16] += rec.getDeductions().doubleValue();
                totals[17] += totalDedVal.add(rec.getDeductions()).doubleValue();
                totals[18] += rec.getNetSalary().doubleValue();
                totals[19] += rec.getAmountPaid().doubleValue();
                totals[20] += balanceDue.doubleValue();
            }

            Row sumRow = sheet.createRow(r);
            sumRow.setHeightInPoints(22);
            setStrCell(sumRow, 0, "TOTAL", sumLabelStyle);
            setStrCell(sumRow, 1, records.size() + " staff", sumLabelStyle);
            setStrCell(sumRow, 2, "", sumLabelStyle);
            setNumCell(sumRow, 3, totals[0], sumNumStyle);
            for (int i = 4; i <= 8; i++) setStrCell(sumRow, i, "", sumLabelStyle);
            setNumCell(sumRow, 9,  totals[6], sumNumStyle);
            setNumCell(sumRow, 10, totals[7], sumNumStyle);
            setNumCell(sumRow, 11, totals[8], sumNumStyle);
            setNumCell(sumRow, 12, totals[9], sumNumStyle);
            setNumCell(sumRow, 13, totals[10], sumNumStyle);
            setNumCell(sumRow, 14, totals[11], sumNumStyle);
            setNumCell(sumRow, 15, totals[12], sumNumStyle);
            setNumCell(sumRow, 16, totals[13], sumNumStyle);
            setNumCell(sumRow, 17, totals[14], sumNumStyle);
            setNumCell(sumRow, 18, totals[15], sumNumStyle);
            setNumCell(sumRow, 19, totals[16], sumNumStyle);
            setNumCell(sumRow, 20, totals[17], sumNumStyle);
            setNumCell(sumRow, 21, totals[18], sumNumStyle);
            setNumCell(sumRow, 22, totals[19], sumNumStyle);
            setNumCell(sumRow, 23, totals[20], sumNumStyle);
            setStrCell(sumRow, 24, "", sumLabelStyle);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Payroll Excel generation failed for {}/{}", year, month, e);
            throw new ServiceException("Failed to generate payroll Excel: " + e.getMessage(), "EXCEL_GENERATION_FAILED");
        }
    }

    // ─── PDF receipt ──────────────────────────────────────────────────────────

    private byte[] buildReceiptPdf(Deal deal, Payment payment) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    18, BLUE_DARK);
            Font subFont     = FontFactory.getFont(FontFactory.HELVETICA,          12, new Color(100, 100, 100));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD,     11, Color.WHITE);
            Font labelFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,     10, TEXT_LABEL);
            Font valueFont   = FontFactory.getFont(FontFactory.HELVETICA,          10, TEXT_DARK);
            Font greenFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,     11, GREEN_DARK);
            Font footerFont  = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,   8, TEXT_FOOTER);

            // ── Header ──
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(15, 25, 35));
            Font brandSubFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(20, 184, 166));
            Font reraFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(130, 130, 130));

            Paragraph brand = new Paragraph("DevAshok Enclave", brandFont);
            brand.setAlignment(Element.ALIGN_CENTER);
            doc.add(brand);

            Paragraph tagline = new Paragraph("PREMIUM REAL ESTATE", brandSubFont);
            tagline.setAlignment(Element.ALIGN_CENTER);
            tagline.setSpacingAfter(2);
            doc.add(tagline);

            Paragraph rera = new Paragraph("RERA Reg. No: MH/NAVI/2021/00342", reraFont);
            rera.setAlignment(Element.ALIGN_CENTER);
            rera.setSpacingAfter(4);
            doc.add(rera);

            doc.add(new Chunk(new LineSeparator(1.5f, 100, new Color(20, 184, 166), Element.ALIGN_CENTER, -2)));

            Paragraph receiptLabel = new Paragraph("\nPAYMENT RECEIPT", subFont);
            receiptLabel.setAlignment(Element.ALIGN_CENTER);
            receiptLabel.setSpacingAfter(6);
            doc.add(receiptLabel);

            doc.add(new Chunk(new LineSeparator(0.5f, 40, new Color(200, 200, 200), Element.ALIGN_CENTER, -2)));

            // ── Receipt meta (# and date side by side) ──
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingBefore(10);
            metaTable.setSpacingAfter(6);
            addMetaCell(metaTable, "Receipt #" + payment.getId(), labelFont, Element.ALIGN_LEFT);
            addMetaCell(metaTable, payment.getPaymentDate().format(DATETIME_FMT), valueFont, Element.ALIGN_RIGHT);
            doc.add(metaTable);

            // ── Customer & Deal section ──
            addSectionBanner(doc, "Customer & Deal Information", BLUE_DARK, sectionFont);
            PdfPTable infoTable = new PdfPTable(new float[]{1.5f, 2.5f, 1.5f, 2.5f});
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(8);
            addRow4(infoTable, "Customer Name", deal.getCustomer().getFullName(),
                               "Phone", deal.getCustomer().getPhoneNumber(), labelFont, valueFont);
            addRow4(infoTable, "Property Type", deal.getPropertyType().getName(),
                               "Deal Date", deal.getDealDate().format(DATE_FMT), labelFont, valueFont);
            if (deal.getCustomer().getEmail() != null && !deal.getCustomer().getEmail().isBlank()) {
                addRow4(infoTable, "Email", deal.getCustomer().getEmail(),
                                   "Deal Status", deal.getStatus().name(), labelFont, valueFont);
            } else {
                addRow4(infoTable, "Deal Status", deal.getStatus().name(),
                                   "", "", labelFont, valueFont);
            }
            if (deal.getPropertyDescription() != null && !deal.getPropertyDescription().isBlank()) {
                addWideRow(infoTable, "Property Description", deal.getPropertyDescription(), labelFont, valueFont);
            }
            doc.add(infoTable);

            // ── EMI Details (only if linked to a schedule) ──
            if (payment.getEmiSchedule() != null) {
                EmiSchedule emi = payment.getEmiSchedule();
                BigDecimal bounce = emi.getBounceCharges() != null ? emi.getBounceCharges() : BigDecimal.ZERO;
                BigDecimal totalDue = emi.getDueAmount().add(bounce);
                BigDecimal remainingAfter = totalDue.subtract(emi.getPaidAmount()).max(BigDecimal.ZERO);

                addSectionBanner(doc, "EMI Details", BLUE_DARK, sectionFont);
                PdfPTable emiTable = new PdfPTable(new float[]{1.5f, 2.5f, 1.5f, 2.5f});
                emiTable.setWidthPercentage(100);
                emiTable.setSpacingAfter(8);
                addRow4(emiTable, "EMI Due Date", emi.getDueDate().format(DATE_FMT),
                                  "Base EMI Amount", fmt(emi.getDueAmount()), labelFont, valueFont);
                if (bounce.compareTo(BigDecimal.ZERO) > 0) {
                    addRow4(emiTable, "Bounce / Penalty Charges", fmt(bounce),
                                      "Total Due (incl. charges)", fmt(totalDue), labelFont, valueFont);
                }
                addRow4(emiTable, "EMI Status", emi.getStatus().name(),
                                  "Remaining (after this payment)", fmt(remainingAfter), labelFont, valueFont);
                doc.add(emiTable);
            }

            // ── Payment Details ──
            addSectionBanner(doc, "Payment Details", GREEN_DARK, sectionFont);
            PdfPTable payTable = new PdfPTable(new float[]{1.5f, 2.5f, 1.5f, 2.5f});
            payTable.setWidthPercentage(100);
            payTable.setSpacingAfter(8);
            addRow4Highlighted(payTable, "Amount Paid", fmt(payment.getAmount()),
                                         "Payment Method", payment.getPaymentMethod().name().replace('_', ' '),
                                         labelFont, greenFont, valueFont);
            if (payment.getUtrNumber() != null && !payment.getUtrNumber().isBlank()) {
                addRow4(payTable, "UTR / Reference #", payment.getUtrNumber(),
                                  "Recorded By", payment.getCreatedByAdmin(), labelFont, valueFont);
            } else {
                addRow4(payTable, "Recorded By", payment.getCreatedByAdmin(), "", "", labelFont, valueFont);
            }
            if (payment.getNotes() != null && !payment.getNotes().isBlank()) {
                addWideRow(payTable, "Notes", payment.getNotes(), labelFont, valueFont);
            }
            doc.add(payTable);

            // ── Footer ──
            doc.add(new Chunk(new LineSeparator(0.5f, 100, new Color(200, 200, 200), Element.ALIGN_CENTER, -2)));

            Paragraph footerBrand = new Paragraph(
                    "\nDevAshok Enclave  |  RERA: MH/NAVI/2021/00342", footerFont);
            footerBrand.setAlignment(Element.ALIGN_CENTER);
            doc.add(footerBrand);

            Paragraph footerNote = new Paragraph(
                    "This is a system-generated receipt and does not require a signature.", footerFont);
            footerNote.setAlignment(Element.ALIGN_CENTER);
            footerNote.setSpacingBefore(2);
            doc.add(footerNote);

            Paragraph footerContact = new Paragraph(
                    "Plot No. 42, Sector 18, Navi Mumbai  |  +91 98765 43210  |  admin@devashokenclave.in", footerFont);
            footerContact.setAlignment(Element.ALIGN_CENTER);
            footerContact.setSpacingBefore(2);
            doc.add(footerContact);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF receipt generation failed for paymentId={}", payment.getId(), e);
            throw new ServiceException("Failed to generate receipt: " + e.getMessage(), "PDF_GENERATION_FAILED");
        }
    }

    // ─── PDF salary slip (with salary structure breakdown) ──────────────────────

    private byte[] buildSalarySlipPdf(SalaryRecord record) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    18, BLUE_DARK);
            Font subFont     = FontFactory.getFont(FontFactory.HELVETICA,          12, new Color(100, 100, 100));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD,     11, Color.WHITE);
            Font labelFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,     10, TEXT_LABEL);
            Font valueFont   = FontFactory.getFont(FontFactory.HELVETICA,          10, TEXT_DARK);
            Font greenFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,     11, GREEN_DARK);
            Font redFont     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,     10, new Color(200, 30, 30));
            Font footerFont  = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,   8, TEXT_FOOTER);

            String orgName = getOrgName();
            String monthName = Month.of(record.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            Staff staff = record.getStaff();

            // ── Header ──
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(15, 25, 35));
            Font brandSubFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(20, 184, 166));

            Paragraph brand = new Paragraph(orgName, brandFont);
            brand.setAlignment(Element.ALIGN_CENTER);
            doc.add(brand);

            Paragraph tagline = new Paragraph("SALARY SLIP", brandSubFont);
            tagline.setAlignment(Element.ALIGN_CENTER);
            tagline.setSpacingAfter(2);
            doc.add(tagline);

            Paragraph period = new Paragraph(monthName + " " + record.getYear(), subFont);
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(4);
            doc.add(period);

            doc.add(new Chunk(new LineSeparator(1.5f, 100, new Color(20, 184, 166), Element.ALIGN_CENTER, -2)));

            // ── Employee Details ──
            addSectionBanner(doc, "Employee Details", BLUE_DARK, sectionFont);
            PdfPTable empTable = new PdfPTable(new float[]{1.5f, 2.5f, 1.5f, 2.5f});
            empTable.setWidthPercentage(100);
            empTable.setSpacingAfter(8);
            String empCode = staff.getEmployeeCode() != null ? staff.getEmployeeCode() : "EMP-" + String.format("%03d", staff.getId());
            addRow4(empTable, "Employee Name", staff.getFullName(),
                              "Employee Code", empCode, labelFont, valueFont);
            addRow4(empTable, "Department", staff.getStaffRole() != null ? staff.getStaffRole().getName() : "-",
                              "Base Salary", fmt(staff.getMonthlySalary()), labelFont, valueFont);
            addRow4(empTable, "Working Days", String.valueOf(record.getWorkingDays()),
                              "Holidays", String.valueOf(record.getHolidayCount() != null ? record.getHolidayCount() : 0), labelFont, valueFont);
            addRow4(empTable, "Present Days", String.valueOf(record.getPresentDays()),
                              "Half Days", String.valueOf(record.getHalfDays()), labelFont, valueFont);
            addRow4(empTable, "Absent Days", String.valueOf(record.getAbsentDays()),
                              "OT Hours", record.getOvertimeHours() != null ? record.getOvertimeHours().toPlainString() : "0", labelFont, valueFont);
            doc.add(empTable);

            // ── Earnings (salary structure breakdown) ──
            BigDecimal basicPay = record.getBasicPay() != null ? record.getBasicPay() : BigDecimal.ZERO;
            BigDecimal hraVal = record.getHra() != null ? record.getHra() : BigDecimal.ZERO;
            BigDecimal convVal = record.getConveyanceAllowance() != null ? record.getConveyanceAllowance() : BigDecimal.ZERO;
            BigDecimal specVal = record.getSpecialAllowance() != null ? record.getSpecialAllowance() : BigDecimal.ZERO;
            BigDecimal grossVal = record.getGrossEarnings() != null ? record.getGrossEarnings() : BigDecimal.ZERO;

            addSectionBanner(doc, "Earnings", GREEN_DARK, sectionFont);
            PdfPTable earnTable = new PdfPTable(new float[]{3f, 2f});
            earnTable.setWidthPercentage(100);
            earnTable.setSpacingAfter(8);
            addRow2(earnTable, "Basic Pay", fmt(basicPay), labelFont, valueFont);
            addRow2(earnTable, "House Rent Allowance (HRA)", fmt(hraVal), labelFont, valueFont);
            addRow2(earnTable, "Conveyance Allowance", fmt(convVal), labelFont, valueFont);
            addRow2(earnTable, "Special Allowance", fmt(specVal), labelFont, valueFont);
            if (record.getOvertimePay().compareTo(BigDecimal.ZERO) > 0) {
                addRow2(earnTable, "Overtime Pay", fmt(record.getOvertimePay()), labelFont, valueFont);
            }
            if (record.getBonus().compareTo(BigDecimal.ZERO) > 0) {
                addRow2(earnTable, "Bonus", fmt(record.getBonus()), labelFont, valueFont);
            }
            addRow2(earnTable, "GROSS EARNINGS", fmt(grossVal), labelFont, greenFont);
            doc.add(earnTable);

            // ── Deductions (statutory + advance) ──
            BigDecimal pfVal = record.getPfDeduction() != null ? record.getPfDeduction() : BigDecimal.ZERO;
            BigDecimal esiVal = record.getEsiDeduction() != null ? record.getEsiDeduction() : BigDecimal.ZERO;
            BigDecimal ptVal = record.getProfessionalTax() != null ? record.getProfessionalTax() : BigDecimal.ZERO;
            BigDecimal advVal = record.getAdvanceDeduction() != null ? record.getAdvanceDeduction() : BigDecimal.ZERO;
            BigDecimal totalDedVal = record.getTotalDeductions() != null ? record.getTotalDeductions() : BigDecimal.ZERO;
            BigDecimal allDeductions = totalDedVal.add(record.getDeductions());

            addSectionBanner(doc, "Deductions", new Color(180, 40, 40), sectionFont);
            PdfPTable dedTable = new PdfPTable(new float[]{3f, 2f});
            dedTable.setWidthPercentage(100);
            dedTable.setSpacingAfter(8);
            if (pfVal.compareTo(BigDecimal.ZERO) > 0) addRow2(dedTable, "Provident Fund (PF)", fmt(pfVal), labelFont, valueFont);
            if (esiVal.compareTo(BigDecimal.ZERO) > 0) addRow2(dedTable, "Employee State Insurance (ESI)", fmt(esiVal), labelFont, valueFont);
            if (ptVal.compareTo(BigDecimal.ZERO) > 0) addRow2(dedTable, "Professional Tax", fmt(ptVal), labelFont, valueFont);
            if (advVal.compareTo(BigDecimal.ZERO) > 0) addRow2(dedTable, "Advance Deduction", fmt(advVal), labelFont, valueFont);
            if (record.getDeductions().compareTo(BigDecimal.ZERO) > 0) addRow2(dedTable, "Other Deductions", fmt(record.getDeductions()), labelFont, valueFont);
            addRow2(dedTable, "TOTAL DEDUCTIONS", fmt(allDeductions), labelFont, redFont);
            doc.add(dedTable);

            // ── Net Salary Summary ──
            addSectionBanner(doc, "Net Salary", BLUE_DARK, sectionFont);
            PdfPTable netTable = new PdfPTable(new float[]{3f, 2f});
            netTable.setWidthPercentage(100);
            netTable.setSpacingAfter(8);
            addRow2(netTable, "Gross Earnings", fmt(grossVal.add(record.getBonus())), labelFont, valueFont);
            addRow2(netTable, "Total Deductions", fmt(allDeductions), labelFont, redFont);
            addRow2(netTable, "NET SALARY", fmt(record.getNetSalary()), labelFont, greenFont);
            addRow2(netTable, "Amount Paid", fmt(record.getAmountPaid()), labelFont, valueFont);
            BigDecimal balance = record.getNetSalary().subtract(record.getAmountPaid()).max(BigDecimal.ZERO);
            addRow2(netTable, "Balance Due", fmt(balance), labelFont, balance.compareTo(BigDecimal.ZERO) > 0 ? redFont : greenFont);
            addRow2(netTable, "Status", record.getStatus().name(), labelFont, valueFont);
            if (record.getPaymentMethod() != null) {
                addRow2(netTable, "Payment Method", record.getPaymentMethod().replace('_', ' '), labelFont, valueFont);
            }
            if (record.getPaymentReference() != null) {
                addRow2(netTable, "Payment Reference", record.getPaymentReference(), labelFont, valueFont);
            }
            doc.add(netTable);

            // ── Footer ──
            doc.add(new Chunk(new LineSeparator(0.5f, 100, new Color(200, 200, 200), Element.ALIGN_CENTER, -2)));
            Paragraph footerNote = new Paragraph(
                    "\nThis is a system-generated salary slip and does not require a signature.", footerFont);
            footerNote.setAlignment(Element.ALIGN_CENTER);
            doc.add(footerNote);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Salary slip PDF generation failed for salaryId={}", record.getId(), e);
            throw new ServiceException("Failed to generate salary slip: " + e.getMessage(), "PDF_GENERATION_FAILED");
        }
    }

    // ─── Stock Transaction PDF receipt ──────────────────────────────────────────

    private byte[] buildStockTransactionReceiptPdf(StockTransaction txn, Organization org) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD,     11, Color.WHITE);
            Font labelFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,     10, TEXT_LABEL);
            Font valueFont   = FontFactory.getFont(FontFactory.HELVETICA,          10, TEXT_DARK);
            Font greenFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,     11, GREEN_DARK);
            Font footerFont  = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,   8, TEXT_FOOTER);
            Font subFont     = FontFactory.getFont(FontFactory.HELVETICA,          12, new Color(100, 100, 100));

            // ── Header (dynamic from Organization) ──
            Font brandFont    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(15, 25, 35));
            Font brandSubFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(20, 184, 166));
            Font reraFont     = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(130, 130, 130));

            String orgName = org != null && org.getName() != null ? org.getName() : "Organization";
            Paragraph brand = new Paragraph(orgName, brandFont);
            brand.setAlignment(Element.ALIGN_CENTER);
            doc.add(brand);

            Paragraph tagline = new Paragraph("INVENTORY MANAGEMENT", brandSubFont);
            tagline.setAlignment(Element.ALIGN_CENTER);
            tagline.setSpacingAfter(2);
            doc.add(tagline);

            if (org != null && org.getReraNumber() != null && !org.getReraNumber().isBlank()) {
                Paragraph rera = new Paragraph("RERA Reg. No: " + org.getReraNumber(), reraFont);
                rera.setAlignment(Element.ALIGN_CENTER);
                rera.setSpacingAfter(4);
                doc.add(rera);
            }

            doc.add(new Chunk(new LineSeparator(1.5f, 100, new Color(20, 184, 166), Element.ALIGN_CENTER, -2)));

            Paragraph receiptLabel = new Paragraph("\nSTOCK TRANSACTION RECEIPT", subFont);
            receiptLabel.setAlignment(Element.ALIGN_CENTER);
            receiptLabel.setSpacingAfter(6);
            doc.add(receiptLabel);

            doc.add(new Chunk(new LineSeparator(0.5f, 40, new Color(200, 200, 200), Element.ALIGN_CENTER, -2)));

            // ── Receipt meta (# and date side by side) ──
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingBefore(10);
            metaTable.setSpacingAfter(6);
            String refNum = txn.getReferenceNumber() != null && !txn.getReferenceNumber().isBlank()
                    ? txn.getReferenceNumber() : "TXN-" + txn.getId();
            addMetaCell(metaTable, "Ref: " + refNum, labelFont, Element.ALIGN_LEFT);
            addMetaCell(metaTable, txn.getTransactionDate().format(DATETIME_FMT), valueFont, Element.ALIGN_RIGHT);
            doc.add(metaTable);

            // ── Transaction Details section ──
            addSectionBanner(doc, "Transaction Details", BLUE_DARK, sectionFont);
            PdfPTable detailTable = new PdfPTable(new float[]{1.5f, 2.5f, 1.5f, 2.5f});
            detailTable.setWidthPercentage(100);
            detailTable.setSpacingAfter(8);
            addRow4(detailTable, "Reference Number", refNum,
                                 "Transaction Date", txn.getTransactionDate().format(DATETIME_FMT), labelFont, valueFont);
            addRow4(detailTable, "Transaction Type", txn.getType().name(),
                                 "Transacted By", txn.getTransactedBy() != null ? txn.getTransactedBy() : "-", labelFont, valueFont);
            doc.add(detailTable);

            // ── Material Details section ──
            addSectionBanner(doc, "Material Details", GREEN_DARK, sectionFont);
            PdfPTable matTable = new PdfPTable(new float[]{1.5f, 2.5f, 1.5f, 2.5f});
            matTable.setWidthPercentage(100);
            matTable.setSpacingAfter(8);
            addRow4(matTable, "Material Name", txn.getMaterial().getName(),
                              "Category", txn.getMaterial().getCategory().name(), labelFont, valueFont);
            addRow4(matTable, "Unit", txn.getMaterial().getUnit().name(),
                              "Quantity", txn.getQuantity().toPlainString(), labelFont, valueFont);
            BigDecimal unitCost = txn.getUnitCostAtTime() != null ? txn.getUnitCostAtTime() : BigDecimal.ZERO;
            BigDecimal totalCost = txn.getTotalCost() != null ? txn.getTotalCost() : BigDecimal.ZERO;
            addRow4Highlighted(matTable, "Unit Cost", fmt(unitCost),
                                         "Total Cost", fmt(totalCost),
                                         labelFont, greenFont, valueFont);
            doc.add(matTable);

            // ── Supplier Info section (if linked) ──
            Supplier supplier = txn.getSupplier();
            if (supplier != null) {
                addSectionBanner(doc, "Supplier Information", BLUE_DARK, sectionFont);
                PdfPTable supTable = new PdfPTable(new float[]{1.5f, 2.5f, 1.5f, 2.5f});
                supTable.setWidthPercentage(100);
                supTable.setSpacingAfter(8);
                addRow4(supTable, "Supplier Name", supplier.getName(),
                                  "Contact Person", supplier.getContactPerson() != null ? supplier.getContactPerson() : "-", labelFont, valueFont);
                addRow4(supTable, "Phone", supplier.getPhone() != null ? supplier.getPhone() : "-",
                                  "GST Number", supplier.getGstNumber() != null ? supplier.getGstNumber() : "-", labelFont, valueFont);
                doc.add(supTable);
            }

            // ── Remarks section (if present) ──
            if (txn.getRemarks() != null && !txn.getRemarks().isBlank()) {
                addSectionBanner(doc, "Remarks", BLUE_DARK, sectionFont);
                PdfPTable remarkTable = new PdfPTable(1);
                remarkTable.setWidthPercentage(100);
                remarkTable.setSpacingAfter(8);
                PdfPCell rc = new PdfPCell(new Phrase(txn.getRemarks(), valueFont));
                rc.setBorder(Rectangle.NO_BORDER);
                rc.setBackgroundColor(GREY_LIGHT);
                rc.setPadding(8);
                remarkTable.addCell(rc);
                doc.add(remarkTable);
            }

            // ── Footer ──
            doc.add(new Chunk(new LineSeparator(0.5f, 100, new Color(200, 200, 200), Element.ALIGN_CENTER, -2)));

            StringBuilder footerBrandText = new StringBuilder("\n" + orgName);
            if (org != null && org.getReraNumber() != null && !org.getReraNumber().isBlank()) {
                footerBrandText.append("  |  RERA: ").append(org.getReraNumber());
            }
            Paragraph footerBrand = new Paragraph(footerBrandText.toString(), footerFont);
            footerBrand.setAlignment(Element.ALIGN_CENTER);
            doc.add(footerBrand);

            Paragraph footerNote = new Paragraph(
                    "This is a system-generated receipt and does not require a signature.", footerFont);
            footerNote.setAlignment(Element.ALIGN_CENTER);
            footerNote.setSpacingBefore(2);
            doc.add(footerNote);

            StringBuilder contactParts = new StringBuilder();
            if (org != null) {
                if (org.getAddress() != null && !org.getAddress().isBlank()) contactParts.append(org.getAddress());
                if (org.getPhone() != null && !org.getPhone().isBlank()) {
                    if (!contactParts.isEmpty()) contactParts.append("  |  ");
                    contactParts.append(org.getPhone());
                }
                if (org.getEmail() != null && !org.getEmail().isBlank()) {
                    if (!contactParts.isEmpty()) contactParts.append("  |  ");
                    contactParts.append(org.getEmail());
                }
            }
            if (!contactParts.isEmpty()) {
                Paragraph footerContact = new Paragraph(contactParts.toString(), footerFont);
                footerContact.setAlignment(Element.ALIGN_CENTER);
                footerContact.setSpacingBefore(2);
                doc.add(footerContact);
            }

            Paragraph generated = new Paragraph(
                    "Generated on: " + LocalDateTime.now().format(DATETIME_FMT), footerFont);
            generated.setAlignment(Element.ALIGN_CENTER);
            generated.setSpacingBefore(4);
            doc.add(generated);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF receipt generation failed for stock transactionId={}", txn.getId(), e);
            throw new ServiceException("Failed to generate receipt: " + e.getMessage(), "PDF_GENERATION_FAILED");
        }
    }

    private void addSectionBanner(Document doc, String text, Color bgColor, Font font) throws DocumentException {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingBefore(8);
        banner.setSpacingAfter(2);
        PdfPCell cell = new PdfPCell(new Phrase(" " + text, font));
        cell.setBackgroundColor(bgColor);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(6);
        banner.addCell(cell);
        doc.add(banner);
    }

    private void addMetaCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private void addRow4(PdfPTable table, String l1, String v1, String l2, String v2,
                         Font labelFont, Font valueFont) {
        for (String[] pair : new String[][]{{l1, v1}, {l2, v2}}) {
            PdfPCell lc = new PdfPCell(new Phrase(pair[0], labelFont));
            lc.setBorder(Rectangle.NO_BORDER);
            lc.setBackgroundColor(GREY_LIGHT);
            lc.setPaddingBottom(5);
            lc.setPaddingLeft(4);
            table.addCell(lc);
            PdfPCell vc = new PdfPCell(new Phrase(pair[1], valueFont));
            vc.setBorder(Rectangle.NO_BORDER);
            vc.setBackgroundColor(GREY_LIGHT);
            vc.setPaddingBottom(5);
            table.addCell(vc);
        }
    }

    /** First value rendered in highlightFont (e.g. green bold for amount). */
    private void addRow4Highlighted(PdfPTable table, String l1, String v1, String l2, String v2,
                                    Font labelFont, Font highlightFont, Font valueFont) {
        PdfPCell lc1 = new PdfPCell(new Phrase(l1, labelFont));
        lc1.setBorder(Rectangle.NO_BORDER); lc1.setBackgroundColor(GREY_LIGHT); lc1.setPaddingBottom(5); lc1.setPaddingLeft(4);
        table.addCell(lc1);
        PdfPCell vc1 = new PdfPCell(new Phrase(v1, highlightFont));
        vc1.setBorder(Rectangle.NO_BORDER); vc1.setBackgroundColor(GREY_LIGHT); vc1.setPaddingBottom(5);
        table.addCell(vc1);
        PdfPCell lc2 = new PdfPCell(new Phrase(l2, labelFont));
        lc2.setBorder(Rectangle.NO_BORDER); lc2.setBackgroundColor(GREY_LIGHT); lc2.setPaddingBottom(5); lc2.setPaddingLeft(4);
        table.addCell(lc2);
        PdfPCell vc2 = new PdfPCell(new Phrase(v2, valueFont));
        vc2.setBorder(Rectangle.NO_BORDER); vc2.setBackgroundColor(GREY_LIGHT); vc2.setPaddingBottom(5);
        table.addCell(vc2);
    }

    private void addWideRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
        lc.setBorder(Rectangle.NO_BORDER); lc.setBackgroundColor(GREY_LIGHT); lc.setPaddingBottom(5); lc.setPaddingLeft(4);
        table.addCell(lc);
        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setBorder(Rectangle.NO_BORDER); vc.setBackgroundColor(GREY_LIGHT); vc.setPaddingBottom(5); vc.setColspan(3);
        table.addCell(vc);
    }

    // ─── Excel schedule ───────────────────────────────────────────────────────

    private byte[] buildScheduleExcel(Deal deal, List<EmiSchedule> schedules) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("EMI Schedule");

            int[] colWidths = {1400, 4200, 5200, 5800, 5200, 5200, 5200, 3500, 3000};
            for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i]);

            // Prebuilt styles
            XSSFCellStyle titleStyle      = makeTitleStyle(wb);
            XSSFCellStyle infoLabelStyle  = makeInfoLabelStyle(wb);
            XSSFCellStyle infoValueStyle  = makeInfoValueStyle(wb);
            XSSFCellStyle headerStyle     = makeHeaderStyle(wb);
            XSSFCellStyle sumLabelStyle   = makeSumLabelStyle(wb);
            XSSFCellStyle sumNumStyle     = makeSumNumStyle(wb);

            // Status-row styles (3 sets: PAID / PARTIAL / PENDING)
            byte[] rgbPaid    = {(byte) 198, (byte) 239, (byte) 206};
            byte[] rgbPartial = {(byte) 255, (byte) 235, (byte) 156};
            byte[] rgbPending = {(byte) 255, (byte) 199, (byte) 206};
            XSSFCellStyle numPaid    = makeNumStyle(wb, rgbPaid);
            XSSFCellStyle numPartial = makeNumStyle(wb, rgbPartial);
            XSSFCellStyle numPending = makeNumStyle(wb, rgbPending);
            XSSFCellStyle txtPaid    = makeTxtStyle(wb, rgbPaid);
            XSSFCellStyle txtPartial = makeTxtStyle(wb, rgbPartial);
            XSSFCellStyle txtPending = makeTxtStyle(wb, rgbPending);

            int r = 0;

            // ── Title ──
            Row titleRow = sheet.createRow(r++);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DevAshok Enclave  |  EMI SCHEDULE  |  "
                    + deal.getCustomer().getFullName() + "  /  " + deal.getPropertyType().getName());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            r++; // blank row

            // ── Deal info ──
            r = addInfoRow(sheet, r, "Customer Name",   deal.getCustomer().getFullName(),
                                     "Phone",           deal.getCustomer().getPhoneNumber(), infoLabelStyle, infoValueStyle);
            r = addInfoRow(sheet, r, "Property Type",   deal.getPropertyType().getName(),
                                     "Deal Date",       deal.getDealDate().format(DATE_FMT), infoLabelStyle, infoValueStyle);
            r = addInfoRow(sheet, r, "Total Amount",    fmt(deal.getTotalAmount()),
                                     "Initial Deposit", fmt(deal.getInitialDeposit()), infoLabelStyle, infoValueStyle);
            r = addInfoRow(sheet, r, "EMI / Month",     fmt(deal.getEmiAmountPerMonth()),
                                     "Tenure",          deal.getEmiTenureMonths() + " months", infoLabelStyle, infoValueStyle);
            r = addInfoRow(sheet, r, "Total Payable",   fmt(deal.getTotalPayableAfterDeposit()),
                                     "Status",          deal.getStatus().name(), infoLabelStyle, infoValueStyle);

            r++; // blank row

            // ── Table header ──
            Row hdr = sheet.createRow(r++);
            hdr.setHeightInPoints(20);
            String[] cols = {"#", "Due Date", "Base EMI (₹)", "Bounce Charges (₹)",
                             "Total Due (₹)", "Paid (₹)", "Remaining (₹)", "Status", "Bounced"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = hdr.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            // ── Data rows ──
            double totalBase = 0, totalBounce = 0, totalPaid = 0, totalRemaining = 0;
            int idx = 1;
            for (EmiSchedule emi : schedules) {
                BigDecimal bounce   = emi.getBounceCharges() != null ? emi.getBounceCharges() : BigDecimal.ZERO;
                BigDecimal totalDue = emi.getDueAmount().add(bounce);
                BigDecimal remaining = totalDue.subtract(emi.getPaidAmount()).max(BigDecimal.ZERO);

                totalBase      += emi.getDueAmount().doubleValue();
                totalBounce    += bounce.doubleValue();
                totalPaid      += emi.getPaidAmount().doubleValue();
                totalRemaining += remaining.doubleValue();

                String status = emi.getStatus().name();
                XSSFCellStyle ns = switch (status) {
                    case "PAID"    -> numPaid;
                    case "PARTIAL" -> numPartial;
                    default        -> numPending;
                };
                XSSFCellStyle ts = switch (status) {
                    case "PAID"    -> txtPaid;
                    case "PARTIAL" -> txtPartial;
                    default        -> txtPending;
                };

                Row row = sheet.createRow(r++);
                row.setHeightInPoints(17);
                setNumCell(row, 0, idx++, ns);
                setStrCell(row, 1, emi.getDueDate().format(DATE_FMT), ts);
                setNumCell(row, 2, emi.getDueAmount().doubleValue(), ns);
                setNumCell(row, 3, bounce.doubleValue(), ns);
                setNumCell(row, 4, totalDue.doubleValue(), ns);
                setNumCell(row, 5, emi.getPaidAmount().doubleValue(), ns);
                setNumCell(row, 6, remaining.doubleValue(), ns);
                setStrCell(row, 7, status, ts);
                setStrCell(row, 8, emi.isBounced() ? "Yes" : "No", ts);
            }

            // ── Summary row ──
            Row sumRow = sheet.createRow(r);
            sumRow.setHeightInPoints(20);
            setStrCell(sumRow, 0, "TOTAL", sumLabelStyle);
            setStrCell(sumRow, 1, "", sumLabelStyle);
            setNumCell(sumRow, 2, totalBase, sumNumStyle);
            setNumCell(sumRow, 3, totalBounce, sumNumStyle);
            setNumCell(sumRow, 4, totalBase + totalBounce, sumNumStyle);
            setNumCell(sumRow, 5, totalPaid, sumNumStyle);
            setNumCell(sumRow, 6, totalRemaining, sumNumStyle);
            setStrCell(sumRow, 7, "", sumLabelStyle);
            setStrCell(sumRow, 8, "", sumLabelStyle);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Excel schedule generation failed for dealId={}", deal.getId(), e);
            throw new ServiceException("Failed to generate Excel: " + e.getMessage(), "EXCEL_GENERATION_FAILED");
        }
    }

    private int addInfoRow(XSSFSheet sheet, int r,
                           String l1, String v1, String l2, String v2,
                           XSSFCellStyle labelStyle, XSSFCellStyle valueStyle) {
        Row row = sheet.createRow(r);
        row.setHeightInPoints(16);
        setStrCell(row, 0, l1, labelStyle);
        setStrCell(row, 1, v1, valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 1, 2));
        setStrCell(row, 3, l2, labelStyle);
        setStrCell(row, 4, v2, valueStyle);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 4, 5));
        return r + 1;
    }

    // ─── Attendance Excel ──────────────────────────────────────────────────────

    private byte[] buildAttendanceExcel(Organization org, List<Staff> staffList, int year, int month) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            YearMonth ym = YearMonth.of(year, month);
            int daysInMonth = ym.lengthOfMonth();
            String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            // Number of columns: Employee Name + days + Present + Absent + Half + Leave + OT Hrs + %
            int totalCols = 1 + daysInMonth + 6;

            XSSFSheet sheet = wb.createSheet("Attendance");
            sheet.setColumnWidth(0, 6000); // Employee name column
            for (int i = 1; i <= daysInMonth; i++) sheet.setColumnWidth(i, 1200);
            for (int i = daysInMonth + 1; i < totalCols; i++) sheet.setColumnWidth(i, 2800);

            // Styles
            XSSFCellStyle titleStyle = makeTitleStyle(wb);
            XSSFCellStyle headerStyle = makeHeaderStyle(wb);
            XSSFCellStyle sumLabelStyle = makeSumLabelStyle(wb);
            XSSFCellStyle sumNumStyle = makeSumNumStyle(wb);

            // Day status styles
            XSSFCellStyle presentStyle = makeAttCellStyle(wb, new byte[]{(byte)198, (byte)239, (byte)206}); // green
            XSSFCellStyle absentStyle = makeAttCellStyle(wb, new byte[]{(byte)255, (byte)199, (byte)206});  // red
            XSSFCellStyle halfDayStyle = makeAttCellStyle(wb, new byte[]{(byte)255, (byte)235, (byte)156}); // yellow
            XSSFCellStyle leaveStyle = makeAttCellStyle(wb, new byte[]{(byte)189, (byte)215, (byte)238});   // blue
            XSSFCellStyle holidayStyle = makeAttCellStyle(wb, new byte[]{(byte)225, (byte)210, (byte)240}); // purple
            XSSFCellStyle unmarkedStyle = makeAttCellStyle(wb, new byte[]{(byte)230, (byte)230, (byte)230}); // grey
            XSSFCellStyle nameStyle = makeInfoValueStyle(wb);
            XSSFCellStyle numDataStyle = makeAttNumStyle(wb);

            int r = 0;

            // Title row
            Row titleRow = sheet.createRow(r++);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            String orgName = org != null && org.getName() != null ? org.getName() : "Organization";
            titleCell.setCellValue(orgName + "  |  MONTHLY ATTENDANCE REPORT  |  " + monthName + " " + year);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalCols - 1));

            r++; // blank row

            // Header row
            Row hdr = sheet.createRow(r++);
            hdr.setHeightInPoints(20);
            setStrCell(hdr, 0, "Employee", headerStyle);
            for (int d = 1; d <= daysInMonth; d++) {
                setStrCell(hdr, d, String.valueOf(d), headerStyle);
            }
            int col = daysInMonth + 1;
            setStrCell(hdr, col++, "Present", headerStyle);
            setStrCell(hdr, col++, "Absent", headerStyle);
            setStrCell(hdr, col++, "Half", headerStyle);
            setStrCell(hdr, col++, "Leave", headerStyle);
            setStrCell(hdr, col++, "OT Hrs", headerStyle);
            setStrCell(hdr, col, "Att %", headerStyle);

            // Fetch all attendance for the month for all staff
            LocalDate from = ym.atDay(1);
            LocalDate to = ym.atEndOfMonth();

            int totalPresent = 0, totalAbsent = 0, totalHalf = 0, totalLeave = 0;
            double totalOT = 0;

            for (Staff staff : staffList) {
                List<Attendance> records = attendanceRepository.findByStaffIdAndDateBetweenOrderByDateAsc(staff.getId(), from, to);
                Map<LocalDate, Attendance> dateMap = records.stream()
                        .collect(Collectors.toMap(Attendance::getDate, a -> a));

                Row row = sheet.createRow(r++);
                row.setHeightInPoints(17);
                setStrCell(row, 0, staff.getFullName(), nameStyle);

                int sPresent = 0, sAbsent = 0, sHalf = 0, sLeave = 0;
                double sOT = 0;

                for (int d = 1; d <= daysInMonth; d++) {
                    LocalDate date = ym.atDay(d);
                    Attendance att = dateMap.get(date);

                    if (att != null) {
                        switch (att.getStatus()) {
                            case PRESENT -> {
                                setStrCell(row, d, "P", presentStyle);
                                sPresent++;
                            }
                            case ABSENT -> {
                                setStrCell(row, d, "A", absentStyle);
                                sAbsent++;
                            }
                            case HALF_DAY -> {
                                setStrCell(row, d, "H", halfDayStyle);
                                sHalf++;
                            }
                            case LEAVE -> {
                                setStrCell(row, d, "L", leaveStyle);
                                sLeave++;
                            }
                            case HOLIDAY -> {
                                setStrCell(row, d, "HD", holidayStyle);
                            }
                        }
                        if (att.getOvertimeHours() != null) sOT += att.getOvertimeHours();
                    } else if (date.getDayOfWeek().getValue() == 7) {
                        setStrCell(row, d, "S", holidayStyle); // Sunday
                    } else {
                        setStrCell(row, d, "-", unmarkedStyle);
                    }
                }

                col = daysInMonth + 1;
                setNumCell(row, col++, sPresent, numDataStyle);
                setNumCell(row, col++, sAbsent, numDataStyle);
                setNumCell(row, col++, sHalf, numDataStyle);
                setNumCell(row, col++, sLeave, numDataStyle);
                setNumCell(row, col++, sOT, numDataStyle);

                // Calculate working days for percentage
                int workingDays = 0;
                for (int d = 1; d <= daysInMonth; d++) {
                    LocalDate date = ym.atDay(d);
                    if (date.getDayOfWeek().getValue() != 7) workingDays++;
                }
                double percent = workingDays > 0 ? ((sPresent + sHalf * 0.5) / workingDays) * 100.0 : 0;
                setNumCell(row, col, Math.round(percent * 100.0) / 100.0, numDataStyle);

                totalPresent += sPresent;
                totalAbsent += sAbsent;
                totalHalf += sHalf;
                totalLeave += sLeave;
                totalOT += sOT;
            }

            // Summary row
            Row sumRow = sheet.createRow(r);
            sumRow.setHeightInPoints(20);
            setStrCell(sumRow, 0, "TOTAL", sumLabelStyle);
            for (int d = 1; d <= daysInMonth; d++) {
                setStrCell(sumRow, d, "", sumLabelStyle);
            }
            col = daysInMonth + 1;
            setNumCell(sumRow, col++, totalPresent, sumNumStyle);
            setNumCell(sumRow, col++, totalAbsent, sumNumStyle);
            setNumCell(sumRow, col++, totalHalf, sumNumStyle);
            setNumCell(sumRow, col++, totalLeave, sumNumStyle);
            setNumCell(sumRow, col++, totalOT, sumNumStyle);
            setStrCell(sumRow, col, "", sumLabelStyle);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Attendance Excel generation failed for {}/{}", year, month, e);
            throw new ServiceException("Failed to generate attendance report: " + e.getMessage(), "EXCEL_GENERATION_FAILED");
        }
    }

    private XSSFCellStyle makeAttCellStyle(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 9);
        f.setFontName("Calibri");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setFillForegroundColor(new XSSFColor(rgb, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyThinBorder(s);
        return s;
    }

    private XSSFCellStyle makeAttNumStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setFontName("Calibri");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.##"));
        applyThinBorder(s);
        return s;
    }

    // ─── Excel style builders ────────────────────────────────────────────────

    private XSSFCellStyle makeTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 14); f.setFontName("Calibri");
        f.setColor(new XSSFColor(new byte[]{(byte)25, (byte)80, (byte)150}, null));
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)220, (byte)230, (byte)245}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private XSSFCellStyle makeInfoLabelStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 10); f.setFontName("Calibri");
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)213, (byte)223, (byte)240}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyThinBorder(s);
        return s;
    }

    private XSSFCellStyle makeInfoValueStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10); f.setFontName("Calibri");
        s.setFont(f);
        applyThinBorder(s);
        return s;
    }

    private XSSFCellStyle makeHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 11); f.setFontName("Calibri");
        f.setColor(new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null));
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)25, (byte)80, (byte)150}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyThinBorder(s);
        return s;
    }

    private XSSFCellStyle makeNumStyle(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10); f.setFontName("Calibri");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        s.setFillForegroundColor(new XSSFColor(rgb, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyThinBorder(s);
        return s;
    }

    private XSSFCellStyle makeTxtStyle(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10); f.setFontName("Calibri");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(new XSSFColor(rgb, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyThinBorder(s);
        return s;
    }

    private XSSFCellStyle makeSumLabelStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 11); f.setFontName("Calibri");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)189, (byte)215, (byte)238}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyThinBorder(s);
        return s;
    }

    private XSSFCellStyle makeSumNumStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 11); f.setFontName("Calibri");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)189, (byte)215, (byte)238}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyThinBorder(s);
        return s;
    }

    private void applyThinBorder(XSSFCellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    // ─── Cell helpers ─────────────────────────────────────────────────────────

    private void setNumCell(org.apache.poi.ss.usermodel.Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col, CellType.NUMERIC);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private void setStrCell(org.apache.poi.ss.usermodel.Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col, CellType.STRING);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    private String fmt(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        return "₹" + String.format("%,.2f", amount);
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
    }

    private void addRow2(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setBackgroundColor(GREY_LIGHT);
        lc.setPaddingBottom(5);
        lc.setPaddingLeft(4);
        table.addCell(lc);
        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setBackgroundColor(GREY_LIGHT);
        vc.setPaddingBottom(5);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setPaddingRight(4);
        table.addCell(vc);
    }

    private String getOrgName() {
        try {
            Long orgId = tenantContext.getCurrentOrganizationId();
            return organizationRepository.findById(orgId)
                    .map(Organization::getName)
                    .orElse("Organization");
        } catch (Exception e) {
            return "Organization";
        }
    }
}
