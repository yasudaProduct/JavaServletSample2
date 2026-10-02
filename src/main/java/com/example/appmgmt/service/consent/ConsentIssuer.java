package com.example.appmgmt.service.consent;

import com.example.appmgmt.common.AppConfig;
import com.example.appmgmt.common.TokenUtil;
import com.example.appmgmt.dao.ApplicantConsentDao;
import com.example.appmgmt.domain.ApplicantConsent;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.service.notification.NotificationService;
import java.sql.Connection;
import java.time.LocalDateTime;

/** 確認用トークンの発行（F06 7.2）。申込者同意を作り、通知 01 を登録する。 */
public class ConsentIssuer {

    private final ApplicantConsentDao consentDao;
    private final NotificationService notificationService;

    public ConsentIssuer(ApplicantConsentDao consentDao, NotificationService notificationService) {
        this.consentDao = consentDao;
        this.notificationService = notificationService;
    }

    /** 進行中の同意を無効にし、新しいトークンで申込者同意と通知 01 を登録する。平文トークンを返す（呼出元は保持しない）。 */
    public String issue(Connection conn, Application app, String consentType) {
        consentDao.invalidateActive(conn, app.getApplicationId());
        String token = TokenUtil.newToken();
        int days = AppConfig.get().getInt("consent.token-expire-days", 14);
        LocalDateTime expires = LocalDateTime.now().plusDays(days).withNano(0);
        ApplicantConsent c = new ApplicantConsent();
        c.setApplicationId(app.getApplicationId());
        c.setVersionNo(app.getCurrentVersionNo());
        c.setConsentType(consentType);
        c.setAccessTokenHash(TokenUtil.sha256Hex(token));
        c.setTokenExpiresAt(expires);
        c.setConsentStatus(Codes.CONSENT_REQUESTED);
        consentDao.insert(conn, c);
        notificationService.registerConsentRequest(conn, app, consentType, token, expires);
        return token;
    }
}
