package com.example.appmgmt.web.form;

import com.example.appmgmt.common.Formats;
import com.example.appmgmt.common.Validation;
import com.example.appmgmt.domain.ApplicationVersion;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.servlet.http.HttpServletRequest;

/**
 * 申込内容の入力フォーム（SC04／SC07／SC08／AP02 共通）。入力値は文字列で保持し、チェック後に版へ変換する。
 * 申込者情報（申込者名・カナ・電話番号・メールアドレス・住所）も申込データとしてここで扱う。
 * 担当（会社区分・部署・担当社員）は SC04 の入力中だけ扱う（存在・組合せの検証はサービス層）。
 */
public class ApplicationForm {
    private String companyDiv = "";
    private String deptCd = "";
    private String ownerEmployeeId = "";
    private String applicantName = "";
    private String applicantKana = "";
    private String telNo = "";
    private String mailAddress = "";
    private String address = "";
    private String productCd = "";
    private String basicFee = "";
    private String optionFee = "";
    private String handlingFee = "";
    private String contractStartDate = "";
    private String contractEndDate = "";
    private String remarks = "";

    public static ApplicationForm bind(HttpServletRequest req) {
        ApplicationForm f = new ApplicationForm();
        f.companyDiv = p(req, "companyDiv");
        f.deptCd = p(req, "deptCd");
        f.ownerEmployeeId = p(req, "ownerEmployeeId");
        f.applicantName = p(req, "applicantName");
        f.applicantKana = p(req, "applicantKana");
        f.telNo = p(req, "telNo");
        f.mailAddress = p(req, "mailAddress");
        f.address = p(req, "address");
        f.productCd = p(req, "productCd");
        f.basicFee = p(req, "basicFee");
        f.optionFee = p(req, "optionFee");
        f.handlingFee = p(req, "handlingFee");
        f.contractStartDate = p(req, "contractStartDate");
        f.contractEndDate = p(req, "contractEndDate");
        f.remarks = req.getParameter("remarks") == null ? "" : req.getParameter("remarks").replace("\r\n", "\n").strip();
        return f;
    }

    private static String p(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? "" : v.trim();
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }

    public static ApplicationForm from(ApplicationVersion v) {
        ApplicationForm f = new ApplicationForm();
        if (v == null) {
            return f;
        }
        f.companyDiv = n(v.getCompanyDiv());
        f.deptCd = n(v.getDeptCd());
        f.ownerEmployeeId = v.getOwnerEmployeeId() == null ? "" : String.valueOf(v.getOwnerEmployeeId());
        f.applicantName = n(v.getApplicantName());
        f.applicantKana = n(v.getApplicantKana());
        f.telNo = n(v.getTelNo());
        f.mailAddress = n(v.getMailAddress());
        f.address = n(v.getAddress());
        f.productCd = n(v.getProductCd());
        f.basicFee = v.getBasicFee() == null ? "" : v.getBasicFee().toPlainString();
        f.optionFee = v.getOptionFee() == null ? "" : v.getOptionFee().toPlainString();
        f.handlingFee = v.getHandlingFee() == null ? "" : v.getHandlingFee().toPlainString();
        f.contractStartDate = Formats.date(v.getContractStartDate());
        f.contractEndDate = Formats.date(v.getContractEndDate());
        f.remarks = n(v.getRemarks());
        return f;
    }

    /** 担当の初期値（ログインユーザーの会社区分・部署と本人）。 */
    public ApplicationForm withAssignment(String companyDiv, String deptCd, long ownerEmployeeId) {
        this.companyDiv = n(companyDiv);
        this.deptCd = n(deptCd);
        this.ownerEmployeeId = String.valueOf(ownerEmployeeId);
        return this;
    }

    /** 担当（会社・部署・担当者）の必須チェック。SC04 の保存時だけ呼ぶ。 */
    public void validateAssignment(Validation v) {
        if (companyDiv.isEmpty()) {
            v.reject("companyDiv", "E001", "会社");
        }
        if (deptCd.isEmpty()) {
            v.reject("deptCd", "E001", "部署");
        }
        if (ownerEmployeeId.isEmpty()) {
            v.reject("ownerEmployeeId", "E001", "担当者");
        } else if (!ownerEmployeeId.matches("[0-9]{1,18}")) {
            v.reject("ownerEmployeeId", "E011", "担当者");
        }
    }

    public com.example.appmgmt.service.application.Assignment toAssignment() {
        return new com.example.appmgmt.service.application.Assignment(companyDiv, deptCd, Long.parseLong(ownerEmployeeId));
    }

