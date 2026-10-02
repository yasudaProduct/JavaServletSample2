package com.example.appmgmt.domain;

import java.io.Serializable;

/** セッションに保持するログイン社員の情報。 */
public class LoginUser implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long employeeId;
    private final String employeeNo;
    private final String employeeName;
    private final String roleCd;
    private final String companyDiv;
    private final String companyDivName;
    private final String deptCd;

    public LoginUser(Employee e, String companyDivName) {
        this.employeeId = e.getEmployeeId();
        this.employeeNo = e.getEmployeeNo();
        this.employeeName = e.getEmployeeName();
        this.roleCd = e.getRoleCd();
        this.companyDiv = e.getCompanyDiv();
        this.companyDivName = companyDivName;
        this.deptCd = e.getDeptCd();
    }

    public long getEmployeeId() { return employeeId; }
    public String getEmployeeNo() { return employeeNo; }
    public String getEmployeeName() { return employeeName; }
    public String getRoleCd() { return roleCd; }
    public String getCompanyDiv() { return companyDiv; }
    public String getCompanyDivName() { return companyDivName; }
    public String getDeptCd() { return deptCd; }
    public String getRoleName() { return Codes.label("ROLE", roleCd); }
    public boolean isOwner() { return Codes.ROLE_OWNER.equals(roleCd); }
    public boolean isApprover() { return Codes.ROLE_APPROVER.equals(roleCd); }
    public boolean isAdmin() { return Codes.ROLE_ADMIN.equals(roleCd); }
}
