package com.example.appmgmt.service.notification;

import com.example.appmgmt.common.AppConfig;
import com.example.appmgmt.common.Formats;
import com.example.appmgmt.dao.ApplicantDao;
import com.example.appmgmt.dao.EmployeeDao;
import com.example.appmgmt.dao.NotificationDao;
import com.example.appmgmt.dao.StatusDao;
import com.example.appmgmt.domain.Applicant;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApprovalRequest;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.Employee;
import com.example.appmgmt.domain.ExternalLink;
import com.example.appmgmt.domain.Notification;
import com.example.appmgmt.domain.Status;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 通知（T_NOTIFICATION）の登録（13. バッチ・通知設計 3 章）。
 * テンプレートを展開して登録するまでを行い、送信は BT01 が行う。
 */
public class NotificationService {

    private final NotificationDao notificationDao;
    private final EmployeeDao employeeDao;
    private final ApplicantDao applicantDao;
    private final StatusDao statusDao;
    private final Properties templates = new Properties();

    public NotificationService(NotificationDao notificationDao, EmployeeDao employeeDao, ApplicantDao applicantDao, StatusDao statusDao) {
        this.notificationDao = notificationDao;
        this.employeeDao = employeeDao;
        this.applicantDao = applicantDao;
        this.statusDao = statusDao;
        try (InputStream in = NotificationService.class.getClassLoader().getResourceAsStream("notification-templates.properties")) {
            if (in != null) {
                templates.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new IllegalStateException("notification-templates.properties を読み込めません", e);
        }
    }

    /** 共通プレースホルダ（申込番号、申込者名、担当者名、申込詳細 URL、ステータス名）。 */
    private Map<String, String> baseParams(Connection conn, Application app) {
        Map<String, String> p = new HashMap<>();
        Applicant applicant = applicantDao.findById(conn, app.getApplicantId()).orElse(null);
        Employee owner = employeeDao.findById(conn, app.getOwnerEmployeeId()).orElse(null);
        Status status = statusDao.find(conn, app.getStatusCd()).orElse(null);
        p.put("applicationNo", app.getApplicationNo());
        p.put("applicantName", applicant == null ? "" : applicant.getApplicantName());
        p.put("ownerName", owner == null ? "" : owner.getEmployeeName());
        p.put("ownerMail", owner == null ? "" : owner.getMailAddress());
        p.put("applicantMail", applicant == null ? "" : applicant.getMailAddress());
        p.put("statusName", status == null ? app.getStatusCd() : status.getStatusName());
        p.put("detailUrl", AppConfig.get().getString("app.employee-base-url") + "/emp/applications/" + app.getApplicationId());
        p.put("occurredAt", Formats.dateTime(LocalDateTime.now()));
        return p;
    }

    private void register(Connection conn, String type, Application app, String toAddress, String subjectKey, String bodyKey, Map<String, String> params) {
        Notification n = new Notification();
        n.setNotificationType(type);
        n.setApplicationId(app.getApplicationId());
        n.setToAddress(toAddress);
        n.setSubject(expand(templates.getProperty(subjectKey, subjectKey), params));
        n.setBody(expand(templates.getProperty(bodyKey, bodyKey), params));
        notificationDao.insert(conn, n);
    }

    static String expand(String template, Map<String, String> params) {
        String s = template;
        for (Map.Entry<String, String> e : params.entrySet()) {
            s = s.replace("${" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return s;
    }

    /** 01 申込者確認依頼。平文トークンは本文にだけ含める。 */
    public void registerConsentRequest(Connection conn, Application app, String consentType, String token, LocalDateTime expiresAt) {
        Map<String, String> p = baseParams(conn, app);
        p.put("baseUrl", AppConfig.get().getString("app.applicant-base-url"));
        p.put("token", token);
        p.put("expiresAt", Formats.dateTime(expiresAt));
        register(conn, Codes.NOTIFY_CONSENT_REQUEST, app, p.get("applicantMail"), "01." + consentType + ".subject", "01." + consentType + ".body", p);
    }

    /** 02 承認依頼（宛先：指定ステップの承認者）。 */
    public void registerApprovalRequest(Connection conn, Application app, ApprovalRequest req, int stepNo) {
        Map<String, String> p = baseParams(conn, app);
        long approverId = req.getSteps().stream().filter(s -> s.getStepNo() == stepNo).findFirst().map(s -> s.getApproverEmployeeId()).orElse(0L);
        Employee approver = employeeDao.findById(conn, approverId).orElse(null);
        Employee requester = employeeDao.findById(conn, req.getRequestEmployeeId()).orElse(null);
        p.put("approverName", approver == null ? "" : approver.getEmployeeName());
        p.put("requesterName", requester == null ? "" : requester.getEmployeeName());
        p.put("approvalTypeName", Codes.label("APPROVAL_TYPE", req.getApprovalType()));
        p.put("stepNo", String.valueOf(stepNo));
        p.put("finalStepNo", String.valueOf(req.getFinalStepNo()));
        boolean finalStepOfFinalApproval = Codes.isFinalApprovalType(req.getApprovalType()) && req.getFinalStepNo() != null && req.getFinalStepNo() == stepNo;
        register(conn, Codes.NOTIFY_APPROVAL_REQUEST, app, approver == null ? "" : approver.getMailAddress(), "02.subject", finalStepOfFinalApproval ? "02.final.body" : "02.body", p);
    }

    /** 03 差戻し通知（宛先：申請社員）。 */
    public void registerReturned(Connection conn, Application app, ApprovalRequest req, long returnedByEmployeeId, String comment) {
        Map<String, String> p = baseParams(conn, app);
        Employee approver = employeeDao.findById(conn, returnedByEmployeeId).orElse(null);
        Employee requester = employeeDao.findById(conn, req.getRequestEmployeeId()).orElse(null);
        p.put("approverName", approver == null ? "" : approver.getEmployeeName());
        p.put("requesterName", requester == null ? "" : requester.getEmployeeName());
        p.put("approvalTypeName", Codes.label("APPROVAL_TYPE", req.getApprovalType()));
        p.put("comment", comment == null ? "" : comment);
        register(conn, Codes.NOTIFY_RETURNED, app, requester == null ? "" : requester.getMailAddress(), "03.subject", "03.body", p);
    }

    /** 04 申込者差戻し通知（宛先：担当社員）。 */
    public void registerApplicantReturned(Connection conn, Application app, String returnReason) {
        Map<String, String> p = baseParams(conn, app);
        p.put("returnReason", returnReason == null ? "" : returnReason);
        register(conn, Codes.NOTIFY_APPLICANT_RETURNED, app, p.get("ownerMail"), "04.subject", "04.body", p);
    }

    /** 05 事前確認結果通知（宛先：担当社員）。 */
    public void registerPrecheckResult(Connection conn, Application app, String resultReason) {
        Map<String, String> p = baseParams(conn, app);
        p.put("resultReason", resultReason == null ? "" : resultReason);
        register(conn, Codes.NOTIFY_PRECHECK_RESULT, app, p.get("ownerMail"), "05.subject", "05.body", p);
    }

    /** 06 審査結果通知（審査完了）。 */
    public void registerReviewCompleted(Connection conn, Application app) {
        Map<String, String> p = baseParams(conn, app);
        p.put("reviewedAt", Formats.dateTime(app.getReviewedAt()));
        p.put("reviewedVersionNo", String.valueOf(app.getReviewedVersionNo()));
        register(conn, Codes.NOTIFY_REVIEW_RESULT, app, p.get("ownerMail"), "06.completed.subject", "06.completed.body", p);
    }

    /** 06 審査結果通知（審査差戻し。契約変更の差戻しは文面を切り替える）。 */
    public void registerReviewReturned(Connection conn, Application app, String resultReason, boolean contractChange) {
        Map<String, String> p = baseParams(conn, app);
        p.put("resultReason", resultReason == null ? "" : resultReason);
        p.put("reviewedVersionNo", String.valueOf(app.getReviewedVersionNo()));
        register(conn, Codes.NOTIFY_REVIEW_RESULT, app, p.get("ownerMail"), "06.returned.subject", contractChange ? "06.returned-change.body" : "06.returned.body", p);
    }

    /** 08 申込者アカウント通知（宛先：申込者）。初期パスワードは本文にだけ含める。 */
    public void registerApplicantAccount(Connection conn, Application app, String loginId, String initialPassword) {
        Map<String, String> p = baseParams(conn, app);
        p.put("baseUrl", AppConfig.get().getString("app.applicant-base-url"));
        p.put("loginId", loginId);
        p.put("initialPassword", initialPassword);
        register(conn, Codes.NOTIFY_APPLICANT_ACCOUNT, app, p.get("applicantMail"), "08.subject", "08.body", p);
    }

    /** 09 パスワード初期化通知（宛先：申込者）。新しい初期パスワードは本文にだけ含める。 */
    public void registerPasswordReset(Connection conn, Application app, String loginId, String initialPassword) {
        Map<String, String> p = baseParams(conn, app);
        p.put("baseUrl", AppConfig.get().getString("app.applicant-base-url"));
        p.put("loginId", loginId);
        p.put("initialPassword", initialPassword);
        register(conn, Codes.NOTIFY_PASSWORD_RESET, app, p.get("applicantMail"), "09.subject", "09.body", p);
    }

    /** 07 送信エラー通知（宛先：担当社員と有効な管理者。重複する宛先は 1 行）。 */
    public void registerSendError(Connection conn, Application app, ExternalLink link) {
        Map<String, String> p = baseParams(conn, app);
        p.put("linkTypeName", Codes.label("LINK_TYPE", link.getLinkType()));
        p.put("externalLinkId", String.valueOf(link.getExternalLinkId()));
        p.put("errorMessage", link.getErrorMessage() == null ? "" : link.getErrorMessage());
        Set<String> to = new LinkedHashSet<>();
        if (!p.get("ownerMail").isEmpty()) {
            to.add(p.get("ownerMail"));
        }
        for (Employee admin : employeeDao.findValidAdmins(conn)) {
            to.add(admin.getMailAddress());
        }
        for (String addr : to) {
            register(conn, Codes.NOTIFY_SEND_ERROR, app, addr, "07.subject", "07.body", p);
        }
    }
}
