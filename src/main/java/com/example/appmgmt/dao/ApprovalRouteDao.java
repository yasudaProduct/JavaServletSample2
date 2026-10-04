package com.example.appmgmt.dao;

import com.example.appmgmt.domain.ApprovalRoute;
import com.example.appmgmt.domain.ApprovalRouteStep;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ApprovalRouteDao extends AbstractDao {

    private static final String SELECT = "SELECT ROUTE_ID, COMPANY_DIV, DEPT_CD, APPROVAL_TYPE, ROUTE_NAME, VALID_FROM, VALID_TO, "
            + "(SELECT d.DEPT_NAME FROM M_DEPARTMENT d WHERE d.COMPANY_DIV = M_APPROVAL_ROUTE.COMPANY_DIV AND d.DEPT_CD = M_APPROVAL_ROUTE.DEPT_CD) AS DEPT_NAME, " + AUDIT_COLS + " FROM M_APPROVAL_ROUTE";
    private static final String SELECT_STEP = "SELECT s.ROUTE_ID, s.STEP_NO, s.APPROVER_EMPLOYEE_ID, e.EMPLOYEE_NAME, e.DEPT_CD AS APPROVER_DEPT_CD, s.CREATED_AT, s.CREATED_BY, s.UPDATED_AT, s.UPDATED_BY, s.ROW_VERSION "
            + "FROM M_APPROVAL_ROUTE_STEP s JOIN M_EMPLOYEE e ON e.EMPLOYEE_ID = s.APPROVER_EMPLOYEE_ID";

    static ApprovalRoute map(ResultSet rs) throws SQLException {
        ApprovalRoute r = new ApprovalRoute();
        r.setRouteId(rs.getLong("ROUTE_ID"));
        r.setCompanyDiv(rs.getString("COMPANY_DIV"));
        r.setDeptCd(rs.getString("DEPT_CD"));
        r.setDeptName(rs.getString("DEPT_NAME"));
        r.setApprovalType(rs.getString("APPROVAL_TYPE"));
        r.setRouteName(rs.getString("ROUTE_NAME"));
        r.setValidFrom(dt(rs, "VALID_FROM"));
        r.setValidTo(dt(rs, "VALID_TO"));
        mapAudit(rs, r);
        return r;
    }

    static ApprovalRouteStep mapStep(ResultSet rs) throws SQLException {
        ApprovalRouteStep s = new ApprovalRouteStep();
        s.setRouteId(rs.getLong("ROUTE_ID"));
        s.setStepNo(rs.getInt("STEP_NO"));
        s.setApproverEmployeeId(rs.getLong("APPROVER_EMPLOYEE_ID"));
        s.setApproverName(rs.getString("EMPLOYEE_NAME"));
        s.setApproverDeptCd(rs.getString("APPROVER_DEPT_CD"));
        mapAudit(rs, s);
        return s;
    }

    /** 会社区分・部署・承認種別で、適用期間に当日を含むテンプレートのうち適用開始日が最も新しいもの。 */
    public Optional<ApprovalRoute> findTemplate(Connection conn, String companyDiv, String deptCd, String approvalType, LocalDate today) {
        Optional<ApprovalRoute> r = queryOne(conn, SELECT + " WHERE COMPANY_DIV = ? AND DEPT_CD = ? AND APPROVAL_TYPE = ? AND VALID_FROM <= ? AND (VALID_TO IS NULL OR VALID_TO >= ?) ORDER BY VALID_FROM DESC, ROUTE_ID DESC",
                ApprovalRouteDao::map, companyDiv, deptCd, approvalType, today, today);
        r.ifPresent(route -> route.setSteps(findSteps(conn, route.getRouteId())));
        return r;
    }

    public Optional<ApprovalRoute> findById(Connection conn, long routeId) {
        Optional<ApprovalRoute> r = queryOne(conn, SELECT + " WHERE ROUTE_ID = ?", ApprovalRouteDao::map, routeId);
        r.ifPresent(route -> route.setSteps(findSteps(conn, route.getRouteId())));
        return r;
    }

    public List<ApprovalRoute> findAll(Connection conn) {
        List<ApprovalRoute> list = query(conn, SELECT + " ORDER BY COMPANY_DIV, DEPT_CD, APPROVAL_TYPE, VALID_FROM", ApprovalRouteDao::map);
        for (ApprovalRoute r : list) {
            r.setSteps(findSteps(conn, r.getRouteId()));
        }
        return list;
    }

    public List<ApprovalRouteStep> findSteps(Connection conn, long routeId) {
        return query(conn, SELECT_STEP + " WHERE s.ROUTE_ID = ? ORDER BY s.STEP_NO", ApprovalRouteDao::mapStep, routeId);
    }

    public long insert(Connection conn, ApprovalRoute r) {
        long id = insertAndGetKey(conn, "INSERT INTO M_APPROVAL_ROUTE (COMPANY_DIV, DEPT_CD, APPROVAL_TYPE, ROUTE_NAME, VALID_FROM, VALID_TO, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                r.getCompanyDiv(), r.getDeptCd(), r.getApprovalType(), r.getRouteName(), r.getValidFrom(), r.getValidTo(), now(), actor(), now(), actor());
        insertSteps(conn, id, r.getSteps());
        return id;
    }

    public int update(Connection conn, ApprovalRoute r, int expectedRowVersion) {
        int n = update(conn, "UPDATE M_APPROVAL_ROUTE SET COMPANY_DIV = ?, DEPT_CD = ?, APPROVAL_TYPE = ?, ROUTE_NAME = ?, VALID_FROM = ?, VALID_TO = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE ROUTE_ID = ? AND ROW_VERSION = ?",
                r.getCompanyDiv(), r.getDeptCd(), r.getApprovalType(), r.getRouteName(), r.getValidFrom(), r.getValidTo(), now(), actor(), r.getRouteId(), expectedRowVersion);
        if (n == 1) {
            update(conn, "DELETE FROM M_APPROVAL_ROUTE_STEP WHERE ROUTE_ID = ?", r.getRouteId());
            insertSteps(conn, r.getRouteId(), r.getSteps());
        }
        return n;
    }

    private void insertSteps(Connection conn, long routeId, List<ApprovalRouteStep> steps) {
        int no = 0;
        for (ApprovalRouteStep s : steps) {
            no++;
            update(conn, "INSERT INTO M_APPROVAL_ROUTE_STEP (ROUTE_ID, STEP_NO, APPROVER_EMPLOYEE_ID, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, 1)",
                    routeId, no, s.getApproverEmployeeId(), now(), actor(), now(), actor());
        }
    }

    public void delete(Connection conn, long routeId) {
        update(conn, "DELETE FROM M_APPROVAL_ROUTE_STEP WHERE ROUTE_ID = ?", routeId);
        update(conn, "DELETE FROM M_APPROVAL_ROUTE WHERE ROUTE_ID = ?", routeId);
    }
}
