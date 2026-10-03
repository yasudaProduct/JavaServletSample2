package com.example.appmgmt.dao;

import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.Notification;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class NotificationDao extends AbstractDao {

    private static final String SELECT = "SELECT n.NOTIFICATION_ID, n.NOTIFICATION_TYPE, n.APPLICATION_ID, n.TO_ADDRESS, n.SUBJECT, n.BODY, n.SEND_STATUS, n.RETRY_COUNT, n.SENT_AT, n.ERROR_MESSAGE, a.APPLICATION_NO, "
            + "n.CREATED_AT, n.CREATED_BY, n.UPDATED_AT, n.UPDATED_BY, n.ROW_VERSION FROM T_NOTIFICATION n JOIN T_APPLICATION a ON a.APPLICATION_ID = n.APPLICATION_ID";

    static Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getLong("NOTIFICATION_ID"));
        n.setNotificationType(rs.getString("NOTIFICATION_TYPE"));
        n.setApplicationId(rs.getLong("APPLICATION_ID"));
        n.setToAddress(rs.getString("TO_ADDRESS"));
        n.setSubject(rs.getString("SUBJECT"));
        n.setBody(rs.getString("BODY"));
        n.setSendStatus(rs.getString("SEND_STATUS"));
        n.setRetryCount(rs.getInt("RETRY_COUNT"));
        n.setSentAt(ts(rs, "SENT_AT"));
        n.setErrorMessage(rs.getString("ERROR_MESSAGE"));
        n.setApplicationNo(rs.getString("APPLICATION_NO"));
        mapAudit(rs, n);
        return n;
    }

    public long insert(Connection conn, Notification n) {
        return insertAndGetKey(conn, "INSERT INTO T_NOTIFICATION (NOTIFICATION_TYPE, APPLICATION_ID, TO_ADDRESS, SUBJECT, BODY, SEND_STATUS, RETRY_COUNT, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, 1)",
                n.getNotificationType(), n.getApplicationId(), n.getToAddress(), truncate(n.getSubject(), 200), truncate(n.getBody(), 4000), Codes.SEND_PENDING, now(), actor(), now(), actor());
    }

    public List<Notification> findPending(Connection conn, int limit) {
        return query(conn, SELECT + " WHERE n.SEND_STATUS = ? ORDER BY n.CREATED_AT, n.NOTIFICATION_ID OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY", NotificationDao::map, Codes.SEND_PENDING, limit);
    }

    public List<Notification> findRecent(Connection conn, int limit) {
        return query(conn, SELECT + " ORDER BY n.NOTIFICATION_ID DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY", NotificationDao::map, limit);
    }

    /** 申込者宛の通知（申込者確認依頼、申込者アカウント通知）。申込者ポータルの「お知らせ」に使う。 */
    public List<Notification> findForApplicant(Connection conn, long applicantId, int limit) {
        return query(conn, SELECT + " WHERE a.APPLICANT_ID = ? AND n.NOTIFICATION_TYPE IN (?, ?, ?) ORDER BY n.NOTIFICATION_ID DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY",
                NotificationDao::map, applicantId, Codes.NOTIFY_CONSENT_REQUEST, Codes.NOTIFY_APPLICANT_ACCOUNT, Codes.NOTIFY_PASSWORD_RESET, limit);
    }

    /** この申込の申込者宛の通知（確認依頼、アカウント通知、パスワード初期化通知）。メンテナンス画面（SC15）の履歴に使う。 */
    public List<Notification> findApplicantNoticesOfApplication(Connection conn, long applicationId) {
        return query(conn, SELECT + " WHERE n.APPLICATION_ID = ? AND n.NOTIFICATION_TYPE IN (?, ?, ?) ORDER BY n.NOTIFICATION_ID DESC",
                NotificationDao::map, applicationId, Codes.NOTIFY_CONSENT_REQUEST, Codes.NOTIFY_APPLICANT_ACCOUNT, Codes.NOTIFY_PASSWORD_RESET);
    }

    public List<Notification> findByApplication(Connection conn, long applicationId) {
        return query(conn, SELECT + " WHERE n.APPLICATION_ID = ? ORDER BY n.NOTIFICATION_ID DESC", NotificationDao::map, applicationId);
    }

    public int markSent(Connection conn, long id, int expectedRowVersion, LocalDateTime sentAt) {
        return update(conn, "UPDATE T_NOTIFICATION SET SEND_STATUS = ?, SENT_AT = ?, ERROR_MESSAGE = NULL, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE NOTIFICATION_ID = ? AND SEND_STATUS = ? AND ROW_VERSION = ?",
                Codes.SEND_SENT, sentAt, now(), actor(), id, Codes.SEND_PENDING, expectedRowVersion);
    }

    public int markFailed(Connection conn, long id, int expectedRowVersion, boolean fixed, String errorMessage) {
        return update(conn, "UPDATE T_NOTIFICATION SET SEND_STATUS = ?, RETRY_COUNT = RETRY_COUNT + 1, ERROR_MESSAGE = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE NOTIFICATION_ID = ? AND SEND_STATUS = ? AND ROW_VERSION = ?",
                fixed ? Codes.SEND_ERROR : Codes.SEND_PENDING, truncate(errorMessage, 1000), now(), actor(), id, Codes.SEND_PENDING, expectedRowVersion);
    }
}
