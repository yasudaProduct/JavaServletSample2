package com.example.appmgmt.web.servlet;

import com.example.appmgmt.common.Messages;
import com.example.appmgmt.domain.LoginUser;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** SC01 ログイン／ログアウト。 */
public class LoginServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (req.getRequestURI().endsWith("/logout")) {
            redirect(req, res, "/emp/login");
            return;
        }
        if (user(req) != null) {
            redirect(req, res, "/emp/applications");
            return;
        }
        if ("1".equals(param(req, "timeout"))) {
            addMessage(req, "warning", Messages.get("E110"));
        }
        render(req, res, "emp/login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (req.getRequestURI().endsWith("/logout")) {
            HttpSession s = req.getSession(false);
            if (s != null) {
                s.invalidate();
            }
            HttpSession n = req.getSession(true);
            csrfToken(req);
            n.setAttribute(SESSION_FLASH, new java.util.ArrayList<>(java.util.List.of(new Msg("info", Messages.get("I013")))));
            redirect(req, res, "/emp/login");
            return;
        }
        String employeeNo = param(req, "employeeNo");
        String password = req.getParameter("password") == null ? "" : req.getParameter("password");
        if (employeeNo.isEmpty() || employeeNo.length() > 10 || password.isEmpty() || password.length() > 64) {
            addMessage(req, "danger", Messages.get("E106"));
            req.setAttribute("employeeNo", employeeNo);
            render(req, res, "emp/login.jsp");
            return;
        }
        Optional<LoginUser> u = services().getAuthService().login(employeeNo, password);
        if (u.isEmpty()) {
            addMessage(req, "danger", Messages.get("E106"));
            req.setAttribute("employeeNo", employeeNo);
            render(req, res, "emp/login.jsp");
            return;
        }
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession s = req.getSession(true);
        s.setAttribute(SESSION_USER, u.get());
        s.setMaxInactiveInterval(com.example.appmgmt.common.AppConfig.get().getInt("app.session-timeout-minutes", 30) * 60);
        csrfToken(req);
        redirect(req, res, "/emp/applications");
    }
}
