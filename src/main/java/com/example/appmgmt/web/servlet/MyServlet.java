package com.example.appmgmt.web.servlet;

import com.example.appmgmt.common.AppConfig;
import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.Messages;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.common.Validation;
import com.example.appmgmt.domain.ApplicantUser;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.LoginUser;
import com.example.appmgmt.domain.StatusCd;
import com.example.appmgmt.service.application.ApplicationDetail;
import com.example.appmgmt.service.consent.ConsentService;
import com.example.appmgmt.service.consent.ConsentView;
import com.example.appmgmt.web.form.ApplicationForm;
import com.example.appmgmt.web.view.MenuTile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 申込者ポータル（/my/*）。
 * AP06 ログイン、AP07 メニュー（お知らせ／メニューのタブ）、AP08 申込確認、AP01〜AP03 相当の内容確認・同意（ログイン済みの申込者本人として）、AP09 パスワード変更。
 */
public class MyServlet extends BaseServlet {

    @Override
    protected String fallbackPath(HttpServletRequest req) {
        return "/my/menu";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        List<String> parts = pathParts(req);
        String page = parts.isEmpty() ? "menu" : parts.get(0);
        switch (page) {
            case "login":
                if (applicant(req) != null) {
                    redirect(req, res, "/my/menu");
                    return;
                }
                if ("1".equals(param(req, "timeout"))) {
                    addMessage(req, "warning", Messages.get("E110"));
                }
                render(req, res, "my/login.jsp");
                return;
            case "logout":
                redirect(req, res, "/my/login");
                return;
            case "menu":
                menu(req, res, requireApplicant(req));
                return;
            case "applications":
                application(req, res, requireApplicant(req), parts);
                return;
            case "password":
                requireApplicant(req);
                render(req, res, "my/password.jsp");
                return;
            default:
                res.sendError(404);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        List<String> parts = pathParts(req);
        String page = parts.isEmpty() ? "" : parts.get(0);
        switch (page) {
            case "login": login(req, res); return;
            case "logout": {
                HttpSession s = req.getSession(false);
                if (s != null) {
                    s.invalidate();
                }
                csrfToken(req);
                flashMessage(req, "info", "I013");
                redirect(req, res, "/my/login");
                return;
            }
            case "applications": postConsent(req, res, requireApplicant(req), parts); return;
            case "password": {
                ApplicantUser a = requireApplicant(req);
                String current = req.getParameter("currentPassword") == null ? "" : req.getParameter("currentPassword");
                String next = req.getParameter("newPassword") == null ? "" : req.getParameter("newPassword");
                String confirm = req.getParameter("confirmPassword") == null ? "" : req.getParameter("confirmPassword");
                try {
                    services().getApplicantAuthService().changePassword(a.getApplicantId(), current, next, confirm);
                } catch (BusinessException e) {
                    addMessage(req, "danger", e.getMessage());
                    render(req, res, "my/password.jsp");
                    return;
                }
                flashMessage(req, "success", "I017");
                redirect(req, res, "/my/menu");
                return;
            }
            default:
                res.sendError(404);
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String loginId = param(req, "loginId");
        String password = req.getParameter("password") == null ? "" : req.getParameter("password");
        Optional<ApplicantUser> u = loginId.isEmpty() || loginId.length() > 12 || password.isEmpty() || password.length() > 64
                ? Optional.empty() : services().getApplicantAuthService().login(loginId, password);
        if (u.isEmpty()) {
            addMessage(req, "danger", Messages.get("E113"));
            req.setAttribute("loginId", loginId);
            render(req, res, "my/login.jsp");
            return;
        }
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession s = req.getSession(true);
        s.setAttribute(SESSION_APPLICANT, u.get());
        s.setMaxInactiveInterval(AppConfig.get().getInt("app.session-timeout-minutes", 30) * 60);
        csrfToken(req);
        redirect(req, res, "/my/menu");
    }

    /** AP07 メニュー：お知らせタブとメニュータブ。申込が複数ある場合は選択できる。 */
    private void menu(HttpServletRequest req, HttpServletResponse res, ApplicantUser a) throws ServletException, IOException {
        List<Application> apps = services().getApplicationQueryService().applicationsOfApplicant(a.getApplicantId());
        Long selectedId = longParam(req, "app");
        Application selected = null;
        for (Application app : apps) {
            if (selectedId != null && app.getApplicationId() == selectedId) {
                selected = app;
            }
        }
        if (selected == null && !apps.isEmpty()) {
            selected = apps.get(0);
        }
        List<MenuTile> tiles = new ArrayList<>();
        ApplicationDetail d = null;
        ConsentView cv = null;
        if (selected != null) {
            d = services().getApplicationQueryService().detailForApplicant(selected.getApplicationId(), a.getApplicantId());
            cv = services().getConsentService().resolve(services().getConsentService().byApplicant(a.getApplicantId(), selected.getApplicationId()));
            tiles = applicantTiles(selected, cv);
        }
        req.setAttribute("applications", apps);
        req.setAttribute("d", d);
        req.setAttribute("cv", cv);
        req.setAttribute("tiles", tiles);
        req.setAttribute("notices", Tx.execute(conn -> services().getNotificationDao().findForApplicant(conn, a.getApplicantId(), 50)));
        req.setAttribute("tab", "notice".equals(param(req, "tab")) ? "notice" : "menu");
        render(req, res, "my/menu.jsp");
    }

    private static List<MenuTile> applicantTiles(Application app, ConsentView cv) {
        String base = "/my/applications/" + app.getApplicationId();
        List<MenuTile> tiles = new ArrayList<>();
        tiles.add(new MenuTile("detail", "申込確認", "お申込内容と現在の手続きの状況を確認します。").link(base).style("outline-primary"));
        MenuTile consent = new MenuTile("consent", "内容確認・同意", "申込内容を確認して確定し、同意または差戻しを行います。");
        if (cv != null && (cv.getOutcome() == ConsentView.Outcome.CONFIRM || cv.getOutcome() == ConsentView.Outcome.AGREE)) {
            consent.link(base + "/consent").style("primary");
        } else if (cv != null && cv.getOutcome() == ConsentView.Outcome.EXPIRED) {
            consent.disabled("確認期限が切れています。担当者にご連絡ください。");
        } else if (StatusCd.isCanceled(app.getStatusCd())) {
            consent.disabled("このお申込は取り消されています。");
        } else {
            consent.disabled("現在、お手続きはありません。手続きが必要になるとお知らせが届きます。");
        }
        tiles.add(consent);
        tiles.add(new MenuTile("notice", "お知らせ", "確認依頼やアカウントのご案内など、お届けしたお知らせを表示します。").link("/my/menu?app=" + app.getApplicationId() + "&tab=notice").style("outline-secondary"));
        tiles.add(new MenuTile("password", "パスワード変更", "ログインパスワードを変更します。初期パスワードのままの場合は変更してください。").link("/my/password").style("outline-secondary"));
        return tiles;
    }

    private void application(HttpServletRequest req, HttpServletResponse res, ApplicantUser a, List<String> parts) throws ServletException, IOException {
        Long id = parts.size() > 1 ? parseId(parts.get(1)) : null;
        if (id == null) {
            res.sendError(404);
            return;
        }
        String sub = parts.size() > 2 ? parts.get(2) : "";
        String sub2 = parts.size() > 3 ? parts.get(3) : "";
        if (sub.isEmpty()) {
            ApplicationDetail d = services().getApplicationQueryService().detailForApplicant(id, a.getApplicantId());
            req.setAttribute("d", d);
            req.setAttribute("cv", services().getConsentService().resolve(services().getConsentService().byApplicant(a.getApplicantId(), id)));
            render(req, res, "my/application.jsp");
            return;
        }
        if (!"consent".equals(sub)) {
            res.sendError(404);
            return;
        }
        ConsentService cs = services().getConsentService();
        ConsentView v = cs.resolve(cs.byApplicant(a.getApplicantId(), id));
        String base = "/my/applications/" + id + "/consent";
        req.setAttribute("v", v);
        req.setAttribute("consentBase", base);
        req.setAttribute("portal", true);
        switch (v.getOutcome()) {
            case CONFIRM:
                if ("edit".equals(sub2)) {
                    if (v.isContractChange()) {
                        redirect(req, res, base);
                        return;
                    }
                    req.setAttribute("form", ApplicationForm.from(v.getVersion()));
                    req.setAttribute("errors", java.util.Map.of());
                    render(req, res, "consent/ap02_edit.jsp");
                } else {
                    render(req, res, "consent/ap01_confirm.jsp");
                }
                return;
            case AGREE:
                render(req, res, "consent/ap03_agree.jsp");
                return;
            case EXPIRED:
                flashMessage(req, "warning", "E202");
                redirect(req, res, "/my/menu?app=" + id);
                return;
            default:
                flash(req, "info", "現在、お手続きはありません。");
                redirect(req, res, "/my/menu?app=" + id);
        }
    }

    private void postConsent(HttpServletRequest req, HttpServletResponse res, ApplicantUser a, List<String> parts) throws ServletException, IOException {
        Long id = parts.size() > 1 ? parseId(parts.get(1)) : null;
        if (id == null || parts.size() < 4 || !"consent".equals(parts.get(2))) {
            res.sendError(404);
            return;
        }
        String op = parts.get(3);
        ConsentService cs = services().getConsentService();
        ConsentService.Resolver r = cs.byApplicant(a.getApplicantId(), id);
        String base = "/my/applications/" + id + "/consent";
        String ip = clientIp(req);
        switch (op) {
            case "edit": {
                ApplicationForm form = ApplicationForm.bind(req);
                Validation errors = form.validate(true, false);
                if (errors.hasErrors()) {
                    req.setAttribute("v", cs.resolve(r));
                    req.setAttribute("consentBase", base);
                    req.setAttribute("portal", true);
                    req.setAttribute("form", form);
                    req.setAttribute("errors", errors.getErrors());
                    render(req, res, "consent/ap02_edit.jsp");
                    return;
                }
                cs.saveEdit(r, form.toVersion());
                flashMessage(req, "success", "I001");
                redirect(req, res, base);
                return;
            }
            case "confirm":
                if (!"1".equals(param(req, "checked"))) {
                    throw new BusinessException("E001", "内容を確認しました");
                }
                cs.confirm(r, ip);
                redirect(req, res, base);
                return;
            case "agree": {
                String action = param(req, "action");
                switch (action) {
                    case "agree":
                        if (!"1".equals(param(req, "agreed"))) {
                            throw new BusinessException("E001", "同意事項に同意します");
                        }
                        cs.agree(r, ip);
                        flashMessage(req, "success", "I007");
                        redirect(req, res, "/my/menu?app=" + id);
                        return;
                    case "return": {
                        String reason = req.getParameter("returnReason") == null ? "" : req.getParameter("returnReason").strip();
                        if (reason.length() > 500) {
                            throw new BusinessException("E002", "差戻し理由", 500);
                        }
                        cs.returnToOwner(r, reason, ip);
                        flashMessage(req, "info", "I008");
                        redirect(req, res, "/my/menu?app=" + id);
                        return;
                    }
                    case "modify":
                        cs.modify(r, ip);
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

    /** 未使用（社員のログイン情報は申込者ポータルでは参照しない）。 */
    @SuppressWarnings("unused")
    private static LoginUser none() {
        return null;
    }
}
