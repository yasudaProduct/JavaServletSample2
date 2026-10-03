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

    /** 同じメールアドレスの申込者（大文字・小文字を区別しない）。excludeId の申込者は除く。重複の確認に使う。 */
    public List<Applicant> findByMail(Connection conn, String mailAddress, Long excludeId) {
        return query(conn, SELECT + " WHERE LOWER(MAIL_ADDRESS) = LOWER(?) AND APPLICANT_ID <> ? ORDER BY APPLICANT_NO", ApplicantDao::map,
                mailAddress, excludeId == null ? -1L : excludeId);
    }

    /** 申込者を参照している申込の件数（取消を含む）。 */
    public int countApplications(Connection conn, long applicantId) {
        return (int) queryLong(conn, "SELECT COUNT(*) FROM T_APPLICATION WHERE APPLICANT_ID = ?", applicantId);
    }

    /** 申込者番号を採番する（C ＋ 10 桁ゼロ埋め）。既に使われている番号は読み飛ばす。 */
    public String nextApplicantNo(Connection conn) {
        for (int i = 0; i < 1000; i++) {
            String no = String.format("C%010d", queryLong(conn, "SELECT NEXT VALUE FOR SEQ_APPLICANT_NO"));
            if (findByNo(conn, no).isEmpty()) {
                return no;
            }
        }
        throw new IllegalStateException("申込者番号を採番できません");
    }

    /** 申込者を登録する（申込者番号は呼び出し側で採番して設定する）。申込者 ID を返す。 */
    public long insert(Connection conn, Applicant a) {
        return insertAndGetKey(conn, "INSERT INTO M_APPLICANT (APPLICANT_NO, APPLICANT_NAME, APPLICANT_KANA, MAIL_ADDRESS, TEL_NO, ADDRESS, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                a.getApplicantNo(), a.getApplicantName(), a.getApplicantKana(), a.getMailAddress(), a.getTelNo(), a.getAddress(), now(), actor(), now(), actor());
    }

    /** 申込者情報（氏名・カナ・メールアドレス・電話番号・住所）を更新する。行バージョン一致で更新し、更新件数を返す。 */
    public int updateProfile(Connection conn, Applicant a, int expectedRowVersion) {
        return update(conn, "UPDATE M_APPLICANT SET APPLICANT_NAME = ?, APPLICANT_KANA = ?, MAIL_ADDRESS = ?, TEL_NO = ?, ADDRESS = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 "
                + "WHERE APPLICANT_ID = ? AND ROW_VERSION = ?",
                a.getApplicantName(), a.getApplicantKana(), a.getMailAddress(), a.getTelNo(), a.getAddress(), now(), actor(), a.getApplicantId(), expectedRowVersion);
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
