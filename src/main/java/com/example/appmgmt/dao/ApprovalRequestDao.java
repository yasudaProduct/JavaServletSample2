package com.example.appmgmt.dao;

import com.example.appmgmt.domain.ApprovalRequest;
import com.example.appmgmt.domain.ApprovalStep;
import com.example.appmgmt.domain.Codes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ApprovalRequestDao extends AbstractDao {

    private static final String SELECT = "SELECT r.APPROVAL_REQUEST_ID, r.APPLICATION_ID, r.VERSION_NO, r.APPROVAL_TYPE, r.ROUTE_ID, m.ROUTE_NAME, r.REQUEST_EMPLOYEE_ID, e.EMPLOYEE_NAME, r.REQUESTED_AT, r.REQUEST_STATUS, "
            + "r.CURRENT_STEP_NO, r.FINAL_STEP_NO, r.COMPLETED_AT, r.CREATED_AT, r.CREATED_BY, r.UPDATED_AT, r.UPDATED_BY, r.ROW_VERSION "
            + "FROM T_APPROVAL_REQUEST r JOIN M_EMPLOYEE e ON e.EMPLOYEE_ID = r.REQUEST_EMPLOYEE_ID LEFT JOIN M_APPROVAL_ROUTE m ON m.ROUTE_ID = r.ROUTE_ID";
    private static final String SELECT_STEP = "SELECT s.APPROVAL_REQUEST_ID, s.STEP_NO, s.APPROVER_EMPLOYEE_ID, e.EMPLOYEE_NAME, s.RESULT_CD, s.COMMENT, s.ACTED_AT, s.CREATED_AT, s.CREATED_BY, s.UPDATED_AT, s.UPDATED_BY, s.ROW_VERSION "
            + "FROM T_APPROVAL_STEP s JOIN M_EMPLOYEE e ON e.EMPLOYEE_ID = s.APPROVER_EMPLOYEE_ID";

    static ApprovalRequest map(ResultSet rs) throws SQLException {
        ApprovalRequest r = new ApprovalRequest();
        r.setApprovalRequestId(rs.getLong("APPROVAL_REQUEST_ID"));
        r.setApplicationId(rs.getLong("APPLICATION_ID"));
        r.setVersionNo(rs.getInt("VERSION_NO"));
        r.setApprovalType(rs.getString("APPROVAL_TYPE"));
        r.setRouteId(longObj(rs, "ROUTE_ID"));
        r.setRouteName(rs.getString("ROUTE_NAME"));
        r.setRequestEmployeeId(rs.getLong("REQUEST_EMPLOYEE_ID"));
        r.setRequestEmployeeName(rs.getString("EMPLOYEE_NAME"));
        r.setRequestedAt(ts(rs, "REQUESTED_AT"));
        r.setRequestStatus(rs.getString("REQUEST_STATUS"));
        r.setCurrentStepNo(intObj(rs, "CURRENT_STEP_NO"));
        r.setFinalStepNo(intObj(rs, "FINAL_STEP_NO"));
        r.setCompletedAt(ts(rs, "COMPLETED_AT"));
        mapAudit(rs, r);
        return r;
    }

    static ApprovalStep mapStep(ResultSet rs) throws SQLException {
        ApprovalStep s = new ApprovalStep();
        s.setApprovalRequestId(rs.getLong("APPROVAL_REQUEST_ID"));
        s.setStepNo(rs.getInt("STEP_NO"));
        s.setApproverEmployeeId(rs.getLong("APPROVER_EMPLOYEE_ID"));
        s.setApproverName(rs.getString("EMPLOYEE_NAME"));
        s.setResultCd(rs.getString("RESULT_CD"));
        s.setComment(rs.getString("COMMENT"));
        s.setActedAt(ts(rs, "ACTED_AT"));
        mapAudit(rs, s);
        return s;
    }

    public Optional<ApprovalRequest> findById(Connection conn, long id) {
        Optional<ApprovalRequest> r = queryOne(conn, SELECT + " WHERE r.APPROVAL_REQUEST_ID = ?", ApprovalRequestDao::map, id);
        r.ifPresent(x -> x.setSteps(findSteps(conn, x.getApprovalRequestId())));
        return r;
    }

    /** 進行中（申請状態 1）の承認申請。2 件以上は不正。 */
    public Optional<ApprovalRequest> findActive(Connection conn, long applicationId) {
        List<ApprovalRequest> list = query(conn, SELECT + " WHERE r.APPLICATION_ID = ? AND r.REQUEST_STATUS = ? ORDER BY r.APPROVAL_REQUEST_ID DESC", ApprovalRequestDao::map, applicationId, Codes.REQUEST_IN_PROGRESS);
        if (list.size() > 1) {
            throw new IllegalStateException("進行中の承認申請が複数あります: applicationId=" + applicationId);
        }
        if (list.isEmpty()) {
            return Optional.empty();
        }
        ApprovalRequest r = list.get(0);
        r.setSteps(findSteps(conn, r.getApprovalRequestId()));
        return Optional.of(r);
    }

    /** 申込の承認申請一覧（申請日時の降順、明細付き）。 */
    public List<ApprovalRequest> findByApplication(Connection conn, long applicationId) {
        List<ApprovalRequest> list = query(conn, SELECT + " WHERE r.APPLICATION_ID = ? ORDER BY r.REQUESTED_AT DESC, r.APPROVAL_REQUEST_ID DESC", ApprovalRequestDao::map, applicationId);
        for (ApprovalRequest r : list) {
            r.setSteps(findSteps(conn, r.getApprovalRequestId()));
        }
        return list;
    }

    public List<ApprovalStep> findSteps(Connection conn, long requestId) {
        return query(conn, SELECT_STEP + " WHERE s.APPROVAL_REQUEST_ID = ? ORDER BY s.STEP_NO", ApprovalRequestDao::mapStep, requestId);
    }

    public long insert(Connection conn, ApprovalRequest r) {
        long id = insertAndGetKey(conn, "INSERT INTO T_APPROVAL_REQUEST (APPLICATION_ID, VERSION_NO, APPROVAL_TYPE, ROUTE_ID, REQUEST_EMPLOYEE_ID, REQUESTED_AT, REQUEST_STATUS, CURRENT_STEP_NO, FINAL_STEP_NO, COMPLETED_AT, "
                + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                r.getApplicationId(), r.getVersionNo(), r.getApprovalType(), r.getRouteId(), r.getRequestEmployeeId(), r.getRequestedAt(), r.getRequestStatus(), r.getCurrentStepNo(), r.getFinalStepNo(), r.getCompletedAt(),
                now(), actor(), now(), actor());
        for (ApprovalStep s : r.getSteps()) {
            update(conn, "INSERT INTO T_APPROVAL_STEP (APPROVAL_REQUEST_ID, STEP_NO, APPROVER_EMPLOYEE_ID, RESULT_CD, COMMENT, ACTED_AT, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                    id, s.getStepNo(), s.getApproverEmployeeId(), Codes.RESULT_PENDING, null, null, now(), actor(), now(), actor());
        }
        return id;
    }

    public void updateProgress(Connection conn, long requestId, String requestStatus, Integer currentStepNo, LocalDateTime completedAt) {
        update(conn, "UPDATE T_APPROVAL_REQUEST SET REQUEST_STATUS = ?, CURRENT_STEP_NO = ?, COMPLETED_AT = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE APPROVAL_REQUEST_ID = ?",
                requestStatus, currentStepNo, completedAt, now(), actor(), requestId);
    }

    public void updateStepResult(Connection conn, long requestId, int stepNo, String resultCd, String comment, LocalDateTime actedAt) {
        update(conn, "UPDATE T_APPROVAL_STEP SET RESULT_CD = ?, COMMENT = ?, ACTED_AT = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE APPROVAL_REQUEST_ID = ? AND STEP_NO = ?",
                resultCd, truncate(comment, 500), actedAt, now(), actor(), requestId, stepNo);
    }
}
