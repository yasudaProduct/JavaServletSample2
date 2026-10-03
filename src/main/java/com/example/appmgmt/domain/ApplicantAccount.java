package com.example.appmgmt.domain;

import java.time.LocalDateTime;

/**
 * 申込者アカウント（M_APPLICANT_ACCOUNT）。申込者が申込者ページにログインするためのデータだけを持つ。
 * 一次承認が通って申込内容確認待ちになったときに発行し、申込（T_APPLICATION.APPLICANT_ID）に紐づける。
 * 申込者名やメールアドレスなどの申込者情報は申込データ（申込内容の版）が持つ。
 */
public class ApplicantAccount extends AuditedEntity {
    private long applicantId;
    private String applicantNo;
    private String passwordHash;
    private LocalDateTime accountIssuedAt;
    private LocalDateTime passwordChangedAt;

    public long getApplicantId() { return applicantId; }
    public void setApplicantId(long applicantId) { this.applicantId = applicantId; }
    /** 申込者番号（申込者ページのユーザー ID）。 */
    public String getApplicantNo() { return applicantNo; }
    public void setApplicantNo(String applicantNo) { this.applicantNo = applicantNo; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDateTime getAccountIssuedAt() { return accountIssuedAt; }
    public void setAccountIssuedAt(LocalDateTime accountIssuedAt) { this.accountIssuedAt = accountIssuedAt; }
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }
    /** 初期パスワードのまま（発行後・初期化後に変更していない）。 */
    public boolean isInitialPassword() { return passwordChangedAt == null; }
}
