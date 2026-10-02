package com.example.appmgmt.dao;

import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.ExternalLink;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ExternalLinkDao extends AbstractDao {

    private static final String SELECT = "SELECT l.EXTERNAL_LINK_ID, l.APPLICATION_ID, l.VERSION_NO, l.LINK_TYPE, l.SEND_STATUS, l.RETRY_COUNT, l.SENT_AT, l.ERROR_MESSAGE, l.EXTERNAL_RECEIPT_NO, l.RESULT_CD, l.RESULT_RECEIVED_AT, l.RESULT_REASON, "
            + "a.APPLICATION_NO, l.CREATED_AT, l.CREATED_BY, l.UPDATED_AT, l.UPDATED_BY, l.ROW_VERSION FROM T_EXTERNAL_LINK l JOIN T_APPLICATION a ON a.APPLICATION_ID = l.APPLICATION_ID";

    static ExternalLink map(ResultSet rs) throws SQLException {
        ExternalLink l = new ExternalLink();
        l.setExternalLinkId(rs.getLong("EXTERNAL_LINK_ID"));
        l.setApplicationId(rs.getLong("APPLICATION_ID"));
        l.setVersionNo(rs.getInt("VERSION_NO"));
        l.setLinkType(rs.getString("LINK_TYPE"));
        l.setSendStatus(rs.getString("SEND_STATUS"));
        l.setRetryCount(rs.getInt("RETRY_COUNT"));
        l.setSentAt(ts(rs, "SENT_AT"));
        l.setErrorMessage(rs.getString("ERROR_MESSAGE"));
        l.setExternalReceiptNo(rs.getString("EXTERNAL_RECEIPT_NO"));
        l.setResultCd(rs.getString("RESULT_CD"));
        l.setResultReceivedAt(ts(rs, "RESULT_RECEIVED_AT"));
        l.setResultReason(rs.getString("RESULT_REASON"));
        l.setApplicationNo(rs.getString("APPLICATION_NO"));
        mapAudit(rs, l);
        return l;
    }

    public Optional<ExternalLink> findById(Connection conn, long id) {
        return queryOne(conn, SELECT + " WHERE l.EXTERNAL_LINK_ID = ?", ExternalLinkDao::map, id);
    }

    public Optional<ExternalLink> findByReceiptNo(Connection conn, String receiptNo) {
        return queryOne(conn, SELECT + " WHERE l.EXTERNAL_RECEIPT_NO = ?", ExternalLinkDao::map, receiptNo);
    }

    public List<ExternalLink> findByApplication(Connection conn, long applicationId) {
        return query(conn, SELECT + " WHERE l.APPLICATION_ID = ? ORDER BY l.EXTERNAL_LINK_ID DESC", ExternalLinkDao::map, applicationId);
    }

    public List<ExternalLink> findPending(Connection conn, int limit) {
        return query(conn, SELECT + " WHERE l.SEND_STATUS = ? ORDER BY l.EXTERNAL_LINK_ID OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY", ExternalLinkDao::map, Codes.SEND_PENDING, limit);
    }

    /** 送信済で結果未受信（開発用モック画面で結果を返す対象）。 */
    public List<ExternalLink> findAwaitingResult(Connection conn, int limit) {
        return query(conn, SELECT + " WHERE l.SEND_STATUS = ? AND l.RESULT_CD IS NULL ORDER BY l.EXTERNAL_LINK_ID DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY", ExternalLinkDao::map, Codes.SEND_SENT, limit);
    }

    public List<ExternalLink> findRecent(Connection conn, int limit) {
        return query(conn, SELECT + " ORDER BY l.EXTERNAL_LINK_ID DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY", ExternalLinkDao::map, limit);
    }

    public long insert(Connection conn, long applicationId, int versionNo, String linkType) {
        return insertAndGetKey(conn, "INSERT INTO T_EXTERNAL_LINK (APPLICATION_ID, VERSION_NO, LINK_TYPE, SEND_STATUS, RETRY_COUNT, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, 1)",
                applicationId, versionNo, linkType, Codes.SEND_PENDING, now(), actor(), now(), actor());
    }

    /** 送信成功。未送信かつ行バージョン一致の行だけ更新する（13. 2.4 節）。 */
    public int markSent(Connection conn, long id, int expectedRowVersion, String receiptNo, LocalDateTime sentAt) {
        return update(conn, "UPDATE T_EXTERNAL_LINK SET SEND_STATUS = ?, SENT_AT = ?, EXTERNAL_RECEIPT_NO = ?, ERROR_MESSAGE = NULL, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE EXTERNAL_LINK_ID = ? AND SEND_STATUS = ? AND ROW_VERSION = ?",
                Codes.SEND_SENT, sentAt, receiptNo, now(), actor(), id, Codes.SEND_PENDING, expectedRowVersion);
    }

    /** 送信失敗。再送回数を加算し、確定なら送信エラーにする。 */
    public int markFailed(Connection conn, long id, int expectedRowVersion, boolean fixed, String errorMessage, boolean countRetry) {
        return update(conn, "UPDATE T_EXTERNAL_LINK SET SEND_STATUS = ?, RETRY_COUNT = RETRY_COUNT + ?, ERROR_MESSAGE = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE EXTERNAL_LINK_ID = ? AND SEND_STATUS = ? AND ROW_VERSION = ?",
                fixed ? Codes.SEND_ERROR : Codes.SEND_PENDING, countRetry ? 1 : 0, truncate(errorMessage, 1000), now(), actor(), id, Codes.SEND_PENDING, expectedRowVersion);
    }

    public void updateResult(Connection conn, long id, String resultCd, LocalDateTime receivedAt, String reason) {
        update(conn, "UPDATE T_EXTERNAL_LINK SET RESULT_CD = ?, RESULT_RECEIVED_AT = ?, RESULT_REASON = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE EXTERNAL_LINK_ID = ?",
                resultCd, receivedAt, truncate(reason, 1000), now(), actor(), id);
    }

    /** 現行版の送信エラー行を未送信に戻す（SC03 外部連携再送）。 */
    public int resetForResend(Connection conn, long applicationId, int versionNo) {
        return update(conn, "UPDATE T_EXTERNAL_LINK SET SEND_STATUS = ?, RETRY_COUNT = 0, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE APPLICATION_ID = ? AND VERSION_NO = ? AND SEND_STATUS = ?",
                Codes.SEND_PENDING, now(), actor(), applicationId, versionNo, Codes.SEND_ERROR);
    }

    public boolean existsReceiptNo(Connection conn, String receiptNo, long excludeId) {
        return queryLong(conn, "SELECT COUNT(*) FROM T_EXTERNAL_LINK WHERE EXTERNAL_RECEIPT_NO = ? AND EXTERNAL_LINK_ID <> ?", receiptNo, excludeId) > 0;
    }
}
