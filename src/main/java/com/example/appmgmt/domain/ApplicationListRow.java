package com.example.appmgmt.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 申込一覧（SC02）の 1 行。 */
public class ApplicationListRow {
    private long applicationId;
    private String applicationNo;
    private String applicantNo;
    private String applicantName;
    private String statusCd;
    private String statusName;
    private BigDecimal totalAmount;
    private String ownerName;
    private String registrationType;
    private LocalDateTime updatedAt;

    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }
    public String getApplicationNo() { return applicationNo; }
    public void setApplicationNo(String applicationNo) { this.applicationNo = applicationNo; }
    public String getApplicantNo() { return applicantNo; }
    public void setApplicantNo(String applicantNo) { this.applicantNo = applicantNo; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getStatusCd() { return statusCd; }
    public void setStatusCd(String statusCd) { this.statusCd = statusCd; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getRegistrationType() { return registrationType; }
    public void setRegistrationType(String registrationType) { this.registrationType = registrationType; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getRegistrationTypeName() { return Codes.label("REGISTRATION_TYPE", registrationType); }
}
