package com.example.appmgmt.service.auth;

import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.PasswordHasher;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.ApplicantAccountDao;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.domain.ApplicantAccount;
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
 * 申込者アカウント（申込者ページのログイン。M_APPLICANT_ACCOUNT）。
 * アカウントはログインに必要なデータ（ユーザー ID・パスワード）だけを持ち、申込者名・メールアドレスなどは申込データ（申込内容の版）が持つ。
 * 一次承認が通って申込内容確認待ちになったとき（F14 の後続処理）に、申込がまだアカウントに紐づいていなければ発行して紐づけ、
 * ユーザー ID（申込者番号）と初期パスワードを通知 08 で申込データのメールアドレスへ連携する。
 */
public class ApplicantAuthService {

    private static final Logger log = LoggerFactory.getLogger(ApplicantAuthService.class);
    private static final String PASSWORD_CHARS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INITIAL_PASSWORD_LENGTH = 10;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 64;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApplicantAccountDao accountDao;
    private final ApplicationDao applicationDao;
    private final NotificationService notificationService;

    public ApplicantAuthService(ApplicantAccountDao accountDao, ApplicationDao applicationDao, NotificationService notificationService) {
        this.accountDao = accountDao;
        this.applicationDao = applicationDao;
        this.notificationService = notificationService;
    }

    /**
     * 申込がアカウントに紐づいていなければ、アカウントを発行して紐づけ、通知 08 を登録する。
     * 紐づいていれば（同じ申込者の 2 件目以降、契約変更）何もしない。F14 の後続処理から同一トランザクションで呼ぶ。
     */
    public void issueIfNeeded(Connection conn, Application app) {
        if (app.getApplicantId() != null) {
            return;
        }
        String applicantNo = accountDao.nextApplicantNo(conn);
        String initialPassword = generatePassword();
        long accountId = accountDao.insert(conn, applicantNo, PasswordHasher.hash(initialPassword), LocalDateTime.now().withNano(0));
        applicationDao.linkAccount(conn, app.getApplicationId(), accountId);
        app.setApplicantId(accountId);
        notificationService.registerApplicantAccount(conn, app, applicantNo, initialPassword);
        log.info("申込者アカウントを発行しました applicationNo={} applicantId={} loginId={}", app.getApplicationNo(), accountId, applicantNo);
    }

    /**
     * パスワードの初期化（SC15 メンテナンス）：新しい初期パスワードを発行して保存し、通知 09 をこの申込の申込データのメールアドレスへ登録する。
     * パスワード変更日時は空に戻す（初期パスワードのままの状態）。アカウント未発行は E117。
     */
    public void resetPassword(long applicationId) {
        Tx.executeVoid(conn -> {
            Application app = applicationDao.findById(conn, applicationId).orElseThrow(() -> new BusinessException("E103"));
            if (app.getApplicantId() == null) {
                throw new BusinessException("E117");
            }
            ApplicantAccount account = accountDao.findById(conn, app.getApplicantId()).orElseThrow(() -> new BusinessException("E117"));
            String initialPassword = generatePassword();
            accountDao.updatePassword(conn, account.getApplicantId(), PasswordHasher.hash(initialPassword), null);
            notificationService.registerPasswordReset(conn, app, account.getApplicantNo(), initialPassword);
            log.info("申込者のパスワードを初期化しました applicantId={} loginId={}", account.getApplicantId(), account.getApplicantNo());
        });
    }

    static String generatePassword() {
        StringBuilder sb = new StringBuilder(INITIAL_PASSWORD_LENGTH);
        for (int i = 0; i < INITIAL_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    /** ユーザー ID（申込者番号）とパスワードで認証する。表示名は最新の申込の申込者名。 */
    public Optional<ApplicantUser> login(String loginId, String password) {
        return Tx.execute(conn -> {
            Optional<ApplicantAccount> a = accountDao.findByNo(conn, loginId);
            if (a.isEmpty() || a.get().getPasswordHash() == null || !PasswordHasher.verify(password, a.get().getPasswordHash())) {
                return Optional.empty();
            }
            return Optional.of(new ApplicantUser(a.get(), applicationDao.latestApplicantName(conn, a.get().getApplicantId())));
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
            ApplicantAccount a = accountDao.findById(conn, applicantId).orElseThrow(() -> new BusinessException("E114"));
            if (a.getPasswordHash() == null || !PasswordHasher.verify(currentPassword, a.getPasswordHash())) {
                throw new BusinessException("E114");
            }
            accountDao.updatePassword(conn, applicantId, PasswordHasher.hash(newPassword), LocalDateTime.now().withNano(0));
        });
    }
}
