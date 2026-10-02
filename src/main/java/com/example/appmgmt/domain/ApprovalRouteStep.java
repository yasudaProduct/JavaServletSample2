package com.example.appmgmt.domain;

public class ApprovalRouteStep extends AuditedEntity {
    private long routeId;
    private int stepNo;
    private long approverEmployeeId;
    private String approverName;
    private String approverDeptCd;

    public long getRouteId() { return routeId; }
    public void setRouteId(long routeId) { this.routeId = routeId; }
    public int getStepNo() { return stepNo; }
    public void setStepNo(int stepNo) { this.stepNo = stepNo; }
    public long getApproverEmployeeId() { return approverEmployeeId; }
    public void setApproverEmployeeId(long approverEmployeeId) { this.approverEmployeeId = approverEmployeeId; }
    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
    public String getApproverDeptCd() { return approverDeptCd; }
    public void setApproverDeptCd(String approverDeptCd) { this.approverDeptCd = approverDeptCd; }
}
