package com.example.appmgmt.dao;

import com.example.appmgmt.domain.ConsentDocument;
import com.example.appmgmt.domain.ConsentDocumentRecord;
import com.example.appmgmt.domain.ConsentDocumentVersion;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 同意事項マスタ・版（M_CONSENT_DOCUMENT／_VERSION）と、申込者同意の同意事項（T_APPLICANT_CONSENT_DOCUMENT）。 */
public class ConsentDocumentDao extends AbstractDao {

    private static final String SELECT_DOC = "SELECT DOCUMENT_CD, DOCUMENT_NAME, TARGET_TYPE, DISPLAY_ORDER, VALID_FLG, " + AUDIT_COLS + " FROM M_CONSENT_DOCUMENT";
    /** 版の一覧（PDF 本体は読まない）。 */
    private static final String SELECT_VER = "SELECT DOCUMENT_CD, VERSION_NO, EFFECTIVE_FROM, FILE_NAME, FILE_SIZE, FILE_HASH, REMARKS, " + AUDIT_COLS + " FROM M_CONSENT_DOCUMENT_VERSION";

    static ConsentDocument mapDoc(ResultSet rs) throws SQLException {
        ConsentDocument d = new ConsentDocument();
        d.setDocumentCd(rs.getString("DOCUMENT_CD"));
        d.setDocumentName(rs.getString("DOCUMENT_NAME"));
        d.setTargetType(rs.getString("TARGET_TYPE"));
        d.setDisplayOrder(rs.getInt("DISPLAY_ORDER"));
        d.setValidFlg(rs.getString("VALID_FLG"));
        mapAudit(rs, d);
        return d;
    }

    static ConsentDocumentVersion mapVer(ResultSet rs) throws SQLException {
        ConsentDocumentVersion v = new ConsentDocumentVersion();
        v.setDocumentCd(rs.getString("DOCUMENT_CD"));
        v.setVersionNo(rs.getInt("VERSION_NO"));
        v.setEffectiveFrom(ts(rs, "EFFECTIVE_FROM"));
        v.setFileName(rs.getString("FILE_NAME"));
        v.setFileSize(rs.getInt("FILE_SIZE"));
        v.setFileHash(rs.getString("FILE_HASH"));
        v.setRemarks(rs.getString("REMARKS"));
        mapAudit(rs, v);
        return v;
    }

    public List<ConsentDocument> findAll(Connection conn) {
        return query(conn, SELECT_DOC + " ORDER BY DISPLAY_ORDER, DOCUMENT_CD", ConsentDocumentDao::mapDoc);
    }

    public Optional<ConsentDocument> find(Connection conn, String documentCd) {
        return queryOne(conn, SELECT_DOC + " WHERE DOCUMENT_CD = ?", ConsentDocumentDao::mapDoc, documentCd);
    }

