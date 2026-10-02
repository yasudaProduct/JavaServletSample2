package com.example.appmgmt.domain;

public class Employee extends AuditedEntity {
    private long employeeId;
    private String employeeNo;
    private String employeeName;
    private String passwordHash;
    private String companyDiv;
    private String deptCd;
    private String roleCd;
    private String mailAddress;
    private String validFlg;

    public long getEmployeeId() { return employeeId; }
    public void setEmployeeId(long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getCompanyDiv() { return companyDiv; }
    public void setCompanyDiv(String companyDiv) { this.companyDiv = companyDiv; }
    public String getDeptCd() { return deptCd; }
    public void setDeptCd(String deptCd) { this.deptCd = deptCd; }
    public String getRoleCd() { return roleCd; }
    public void setRoleCd(String roleCd) { this.roleCd = roleCd; }
    public String getMailAddress() { return mailAddress; }
    public void setMailAddress(String mailAddress) { this.mailAddress = mailAddress; }
    public String getValidFlg() { return validFlg; }
    public void setValidFlg(String validFlg) { this.validFlg = validFlg; }
    public boolean isValid() { return Codes.FLG_ON.equals(validFlg); }
    public String getRoleName() { return Codes.label("ROLE", roleCd); }
}
