package com.example.appmgmt.domain;

import java.io.Serializable;

/** セッションに保持するログイン申込者の情報（申込者ポータル）。 */
public class ApplicantUser implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long applicantId;
    private final String applicantNo;
    private final String applicantName;

    public ApplicantUser(Applicant a) {
        this.applicantId = a.getApplicantId();
        this.applicantNo = a.getApplicantNo();
        this.applicantName = a.getApplicantName();
    }

    public long getApplicantId() { return applicantId; }
    public String getApplicantNo() { return applicantNo; }
    public String getApplicantName() { return applicantName; }
}
