package com.example.appmgmt.domain;

import java.time.LocalDateTime;

public class Applicant extends AuditedEntity {
    private long applicantId;
    private String applicantNo;
    private String applicantName;
    private String applicantKana;
    private String mailAddress;
    private String telNo;
    private String address;
    private String passwordHash;
    private LocalDateTime accountIssuedAt;
    private LocalDateTime passwordChangedAt;

    public long getApplicantId() { return applicantId; }
    public void setApplicantId(long applicantId) { this.applicantId = applicantId; }
    public String getApplicantNo() { return applicantNo; }
    public void setApplicantNo(String applicantNo) { this.applicantNo = applicantNo; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getApplicantKana() { return applicantKana; }
    public void setApplicantKana(String applicantKana) { this.applicantKana = applicantKana; }
    public String getMailAddress() { return mailAddress; }
    public void setMailAddress(String mailAddress) { this.mailAddress = mailAddress; }
    public String getTelNo() { return telNo; }
    public void setTelNo(String telNo) { this.telNo = telNo; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDateTime getAccountIssuedAt() { return accountIssuedAt; }
    public void setAccountIssuedAt(LocalDateTime accountIssuedAt) { this.accountIssuedAt = accountIssuedAt; }
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }
    /** 申込者ページのアカウントが発行済みか。ログイン ID は申込者番号。 */
    public boolean isAccountIssued() { return passwordHash != null; }
}
