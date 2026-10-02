package com.example.appmgmt.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ApprovalRequest extends AuditedEntity {
    private long approvalRequestId;
    private long applicationId;
    private int versionNo;
    private String approvalType;
    private Long routeId;
    private String routeName;
    private long requestEmployeeId;
    private String requestEmployeeName;
    private LocalDateTime requestedAt;
    private String requestStatus;
    private Integer currentStepNo;
    private Integer finalStepNo;
    private LocalDateTime completedAt;
    private List<ApprovalStep> steps = new ArrayList<>();

    public long getApprovalRequestId() { return approvalRequestId; }
    public void setApprovalRequestId(long approvalRequestId) { this.approvalRequestId = approvalRequestId; }
    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public String getApprovalType() { return approvalType; }
    public void setApprovalType(String approvalType) { this.approvalType = approvalType; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public long getRequestEmployeeId() { return requestEmployeeId; }
    public void setRequestEmployeeId(long requestEmployeeId) { this.requestEmployeeId = requestEmployeeId; }
    public String getRequestEmployeeName() { return requestEmployeeName; }
    public void setRequestEmployeeName(String requestEmployeeName) { this.requestEmployeeName = requestEmployeeName; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }
    public Integer getCurrentStepNo() { return currentStepNo; }
    public void setCurrentStepNo(Integer currentStepNo) { this.currentStepNo = currentStepNo; }
    public Integer getFinalStepNo() { return finalStepNo; }
    public void setFinalStepNo(Integer finalStepNo) { this.finalStepNo = finalStepNo; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public List<ApprovalStep> getSteps() { return steps; }
    public void setSteps(List<ApprovalStep> steps) { this.steps = steps; }
    public boolean isInProgress() { return Codes.REQUEST_IN_PROGRESS.equals(requestStatus); }
    public String getApprovalTypeName() { return Codes.label("APPROVAL_TYPE", approvalType); }
    public String getRequestStatusName() { return Codes.label("REQUEST_STATUS", requestStatus); }

    public ApprovalStep currentStep() {
        if (currentStepNo == null) {
            return null;
        }
        for (ApprovalStep s : steps) {
            if (s.getStepNo() == currentStepNo) {
                return s;
            }
        }
        return null;
    }
}
