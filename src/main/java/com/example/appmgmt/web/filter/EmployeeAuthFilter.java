package com.example.appmgmt.web.filter;

import com.example.appmgmt.common.AppConfig;
import com.example.appmgmt.common.Messages;
import com.example.appmgmt.domain.LoginUser;
import com.example.appmgmt.web.servlet.BaseServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 申込受付会社向け画面（/emp/*）の認証・認可 Filter。
 * 未ログインはログイン画面へ。権限外の URL は E103 を表示して申込一覧へ戻す。
 */
public class EmployeeAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.equals("/emp/login") || path.equals("/emp/logout")) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = req.getSession(false);
        LoginUser user = session == null ? null : (LoginUser) session.getAttribute(BaseServlet.SESSION_USER);
        if (user == null) {
            boolean timedOut = req.getRequestedSessionId() != null && !req.isRequestedSessionIdValid();
            res.sendRedirect(req.getContextPath() + "/emp/login" + (timedOut ? "?timeout=1" : ""));
            return;
        }
        if (!authorized(path, user)) {
            addFlash(session, "danger", Messages.get("E103"));
            res.sendRedirect(req.getContextPath() + "/emp/applications");
            return;
        }
        chain.doFilter(request, response);
    }

    static boolean authorized(String path, LoginUser user) {
        if (path.startsWith("/emp/import")) {
            return user.isOwner();
        }
        if (path.startsWith("/emp/master")) {
            return user.isAdmin();
        }
        if (path.equals("/emp/applications/new")) {
            return user.isOwner();
        }
        if (path.startsWith("/emp/dev")) {
            return AppConfig.get().getBoolean("app.dev-tools.enabled", false);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static void addFlash(HttpSession session, String level, String text) {
        List<BaseServlet.Msg> list = (List<BaseServlet.Msg>) session.getAttribute(BaseServlet.SESSION_FLASH);
        if (list == null) {
            list = new ArrayList<>();
            session.setAttribute(BaseServlet.SESSION_FLASH, list);
        }
        list.add(new BaseServlet.Msg(level, text));
    }
}
