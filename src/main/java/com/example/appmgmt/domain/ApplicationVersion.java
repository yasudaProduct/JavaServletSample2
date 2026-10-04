package com.example.appmgmt.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class ApplicationVersion extends AuditedEntity {
    private long applicationId;
    private int versionNo;
    private String versionType;
    private Integer copiedFromVersionNo;
    // 申込者情報（申込データの一部。版ごとに持ち、全体修正・契約変更・申込者の修正で変わる）
    private String applicantName;
    private String applicantKana;
    private String telNo;
    private String mailAddress;
    private String address;
    private String productCd;
    private BigDecimal basicFee;
    private BigDecimal optionFee;
    private BigDecimal handlingFee;
    private BigDecimal totalAmount;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private BigDecimal amountRatio;
    private String remarks;
    private LocalDateTime confirmedAt;
    private String fixedFlg;
    private String canceledFlg;
    /** 担当（申込受付会社側の会社区分・部署・担当社員）。申込（T_APPLICATION）の値と同じものを版にも記録する。入力中だけ変更できる。 */
    private String companyDiv;
    private String deptCd;
    private Long ownerEmployeeId;
    /** 表示用（会社区分名・部署名・担当社員名）。 */
    private String companyDivName;
    private String deptName;
    private String ownerName;

    public String getCompanyDiv() { return companyDiv; }
    public void setCompanyDiv(String companyDiv) { this.companyDiv = companyDiv; }
    public String getDeptCd() { return deptCd; }
    public void setDeptCd(String deptCd) { this.deptCd = deptCd; }
    public Long getOwnerEmployeeId() { return ownerEmployeeId; }
    public void setOwnerEmployeeId(Long ownerEmployeeId) { this.ownerEmployeeId = ownerEmployeeId; }
    public String getCompanyDivName() { return companyDivName; }
    public void setCompanyDivName(String companyDivName) { this.companyDivName = companyDivName; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    /** 担当（会社区分・部署・担当社員）を写す。 */
    public void applyAssignmentFrom(String companyDiv, String deptCd, Long ownerEmployeeId) {
        this.companyDiv = companyDiv;
        this.deptCd = deptCd;
        this.ownerEmployeeId = ownerEmployeeId;
    }

    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public String getVersionType() { return versionType; }
    public void setVersionType(String versionType) { this.versionType = versionType; }
    public Integer getCopiedFromVersionNo() { return copiedFromVersionNo; }
    public void setCopiedFromVersionNo(Integer copiedFromVersionNo) { this.copiedFromVersionNo = copiedFromVersionNo; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getApplicantKana() { return applicantKana; }
    public void setApplicantKana(String applicantKana) { this.applicantKana = applicantKana; }
    public String getTelNo() { return telNo; }
    public void setTelNo(String telNo) { this.telNo = telNo; }
    public String getMailAddress() { return mailAddress; }
    public void setMailAddress(String mailAddress) { this.mailAddress = mailAddress; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getProductCd() { return productCd; }
    public void setProductCd(String productCd) { this.productCd = productCd; }
    public BigDecimal getBasicFee() { return basicFee; }
    public void setBasicFee(BigDecimal basicFee) { this.basicFee = basicFee; }
    public BigDecimal getOptionFee() { return optionFee; }
    public void setOptionFee(BigDecimal optionFee) { this.optionFee = optionFee; }
    public BigDecimal getHandlingFee() { return handlingFee; }
    public void setHandlingFee(BigDecimal handlingFee) { this.handlingFee = handlingFee; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDate getContractStartDate() { return contractStartDate; }
    public void setContractStartDate(LocalDate contractStartDate) { this.contractStartDate = contractStartDate; }
    public LocalDate getContractEndDate() { return contractEndDate; }
    public void setContractEndDate(LocalDate contractEndDate) { this.contractEndDate = contractEndDate; }
    public BigDecimal getAmountRatio() { return amountRatio; }
    public void setAmountRatio(BigDecimal amountRatio) { this.amountRatio = amountRatio; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getFixedFlg() { return fixedFlg; }
    public void setFixedFlg(String fixedFlg) { this.fixedFlg = fixedFlg; }
    public String getCanceledFlg() { return canceledFlg; }
    public void setCanceledFlg(String canceledFlg) { this.canceledFlg = canceledFlg; }
    public boolean isFixed() { return Codes.FLG_ON.equals(fixedFlg); }
    public boolean isCanceled() { return Codes.FLG_ON.equals(canceledFlg); }
    public String getVersionTypeName() { return Codes.label("VERSION_TYPE", versionType); }

    /** 申込金額合計を金額項目から再計算する。 */
    public void recalcTotal() {
        BigDecimal b = basicFee == null ? BigDecimal.ZERO : basicFee;
        BigDecimal o = optionFee == null ? BigDecimal.ZERO : optionFee;
        BigDecimal h = handlingFee == null ? BigDecimal.ZERO : handlingFee;
        this.totalAmount = b.add(o).add(h);
    }

    /** 申込内容（申込者情報・商品・金額・契約期間・備考）が同じか。 */
    public boolean sameContentAs(ApplicationVersion other) {
        return other != null
                && sameApplicantAs(other)
                && Objects.equals(productCd, other.productCd)
                && cmp(basicFee, other.basicFee)
                && cmp(optionFee, other.optionFee)
                && cmp(handlingFee, other.handlingFee)
                && Objects.equals(contractStartDate, other.contractStartDate)
                && Objects.equals(contractEndDate, other.contractEndDate)
                && Objects.equals(normalize(remarks), normalize(other.remarks));
    }

    /** 申込者情報（申込者名・カナ・電話番号・メールアドレス・住所）が同じか。 */
    public boolean sameApplicantAs(ApplicationVersion other) {
        return other != null
                && Objects.equals(normalize(applicantName), normalize(other.applicantName))
                && Objects.equals(normalize(applicantKana), normalize(other.applicantKana))
                && Objects.equals(normalize(telNo), normalize(other.telNo))
                && Objects.equals(normalize(mailAddress), normalize(other.mailAddress))
                && Objects.equals(normalize(address), normalize(other.address));
    }

    /** 金額項目だけが同じか。 */
    public boolean sameAmountsAs(ApplicationVersion other) {
        return other != null && cmp(basicFee, other.basicFee) && cmp(optionFee, other.optionFee) && cmp(handlingFee, other.handlingFee);
    }

    private static boolean cmp(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a.compareTo(b) == 0;
    }

    private static String normalize(String s) {
        return s == null || s.isEmpty() ? null : s.replace("\r\n", "\n");
    }

    /** 入力された申込内容（申込者情報を含む）をこの版へ写す。版番号・版種別・確定状態などは変えない。 */
    public void applyContentFrom(ApplicationVersion content) {
        applicantName = content.applicantName;
        applicantKana = content.applicantKana;
        telNo = content.telNo;
        mailAddress = content.mailAddress;
        address = content.address;
        productCd = content.productCd;
        basicFee = content.basicFee;
        optionFee = content.optionFee;
        handlingFee = content.handlingFee;
        contractStartDate = content.contractStartDate;
        contractEndDate = content.contractEndDate;
        remarks = content.remarks;
        recalcTotal();
    }

    public ApplicationVersion copyContent() {
        ApplicationVersion v = new ApplicationVersion();
        v.applicantName = applicantName;
        v.applicantKana = applicantKana;
        v.telNo = telNo;
        v.mailAddress = mailAddress;
        v.address = address;
        v.productCd = productCd;
        v.basicFee = basicFee;
        v.optionFee = optionFee;
        v.handlingFee = handlingFee;
        v.totalAmount = totalAmount;
        v.contractStartDate = contractStartDate;
        v.contractEndDate = contractEndDate;
        v.remarks = remarks;
        v.companyDiv = companyDiv;
        v.deptCd = deptCd;
        v.ownerEmployeeId = ownerEmployeeId;
        return v;
    }
}
