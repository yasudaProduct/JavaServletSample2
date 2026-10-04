package com.example.appmgmt.dao;

import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.Employee;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class EmployeeDao extends AbstractDao {

    private static final String SELECT = "SELECT EMPLOYEE_ID, EMPLOYEE_NO, EMPLOYEE_NAME, PASSWORD_HASH, COMPANY_DIV, DEPT_CD, ROLE_CD, MAIL_ADDRESS, VALID_FLG, "
            + "(SELECT d.DEPT_NAME FROM M_DEPARTMENT d WHERE d.COMPANY_DIV = M_EMPLOYEE.COMPANY_DIV AND d.DEPT_CD = M_EMPLOYEE.DEPT_CD) AS DEPT_NAME, " + AUDIT_COLS + " FROM M_EMPLOYEE";

    static Employee map(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setEmployeeId(rs.getLong("EMPLOYEE_ID"));
        e.setEmployeeNo(rs.getString("EMPLOYEE_NO"));
        e.setEmployeeName(rs.getString("EMPLOYEE_NAME"));
        e.setPasswordHash(rs.getString("PASSWORD_HASH"));
        e.setCompanyDiv(rs.getString("COMPANY_DIV"));
        e.setDeptCd(rs.getString("DEPT_CD"));
        e.setDeptName(rs.getString("DEPT_NAME"));
        e.setRoleCd(rs.getString("ROLE_CD"));
        e.setMailAddress(rs.getString("MAIL_ADDRESS"));
        e.setValidFlg(rs.getString("VALID_FLG"));
        mapAudit(rs, e);
        return e;
    }

    public Optional<Employee> findByNo(Connection conn, String employeeNo) {
        return queryOne(conn, SELECT + " WHERE EMPLOYEE_NO = ?", EmployeeDao::map, employeeNo);
    }

    public Optional<Employee> findById(Connection conn, long employeeId) {
        return queryOne(conn, SELECT + " WHERE EMPLOYEE_ID = ?", EmployeeDao::map, employeeId);
    }

    public List<Employee> findAll(Connection conn) {
        return query(conn, SELECT + " ORDER BY EMPLOYEE_NO", EmployeeDao::map);
    }

    public List<Employee> findValid(Connection conn) {
        return query(conn, SELECT + " WHERE VALID_FLG = '1' ORDER BY EMPLOYEE_NO", EmployeeDao::map);
    }

    /** 承認者候補：有効、権限 02、同じ会社区分。 */
    public List<Employee> findApproverCandidates(Connection conn, String companyDiv) {
        return query(conn, SELECT + " WHERE VALID_FLG = '1' AND ROLE_CD = ? AND COMPANY_DIV = ? ORDER BY EMPLOYEE_NO", EmployeeDao::map, Codes.ROLE_APPROVER, companyDiv);
    }

    public List<Employee> findValidAdmins(Connection conn) {
        return query(conn, SELECT + " WHERE VALID_FLG = '1' AND ROLE_CD = ? ORDER BY EMPLOYEE_NO", EmployeeDao::map, Codes.ROLE_ADMIN);
    }

    public long insert(Connection conn, Employee e) {
        return insertAndGetKey(conn, "INSERT INTO M_EMPLOYEE (EMPLOYEE_NO, EMPLOYEE_NAME, PASSWORD_HASH, COMPANY_DIV, DEPT_CD, ROLE_CD, MAIL_ADDRESS, VALID_FLG, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                e.getEmployeeNo(), e.getEmployeeName(), e.getPasswordHash(), e.getCompanyDiv(), e.getDeptCd(), e.getRoleCd(), e.getMailAddress(), e.getValidFlg(), now(), actor(), now(), actor());
    }

    public int update(Connection conn, Employee e, int expectedRowVersion) {
        return update(conn, "UPDATE M_EMPLOYEE SET EMPLOYEE_NAME = ?, PASSWORD_HASH = ?, COMPANY_DIV = ?, DEPT_CD = ?, ROLE_CD = ?, MAIL_ADDRESS = ?, VALID_FLG = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE EMPLOYEE_ID = ? AND ROW_VERSION = ?",
                e.getEmployeeName(), e.getPasswordHash(), e.getCompanyDiv(), e.getDeptCd(), e.getRoleCd(), e.getMailAddress(), e.getValidFlg(), now(), actor(), e.getEmployeeId(), expectedRowVersion);
    }
}
