package com.example.appmgmt.domain;

import java.time.LocalDateTime;

public class Application extends AuditedEntity {
    private long applicationId;
    private String applicationNo;
    private long applicantId;
    private long ownerEmployeeId;
    private String companyDiv;
    private String deptCd;
    private String statusCd;
    private int currentVersionNo;
    private Integer baseVersionNo;
    private Integer reviewedVersionNo;
    private String registrationType;
    private Long sourceApplicationId;
    private Long importBatchId;
    private LocalDateTime reviewedAt;

    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }
    public String getApplicationNo() { return applicationNo; }
    public void setApplicationNo(String applicationNo) { this.applicationNo = applicationNo; }
    public long getApplicantId() { return applicantId; }
    public void setApplicantId(long applicantId) { this.applicantId = applicantId; }
    public long getOwnerEmployeeId() { return ownerEmployeeId; }
    public void setOwnerEmployeeId(long ownerEmployeeId) { this.ownerEmployeeId = ownerEmployeeId; }
    public String getCompanyDiv() { return companyDiv; }
    public void setCompanyDiv(String companyDiv) { this.companyDiv = companyDiv; }
    public String getDeptCd() { return deptCd; }
    public void setDeptCd(String deptCd) { this.deptCd = deptCd; }
    public String getStatusCd() { return statusCd; }
    public void setStatusCd(String statusCd) { this.statusCd = statusCd; }
    public int getCurrentVersionNo() { return currentVersionNo; }
    public void setCurrentVersionNo(int currentVersionNo) { this.currentVersionNo = currentVersionNo; }
    public Integer getBaseVersionNo() { return baseVersionNo; }
    public void setBaseVersionNo(Integer baseVersionNo) { this.baseVersionNo = baseVersionNo; }
    public Integer getReviewedVersionNo() { return reviewedVersionNo; }
    public void setReviewedVersionNo(Integer reviewedVersionNo) { this.reviewedVersionNo = reviewedVersionNo; }
    public String getRegistrationType() { return registrationType; }
    public void setRegistrationType(String registrationType) { this.registrationType = registrationType; }
    public Long getSourceApplicationId() { return sourceApplicationId; }
    public void setSourceApplicationId(Long sourceApplicationId) { this.sourceApplicationId = sourceApplicationId; }
    public Long getImportBatchId() { return importBatchId; }
    public void setImportBatchId(Long importBatchId) { this.importBatchId = importBatchId; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
