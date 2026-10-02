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

    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public String getVersionType() { return versionType; }
    public void setVersionType(String versionType) { this.versionType = versionType; }
    public Integer getCopiedFromVersionNo() { return copiedFromVersionNo; }
    public void setCopiedFromVersionNo(Integer copiedFromVersionNo) { this.copiedFromVersionNo = copiedFromVersionNo; }
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

    /** 申込内容（商品・金額・契約期間・備考）が同じか。 */
    public boolean sameContentAs(ApplicationVersion other) {
        return other != null
                && Objects.equals(productCd, other.productCd)
                && cmp(basicFee, other.basicFee)
                && cmp(optionFee, other.optionFee)
                && cmp(handlingFee, other.handlingFee)
                && Objects.equals(contractStartDate, other.contractStartDate)
                && Objects.equals(contractEndDate, other.contractEndDate)
                && Objects.equals(normalize(remarks), normalize(other.remarks));
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

    public ApplicationVersion copyContent() {
        ApplicationVersion v = new ApplicationVersion();
        v.productCd = productCd;
        v.basicFee = basicFee;
        v.optionFee = optionFee;
        v.handlingFee = handlingFee;
        v.totalAmount = totalAmount;
        v.contractStartDate = contractStartDate;
        v.contractEndDate = contractEndDate;
        v.remarks = remarks;
        return v;
    }
}
