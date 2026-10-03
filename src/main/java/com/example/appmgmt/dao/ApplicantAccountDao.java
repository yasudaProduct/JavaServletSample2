package com.example.appmgmt.dao;

import com.example.appmgmt.domain.ApplicantAccount;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 申込者アカウント（M_APPLICANT_ACCOUNT）。申込者ページへのログインに必要なデータ（ユーザー ID・パスワード）だけを扱う。
 * 申込者名・メールアドレスなどの申込者情報は申込データ（T_APPLICATION_VERSION）にある。
 */
public class ApplicantAccountDao extends AbstractDao {

    private static final String SELECT = "SELECT APPLICANT_ID, APPLICANT_NO, PASSWORD_HASH, ACCOUNT_ISSUED_AT, PASSWORD_CHANGED_AT, " + AUDIT_COLS + " FROM M_APPLICANT_ACCOUNT";

    static ApplicantAccount map(ResultSet rs) throws SQLException {
        ApplicantAccount a = new ApplicantAccount();
        a.setApplicantId(rs.getLong("APPLICANT_ID"));
        a.setApplicantNo(rs.getString("APPLICANT_NO"));
        a.setPasswordHash(rs.getString("PASSWORD_HASH"));
        a.setAccountIssuedAt(ts(rs, "ACCOUNT_ISSUED_AT"));
        a.setPasswordChangedAt(ts(rs, "PASSWORD_CHANGED_AT"));
        mapAudit(rs, a);
        return a;
    }

    /** ユーザー ID（申込者番号）で検索する。 */
    public Optional<ApplicantAccount> findByNo(Connection conn, String applicantNo) {
        return queryOne(conn, SELECT + " WHERE APPLICANT_NO = ?", ApplicantAccountDao::map, applicantNo);
    }

    public Optional<ApplicantAccount> findById(Connection conn, long applicantId) {
        return queryOne(conn, SELECT + " WHERE APPLICANT_ID = ?", ApplicantAccountDao::map, applicantId);
    }

    /** ユーザー ID（申込者番号）を採番する（C ＋ 10 桁ゼロ埋め）。既に使われている番号は読み飛ばす。 */
    public String nextApplicantNo(Connection conn) {
        for (int i = 0; i < 1000; i++) {
            String no = String.format("C%010d", queryLong(conn, "SELECT NEXT VALUE FOR SEQ_APPLICANT_NO"));
            if (findByNo(conn, no).isEmpty()) {
                return no;
            }
        }
        throw new IllegalStateException("申込者番号を採番できません");
    }

    /** アカウントを発行する（ユーザー ID・パスワードハッシュ・発行日時）。申込者アカウント ID を返す。 */
    public long insert(Connection conn, String applicantNo, String passwordHash, LocalDateTime issuedAt) {
        return insertAndGetKey(conn, "INSERT INTO M_APPLICANT_ACCOUNT (APPLICANT_NO, PASSWORD_HASH, ACCOUNT_ISSUED_AT, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, 1)",
                applicantNo, passwordHash, issuedAt, now(), actor(), now(), actor());
    }

    /** パスワードを更新する。changedAt が null なら初期パスワード（初期化）として扱う。 */
    public void updatePassword(Connection conn, long applicantId, String passwordHash, LocalDateTime changedAt) {
        update(conn, "UPDATE M_APPLICANT_ACCOUNT SET PASSWORD_HASH = ?, PASSWORD_CHANGED_AT = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE APPLICANT_ID = ?",
                passwordHash, changedAt, now(), actor(), applicantId);
    }
}