    /**
     * 入力チェック（14. 共通仕様 6 章の順序）。
     * @param full true = 必須・相関を含む（確認へ・確定）、false = 桁・書式のみ（一時保存）
     * @param amountsOnly 金額項目だけを検証する（SC07 の一部修正）
     */
    public Validation validate(boolean full, boolean amountsOnly) {
        Validation v = new Validation();
        if (!amountsOnly) {
            validateApplicant(v, full);
            if (full && productCd.isEmpty()) {
                v.reject("productCd", "E001", "商品コード");
            } else if (productCd.length() > 10) {
                v.reject("productCd", "E002", "商品コード", 10);
            } else if (!Validation.isAlnum(productCd)) {
                v.reject("productCd", "E003", "商品コード");
            }
        }
        checkAmount(v, "basicFee", basicFee, "基本料金", full);
        checkAmount(v, "optionFee", optionFee, "オプション料金", false);
        checkAmount(v, "handlingFee", handlingFee, "事務手数料", false);
        if (!amountsOnly) {
            LocalDate s = checkDate(v, "contractStartDate", contractStartDate, "契約開始日");
            LocalDate e = checkDate(v, "contractEndDate", contractEndDate, "契約終了日");
            if (full && s != null && e != null && e.isBefore(s)) {
                v.reject("contractEndDate", "E004");
            }
            if (remarks.length() > 1000) {
                v.reject("remarks", "E002", "備考", 1000);
            } else if (Validation.hasControlChars(remarks)) {
                v.reject("remarks", "E003", "備考");
            }
        }
        if (full && !v.has("basicFee") && !v.has("optionFee") && !v.has("handlingFee")) {
            BigDecimal total = total();
            if (total == null || total.signum() <= 0) {
                v.reject("basicFee", "E005", "申込金額合計", 1);
            }
        }
        return v;
    }

    /** 申込者情報のチェック。申込者名・メールアドレスは確認へ・確定で必須（一時保存は桁・書式のみ）。 */
    private void validateApplicant(Validation v, boolean full) {
        if (applicantName.isEmpty()) {
            if (full) {
                v.reject("applicantName", "E001", "申込者名");
            }
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
        if (telNo.length() > 15) {
            v.reject("telNo", "E002", "電話番号", 15);
        } else if (!Validation.isTel(telNo)) {
            v.reject("telNo", "E003", "電話番号");
        }
        if (mailAddress.isEmpty()) {
            if (full) {
                v.reject("mailAddress", "E001", "メールアドレス");
            }
        } else if (mailAddress.length() > 254) {
            v.reject("mailAddress", "E002", "メールアドレス", 254);
        } else if (!Validation.isMail(mailAddress)) {
            v.reject("mailAddress", "E003", "メールアドレス");
        }
        if (address.length() > 200) {
            v.reject("address", "E002", "住所", 200);
        } else if (Validation.hasControlChars(address)) {
            v.reject("address", "E003", "住所");
        }
    }

    private static void checkAmount(Validation v, String field, String value, String name, boolean required) {
        if (value.isEmpty()) {
            if (required) {
                v.reject(field, "E001", name);
            }
            return;
        }
        BigDecimal a = Formats.parseAmount(value);
        if (a == null) {
            v.reject(field, "E003", name);
        } else if (a.signum() < 0) {
            v.reject(field, "E005", name, 0);
        } else if (a.precision() > 13) {
            v.reject(field, "E002", name, 13);
        }
    }

    private static LocalDate checkDate(Validation v, String field, String value, String name) {
        if (value.isEmpty()) {
            return null;
        }
        LocalDate d = Formats.parseDate(value);
        if (d == null) {
            v.reject(field, "E003", name);
        }
        return d;
    }

    public BigDecimal total() {
        BigDecimal b = basicFee.isEmpty() ? BigDecimal.ZERO : Formats.parseAmount(basicFee);
        BigDecimal o = optionFee.isEmpty() ? BigDecimal.ZERO : Formats.parseAmount(optionFee);
        BigDecimal h = handlingFee.isEmpty() ? BigDecimal.ZERO : Formats.parseAmount(handlingFee);
        if (b == null || o == null || h == null) {
            return null;
        }
        return b.add(o).add(h);
    }

    private static String nullIfEmpty(String s) {
        return s.isEmpty() ? null : s;
    }

    /** チェック済みの入力を版の内容（申込者情報を含む）に変換する。未入力の金額は 0、未入力の任意項目は null。 */
    public ApplicationVersion toVersion() {
        ApplicationVersion v = new ApplicationVersion();
        v.setApplicantName(nullIfEmpty(applicantName));
        v.setApplicantKana(nullIfEmpty(applicantKana));
        v.setTelNo(nullIfEmpty(telNo));
        v.setMailAddress(nullIfEmpty(mailAddress));
        v.setAddress(nullIfEmpty(address));
        v.setProductCd(productCd);
        v.setBasicFee(basicFee.isEmpty() ? BigDecimal.ZERO : Formats.parseAmount(basicFee));
        v.setOptionFee(optionFee.isEmpty() ? BigDecimal.ZERO : Formats.parseAmount(optionFee));
        v.setHandlingFee(handlingFee.isEmpty() ? BigDecimal.ZERO : Formats.parseAmount(handlingFee));
        v.setContractStartDate(Formats.parseDate(contractStartDate));
        v.setContractEndDate(Formats.parseDate(contractEndDate));
        v.setRemarks(nullIfEmpty(remarks));
        v.recalcTotal();
        return v;
    }

    public String getCompanyDiv() { return companyDiv; }
    public String getDeptCd() { return deptCd; }
    public String getOwnerEmployeeId() { return ownerEmployeeId; }
    public String getApplicantName() { return applicantName; }
    public String getApplicantKana() { return applicantKana; }
    public String getTelNo() { return telNo; }
    public String getMailAddress() { return mailAddress; }
    public String getAddress() { return address; }
    public String getProductCd() { return productCd; }
    public String getBasicFee() { return basicFee; }
    public String getOptionFee() { return optionFee; }
    public String getHandlingFee() { return handlingFee; }
    public String getContractStartDate() { return contractStartDate; }
    public String getContractEndDate() { return contractEndDate; }
    public String getRemarks() { return remarks; }
    public String getTotalAmount() { BigDecimal t = total(); return t == null ? "" : Formats.amount(t); }
}
