package com.example.appmgmt.dao;

import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApplicationListRow;
import com.example.appmgmt.domain.Codes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ApplicationDao extends AbstractDao {

    private static final String SELECT = "SELECT APPLICATION_ID, APPLICATION_NO, APPLICANT_ID, OWNER_EMPLOYEE_ID, COMPANY_DIV, DEPT_CD, STATUS_CD, CURRENT_VERSION_NO, BASE_VERSION_NO, REVIEWED_VERSION_NO, "
            + "REGISTRATION_TYPE, SOURCE_APPLICATION_ID, IMPORT_BATCH_ID, REVIEWED_AT, " + AUDIT_COLS + " FROM T_APPLICATION";

    static Application map(ResultSet rs) throws SQLException {
        Application a = new Application();
        a.setApplicationId(rs.getLong("APPLICATION_ID"));
        a.setApplicationNo(rs.getString("APPLICATION_NO"));
        a.setApplicantId(longObj(rs, "APPLICANT_ID"));
        a.setOwnerEmployeeId(rs.getLong("OWNER_EMPLOYEE_ID"));
        a.setCompanyDiv(rs.getString("COMPANY_DIV"));
        a.setDeptCd(rs.getString("DEPT_CD"));
        a.setStatusCd(rs.getString("STATUS_CD"));
        a.setCurrentVersionNo(rs.getInt("CURRENT_VERSION_NO"));
        a.setBaseVersionNo(intObj(rs, "BASE_VERSION_NO"));
        a.setReviewedVersionNo(intObj(rs, "REVIEWED_VERSION_NO"));
        a.setRegistrationType(rs.getString("REGISTRATION_TYPE"));
        a.setSourceApplicationId(longObj(rs, "SOURCE_APPLICATION_ID"));
        a.setImportBatchId(longObj(rs, "IMPORT_BATCH_ID"));
        a.setReviewedAt(ts(rs, "REVIEWED_AT"));
        mapAudit(rs, a);
        return a;
    }

    public Optional<Application> findById(Connection conn, long applicationId) {
        return queryOne(conn, SELECT + " WHERE APPLICATION_ID = ?", ApplicationDao::map, applicationId);
    }

    /** 申込者アカウントに紐づく申込の一覧（更新日時の降順）。申込者ポータルで使う。 */
    public List<Application> findByApplicant(Connection conn, long applicantId) {
        return query(conn, SELECT + " WHERE APPLICANT_ID = ? ORDER BY UPDATED_AT DESC, APPLICATION_ID DESC", ApplicationDao::map, applicantId);
    }

    /** 申込に申込者アカウントを紐づける（F14 後続処理のアカウント発行と同一トランザクション内の付随更新。行バージョンは進めない）。 */
    public void linkAccount(Connection conn, long applicationId, long applicantId) {
        update(conn, "UPDATE T_APPLICATION SET APPLICANT_ID = ?, UPDATED_AT = ?, UPDATED_BY = ? WHERE APPLICATION_ID = ?", applicantId, now(), actor(), applicationId);
    }

    /** 申込者アカウントの最新の申込の申込者名（申込者ポータルの表示名）。 */
    public String latestApplicantName(Connection conn, long applicantId) {
        List<String> names = query(conn, "SELECT v.APPLICANT_NAME FROM T_APPLICATION a JOIN T_APPLICATION_VERSION v ON v.APPLICATION_ID = a.APPLICATION_ID AND v.VERSION_NO = a.CURRENT_VERSION_NO "
                + "WHERE a.APPLICANT_ID = ? ORDER BY a.UPDATED_AT DESC, a.APPLICATION_ID DESC OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY", rs -> rs.getString("APPLICANT_NAME"), applicantId);
        return names.isEmpty() ? "" : names.get(0);
    }

    /** 申込番号を採番する（AP ＋ 10 桁ゼロ埋め）。 */
    public String nextApplicationNo(Connection conn) {
        long seq = queryLong(conn, "SELECT NEXT VALUE FOR SEQ_APPLICATION_NO");
        return String.format("AP%010d", seq);
    }

    public long insert(Connection conn, Application a) {
        return insertAndGetKey(conn, "INSERT INTO T_APPLICATION (APPLICATION_NO, APPLICANT_ID, OWNER_EMPLOYEE_ID, COMPANY_DIV, DEPT_CD, STATUS_CD, CURRENT_VERSION_NO, BASE_VERSION_NO, REVIEWED_VERSION_NO, "
                + "REGISTRATION_TYPE, SOURCE_APPLICATION_ID, IMPORT_BATCH_ID, REVIEWED_AT, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                a.getApplicationNo(), a.getApplicantId(), a.getOwnerEmployeeId(), a.getCompanyDiv(), a.getDeptCd(), a.getStatusCd(), a.getCurrentVersionNo(), a.getBaseVersionNo(), a.getReviewedVersionNo(),
                a.getRegistrationType(), a.getSourceApplicationId(), a.getImportBatchId(), a.getReviewedAt(), now(), actor(), now(), actor());
    }

    /** ステータス更新（F14）。行バージョン一致で更新し、更新件数を返す。 */
    public int updateStatus(Connection conn, long applicationId, String newStatusCd, int expectedRowVersion) {
        return update(conn, "UPDATE T_APPLICATION SET STATUS_CD = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE APPLICATION_ID = ? AND ROW_VERSION = ?",
                newStatusCd, now(), actor(), applicationId, expectedRowVersion);
    }

    /** 版番号類の更新（F14 後続処理や呼出元の版作成）。行バージョンは +1 しない（同一トランザクション内の付随更新）。 */
    public void updateVersions(Connection conn, Application a) {
        update(conn, "UPDATE T_APPLICATION SET CURRENT_VERSION_NO = ?, BASE_VERSION_NO = ?, REVIEWED_VERSION_NO = ?, REVIEWED_AT = ?, UPDATED_AT = ?, UPDATED_BY = ? WHERE APPLICATION_ID = ?",
                a.getCurrentVersionNo(), a.getBaseVersionNo(), a.getReviewedVersionNo(), a.getReviewedAt(), now(), actor(), a.getApplicationId());
    }

    /** 担当（会社区分・部署・担当社員）の更新。申込入力（SC04、入力中）の保存と同じトランザクションで行い、行バージョンは touch で進める。 */
    public void updateAssignment(Connection conn, long applicationId, String companyDiv, String deptCd, long ownerEmployeeId) {
        update(conn, "UPDATE T_APPLICATION SET COMPANY_DIV = ?, DEPT_CD = ?, OWNER_EMPLOYEE_ID = ?, UPDATED_AT = ?, UPDATED_BY = ? WHERE APPLICATION_ID = ?",
                companyDiv, deptCd, ownerEmployeeId, now(), actor(), applicationId);
    }

    /** 申込内容の一時保存など、ステータスを変えない更新で行バージョンだけ進める。 */
    public int touch(Connection conn, long applicationId, int expectedRowVersion) {
        return update(conn, "UPDATE T_APPLICATION SET UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE APPLICATION_ID = ? AND ROW_VERSION = ?",
                now(), actor(), applicationId, expectedRowVersion);
    }

    /** 検索条件（SC02）。 */
    public static class Criteria {
        public String applicationNo;
        public String applicantKey;
        public List<String> statusCds = new ArrayList<>();
        public Long ownerEmployeeId;
        public LocalDate createdFrom;
        public LocalDate createdTo;
        public boolean myTasksOnly;
        public String scopeRole;
        public long loginEmployeeId;
        public String loginCompanyDiv;
        public String loginDeptCd;
        public String sort = "updatedAt";
        public boolean desc = true;
        public int page = 1;
        public int pageSize = 20;

        public String getApplicationNo() { return applicationNo; }
        public String getApplicantKey() { return applicantKey; }
        public List<String> getStatusCds() { return statusCds; }
        public Long getOwnerEmployeeId() { return ownerEmployeeId; }
        public LocalDate getCreatedFrom() { return createdFrom; }
        public LocalDate getCreatedTo() { return createdTo; }
        public boolean isMyTasksOnly() { return myTasksOnly; }
        public String getSort() { return sort; }
        public boolean isDesc() { return desc; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
    }

    // 申込者名は申込データ（現行版）から、申込者番号は申込者アカウント（未発行なら NULL）から取る
    private static final String LIST_FROM = " FROM T_APPLICATION a LEFT JOIN M_APPLICANT_ACCOUNT p ON p.APPLICANT_ID = a.APPLICANT_ID JOIN M_EMPLOYEE e ON e.EMPLOYEE_ID = a.OWNER_EMPLOYEE_ID "
            + "JOIN M_STATUS s ON s.STATUS_CD = a.STATUS_CD JOIN T_APPLICATION_VERSION v ON v.APPLICATION_ID = a.APPLICATION_ID AND v.VERSION_NO = a.CURRENT_VERSION_NO";

    private void buildWhere(Criteria c, StringBuilder sql, List<Object> params) {
        sql.append(" WHERE 1 = 1");
        if (c.applicationNo != null && !c.applicationNo.isBlank()) {
            sql.append(" AND a.APPLICATION_NO LIKE ?");
            params.add(escapeLike(c.applicationNo.trim()) + "%");
        }
        if (c.applicantKey != null && !c.applicantKey.isBlank()) {
            sql.append(" AND (p.APPLICANT_NO = ? OR v.APPLICANT_NAME LIKE ?)");
            params.add(c.applicantKey.trim());
            params.add("%" + escapeLike(c.applicantKey.trim()) + "%");
        }
        if (c.statusCds != null && !c.statusCds.isEmpty()) {
            sql.append(" AND a.STATUS_CD IN (");
            for (int i = 0; i < c.statusCds.size(); i++) {
                sql.append(i == 0 ? "?" : ", ?");
                params.add(c.statusCds.get(i));
            }
            sql.append(")");
        }
        if (c.ownerEmployeeId != null) {
            sql.append(" AND a.OWNER_EMPLOYEE_ID = ?");
            params.add(c.ownerEmployeeId);
        }
        if (c.createdFrom != null) {
            sql.append(" AND a.CREATED_AT >= ?");
            params.add(c.createdFrom.atStartOfDay());
        }
        if (c.createdTo != null) {
            sql.append(" AND a.CREATED_AT < ?");
            params.add(c.createdTo.plusDays(1).atStartOfDay());
        }
        // 参照範囲（11. 1.2 節）
        if (Codes.ROLE_OWNER.equals(c.scopeRole)) {
            sql.append(" AND a.OWNER_EMPLOYEE_ID = ?");
            params.add(c.loginEmployeeId);
        } else if (Codes.ROLE_APPROVER.equals(c.scopeRole)) {
            sql.append(" AND ((a.COMPANY_DIV = ? AND a.DEPT_CD = ?) OR EXISTS (SELECT 1 FROM T_APPROVAL_REQUEST r JOIN T_APPROVAL_STEP st ON st.APPROVAL_REQUEST_ID = r.APPROVAL_REQUEST_ID "
                    + "WHERE r.APPLICATION_ID = a.APPLICATION_ID AND st.APPROVER_EMPLOYEE_ID = ?))");
            params.add(c.loginCompanyDiv);
            params.add(c.loginDeptCd);
            params.add(c.loginEmployeeId);
        }
        // 自分の操作待ち
        if (c.myTasksOnly) {
            if (Codes.ROLE_OWNER.equals(c.scopeRole)) {
                sql.append(" AND a.OWNER_EMPLOYEE_ID = ? AND s.ACTOR_TYPE = '1'");
                params.add(c.loginEmployeeId);
            } else if (Codes.ROLE_APPROVER.equals(c.scopeRole)) {
                sql.append(" AND EXISTS (SELECT 1 FROM T_APPROVAL_REQUEST r JOIN T_APPROVAL_STEP st ON st.APPROVAL_REQUEST_ID = r.APPROVAL_REQUEST_ID AND st.STEP_NO = r.CURRENT_STEP_NO "
                        + "WHERE r.APPLICATION_ID = a.APPLICATION_ID AND r.REQUEST_STATUS = '1' AND st.APPROVER_EMPLOYEE_ID = ?)");
                params.add(c.loginEmployeeId);
            }
        }
    }

    public long count(Connection conn, Criteria c) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*)").append(LIST_FROM);
        List<Object> params = new ArrayList<>();
        buildWhere(c, sql, params);
        return queryLong(conn, sql.toString(), params.toArray());
    }

    public List<ApplicationListRow> search(Connection conn, Criteria c) {
        StringBuilder sql = new StringBuilder("SELECT a.APPLICATION_ID, a.APPLICATION_NO, p.APPLICANT_NO, v.APPLICANT_NAME, a.STATUS_CD, s.STATUS_NAME, v.TOTAL_AMOUNT, e.EMPLOYEE_NAME, a.REGISTRATION_TYPE, a.UPDATED_AT")
                .append(LIST_FROM);
        List<Object> params = new ArrayList<>();
        buildWhere(c, sql, params);
        String order;
        switch (c.sort == null ? "" : c.sort) {
            case "applicationNo": order = "a.APPLICATION_NO"; break;
            case "statusCd": order = "a.STATUS_CD"; break;
            case "totalAmount": order = "v.TOTAL_AMOUNT"; break;
            case "applicantName": order = "v.APPLICANT_NAME"; break;
            default: order = "a.UPDATED_AT"; break;
        }
        sql.append(" ORDER BY ").append(order).append(c.desc ? " DESC" : " ASC").append(", a.APPLICATION_ID DESC");
        sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add((Math.max(c.page, 1) - 1) * c.pageSize);
        params.add(c.pageSize);
        return query(conn, sql.toString(), rs -> {
            ApplicationListRow r = new ApplicationListRow();
            r.setApplicationId(rs.getLong("APPLICATION_ID"));
            r.setApplicationNo(rs.getString("APPLICATION_NO"));
            r.setApplicantNo(rs.getString("APPLICANT_NO"));
            r.setApplicantName(rs.getString("APPLICANT_NAME"));
            r.setStatusCd(rs.getString("STATUS_CD"));
            r.setStatusName(rs.getString("STATUS_NAME"));
            r.setTotalAmount(rs.getBigDecimal("TOTAL_AMOUNT"));
            r.setOwnerName(rs.getString("EMPLOYEE_NAME"));
            r.setRegistrationType(rs.getString("REGISTRATION_TYPE"));
            r.setUpdatedAt(ts(rs, "UPDATED_AT"));
            return r;
        }, params.toArray());
    }

    private static String escapeLike(String s) {
        return s.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }

    public boolean existsForApprover(Connection conn, long applicationId, long employeeId) {
        return queryLong(conn, "SELECT COUNT(*) FROM T_APPROVAL_REQUEST r JOIN T_APPROVAL_STEP st ON st.APPROVAL_REQUEST_ID = r.APPROVAL_REQUEST_ID WHERE r.APPLICATION_ID = ? AND st.APPROVER_EMPLOYEE_ID = ?",
                applicationId, employeeId) > 0;
    }

    public LocalDateTime currentTime() {
        return now();
    }
}
