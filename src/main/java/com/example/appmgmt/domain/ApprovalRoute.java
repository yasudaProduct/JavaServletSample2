package com.example.appmgmt.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ApprovalRoute extends AuditedEntity {
    private long routeId;
    private String companyDiv;
    private String deptCd;
    /** 表示用：部署名（M_DEPARTMENT） */
    private String deptName;
    private String approvalType;
    private String routeName;
    private LocalDate validFrom;
    private LocalDate validTo;
    private List<ApprovalRouteStep> steps = new ArrayList<>();

    public long getRouteId() { return routeId; }
    public void setRouteId(long routeId) { this.routeId = routeId; }
    public String getCompanyDiv() { return companyDiv; }
    public void setCompanyDiv(String companyDiv) { this.companyDiv = companyDiv; }
    public String getDeptCd() { return deptCd; }
    public void setDeptCd(String deptCd) { this.deptCd = deptCd; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getApprovalType() { return approvalType; }
    public void setApprovalType(String approvalType) { this.approvalType = approvalType; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public List<ApprovalRouteStep> getSteps() { return steps; }
    public void setSteps(List<ApprovalRouteStep> steps) { this.steps = steps; }
}
