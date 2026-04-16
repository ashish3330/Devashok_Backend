package com.realestate.emi.service;

import com.realestate.emi.dto.request.StaffRequest;
import com.realestate.emi.dto.response.StaffResponse;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Staff;
import com.realestate.emi.entity.StaffRole;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.SalaryAdvanceRepository;
import com.realestate.emi.repository.StaffRepository;
import com.realestate.emi.repository.StaffRoleRepository;
import com.realestate.emi.security.TenantContext;
import com.realestate.emi.specification.DateRangeSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    private final StaffRepository staffRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;
    private final SalaryAdvanceRepository salaryAdvanceRepository;

    @Transactional(readOnly = true)
    public List<StaffResponse> findAll() {
        return findAll(null, null);
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> findAll(LocalDate from, LocalDate to) {
        Long orgId = tenantContext.getCurrentOrganizationId();

        Specification<Staff> spec = Specification.where(DateRangeSpec.<Staff>orgEquals("organization", orgId))
                .and((root, query, cb) -> cb.isTrue(root.get("isActive")))
                .and(DateRangeSpec.dateRange("joiningDate", from, to));

        return staffRepository.findAll(spec).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StaffResponse findById(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (staff.getOrganization() != null && !staff.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Staff", id);
        }
        return toResponse(staff);
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> findByRole(Long roleId) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return staffRepository.findByStaffRoleIdAndIsActiveTrueAndOrganizationId(roleId, orgId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StaffResponse create(StaffRequest request) {
        StaffRole role = staffRoleRepository.findById(request.getStaffRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffRole", request.getStaffRoleId()));

        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));

        String employeeCode = generateEmployeeCode(org);

        Staff staff = Staff.builder()
                .employeeCode(employeeCode)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .staffRole(role)
                .monthlySalary(request.getMonthlySalary())
                .joiningDate(request.getJoiningDate())
                .bankAccountNumber(request.getBankAccountNumber())
                .ifscCode(request.getIfscCode())
                .aadharNumber(request.getAadharNumber())
                .panNumber(request.getPanNumber())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .employmentType(request.getEmploymentType())
                .bloodGroup(request.getBloodGroup())
                .isActive(true)
                .organization(org)
                .build();

        staff = staffRepository.save(staff);
        log.info("Created staff: {} [{}] with role {}", staff.getFullName(), employeeCode, role.getName());
        return toResponse(staff);
    }

    @Transactional
    public StaffResponse update(Long id, StaffRequest request) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (staff.getOrganization() != null && !staff.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Staff", id);
        }

        StaffRole role = staffRoleRepository.findById(request.getStaffRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffRole", request.getStaffRoleId()));

        staff.setFullName(request.getFullName());
        staff.setPhone(request.getPhone());
        staff.setEmail(request.getEmail());
        staff.setAddress(request.getAddress());
        staff.setStaffRole(role);
        staff.setMonthlySalary(request.getMonthlySalary());
        staff.setJoiningDate(request.getJoiningDate());
        staff.setBankAccountNumber(request.getBankAccountNumber());
        staff.setIfscCode(request.getIfscCode());
        staff.setAadharNumber(request.getAadharNumber());
        staff.setPanNumber(request.getPanNumber());
        staff.setDepartment(request.getDepartment());
        staff.setDesignation(request.getDesignation());
        staff.setEmergencyContactName(request.getEmergencyContactName());
        staff.setEmergencyContactPhone(request.getEmergencyContactPhone());
        staff.setEmploymentType(request.getEmploymentType());
        staff.setBloodGroup(request.getBloodGroup());

        staff = staffRepository.save(staff);
        log.info("Updated staff: {}", staff.getFullName());
        return toResponse(staff);
    }

    @Transactional
    public void deactivate(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (staff.getOrganization() != null && !staff.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Staff", id);
        }
        staff.setIsActive(false);
        staff.setExitDate(LocalDate.now());
        staffRepository.save(staff);
        log.info("Deactivated staff: {}", staff.getFullName());
    }

    @Transactional(readOnly = true)
    public byte[] exportStaffExcel() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        List<Staff> staffList = staffRepository.findByOrganizationIdOrderByFullNameAsc(orgId);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Staff Directory");

            String[] headers = {
                    "Employee Code", "Name", "Department", "Designation", "Role",
                    "Phone", "Email", "Monthly Salary", "Employment Type", "Joining Date",
                    "Bank A/C", "IFSC", "Aadhar", "PAN", "Blood Group",
                    "Emergency Contact", "Status"
            };

            int[] colWidths = {
                    4500, 6000, 4000, 4500, 4000,
                    4000, 6500, 4000, 4000, 3800,
                    5000, 3500, 4000, 3500, 3000,
                    6000, 3000
            };
            for (int i = 0; i < colWidths.length; i++) sheet.setColumnWidth(i, colWidths[i]);

            // Styles
            XSSFCellStyle titleStyle = makeTitleStyle(wb);
            XSSFCellStyle headerStyle = makeHeaderStyle(wb);
            XSSFCellStyle textStyle = makeTextStyle(wb);
            XSSFCellStyle numStyle = makeNumStyle(wb);

            int r = 0;

            // Title row
            Row titleRow = sheet.createRow(r++);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(org.getName() + "  |  STAFF DIRECTORY  |  Generated: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a")));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));

            r++; // blank

            // Header row
            Row hdr = sheet.createRow(r++);
            hdr.setHeightInPoints(22);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hdr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // Data rows
            for (Staff s : staffList) {
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(18);
                int col = 0;
                setStr(row, col++, s.getEmployeeCode(), textStyle);
                setStr(row, col++, s.getFullName(), textStyle);
                setStr(row, col++, s.getDepartment(), textStyle);
                setStr(row, col++, s.getDesignation(), textStyle);
                setStr(row, col++, s.getStaffRole() != null ? s.getStaffRole().getName() : "-", textStyle);
                setStr(row, col++, s.getPhone(), textStyle);
                setStr(row, col++, s.getEmail(), textStyle);
                setNum(row, col++, s.getMonthlySalary() != null ? s.getMonthlySalary().doubleValue() : 0, numStyle);
                setStr(row, col++, s.getEmploymentType() != null ? s.getEmploymentType().name() : "-", textStyle);
                setStr(row, col++, s.getJoiningDate() != null ? s.getJoiningDate().format(DATE_FMT) : "-", textStyle);
                setStr(row, col++, s.getBankAccountNumber(), textStyle);
                setStr(row, col++, s.getIfscCode(), textStyle);
                setStr(row, col++, s.getAadharNumber(), textStyle);
                setStr(row, col++, s.getPanNumber(), textStyle);
                setStr(row, col++, s.getBloodGroup(), textStyle);

                String emergencyContact = "";
                if (s.getEmergencyContactName() != null && !s.getEmergencyContactName().isBlank()) {
                    emergencyContact = s.getEmergencyContactName();
                    if (s.getEmergencyContactPhone() != null && !s.getEmergencyContactPhone().isBlank()) {
                        emergencyContact += " (" + s.getEmergencyContactPhone() + ")";
                    }
                }
                setStr(row, col++, emergencyContact, textStyle);
                setStr(row, col, s.getIsActive() ? "Active" : "Inactive", textStyle);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Staff Excel export failed for orgId={}", orgId, e);
            throw new ServiceException("Failed to generate staff Excel: " + e.getMessage(), "EXCEL_GENERATION_FAILED");
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private String generateEmployeeCode(Organization org) {
        String orgCode = org.getCode();
        String maxCode = staffRepository.findMaxEmployeeCodeByOrg(org.getId());

        int nextNum = 1;
        if (maxCode != null && maxCode.contains("-")) {
            try {
                String numPart = maxCode.substring(maxCode.lastIndexOf('-') + 1);
                nextNum = Integer.parseInt(numPart) + 1;
            } catch (NumberFormatException e) {
                log.warn("Could not parse employee code: {}", maxCode);
            }
        }

        return orgCode + "-" + String.format("%03d", nextNum);
    }

    private StaffResponse toResponse(Staff staff) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        BigDecimal outstandingAdvance = salaryAdvanceRepository.sumActiveBalanceByStaff(staff.getId(), orgId);

        // Mask sensitive fields
        String maskedAadhar = staff.getAadharNumber() != null && staff.getAadharNumber().length() == 12
                ? "XXXX-XXXX-" + staff.getAadharNumber().substring(8) : staff.getAadharNumber();
        String maskedPan = staff.getPanNumber() != null && staff.getPanNumber().length() == 10
                ? "XXXXXX" + staff.getPanNumber().substring(6) : staff.getPanNumber();
        String maskedBankAccount = staff.getBankAccountNumber() != null && staff.getBankAccountNumber().length() > 4
                ? "XXXX..." + staff.getBankAccountNumber().substring(staff.getBankAccountNumber().length() - 4)
                : staff.getBankAccountNumber();

        return StaffResponse.builder()
                .id(staff.getId())
                .employeeCode(staff.getEmployeeCode())
                .fullName(staff.getFullName())
                .phone(staff.getPhone())
                .email(staff.getEmail())
                .address(staff.getAddress())
                .staffRoleId(staff.getStaffRole().getId())
                .staffRoleName(staff.getStaffRole().getName())
                .monthlySalary(staff.getMonthlySalary())
                .joiningDate(staff.getJoiningDate())
                .exitDate(staff.getExitDate())
                .bankAccountNumber(maskedBankAccount)
                .ifscCode(staff.getIfscCode())
                .aadharNumber(maskedAadhar)
                .panNumber(maskedPan)
                .department(staff.getDepartment())
                .designation(staff.getDesignation())
                .emergencyContactName(staff.getEmergencyContactName())
                .emergencyContactPhone(staff.getEmergencyContactPhone())
                .employmentType(staff.getEmploymentType())
                .bloodGroup(staff.getBloodGroup())
                .outstandingAdvance(outstandingAdvance)
                .isActive(staff.getIsActive())
                .build();
    }

    // ─── Excel style helpers ────────────────────────────────────────────────

    private XSSFCellStyle makeTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        f.setFontName("Calibri");
        f.setColor(new XSSFColor(new byte[]{(byte) 25, (byte) 80, (byte) 150}, null));
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 220, (byte) 230, (byte) 245}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private XSSFCellStyle makeHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setFontName("Calibri");
        f.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 25, (byte) 80, (byte) 150}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyThinBorder(s);
        return s;
    }

    private XSSFCellStyle makeTextStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setFontName("Calibri");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.LEFT);
        applyThinBorder(s);
        return s;
    }

    private XSSFCellStyle makeNumStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setFontName("Calibri");
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        applyThinBorder(s);
        return s;
    }

    private void applyThinBorder(XSSFCellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    private void setStr(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col, CellType.STRING);
        c.setCellValue(value != null ? value : "-");
        c.setCellStyle(style);
    }

    private void setNum(Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col, CellType.NUMERIC);
        c.setCellValue(value);
        c.setCellStyle(style);
    }
}
