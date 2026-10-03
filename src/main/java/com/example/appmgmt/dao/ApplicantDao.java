package com.example.appmgmt.dao;

import com.example.appmgmt.domain.Applicant;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ApplicantDao extends AbstractDao {

    private static final String SELECT = "SELECT APPLICANT_ID, APPLICANT_NO, APPLICANT_NAME, APPLICANT_KANA, MAIL_ADDRESS, TEL_NO, ADDRESS, PASSWORD_HASH, ACCOUNT_ISSUED_AT, PASSWORD_CHANGED_AT, " + AUDIT_COLS + " FROM M_APPLICANT";

    static Applicant map(ResultSet rs) throws SQLException {
        Applicant a = new Applicant();
        a.setApplicantId(rs.getLong("APPLICANT_ID"));
        a.setApplicantNo(rs.getString("APPLICANT_NO"));
        a.setApplicantName(rs.getString("APPLICANT_NAME"));
        a.setApplicantKana(rs.getString("APPLICANT_KANA"));
        a.setMailAddress(rs.getString("MAIL_ADDRESS"));
        a.setTelNo(rs.getString("TEL_NO"));
        a.setAddress(rs.getString("ADDRESS"));
        a.setPasswordHash(rs.getString("PASSWORD_HASH"));
        a.setAccountIssuedAt(ts(rs, "ACCOUNT_ISSUED_AT"));
        a.setPasswordChangedAt(ts(rs, "PASSWORD_CHANGED_AT"));
        mapAudit(rs, a);
        return a;
    }

    public Optional<Applicant> findByNo(Connection conn, String applicantNo) {
        return queryOne(conn, SELECT + " WHERE APPLICANT_NO = ?", ApplicantDao::map, applicantNo);
    }

    public Optional<Applicant> findById(Connection conn, long applicantId) {
        return queryOne(conn, SELECT + " WHERE APPLICANT_ID = ?", ApplicantDao::map, applicantId);
    }

    public List<Applicant> findAll(Connection conn) {
        return query(conn, SELECT + " ORDER BY APPLICANT_NO", ApplicantDao::map);
    }

    /** アカウント発行（未発行のときだけ更新する。更新件数を返す）。 */
    public int issueAccount(Connection conn, long applicantId, String passwordHash, java.time.LocalDateTime issuedAt) {
        return update(conn, "UPDATE M_APPLICANT SET PASSWORD_HASH = ?, ACCOUNT_ISSUED_AT = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE APPLICANT_ID = ? AND PASSWORD_HASH IS NULL",
                passwordHash, issuedAt, now(), actor(), applicantId);
    }

    public void updatePassword(Connection conn, long applicantId, String passwordHash, java.time.LocalDateTime changedAt) {
        update(conn, "UPDATE M_APPLICANT SET PASSWORD_HASH = ?, PASSWORD_CHANGED_AT = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE APPLICANT_ID = ?",
                passwordHash, changedAt, now(), actor(), applicantId);
    }
}
