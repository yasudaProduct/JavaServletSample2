package com.example.appmgmt.dao;

import com.example.appmgmt.domain.CompanyDiv;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CompanyDivDao extends AbstractDao {

    private static final String SELECT = "SELECT COMPANY_DIV, COMPANY_DIV_NAME, PRE_CHECK_FLG, AMOUNT_RATIO_LIMIT, " + AUDIT_COLS + " FROM M_COMPANY_DIV";

    private static CompanyDiv map(ResultSet rs) throws SQLException {
        CompanyDiv c = new CompanyDiv();
        c.setCompanyDiv(rs.getString("COMPANY_DIV"));
        c.setCompanyDivName(rs.getString("COMPANY_DIV_NAME"));
        c.setPreCheckFlg(rs.getString("PRE_CHECK_FLG"));
        c.setAmountRatioLimit(rs.getBigDecimal("AMOUNT_RATIO_LIMIT"));
        mapAudit(rs, c);
        return c;
    }

    public List<CompanyDiv> findAll(Connection conn) {
        return query(conn, SELECT + " ORDER BY COMPANY_DIV", CompanyDivDao::map);
    }

    public Optional<CompanyDiv> find(Connection conn, String companyDiv) {
        return queryOne(conn, SELECT + " WHERE COMPANY_DIV = ?", CompanyDivDao::map, companyDiv);
    }

    public int update(Connection conn, CompanyDiv c, int expectedRowVersion) {
        return update(conn, "UPDATE M_COMPANY_DIV SET COMPANY_DIV_NAME = ?, PRE_CHECK_FLG = ?, AMOUNT_RATIO_LIMIT = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE COMPANY_DIV = ? AND ROW_VERSION = ?",
                c.getCompanyDivName(), c.getPreCheckFlg(), c.getAmountRatioLimit(), now(), actor(), c.getCompanyDiv(), expectedRowVersion);
    }
}
