package com.example.appmgmt.domain;

/** 部署マスタ（M_DEPARTMENT）。会社区分ごとの部署コードと部署名。会社 > 部署 > 担当者の階層の「部署」。 */
public class Department extends AuditedEntity {
    private String companyDiv;
    private String deptCd;
    private String deptName;
    private String validFlg;
    private String companyDivName;

    public String getCompanyDiv() { return companyDiv; }
    public void setCompanyDiv(String companyDiv) { this.companyDiv = companyDiv; }
    public String getDeptCd() { return deptCd; }
    public void setDeptCd(String deptCd) { this.deptCd = deptCd; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getValidFlg() { return validFlg; }
    public void setValidFlg(String validFlg) { this.validFlg = validFlg; }
    public boolean isValid() { return Codes.FLG_ON.equals(validFlg); }
    /** 表示用（M_COMPANY_DIV.COMPANY_DIV_NAME）。 */
    public String getCompanyDivName() { return companyDivName; }
    public void setCompanyDivName(String companyDivName) { this.companyDivName = companyDivName; }
    /** 画面の選択肢・表示用のキー（会社区分:部署コード）。 */
    public String getKey() { return companyDiv + ":" + deptCd; }
}
