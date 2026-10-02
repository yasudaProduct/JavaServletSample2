package com.example.appmgmt.web.form;

import com.example.appmgmt.common.Formats;
import com.example.appmgmt.common.Validation;
import com.example.appmgmt.domain.ApplicationVersion;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.servlet.http.HttpServletRequest;

/** 申込内容の入力フォーム（SC04／SC07／SC08／AP02 共通）。入力値は文字列で保持し、チェック後に版へ変換する。 */
public class ApplicationForm {
    private String applicantNo = "";
    private String productCd = "";
    private String basicFee = "";
    private String optionFee = "";
    private String handlingFee = "";
    private String contractStartDate = "";
    private String contractEndDate = "";
    private String remarks = "";

    public static ApplicationForm bind(HttpServletRequest req) {
        ApplicationForm f = new ApplicationForm();
        f.applicantNo = p(req, "applicantNo");
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

    public static ApplicationForm from(ApplicationVersion v) {
        ApplicationForm f = new ApplicationForm();
        if (v == null) {
            return f;
        }
        f.productCd = v.getProductCd() == null ? "" : v.getProductCd();
        f.basicFee = v.getBasicFee() == null ? "" : v.getBasicFee().toPlainString();
        f.optionFee = v.getOptionFee() == null ? "" : v.getOptionFee().toPlainString();
        f.handlingFee = v.getHandlingFee() == null ? "" : v.getHandlingFee().toPlainString();
        f.contractStartDate = Formats.date(v.getContractStartDate());
        f.contractEndDate = Formats.date(v.getContractEndDate());
        f.remarks = v.getRemarks() == null ? "" : v.getRemarks();
        return f;
    }

    /**
     * 入力チェック（14. 共通仕様 6 章の順序）。
     * @param full true = 必須・相関を含む（確認へ・確定）、false = 桁・書式のみ（一時保存）
     * @param requireApplicant 申込者番号を必須にする（新規申込）
     * @param amountsOnly 金額項目だけを検証する（SC07 の一部修正）
     */
    public Validation validate(boolean full, boolean requireApplicant, boolean amountsOnly) {
        Validation v = new Validation();
        if (requireApplicant && !amountsOnly) {
            if (applicantNo.isEmpty()) {
                v.reject("applicantNo", "E001", "申込者番号");
            } else if (applicantNo.length() > 12) {
                v.reject("applicantNo", "E002", "申込者番号", 12);
            } else if (!Validation.isAlnum(applicantNo)) {
                v.reject("applicantNo", "E003", "申込者番号");
            }
        }
        if (!amountsOnly) {
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

    /** チェック済みの入力を版の内容に変換する。未入力の金額は 0、未入力の日付・備考は null。 */
    public ApplicationVersion toVersion() {
        ApplicationVersion v = new ApplicationVersion();
        v.setProductCd(productCd);
        v.setBasicFee(basicFee.isEmpty() ? BigDecimal.ZERO : Formats.parseAmount(basicFee));
        v.setOptionFee(optionFee.isEmpty() ? BigDecimal.ZERO : Formats.parseAmount(optionFee));
        v.setHandlingFee(handlingFee.isEmpty() ? BigDecimal.ZERO : Formats.parseAmount(handlingFee));
        v.setContractStartDate(Formats.parseDate(contractStartDate));
        v.setContractEndDate(Formats.parseDate(contractEndDate));
        v.setRemarks(remarks.isEmpty() ? null : remarks);
        v.recalcTotal();
        return v;
    }

    public String getApplicantNo() { return applicantNo; }
    public String getProductCd() { return productCd; }
    public String getBasicFee() { return basicFee; }
    public String getOptionFee() { return optionFee; }
    public String getHandlingFee() { return handlingFee; }
    public String getContractStartDate() { return contractStartDate; }
    public String getContractEndDate() { return contractEndDate; }
    public String getRemarks() { return remarks; }
    public String getTotalAmount() { BigDecimal t = total(); return t == null ? "" : Formats.amount(t); }
}