    public void insert(Connection conn, ConsentDocument d) {
        update(conn, "INSERT INTO M_CONSENT_DOCUMENT (DOCUMENT_CD, DOCUMENT_NAME, TARGET_TYPE, DISPLAY_ORDER, VALID_FLG, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                d.getDocumentCd(), d.getDocumentName(), d.getTargetType(), d.getDisplayOrder(), d.getValidFlg(), now(), actor(), now(), actor());
    }

    public int updateDocument(Connection conn, ConsentDocument d, int expectedRowVersion) {
        return update(conn, "UPDATE M_CONSENT_DOCUMENT SET DOCUMENT_NAME = ?, TARGET_TYPE = ?, DISPLAY_ORDER = ?, VALID_FLG = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 "
                + "WHERE DOCUMENT_CD = ? AND ROW_VERSION = ?",
                d.getDocumentName(), d.getTargetType(), d.getDisplayOrder(), d.getValidFlg(), now(), actor(), d.getDocumentCd(), expectedRowVersion);
    }

    /** 文書の全版（版番号の降順。PDF 本体なし）。 */
    public List<ConsentDocumentVersion> findVersions(Connection conn, String documentCd) {
        return query(conn, SELECT_VER + " WHERE DOCUMENT_CD = ? ORDER BY VERSION_NO DESC", ConsentDocumentDao::mapVer, documentCd);
    }

    /** 指定日時に適用中の版（適用開始日時が指定日時以前で最も新しい版。同じ日時なら版番号の大きい方）。 */
    public Optional<ConsentDocumentVersion> findEffective(Connection conn, String documentCd, LocalDateTime at) {
        List<ConsentDocumentVersion> list = query(conn, SELECT_VER + " WHERE DOCUMENT_CD = ? AND EFFECTIVE_FROM <= ? ORDER BY EFFECTIVE_FROM DESC, VERSION_NO DESC",
                ConsentDocumentDao::mapVer, documentCd, at);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** 版を PDF 本体付きで読む。 */
    public Optional<ConsentDocumentVersion> findVersionWithData(Connection conn, String documentCd, int versionNo) {
        return queryOne(conn, "SELECT DOCUMENT_CD, VERSION_NO, EFFECTIVE_FROM, FILE_NAME, FILE_SIZE, FILE_HASH, REMARKS, FILE_DATA, " + AUDIT_COLS
                + " FROM M_CONSENT_DOCUMENT_VERSION WHERE DOCUMENT_CD = ? AND VERSION_NO = ?", rs -> {
                    ConsentDocumentVersion v = mapVer(rs);
                    v.setData(rs.getBytes("FILE_DATA"));
                    return v;
                }, documentCd, versionNo);
    }

    public int nextVersionNo(Connection conn, String documentCd) {
        return (int) queryLong(conn, "SELECT COALESCE(MAX(VERSION_NO), 0) + 1 FROM M_CONSENT_DOCUMENT_VERSION WHERE DOCUMENT_CD = ?", documentCd);
    }

    public void insertVersion(Connection conn, ConsentDocumentVersion v) {
        update(conn, "INSERT INTO M_CONSENT_DOCUMENT_VERSION (DOCUMENT_CD, VERSION_NO, EFFECTIVE_FROM, FILE_NAME, FILE_SIZE, FILE_HASH, FILE_DATA, REMARKS, " + AUDIT_COLS
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                v.getDocumentCd(), v.getVersionNo(), v.getEffectiveFrom(), v.getFileName(), v.getFileSize(), v.getFileHash(), v.getData(), v.getRemarks(), now(), actor(), now(), actor());
    }

    // ---------------------------------------------------------------- 申込者同意の同意事項

    private static final String SELECT_REC = "SELECT r.CONSENT_ID, r.DOCUMENT_CD, r.VERSION_NO, r.VIEWED_AT, r.AGREED_AT, d.DOCUMENT_NAME, v.EFFECTIVE_FROM, v.FILE_HASH, "
            + "c.CONSENT_TYPE, c.VERSION_NO AS CONTENT_VERSION_NO FROM T_APPLICANT_CONSENT_DOCUMENT r "
            + "JOIN M_CONSENT_DOCUMENT d ON d.DOCUMENT_CD = r.DOCUMENT_CD "
            + "JOIN M_CONSENT_DOCUMENT_VERSION v ON v.DOCUMENT_CD = r.DOCUMENT_CD AND v.VERSION_NO = r.VERSION_NO "
            + "JOIN T_APPLICANT_CONSENT c ON c.CONSENT_ID = r.CONSENT_ID";

    static ConsentDocumentRecord mapRec(ResultSet rs) throws SQLException {
        ConsentDocumentRecord r = new ConsentDocumentRecord();
        r.setConsentId(rs.getLong("CONSENT_ID"));
        r.setDocumentCd(rs.getString("DOCUMENT_CD"));
        r.setVersionNo(rs.getInt("VERSION_NO"));
        r.setViewedAt(ts(rs, "VIEWED_AT"));
        r.setAgreedAt(ts(rs, "AGREED_AT"));
        r.setDocumentName(rs.getString("DOCUMENT_NAME"));
        r.setEffectiveFrom(ts(rs, "EFFECTIVE_FROM"));
        r.setFileHash(rs.getString("FILE_HASH"));
        r.setConsentType(rs.getString("CONSENT_TYPE"));
        r.setContentVersionNo(rs.getInt("CONTENT_VERSION_NO"));
        return r;
    }

    /** この同意で開いた・同意した同意事項。 */
    public List<ConsentDocumentRecord> findRecords(Connection conn, long consentId) {
        return query(conn, SELECT_REC + " WHERE r.CONSENT_ID = ? ORDER BY d.DISPLAY_ORDER, r.DOCUMENT_CD, r.VERSION_NO", ConsentDocumentDao::mapRec, consentId);
    }

    /** 申込で同意した同意事項（同意日時の新しい順）。 */
    public List<ConsentDocumentRecord> findAgreedByApplication(Connection conn, long applicationId) {
        return query(conn, SELECT_REC + " WHERE c.APPLICATION_ID = ? AND r.AGREED_AT IS NOT NULL ORDER BY r.AGREED_AT DESC, d.DISPLAY_ORDER, r.DOCUMENT_CD",
                ConsentDocumentDao::mapRec, applicationId);
    }

    /** 閲覧を記録する（同じ版を 2 回目以降に開いた場合は最初の閲覧日時のまま）。 */
    public void recordView(Connection conn, long consentId, String documentCd, int versionNo, LocalDateTime at) {
        long exists = queryLong(conn, "SELECT COUNT(*) FROM T_APPLICANT_CONSENT_DOCUMENT WHERE CONSENT_ID = ? AND DOCUMENT_CD = ? AND VERSION_NO = ?", consentId, documentCd, versionNo);
        if (exists == 0) {
            update(conn, "INSERT INTO T_APPLICANT_CONSENT_DOCUMENT (CONSENT_ID, DOCUMENT_CD, VERSION_NO, VIEWED_AT, AGREED_AT, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?, 1)",
                    consentId, documentCd, versionNo, at, now(), actor(), now(), actor());
        }
    }

    public void markAgreed(Connection conn, long consentId, String documentCd, int versionNo, LocalDateTime at) {
        update(conn, "UPDATE T_APPLICANT_CONSENT_DOCUMENT SET AGREED_AT = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE CONSENT_ID = ? AND DOCUMENT_CD = ? AND VERSION_NO = ?",
                at, now(), actor(), consentId, documentCd, versionNo);
    }
}
