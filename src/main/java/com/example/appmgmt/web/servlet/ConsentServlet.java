package com.example.appmgmt.web.servlet;

import com.example.appmgmt.common.AuditContext;
import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.Messages;
import com.example.appmgmt.common.Validation;
import com.example.appmgmt.service.consent.ConsentView;
import com.example.appmgmt.web.form.ApplicationForm;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** 申込者向け画面（/consent/{token}...）AP01〜AP05。 */
public class ConsentServlet extends BaseServlet {

    @Override
    protected String fallbackPath(HttpServletRequest req) {
        List<String> parts = pathParts(req);
        return parts.isEmpty() ? "/consent/invalid" : "/consent/" + parts.get(0);
    }

    private ConsentView resolve(HttpServletRequest req, HttpServletResponse res, String token) throws ServletException, IOException {
        ConsentView v = services().getConsentService().resolve(services().getConsentService().byToken(token));
        req.setAttribute("v", v);
        req.setAttribute("token", token);
        req.setAttribute("consentBase", "/consent/" + token);
        req.setAttribute("portal", false);
        if (v.getApplication() != null) {
            AuditContext.set(AuditContext.applicant(v.getApplication().getApplicantId()));
        }
        switch (v.getOutcome()) {
            case INVALID:
                delay();
                req.setAttribute("errorMessage", Messages.get("E201"));
                render(req, res, "consent/ap05_error.jsp");
                return null;
            case EXPIRED:
                delay();
                req.setAttribute("errorMessage", Messages.get("E202"));
                render(req, res, "consent/ap05_error.jsp");
                return null;
            default:
                return v;
        }
    }

    private static void delay() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        List<String> parts = pathParts(req);
        if (parts.isEmpty()) {
            res.sendError(404);
            return;
        }
        String token = parts.get(0);
        String sub = parts.size() > 1 ? parts.get(1) : "";
        ConsentView v = resolve(req, res, token);
        if (v == null) {
            return;
        }
        String base = "/consent/" + token;
        switch (sub) {
            case "":
                if (v.getOutcome() == ConsentView.Outcome.AGREE) {
                    redirect(req, res, base + "/agree");
                } else if (v.getOutcome() == ConsentView.Outcome.COMPLETED) {
                    redirect(req, res, base + "/complete");
                } else {
                    render(req, res, "consent/ap01_confirm.jsp");
                }
                return;
            case "edit":
                if (v.getOutcome() != ConsentView.Outcome.CONFIRM || v.isContractChange()) {
                    redirect(req, res, base);
                    return;
                }
                req.setAttribute("form", ApplicationForm.from(v.getVersion()));
                req.setAttribute("errors", java.util.Map.of());
                render(req, res, "consent/ap02_edit.jsp");
                return;
            case "agree":
                if (v.getOutcome() != ConsentView.Outcome.AGREE) {
                    redirect(req, res, base);
                    return;
                }
                render(req, res, "consent/ap03_agree.jsp");
                return;
            case "complete":
                render(req, res, "consent/ap04_complete.jsp");
                return;
            case "terms": {
                // AP03 の同意事項 PDF（/consent/{token}/terms/{文書コード}/{版番号}）。開いたことを記録する
                Long ver = parts.size() > 3 ? parseId(parts.get(3)) : null;
                if (ver == null) {
                    res.sendError(404);
                    return;
                }
                com.example.appmgmt.domain.ConsentDocumentVersion d = services().getConsentService()
                        .openDocument(services().getConsentService().byToken(token), parts.get(2), ver.intValue());
                sendPdf(res, d.getData(), d.getFileName());
                return;
            }
            default:
                res.sendError(404);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        List<String> parts = pathParts(req);
        if (parts.size() < 2) {
            res.sendError(404);
            return;
        }
        String token = parts.get(0);
        String sub = parts.get(1);
        String base = "/consent/" + token;
        ConsentView v = resolve(req, res, token);
        if (v == null) {
            return;
        }
        String ip = clientIp(req);
        switch (sub) {
            case "edit": {
                ApplicationForm form = ApplicationForm.bind(req);
                Validation errors = form.validate(true, false);
                if (errors.hasErrors()) {
                    req.setAttribute("form", form);
                    req.setAttribute("errors", errors.getErrors());
                    render(req, res, "consent/ap02_edit.jsp");
                    return;
                }
                services().getConsentService().saveEdit(services().getConsentService().byToken(token), form.toVersion());
                flashMessage(req, "success", "I001");
                redirect(req, res, base);
                return;
            }
            case "confirm":
                if (!"1".equals(param(req, "checked"))) {
                    throw new BusinessException("E001", "内容を確認しました");
                }
                services().getConsentService().confirm(services().getConsentService().byToken(token), ip);
                redirect(req, res, base + "/agree");
                return;
            case "agree": {
                String action = param(req, "action");
                switch (action) {
                    case "agree":
                        if (!"1".equals(param(req, "agreed"))) {
                            throw new BusinessException("E001", "同意事項に同意します");
                        }
                        services().getConsentService().agree(services().getConsentService().byToken(token), ip);
                        flashMessage(req, "success", "I007");
                        req.getSession(true).setAttribute("consentDone", "agree");
                        redirect(req, res, base + "/complete");
                        return;
                    case "return": {
                        String reason = req.getParameter("returnReason") == null ? "" : req.getParameter("returnReason").strip();
                        if (reason.length() > 500) {
                            throw new BusinessException("E002", "差戻し理由", 500);
                        }
                        services().getConsentService().returnToOwner(services().getConsentService().byToken(token), reason, ip);
                        flashMessage(req, "info", "I008");
                        req.getSession(true).setAttribute("consentDone", "return");
                        redirect(req, res, base + "/complete");
                        return;
                    }
                    case "modify":
                        services().getConsentService().modify(services().getConsentService().byToken(token), ip);
                        redirect(req, res, base);
                        return;
                    default:
                        res.sendError(404);
                        return;
                }
            }
            default:
                res.sendError(404);
        }
    }
}
