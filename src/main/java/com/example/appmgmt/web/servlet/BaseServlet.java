package com.example.appmgmt.web.servlet;

import com.example.appmgmt.common.AuditContext;
import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.ForbiddenException;
import com.example.appmgmt.common.Messages;
import com.example.appmgmt.common.TokenUtil;
import com.example.appmgmt.domain.ApplicantUser;
import com.example.appmgmt.domain.LoginUser;
import com.example.appmgmt.service.Services;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Servlet 共通：ログイン情報、CSRF トークン、フラッシュメッセージ、描画、業務エラーの扱い。 */
public abstract class BaseServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(BaseServlet.class);
    protected static final String VIEW_ROOT = "/WEB-INF/views/";
    public static final String SESSION_USER = "loginUser";
    public static final String SESSION_APPLICANT = "loginApplicant";
    public static final String SESSION_CSRF = "csrfToken";
    public static final String SESSION_FLASH = "flashMessages";

    /** 画面メッセージ（level は Bootstrap の alert 種別：success／info／warning／danger）。 */
    public static class Msg {
        private final String level;
        private final String text;

        public Msg(String level, String text) {
            this.level = level;
            this.text = text;
        }

        public String getLevel() { return level; }
        public String getText() { return text; }
    }

    protected Services services() {
        return Services.get();
    }

    protected LoginUser user(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (LoginUser) s.getAttribute(SESSION_USER);
    }

    protected LoginUser requireUser(HttpServletRequest req) {
        LoginUser u = user(req);
        if (u == null) {
            throw new ForbiddenException();
        }
        return u;
    }

    protected ApplicantUser applicant(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (ApplicantUser) s.getAttribute(SESSION_APPLICANT);
    }

    protected ApplicantUser requireApplicant(HttpServletRequest req) {
        ApplicantUser a = applicant(req);
        if (a == null) {
            throw new ForbiddenException();
        }
        return a;
    }

    public static String csrfToken(HttpServletRequest req) {
        HttpSession s = req.getSession(true);
        String t = (String) s.getAttribute(SESSION_CSRF);
        if (t == null) {
            t = TokenUtil.newToken();
            s.setAttribute(SESSION_CSRF, t);
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    protected void flash(HttpServletRequest req, String level, String text) {
        HttpSession s = req.getSession(true);
        List<Msg> list = (List<Msg>) s.getAttribute(SESSION_FLASH);
        if (list == null) {
            list = new ArrayList<>();
            s.setAttribute(SESSION_FLASH, list);
        }
        list.add(new Msg(level, text));
    }

    protected void flashMessage(HttpServletRequest req, String level, String messageId, Object... args) {
        flash(req, level, Messages.get(messageId, args));
    }

    /** 画面に出すメッセージをリクエストに積む（リダイレクトしない場合）。 */
    @SuppressWarnings("unchecked")
    protected void addMessage(HttpServletRequest req, String level, String text) {
        List<Msg> list = (List<Msg>) req.getAttribute("messages");
        if (list == null) {
            list = new ArrayList<>();
            req.setAttribute("messages", list);
        }
        list.add(new Msg(level, text));
    }

    @SuppressWarnings("unchecked")
    protected void render(HttpServletRequest req, HttpServletResponse res, String view) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s != null) {
            List<Msg> flashed = (List<Msg>) s.getAttribute(SESSION_FLASH);
            if (flashed != null) {
                s.removeAttribute(SESSION_FLASH);
                for (Msg m : flashed) {
                    addMessage(req, m.getLevel(), m.getText());
                }
            }
        }
        req.setAttribute("user", user(req));
        req.setAttribute("applicantUser", applicant(req));
        req.setAttribute("csrf", csrfToken(req));
        req.setAttribute("ctx", req.getContextPath());
        res.setContentType("text/html; charset=UTF-8");
        req.getRequestDispatcher(VIEW_ROOT + view).forward(req, res);
    }

    protected void redirect(HttpServletRequest req, HttpServletResponse res, String path) throws IOException {
        res.sendRedirect(req.getContextPath() + path);
    }

    protected static String param(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? "" : v.trim();
    }

    protected static Long longParam(HttpServletRequest req, String name) {
        String v = param(req, name);
        if (v.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static int intParam(HttpServletRequest req, String name, int def) {
        String v = param(req, name);
        if (v.isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    protected static int rowVersion(HttpServletRequest req) {
        return intParam(req, "rowVersion", -1);
    }

    protected static List<String> pathParts(HttpServletRequest req) {
        String p = req.getPathInfo();
        if (p == null) {
            return List.of();
        }
        return Arrays.stream(p.split("/")).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    protected static Long parseId(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    /** 業務エラー時の戻り先（既定は申込一覧）。 */
    protected String fallbackPath(HttpServletRequest req) {
        return "/emp/applications";
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        LoginUser u = user(req);
        ApplicantUser a = applicant(req);
        AuditContext.set(u != null ? AuditContext.employee(u.getEmployeeId()) : a != null ? AuditContext.applicant(a.getApplicantId()) : "ANONYMOUS");
        try {
            super.service(req, res);
        } catch (BusinessException e) {
            log.info("業務エラー {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
            flash(req, "danger", e.getMessage());
            redirect(req, res, fallbackPath(req));
        } catch (ServletException | IOException | RuntimeException e) {
            String errorId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.error("システムエラー errorId={} {} {}", errorId, req.getMethod(), req.getRequestURI(), e);
            req.setAttribute("errorId", errorId);
            throw e;
        } finally {
            AuditContext.clear();
        }
    }

    protected static boolean eq(Object a, Object b) {
        return Objects.equals(a, b);
    }
}
