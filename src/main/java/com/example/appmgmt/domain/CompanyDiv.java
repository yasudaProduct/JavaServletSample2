package com.example.appmgmt.domain;

import java.math.BigDecimal;

public class CompanyDiv extends AuditedEntity {
    private String companyDiv;
    private String companyDivName;
    private String preCheckFlg;
    private BigDecimal amountRatioLimit;

    public String getCompanyDiv() { return companyDiv; }
    public void setCompanyDiv(String companyDiv) { this.companyDiv = companyDiv; }
    public String getCompanyDivName() { return companyDivName; }
    public void setCompanyDivName(String companyDivName) { this.companyDivName = companyDivName; }
    public String getPreCheckFlg() { return preCheckFlg; }
    public void setPreCheckFlg(String preCheckFlg) { this.preCheckFlg = preCheckFlg; }
    public BigDecimal getAmountRatioLimit() { return amountRatioLimit; }
    public void setAmountRatioLimit(BigDecimal amountRatioLimit) { this.amountRatioLimit = amountRatioLimit; }
    public boolean isPreCheck() { return Codes.FLG_ON.equals(preCheckFlg); }
}
