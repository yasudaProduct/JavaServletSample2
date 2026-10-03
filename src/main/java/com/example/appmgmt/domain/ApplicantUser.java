package com.example.appmgmt.domain;

import java.io.Serializable;

/**
 * セッションに保持するログイン申込者の情報（申込者ポータル）。
 * ID・ユーザー ID は申込者アカウントから、表示名は申込者の最新の申込データ（現行版の申込者名）から取る。
 */
public class ApplicantUser implements Serializable {
    private static final long serialVersionUID = 2L;
    private final long applicantId;
    private final String applicantNo;
    private final String applicantName;

    public ApplicantUser(ApplicantAccount account, String displayName) {
        this.applicantId = account.getApplicantId();
        this.applicantNo = account.getApplicantNo();
        this.applicantName = displayName == null ? "" : displayName;
    }

    public long getApplicantId() { return applicantId; }
    public String getApplicantNo() { return applicantNo; }
    public String getApplicantName() { return applicantName; }
}
