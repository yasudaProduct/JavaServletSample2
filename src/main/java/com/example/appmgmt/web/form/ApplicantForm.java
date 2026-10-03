package com.example.appmgmt.web.form;

import com.example.appmgmt.common.Validation;
import com.example.appmgmt.domain.Applicant;
import javax.servlet.http.HttpServletRequest;

/**
 * 申込者情報の入力フォーム（SC04 新規申込・入力中の補正、SC15 申込者情報の変更）。
 * 新規申込では「新規の申込者を登録する」（申込者情報を入力し、申込の作成と同時に登録・採番）と
 * 「登録済みの申込者を指定する」（申込者番号を入力）を選ぶ。
 */
public class ApplicantForm {
    public static final String MODE_NEW = "new";
    public static final String MODE_EXISTING = "existing";

    private String mode = MODE_NEW;
    private String applicantNo = "";
    private String applicantName = "";
    private String applicantKana = "";
    private String mailAddress = "";
    private String telNo = "";
    private String address = "";
    private boolean allowDuplicate;

    public static ApplicantForm bind(HttpServletRequest req) {
        ApplicantForm f = new ApplicantForm();
        f.mode = MODE_EXISTING.equals(p(req, "applicantMode")) ? MODE_EXISTING : MODE_NEW;
        f.applicantNo = p(req, "applicantNo");
        f.applicantName = p(req, "applicantName");
        f.applicantKana = p(req, "applicantKana");
        f.mailAddress = p(req, "mailAddress");
        f.telNo = p(req, "telNo");
        f.address = p(req, "address");
        f.allowDuplicate = "1".equals(p(req, "allowDuplicate"));
        return f;
    }

    public static ApplicantForm from(Applicant a) {
        ApplicantForm f = new ApplicantForm();
        if (a == null) {
            return f;
        }
        f.mode = MODE_EXISTING;
        f.applicantNo = n(a.getApplicantNo());
        f.applicantName = n(a.getApplicantName());
        f.applicantKana = n(a.getApplicantKana());
        f.mailAddress = n(a.getMailAddress());
        f.telNo = n(a.getTelNo());
        f.address = n(a.getAddress());
        return f;
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }

    private static String p(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? "" : v.trim();
    }

    /** 申込者情報のチェック（申込者名・メールアドレスは一時保存でも必須。申込者マスタに登録するため）。 */
    public void validateProfile(Validation v) {
        if (applicantName.isEmpty()) {
            v.reject("applicantName", "E001", "申込者名");
        } else if (applicantName.length() > 100) {
            v.reject("applicantName", "E002", "申込者名", 100);
        } else if (Validation.hasControlChars(applicantName)) {
            v.reject("applicantName", "E003", "申込者名");
        }
        if (applicantKana.length() > 100) {
            v.reject("applicantKana", "E002", "申込者名カナ", 100);
        } else if (!applicantKana.matches("[\\u30A0-\\u30FF\\u3000 ]*")) {
            v.reject("applicantKana", "E003", "申込者名カナ");
        }
        if (mailAddress.isEmpty()) {
            v.reject("mailAddress", "E001", "メールアドレス");
        } else if (mailAddress.length() > 254) {
            v.reject("mailAddress", "E002", "メールアドレス", 254);
        } else if (!Validation.isMail(mailAddress)) {
            v.reject("mailAddress", "E003", "メールアドレス");
        }
        if (telNo.length() > 15) {
            v.reject("telNo", "E002", "電話番号", 15);
        } else if (!Validation.isTel(telNo)) {
            v.reject("telNo", "E003", "電話番号");
        }
        if (address.length() > 200) {
            v.reject("address", "E002", "住所", 200);
        } else if (Validation.hasControlChars(address)) {
            v.reject("address", "E003", "住所");
        }
    }

    /** 入力値を申込者に変換する（空の任意項目は null）。 */
    public Applicant toApplicant() {
        Applicant a = new Applicant();
        a.setApplicantNo(applicantNo.isEmpty() ? null : applicantNo);
        a.setApplicantName(applicantName);
        a.setApplicantKana(applicantKana.isEmpty() ? null : applicantKana);
        a.setMailAddress(mailAddress);
        a.setTelNo(telNo.isEmpty() ? null : telNo);
        a.setAddress(address.isEmpty() ? null : address);
        return a;
    }

    public boolean isNewApplicant() { return MODE_NEW.equals(mode); }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getApplicantNo() { return applicantNo; }
    public String getApplicantName() { return applicantName; }
    public String getApplicantKana() { return applicantKana; }
    public String getMailAddress() { return mailAddress; }
    public String getTelNo() { return telNo; }
    public String getAddress() { return address; }
    public boolean isAllowDuplicate() { return allowDuplicate; }
}
