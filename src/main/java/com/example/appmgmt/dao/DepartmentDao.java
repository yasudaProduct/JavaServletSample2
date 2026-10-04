package com.example.appmgmt.dao;

import com.example.appmgmt.domain.Department;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** 部署マスタ（M_DEPARTMENT）。 */
public class DepartmentDao extends AbstractDao {

    private static final String SELECT = "SELECT d.COMPANY_DIV, d.DEPT_CD, d.DEPT_NAME, d.VALID_FLG, c.COMPANY_DIV_NAME, "
            + "d.CREATED_AT, d.CREATED_BY, d.UPDATED_AT, d.UPDATED_BY, d.ROW_VERSION FROM M_DEPARTMENT d JOIN M_COMPANY_DIV c ON c.COMPANY_DIV = d.COMPANY_DIV";

    private static Department map(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setCompanyDiv(rs.getString("COMPANY_DIV"));
        d.setDeptCd(rs.getString("DEPT_CD"));
        d.setDeptName(rs.getString("DEPT_NAME"));
        d.setValidFlg(rs.getString("VALID_FLG"));
        d.setCompanyDivName(rs.getString("COMPANY_DIV_NAME"));
        mapAudit(rs, d);
        return d;
    }

    public List<Department> findAll(Connection conn) {
        return query(conn, SELECT + " ORDER BY d.COMPANY_DIV, d.DEPT_CD", DepartmentDao::map);
    }

    public List<Department> findValid(Connection conn) {
        return query(conn, SELECT + " WHERE d.VALID_FLG = '1' ORDER BY d.COMPANY_DIV, d.DEPT_CD", DepartmentDao::map);
    }

    public Optional<Department> find(Connection conn, String companyDiv, String deptCd) {
        return queryOne(conn, SELECT + " WHERE d.COMPANY_DIV = ? AND d.DEPT_CD = ?", DepartmentDao::map, companyDiv, deptCd);
    }

    public void insert(Connection conn, Department d) {
        update(conn, "INSERT INTO M_DEPARTMENT (COMPANY_DIV, DEPT_CD, DEPT_NAME, VALID_FLG, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
                d.getCompanyDiv(), d.getDeptCd(), d.getDeptName(), d.getValidFlg(), now(), actor(), now(), actor());
    }

    /** 部署名・有効フラグの更新（会社区分・部署コードは変えない）。行バージョンで楽観排他。 */
    public int update(Connection conn, Department d, int expectedRowVersion) {
        return update(conn, "UPDATE M_DEPARTMENT SET DEPT_NAME = ?, VALID_FLG = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE COMPANY_DIV = ? AND DEPT_CD = ? AND ROW_VERSION = ?",
                d.getDeptName(), d.getValidFlg(), now(), actor(), d.getCompanyDiv(), d.getDeptCd(), expectedRowVersion);
    }
}
