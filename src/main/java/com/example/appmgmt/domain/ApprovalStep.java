package com.example.appmgmt.domain;

import java.time.LocalDateTime;

public class ApprovalStep extends AuditedEntity {
    private long approvalRequestId;
    private int stepNo;
    private long approverEmployeeId;
    private String approverName;
    private String resultCd;
    private String comment;
    private LocalDateTime actedAt;

    public long getApprovalRequestId() { return approvalRequestId; }
    public void setApprovalRequestId(long approvalRequestId) { this.approvalRequestId = approvalRequestId; }
    public int getStepNo() { return stepNo; }
    public void setStepNo(int stepNo) { this.stepNo = stepNo; }
    public long getApproverEmployeeId() { return approverEmployeeId; }
    public void setApproverEmployeeId(long approverEmployeeId) { this.approverEmployeeId = approverEmployeeId; }
    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
    public String getResultCd() { return resultCd; }
    public void setResultCd(String resultCd) { this.resultCd = resultCd; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getActedAt() { return actedAt; }
    public void setActedAt(LocalDateTime actedAt) { this.actedAt = actedAt; }
    public String getResultName() { return Codes.label("APPROVAL_RESULT", resultCd); }
}
