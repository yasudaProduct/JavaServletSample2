package com.example.appmgmt.service.auth;

import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.PasswordHasher;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.ApplicantDao;
import com.example.appmgmt.domain.Applicant;
import com.example.appmgmt.domain.ApplicantUser;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.service.notification.NotificationService;
import java.security.SecureRandom;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 申込者アカウント（申込者ポータルのログイン）。
 * 一次承認が通って申込内容確認待ちになったとき（F14 の後続処理）にアカウントを発行し、ユーザー ID（申込者番号）と初期パスワードを通知 08 で連携する。
 */
public class ApplicantAuthService {

    private static final Logger log = LoggerFactory.getLogger(ApplicantAuthService.class);
    private static final String PASSWORD_CHARS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INITIAL_PASSWORD_LENGTH = 10;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 64;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApplicantDao applicantDao;
    private final NotificationService notificationService;

    public ApplicantAuthService(ApplicantDao applicantDao, NotificationService notificationService) {
        this.applicantDao = applicantDao;
        this.notificationService = notificationService;
    }

    /** 未発行ならアカウントを発行して通知 08 を登録する（発行済みなら何もしない）。F14 の後続処理から同一トランザクションで呼ぶ。 */
    public void issueIfNeeded(Connection conn, Application app) {
        Applicant applicant = applicantDao.findById(conn, app.getApplicantId()).orElse(null);
        if (applicant == null || applicant.isAccountIssued()) {
            return;
        }
        String initialPassword = generatePassword();
        int updated = applicantDao.issueAccount(conn, applicant.getApplicantId(), PasswordHasher.hash(initialPassword), LocalDateTime.now().withNano(0));
        if (updated == 1) {
            notificationService.registerApplicantAccount(conn, app, applicant.getApplicantNo(), initialPassword);
            log.info("申込者アカウントを発行しました applicantId={} loginId={}", applicant.getApplicantId(), applicant.getApplicantNo());
        }
    }

    /**
     * パスワードの初期化（SC15 メンテナンス）：新しい初期パスワードを発行して保存し、通知 09 を登録する。
     * パスワード変更日時は空に戻す（初期パスワードのままの状態）。アカウント未発行は E117。
     */
    public void resetPassword(long applicationId, com.example.appmgmt.dao.ApplicationDao applicationDao) {
        Tx.executeVoid(conn -> {
            Application app = applicationDao.findById(conn, applicationId).orElseThrow(() -> new BusinessException("E103"));
            Applicant applicant = applicantDao.findById(conn, app.getApplicantId()).orElseThrow(() -> new BusinessException("E103"));
            if (!applicant.isAccountIssued()) {
                throw new BusinessException("E117");
            }
            String initialPassword = generatePassword();
            applicantDao.updatePassword(conn, applicant.getApplicantId(), PasswordHasher.hash(initialPassword), null);
            notificationService.registerPasswordReset(conn, app, applicant.getApplicantNo(), initialPassword);
            log.info("申込者のパスワードを初期化しました applicantId={} loginId={}", applicant.getApplicantId(), applicant.getApplicantNo());
        });
    }

    static String generatePassword() {
        StringBuilder sb = new StringBuilder(INITIAL_PASSWORD_LENGTH);
        for (int i = 0; i < INITIAL_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    /** ログイン ID（申込者番号）とパスワードで認証する。アカウント未発行は失敗。 */
    public Optional<ApplicantUser> login(String loginId, String password) {
        return Tx.execute(conn -> {
            Optional<Applicant> a = applicantDao.findByNo(conn, loginId);
            if (a.isEmpty() || !a.get().isAccountIssued() || !PasswordHasher.verify(password, a.get().getPasswordHash())) {
                return Optional.empty();
            }
            return Optional.of(new ApplicantUser(a.get()));
        });
    }

    public void changePassword(long applicantId, String currentPassword, String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.length() < PASSWORD_MIN || newPassword.length() > PASSWORD_MAX) {
            throw new BusinessException("E116", PASSWORD_MIN, PASSWORD_MAX);
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("E115");
        }
        Tx.executeVoid(conn -> {
            Applicant a = applicantDao.findById(conn, applicantId).orElseThrow(() -> new BusinessException("E114"));
            if (!a.isAccountIssued() || !PasswordHasher.verify(currentPassword, a.getPasswordHash())) {
                throw new BusinessException("E114");
            }
            applicantDao.updatePassword(conn, applicantId, PasswordHasher.hash(newPassword), LocalDateTime.now().withNano(0));
        });
    }
}
