package com.example.appmgmt.domain;

public class Applicant extends AuditedEntity {
    private long applicantId;
    private String applicantNo;
    private String applicantName;
    private String applicantKana;
    private String mailAddress;
    private String telNo;
    private String address;

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
}
