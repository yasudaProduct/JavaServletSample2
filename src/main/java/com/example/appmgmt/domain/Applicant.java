package com.example.appmgmt.domain;

/**
 * 申込者情報（表示用の値）。申込者名・カナ・電話番号・メールアドレス・住所は申込データ（申込内容の版）が持ち、
 * ユーザー ID（申込者番号）は申込者アカウントが持つ。両者をまとめて画面・通知・電文で使う。テーブルには対応しない。
 */
public class Applicant {
    private String applicantNo;
    private String applicantName;
    private String applicantKana;
    private String mailAddress;
    private String telNo;
    private String address;

    /** 版の申込者情報と、申込者アカウント（未発行なら null）から組み立てる。 */
    public static Applicant of(ApplicationVersion v, ApplicantAccount account) {
        Applicant a = new Applicant();
        if (v != null) {
            a.applicantName = v.getApplicantName();
            a.applicantKana = v.getApplicantKana();
            a.mailAddress = v.getMailAddress();
            a.telNo = v.getTelNo();
            a.address = v.getAddress();
        }
        a.applicantNo = account == null ? null : account.getApplicantNo();
        return a;
    }

    /** 申込者番号（申込者ページのユーザー ID）。アカウント未発行なら null。 */
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
    /** 申込者アカウントが発行済みか。 */
    public boolean isAccountIssued() { return applicantNo != null; }
}
