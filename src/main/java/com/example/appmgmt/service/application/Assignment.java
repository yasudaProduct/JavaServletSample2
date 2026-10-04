package com.example.appmgmt.service.application;

import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.LoginUser;

/**
 * 申込の担当（申込受付会社側の会社区分 > 部署 > 担当社員）。申込入力（SC04）の入力中だけ変更できる。
 * 部署は担当社員の所属部署と異なってよいが、担当社員は選んだ会社区分の社員とする。
 */
public final class Assignment {
    private final String companyDiv;
    private final String deptCd;
    private final long ownerEmployeeId;

    public Assignment(String companyDiv, String deptCd, long ownerEmployeeId) {
        this.companyDiv = companyDiv;
        this.deptCd = deptCd;
        this.ownerEmployeeId = ownerEmployeeId;
    }

    /** 既定値：ログインユーザーの会社区分・部署と、ログインユーザー本人。 */
    public static Assignment of(LoginUser user) {
        return new Assignment(user.getCompanyDiv(), user.getDeptCd(), user.getEmployeeId());
    }

    public static Assignment of(Application app) {
        return new Assignment(app.getCompanyDiv(), app.getDeptCd(), app.getOwnerEmployeeId());
    }

    public String getCompanyDiv() { return companyDiv; }
    public String getDeptCd() { return deptCd; }
    public long getOwnerEmployeeId() { return ownerEmployeeId; }

    public boolean sameAs(Assignment o) {
        return o != null && companyDiv.equals(o.companyDiv) && deptCd.equals(o.deptCd) && ownerEmployeeId == o.ownerEmployeeId;
    }
}
