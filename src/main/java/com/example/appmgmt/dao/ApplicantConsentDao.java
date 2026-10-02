package com.example.appmgmt.dao;

import com.example.appmgmt.domain.ApplicantConsent;
import com.example.appmgmt.domain.Codes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ApplicantConsentDao extends AbstractDao {

    private static final String SELECT = "SELECT CONSENT_ID, APPLICATION_ID, VERSION_NO, CONSENT_TYPE, ACCESS_TOKEN_HASH, TOKEN_EXPIRES_AT, CONSENT_STATUS, CONTENT_CONFIRMED_AT, CONSENTED_AT, RETURNED_AT, RETURN_REASON, CLIENT_IP, "
            + AUDIT_COLS + " FROM T_APPLICANT_CONSENT";

    static ApplicantConsent map(ResultSet rs) throws SQLException {
        ApplicantConsent c = new ApplicantConsent();
        c.setConsentId(rs.getLong("CONSENT_ID"));
        c.setApplicationId(rs.getLong("APPLICATION_ID"));
        c.setVersionNo(rs.getInt("VERSION_NO"));
        c.setConsentType(rs.getString("CONSENT_TYPE"));
        c.setAccessTokenHash(rs.getString("ACCESS_TOKEN_HASH"));
        c.setTokenExpiresAt(ts(rs, "TOKEN_EXPIRES_AT"));
        c.setConsentStatus(rs.getString("CONSENT_STATUS"));
        c.setContentConfirmedAt(ts(rs, "CONTENT_CONFIRMED_AT"));
        c.setConsentedAt(ts(rs, "CONSENTED_AT"));
        c.setReturnedAt(ts(rs, "RETURNED_AT"));
        c.setReturnReason(rs.getString("RETURN_REASON"));
        c.setClientIp(rs.getString("CLIENT_IP"));
        mapAudit(rs, c);
        return c;
    }

    public Optional<ApplicantConsent> findByTokenHash(Connection conn, String hash) {
        return queryOne(conn, SELECT + " WHERE ACCESS_TOKEN_HASH = ?", ApplicantConsentDao::map, hash);
    }

    public Optional<ApplicantConsent> findById(Connection conn, long consentId) {
        return queryOne(conn, SELECT + " WHERE CONSENT_ID = ?", ApplicantConsentDao::map, consentId);
    }

    /** 進行中（確認依頼中）の申込者同意。 */
    public Optional<ApplicantConsent> findActive(Connection conn, long applicationId) {
        return queryOne(conn, SELECT + " WHERE APPLICATION_ID = ? AND CONSENT_STATUS = ? ORDER BY CONSENT_ID DESC", ApplicantConsentDao::map, applicationId, Codes.CONSENT_REQUESTED);
    }

    public List<ApplicantConsent> findByApplication(Connection conn, long applicationId) {
        return query(conn, SELECT + " WHERE APPLICATION_ID = ? ORDER BY CONSENT_ID DESC", ApplicantConsentDao::map, applicationId);
    }

    public long insert(Connection conn, ApplicantConsent c) {
        return insertAndGetKey(conn, "INSERT INTO T_APPLICANT_CONSENT (APPLICATION_ID, VERSION_NO, CONSENT_TYPE, ACCESS_TOKEN_HASH, TOKEN_EXPIRES_AT, CONSENT_STATUS, CONTENT_CONFIRMED_AT, CONSENTED_AT, RETURNED_AT, RETURN_REASON, CLIENT_IP, "
                + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                c.getApplicationId(), c.getVersionNo(), c.getConsentType(), c.getAccessTokenHash(), c.getTokenExpiresAt(), c.getConsentStatus(), c.getContentConfirmedAt(), c.getConsentedAt(), c.getReturnedAt(), c.getReturnReason(), c.getClientIp(),
                now(), actor(), now(), actor());
    }

    /** 進行中の同意を無効（9）にする。 */
    public int invalidateActive(Connection conn, long applicationId) {
        return update(conn, "UPDATE T_APPLICANT_CONSENT SET CONSENT_STATUS = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE APPLICATION_ID = ? AND CONSENT_STATUS = ?",
                Codes.CONSENT_INVALID, now(), actor(), applicationId, Codes.CONSENT_REQUESTED);
    }

    public void updateContentConfirmed(Connection conn, long consentId, LocalDateTime at, String clientIp) {
        update(conn, "UPDATE T_APPLICANT_CONSENT SET CONTENT_CONFIRMED_AT = ?, CLIENT_IP = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE CONSENT_ID = ?",
                at, clientIp, now(), actor(), consentId);
    }

    public void updateAgreed(Connection conn, long consentId, LocalDateTime at, String clientIp) {
        update(conn, "UPDATE T_APPLICANT_CONSENT SET CONSENT_STATUS = ?, CONSENTED_AT = ?, CLIENT_IP = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE CONSENT_ID = ?",
                Codes.CONSENT_AGREED, at, clientIp, now(), actor(), consentId);
    }

    public void updateReturned(Connection conn, long consentId, LocalDateTime at, String reason, String clientIp) {
        update(conn, "UPDATE T_APPLICANT_CONSENT SET CONSENT_STATUS = ?, RETURNED_AT = ?, RETURN_REASON = ?, CLIENT_IP = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE CONSENT_ID = ?",
                Codes.CONSENT_RETURNED, at, truncate(reason, 500), clientIp, now(), actor(), consentId);
    }
}
