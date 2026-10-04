package com.example.appmgmt.domain;

import java.time.LocalDateTime;

public class Application extends AuditedEntity {
    private long applicationId;
    private String applicationNo;
    /** 申込者アカウント ID。一次承認が通ってアカウントを発行（または既存のアカウントを指定）するまでは null。 */
    private Long applicantId;
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
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public long getOwnerEmployeeId() { return ownerEmployeeId; }
    public void setOwnerEmployeeId(long ownerEmployeeId) { this.ownerEmployeeId = ownerEmployeeId; }
    public String getCompanyDiv() { return companyDiv; }
    public void setCompanyDiv(String companyDiv) { this.companyDiv = companyDiv; }
    /** 会社B の追加項目を入力する申込か（担当会社が会社区分 2）。 */
    public boolean isCompanyExtraTarget() { return Codes.hasCompanyExtra(companyDiv); }
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
