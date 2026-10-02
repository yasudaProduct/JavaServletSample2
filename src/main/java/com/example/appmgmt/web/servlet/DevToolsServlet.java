package com.example.appmgmt.web.servlet;

import com.example.appmgmt.common.Tx;
import com.example.appmgmt.domain.ExternalLink;
import com.example.appmgmt.service.external.ReceiveResult;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 開発支援画面（app.dev-tools.enabled=true のときだけ有効）。
 * DV01 通知一覧（確認用 URL を含む本文を表示）、DV02 審査担当部門システムモック（IF02／IF04 と同じ処理で結果を返す）、バッチ即時実行。
 */
public class DevToolsServlet extends BaseServlet {

    @Override
    protected String fallbackPath(HttpServletRequest req) {
        return "/emp/dev/external-mock";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        requireUser(req);
        List<String> parts = pathParts(req);
        String page = parts.isEmpty() ? "" : parts.get(0);
        switch (page) {
            case "notifications":
                req.setAttribute("notifications", Tx.execute(conn -> services().getNotificationDao().findRecent(conn, 50)));
                req.setAttribute("mailMode", com.example.appmgmt.common.AppConfig.get().getString("mail.mode", "log"));
                render(req, res, "dev/notifications.jsp");
                return;
            case "external-mock":
                req.setAttribute("awaiting", Tx.execute(conn -> services().getExternalLinkDao().findAwaitingResult(conn, 50)));
                req.setAttribute("recent", Tx.execute(conn -> services().getExternalLinkDao().findRecent(conn, 30)));
                req.setAttribute("clientDescription", services().getExternalApiClient().describe());
                render(req, res, "dev/external_mock.jsp");
                return;
            default:
                redirect(req, res, "/emp/dev/external-mock");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        requireUser(req);
        List<String> parts = pathParts(req);
        String page = parts.isEmpty() ? "" : parts.get(0);
        String sub = parts.size() > 1 ? parts.get(1) : "";
        if ("batch".equals(page) && "run".equals(sub)) {
            String job = param(req, "job");
            if ("BT01".equals(job)) {
                services().getNotificationSendJob().run();
            } else if ("BT02".equals(job)) {
                services().getExternalLinkSendJob().run();
            } else {
                services().getExternalLinkSendJob().run();
                services().getNotificationSendJob().run();
            }
            flash(req, "success", (job.isEmpty() ? "BT02 と BT01" : job) + " を実行しました。");
            String back = param(req, "back");
            redirect(req, res, back.startsWith("/emp/") ? back : "/emp/dev/external-mock");
            return;
        }
        if ("external-mock".equals(page) && "result".equals(sub)) {
            Long linkId = longParam(req, "externalLinkId");
            String result = param(req, "result");
            String reason = req.getParameter("reason") == null ? "" : req.getParameter("reason").strip();
            ExternalLink link = linkId == null ? null : Tx.execute(conn -> services().getExternalLinkDao().findById(conn, linkId).orElse(null));
            if (link == null) {
                flash(req, "danger", "外部連携が見つかりません。");
            } else {
                ReceiveResult r = services().getExternalResultService().receive(link.isPrecheck(), link.getExternalReceiptNo(), link.getExternalLinkId(), result, reason);
                flash(req, "OK".equals(r.getStatus()) ? "success" : "danger",
                        (r.getErrorCode() == null ? "" : r.getErrorCode() + " ") + r.getMessage() + (r.getNewStatusCd() == null ? "" : "（申込番号 " + r.getApplicationNo() + "、ステータス " + r.getNewStatusCd() + "）"));
            }
            redirect(req, res, "/emp/dev/external-mock");
            return;
        }
        res.sendError(404);
    }
}
