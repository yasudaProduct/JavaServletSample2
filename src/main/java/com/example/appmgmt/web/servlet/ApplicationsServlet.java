package com.example.appmgmt.web.servlet;

import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.ForbiddenException;
import com.example.appmgmt.common.Formats;
import com.example.appmgmt.common.Messages;
import com.example.appmgmt.common.TransitionNotAllowedException;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.common.Validation;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.LoginUser;
import com.example.appmgmt.domain.StatusCd;
import com.example.appmgmt.service.application.ApplicationDetail;
import com.example.appmgmt.service.application.ApplicationQueryService;
import com.example.appmgmt.service.application.PhaseGroup;
import com.example.appmgmt.service.approval.ApprovalFlowView;
import com.example.appmgmt.service.approval.ApprovalService;
import com.example.appmgmt.service.transition.StatusTransitionService;
import com.example.appmgmt.service.transition.TransitionResult;
import com.example.appmgmt.web.form.ApplicationForm;
import com.example.appmgmt.web.view.MenuSection;
import com.example.appmgmt.web.view.MenuTile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 申込受付会社向けの申込画面（/emp/applications/*）。
 * SC02 一覧、SC03 詳細、SC04 入力、SC05 内容確認、SC06 承認フロー、SC07 申込修正、SC08 契約変更入力、SC09 契約変更内容確認。
 */
public class ApplicationsServlet extends BaseServlet {

    @Override
    protected String fallbackPath(HttpServletRequest req) {
        List<String> parts = pathParts(req);
        if (!parts.isEmpty() && parseId(parts.get(0)) != null) {
            return "/emp/applications/" + parts.get(0) + "/menu";
        }
        return "/emp/applications";
    }

    private static String menuPath(long id) {
        return "/emp/applications/" + id + "/menu";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        LoginUser user = requireUser(req);
        List<String> parts = pathParts(req);
        if (parts.isEmpty()) {
            list(req, res, user);
            return;
        }
        if (parts.size() == 1 && "new".equals(parts.get(0))) {
            showInput(req, res, user, null, "new", ApplicationForm.bind(req), null);
            return;
        }
        Long id = parseId(parts.get(0));
        if (id == null) {
            res.sendError(404);
            return;
        }
        String sub = parts.size() > 1 ? parts.get(1) : "";
        String sub2 = parts.size() > 2 ? parts.get(2) : "";
        switch (sub) {
            case "": detail(req, res, user, id); break;
            case "menu": menu(req, res, user, id); break;
            case "maintenance": maintenance(req, res, user, id); break;
            case "additional": showAdditional(req, res, user, id); break;
            case "edit": showEdit(req, res, user, id); break;
            case "confirm": showConfirm(req, res, user, id); break;
            case "approval": showApproval(req, res, user, id); break;
            case "revise": showRevise(req, res, user, id, null, null); break;
            case "change":
                if ("confirm".equals(sub2)) {
                    showChangeConfirm(req, res, user, id);
                } else {
                    showChangeInput(req, res, user, id, null, null);
                }
                break;
            default: res.sendError(404);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        LoginUser user = requireUser(req);
        List<String> parts = pathParts(req);
        if (parts.size() == 1 && "new".equals(parts.get(0))) {
            postInput(req, res, user, null, null);
            return;
        }
        Long id = parts.isEmpty() ? null : parseId(parts.get(0));
        if (id == null) {
            res.sendError(404);
            return;
        }
        String sub = parts.size() > 1 ? parts.get(1) : "";
        String sub2 = parts.size() > 2 ? parts.get(2) : "";
        switch (sub) {
            case "additional": postInput(req, res, user, null, id); break;
            case "edit": postInput(req, res, user, id, null); break;
            case "confirm": postConfirm(req, res, user, id); break;
            case "approval": postApproval(req, res, user, id); break;
            case "revise": postRevise(req, res, user, id); break;
            case "maintenance": postMaintenance(req, res, user, id, sub2); break;
            case "change":
                if ("confirm".equals(sub2)) {
                    postChangeConfirm(req, res, user, id);
                } else {
                    postChangeInput(req, res, user, id);
                }
                break;
            default: postAction(req, res, user, id, sub);
        }
    }

    // ------------------------------------------------------------------ SC02

    private void list(HttpServletRequest req, HttpServletResponse res, LoginUser user) throws ServletException, IOException {
        ApplicationDao.Criteria c = new ApplicationDao.Criteria();
        boolean searched = req.getParameter("search") != null;
        c.applicationNo = param(req, "applicationNo");
        c.applicantKey = param(req, "applicantKey");
        String[] sts = req.getParameterValues("statusCd");
        if (sts != null) {
            c.statusCds = new ArrayList<>(Arrays.asList(sts));
        }
        String[] cats = req.getParameterValues("categoryL");
        String[] mids = req.getParameterValues("categoryM");
        c.ownerEmployeeId = longParam(req, "ownerEmployeeId");
        Validation v = new Validation();
        c.createdFrom = parseDateParam(req, "createdFrom", v);
        c.createdTo = parseDateParam(req, "createdTo", v);
        if (c.createdFrom != null && c.createdTo != null && c.createdTo.isBefore(c.createdFrom)) {
            v.reject("createdTo", "E007", "登録日 To", "登録日 From");
        }
        c.myTasksOnly = searched ? "1".equals(param(req, "myTasksOnly")) : !user.isAdmin();
        c.page = intParam(req, "page", 1);
        c.sort = param(req, "sort").isEmpty() ? "updatedAt" : param(req, "sort");
        c.desc = !"asc".equals(param(req, "order"));
        // 大分類・中分類の選択を個別ステータスに展開する
        List<com.example.appmgmt.domain.Status> statuses = services().getApplicationQueryService().allStatuses();
        if ((cats != null && cats.length > 0) || (mids != null && mids.length > 0)) {
            Set<String> catSet = cats == null ? Set.of() : Set.of(cats);
            Set<String> midSet = mids == null ? Set.of() : Set.of(mids);
            for (com.example.appmgmt.domain.Status s : statuses) {
                boolean catOk = catSet.isEmpty() || catSet.contains(s.getCategoryL());
                boolean midOk = midSet.isEmpty() || midSet.contains(s.getCategoryM());
                if (catOk && midOk && !c.statusCds.contains(s.getStatusCd())) {
                    c.statusCds.add(s.getStatusCd());
                }
            }
        }
        req.setAttribute("criteria", c);
        req.setAttribute("statuses", statuses);
        req.setAttribute("employees", services().getApplicationQueryService().validEmployees());
        req.setAttribute("errors", v.getErrors());
        req.setAttribute("selectedCategoryL", cats == null ? List.of() : Arrays.asList(cats));
        req.setAttribute("selectedCategoryM", mids == null ? List.of() : Arrays.asList(mids));
        if (v.hasErrors()) {
            ApplicationQueryService.SearchResult empty = new ApplicationQueryService.SearchResult();
            empty.rows = List.of();
            empty.page = 1;
            empty.pageSize = 20;
            req.setAttribute("result", empty);
        } else {
            req.setAttribute("result", services().getApplicationQueryService().search(c, user));
        }
        StringBuilder qs = new StringBuilder();
        for (java.util.Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
            if ("page".equals(e.getKey())) {
                continue;
            }
            for (String val : e.getValue()) {
                qs.append(qs.length() == 0 ? "?" : "&").append(java.net.URLEncoder.encode(e.getKey(), java.nio.charset.StandardCharsets.UTF_8)).append('=')
                        .append(java.net.URLEncoder.encode(val, java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        req.setAttribute("queryString", qs.toString());
        render(req, res, "emp/applications/list.jsp");
    }

    private static java.time.LocalDate parseDateParam(HttpServletRequest req, String name, Validation v) {
        String s = param(req, name);
        if (s.isEmpty()) {
            return null;
        }
        java.time.LocalDate d = Formats.parseDate(s);
        if (d == null) {
            v.reject(name, "E003", "createdFrom".equals(name) ? "登録日 From" : "登録日 To");
        }
        return d;
    }

    // ------------------------------------------------------------------ SC03

    private void detail(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        req.setAttribute("d", d);
        render(req, res, "emp/applications/detail.jsp");
    }

    /**
     * SC14 申込メニュー：申込に関する操作の起点。
     * 「申込全体」の操作（契約変更・追加申込・メンテナンス）と、手続きの区切りごとの領域（新規申込、契約変更 1、契約変更 2 …）に分けて表示する。
     * 領域は折りたたみを切り替えられ、各領域のタイルは常に同じ並びで、ステータス×権限で活性・非活性だけを切り替える。
     * 申込入力画面（SC04／SC08）へはメニューから直接進まず、申込内容確認（SC05／SC09）の「修正」から進む。
     */
    private void menu(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        String st = d.getApplication().getStatusCd();
        boolean inChange = StatusCd.isContractChange(st) && !StatusCd.CHG_REVIEWED.equals(st);
        String base = "/emp/applications/" + id;
        List<PhaseGroup> groups = PhaseGroup.build(d.getApplication(), d.getVersions());

        // ---- 申込全体の操作 ----
        List<MenuTile> general = new ArrayList<>();
        if (d.has("startChange")) {
            general.add(new MenuTile("startChange", "契約変更", "審査完了版を複写した版を作り、契約変更の入力を始めます。新しい「契約変更 " + groups.size() + "」の領域が増えます。").post(base + "/startChange").style("info")
                    .confirm("契約変更手続きを開始します。審査完了版を複写した新しい版を作成します。よろしいですか？"));
        } else {
            general.add(new MenuTile("startChange", "契約変更", "審査完了後に契約変更手続きを始めます。")
                    .disabled(!user.isOwner() ? "担当者だけが行えます。" : inChange ? "契約変更の手続き中です。" : StatusCd.isCanceled(st) ? "取消済みの申込では行えません。" : "審査完了（10701／20701）の申込だけ開始できます。"));
        }
        if (d.has("additional")) {
            general.add(new MenuTile("additional", "追加申込", "同じ申込者の新しい申込を作ります。申込者アカウントを引き継ぎ、申込者情報と申込内容は複写して入力します。").link(base + "/additional").style("outline-primary"));
        } else {
            general.add(new MenuTile("additional", "追加申込", "同じ申込者の新しい申込を作ります。").disabled(user.isOwner() ? "審査完了（10701／20701）の申込だけ追加申込できます。" : "担当者だけが行えます。"));
        }
        boolean maintainer = user.isAdmin() || (user.isOwner() && d.getApplication().getOwnerEmployeeId() == user.getEmployeeId());
        if (maintainer) {
            general.add(new MenuTile("maintenance", "メンテナンス", "申込者への通知の履歴を確認し、申込者ページのパスワードを初期化します。").link(base + "/maintenance").style("outline-secondary"));
        } else {
            general.add(new MenuTile("maintenance", "メンテナンス", "申込者への通知の履歴の確認とパスワードの初期化を行います。").disabled("担当者または管理者だけが行えます。"));
        }

        // ---- 手続きごとの領域 ----
        List<MenuSection> sections = new ArrayList<>();
        for (PhaseGroup g : groups) {
            boolean live = g.isCurrent();
            boolean change = g.isContractChange();
            String gsuf = "-g" + g.getIndex();
            String why;
            if (live) {
                why = null;
            } else if (g.getState() == PhaseGroup.State.CANCELED) {
                why = change ? "この契約変更は取り消されています（審査差戻しまたは取消）。" : "申込は取り消されています。";
            } else {
                why = change ? "この契約変更は審査完了しています。操作は進行中の領域で行います。" : "新規申込の手続きは完了しています。操作は進行中の領域で行います。";
            }
            String badge = g.getState() == PhaseGroup.State.ACTIVE ? "primary" : g.getState() == PhaseGroup.State.COMPLETED ? "success" : "dark";
            String desc = "第 " + g.getFirstVersionNo() + " 版" + (g.getLastVersionNo() != g.getFirstVersionNo() ? "〜第 " + g.getLastVersionNo() + " 版" : "")
                    + (change && g.getBaseline() != null ? "　変更前：第 " + g.getBaseline().getVersionNo() + " 版" : "");
            MenuSection sec = new MenuSection(String.valueOf(g.getIndex()), g.getTitle(), g.getStateName(), badge, desc, live || groups.size() == 1);
            // 1. 内容確認（常に活性）
            sec.add(new MenuTile("confirm" + gsuf, change ? "契約変更内容確認" : "申込内容確認", change
                    ? (live ? "契約変更の変更前後を確認します。入力中・申請待ちでは、ここから「修正」で契約変更入力へ進み、「確定」で申請待ちにします。" : "この契約変更の変更前後の内容を参照します。")
                    : (live ? "申込内容を確認します。入力中・申請待ちでは、ここから「修正」で申込入力へ進み、「確定」で一次承認申請待ちにします。" : "新規申込の内容を参照します。"))
                    .link((change ? base + "/change/confirm" : base + "/confirm") + "?group=" + g.getIndex()).style(live ? "primary" : "outline-primary"));
            // 2. 申込修正（SC07）
            if (live && d.has("revise")) {
                sec.add(new MenuTile("revise" + gsuf, "申込修正", "同意後の金額項目を修正します（一部修正）。基準版との倍率で遷移先が決まります。").link(base + "/revise").style("outline-primary"));
            } else if (live && d.has("fix")) {
                sec.add(new MenuTile("revise" + gsuf, "申込修正", "審査担当部門の指摘に対して申込内容を修正し、事前確認を再依頼します（修正対応）。").link(base + "/revise").style("outline-primary"));
            } else {
                sec.add(new MenuTile("revise" + gsuf, "申込修正", "同意後の金額修正（一部修正）と、事前確認の指摘への修正対応を行います。")
                        .disabled(why != null ? why : user.isOwner() ? "最終承認申請待ち（金額の一部修正）と修正対応待ちの申込だけ行えます。" : "担当者だけが行えます。"));
            }
            // 3. 承認フロー（常に活性。過去の領域は履歴の参照）
            if (live && d.has("request")) {
                sec.add(new MenuTile("approval" + gsuf, "承認フロー", "回付先を設定して承認を申請します。").link(base + "/approval?group=" + g.getIndex()).style("success"));
            } else if (live && d.has("approvalFlow")) {
                sec.add(new MenuTile("approval" + gsuf, "承認フロー", "承認・差戻し・審査申請を行います。").link(base + "/approval?group=" + g.getIndex()).style("success"));
            } else {
                sec.add(new MenuTile("approval" + gsuf, "承認フロー", live && StatusCd.APPROVAL_IN_PROGRESS.contains(st)
                        ? "進行中の承認申請の状況（回付先・各ステップの結果）を確認します。申請中は申請できず、申請者は引戻しできません。"
                        : (change ? "この契約変更の" : "新規申込の") + "承認・申請履歴を確認します。").link(base + "/approval?group=" + g.getIndex()).style("outline-success"));
            }
            // 4. 引戻し
            if (live && d.has("pullBack")) {
                sec.add(new MenuTile("pullBack" + gsuf, "引戻し", "申込者確認中の申込を一次承認申請待ちへ戻します。確認用 URL は無効になります。").post(base + "/pullBack").style("warning")
                        .confirm("引戻しを行うと申込者の確認用 URL は無効になります。よろしいですか？"));
            } else {
                sec.add(new MenuTile("pullBack" + gsuf, "引戻し", "申込者確認中の申込を一次承認申請待ちへ戻します。")
                        .disabled(why != null ? why : !user.isOwner() ? "担当者だけが行えます。" : StatusCd.APPROVAL_IN_PROGRESS.contains(st)
                                ? "承認申請中は引戻しできません。承認者の処理をお待ちください。" : "申込者確認中（内容確認待ち・同意確認待ち）の申込だけ行えます。"));
            }
            // 5. 取消（新規申込：申込取消、契約変更：契約変更の取消）
            if (live && !change && d.has("cancel")) {
                sec.add(new MenuTile("cancel" + gsuf, "申込取消", "申込を取り消します。取り消した申込は以降操作できません。").post(base + "/cancel").style("danger")
                        .confirm("この申込を取り消します。取り消した申込は元に戻せません。よろしいですか？").withReasonInput());
            } else if (live && change && d.has("cancelChange")) {
                sec.add(new MenuTile("cancel" + gsuf, "契約変更の取消", "この契約変更を取り消して、契約変更前の審査完了の状態へ戻します（申込自体は残ります）。").post(base + "/cancel").style("danger")
                        .confirm("契約変更を取り消し、契約変更前の状態へ戻します。よろしいですか？").withReasonInput());
            } else {
                sec.add(new MenuTile("cancel" + gsuf, change ? "契約変更の取消" : "申込取消", change ? "この契約変更を取り消します。" : "申込を取り消します。")
                        .disabled(why != null ? why : !user.isOwner() ? "担当者だけが行えます。" : StatusCd.isCanceled(st) ? "取消済みです。" : "現在のステータスでは取り消せません（承認申請中・事前確認待ち・審査中・審査完了は不可）。"));
            }
            // 6. 確認依頼メール再送
            if (live && d.has("resendConsent")) {
                sec.add(new MenuTile("resendConsent" + gsuf, "確認依頼メール再送", "新しい確認用 URL を発行して申込者へ再送します。旧 URL は無効になります。").post(base + "/resendConsent").style("outline-secondary")
                        .confirm("確認依頼メールを再送します。旧 URL は使えなくなります。よろしいですか？"));
            } else {
                sec.add(new MenuTile("resendConsent" + gsuf, "確認依頼メール再送", "申込者への確認依頼メールを再送します。")
                        .disabled(why != null ? why : !user.isOwner() ? "担当者だけが行えます。" : "申込者確認中（内容確認待ち・同意確認待ち）の申込だけ再送できます。"));
            }
            // 7. 外部連携再送
            if (live && d.has("resendExternal")) {
                sec.add(new MenuTile("resendExternal" + gsuf, "外部連携再送", "送信エラーになった審査担当部門への依頼を再送します。").post(base + "/resendExternal").style("outline-danger")
                        .confirm("送信エラーの外部連携を再送します。よろしいですか？"));
            } else {
                sec.add(new MenuTile("resendExternal" + gsuf, "外部連携再送", "送信エラーになった審査担当部門への依頼を再送します。").disabled(why != null ? why : "送信エラーになった外部連携はありません。"));
            }
            sections.add(sec);
        }
        req.setAttribute("d", d);
        req.setAttribute("generalTiles", general);
        req.setAttribute("sections", sections);
        req.setAttribute("rowVersion", d.getApplication().getRowVersion());
        render(req, res, "emp/applications/menu.jsp");
    }

    // ------------------------------------------------------------------ SC15 メンテナンス

    private boolean canMaintain(LoginUser user, ApplicationDetail d) {
        return user.isAdmin() || (user.isOwner() && d.getApplication().getOwnerEmployeeId() == user.getEmployeeId());
    }

    /**
     * SC15 メンテナンス：申込者アカウント（ログイン用のデータ）の状態、申込者への通知の履歴、パスワードの初期化。担当者と管理者が使う。
     * 申込者情報（申込者名・メールアドレスなど）は申込データなので、ここでは変更しない（申込内容の入力・修正で変える）。
     */
    private void maintenance(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        if (!canMaintain(user, d)) {
            throw new ForbiddenException();
        }
        List<com.example.appmgmt.domain.Notification> notices = Tx.execute(conn -> services().getNotificationDao().findApplicantNoticesOfApplication(conn, id));
        req.setAttribute("d", d);
        req.setAttribute("account", d.getAccount());
        req.setAttribute("notices", notices);
        req.setAttribute("canReset", d.getAccount() != null);
        render(req, res, "emp/applications/maintenance.jsp");
    }

    private void postMaintenance(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id, String action) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        if (!canMaintain(user, d)) {
            throw new ForbiddenException();
        }
        if (!"resetPassword".equals(action)) {
            res.sendError(404);
            return;
        }
        services().getApplicantAuthService().resetPassword(id);
        flashMessage(req, "success", "I018");
        redirect(req, res, "/emp/applications/" + id + "/maintenance");
    }

    private static Integer groupParam(HttpServletRequest req) {
        return parseId(param(req, "group")) == null ? null : parseId(param(req, "group")).intValue();
    }

    /** SC14 の操作ボタン（POST /emp/applications/{id}/{action}）。 */
    private void postAction(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id, String action) throws ServletException, IOException {
        int rv = rowVersion(req);
        switch (action) {
            case "pullBack":
                services().getApplicationService().pullBack(id, rv, user);
                flashMessage(req, "success", "I009");
                break;
            case "startChange":
                services().getApplicationService().startChange(id, rv, user);
                flashMessage(req, "success", "I011");
                redirect(req, res, "/emp/applications/" + id + "/change");
                return;
            case "resendConsent":
                services().getApplicationService().resendConsent(id, rv, user);
                flashMessage(req, "success", "I006");
                break;
            case "resendExternal":
                services().getApplicationService().resendExternal(id, rv, user);
                flashMessage(req, "success", "I006");
                break;
            case "cancel": {
                String reason = req.getParameter("reason") == null ? "" : req.getParameter("reason").strip();
                if (reason.length() > 500) {
                    throw new BusinessException("E002", "取消理由", 500);
                }
                TransitionResult r = services().getApplicationService().cancel(id, rv, reason.isEmpty() ? null : reason, user);
                flashMessage(req, "success", StatusCd.isCanceled(r.getToStatusCd()) ? "I015" : "I016");
                break;
            }
            default:
                res.sendError(404);
                return;
        }
        redirect(req, res, menuPath(id));
    }

    // ------------------------------------------------------------------ SC04 申込入力

    /**
     * SC04 の表示。申込者情報（申込者名・カナ・電話番号・メールアドレス・住所）は申込データとして申込内容と同じ欄で入力する。
     * 新規申込は同じ氏名・メールアドレスでも別の申込者として扱い、申込者アカウントは一次承認で新しく発行する。
     * 同じ申込者の申込は追加申込（元の申込が審査完了のときだけ）で作り、元の申込のアカウントを引き継ぐ。
     */
    private void showInput(HttpServletRequest req, HttpServletResponse res, LoginUser user, ApplicationDetail d, String mode, ApplicationForm form,
            Validation errors) throws ServletException, IOException {
        if (!user.isOwner()) {
            throw new ForbiddenException();
        }
        req.setAttribute("d", d);
        req.setAttribute("mode", mode);
        req.setAttribute("form", form);
        req.setAttribute("errors", errors == null ? java.util.Map.of() : errors.getErrors());
        render(req, res, "emp/applications/input.jsp");
    }

    /** 追加申込の元にする申込。担当社員本人で、審査完了（10701／20701）の申込に限る。 */
    private ApplicationDetail additionalSource(HttpServletRequest req, LoginUser user, long sourceId) {
        ApplicationDetail src = services().getApplicationQueryService().detail(sourceId, user);
        if (src.getApplication().getOwnerEmployeeId() != user.getEmployeeId()) {
            throw new ForbiddenException();
        }
        if (!StatusCd.REVIEWED_ALL.contains(src.getApplication().getStatusCd())) {
            throw new TransitionNotAllowedException();
        }
        req.setAttribute("sourceApplication", src.getApplication());
        req.setAttribute("sourceApplicant", src.getApplicant());
        return src;
    }

    private void showAdditional(HttpServletRequest req, HttpServletResponse res, LoginUser user, long sourceId) throws ServletException, IOException {
        ApplicationDetail src = additionalSource(req, user, sourceId);
        showInput(req, res, user, null, "additional", ApplicationForm.from(src.getCurrentVersion()), null);
    }

    private void showEdit(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        if (!StatusCd.INPUT.equals(d.getApplication().getStatusCd())) {
            throw new TransitionNotAllowedException();
        }
        showInput(req, res, user, d, "edit", ApplicationForm.from(d.getCurrentVersion()), null);
    }

    /** SC04 の一時保存／確認へ。applicationId が null なら新規申込・追加申込（作成）。 */
    private void postInput(HttpServletRequest req, HttpServletResponse res, LoginUser user, Long applicationId, Long sourceId) throws ServletException, IOException {
        String action = param(req, "action");
        boolean toConfirm = "confirm".equals(action);
        ApplicationForm form = ApplicationForm.bind(req);
        boolean creating = applicationId == null;
        String mode = creating ? (sourceId == null ? "new" : "additional") : "edit";
        ApplicationDetail d = creating ? null : services().getApplicationQueryService().detail(applicationId, user);
        if (sourceId != null) {
            additionalSource(req, user, sourceId);
        }
        Validation v = form.validate(toConfirm, false);
        if (v.hasErrors()) {
            showInput(req, res, user, d, mode, form, v);
            return;
        }
        long id;
        if (creating) {
            id = services().getApplicationService().create(user, form.toVersion(), sourceId);
        } else {
            services().getApplicationService().saveDraft(applicationId, rowVersion(req), form.toVersion(), user);
            id = applicationId;
        }
        if (toConfirm) {
            redirect(req, res, "/emp/applications/" + id + "/confirm");
        } else {
            flashMessage(req, "success", "I012");
            redirect(req, res, "/emp/applications/" + id + "/edit");
        }
    }

    // ------------------------------------------------------------------ SC05 申込内容確認

    /**
     * SC05 申込内容確認。申込を参照できる利用者なら全ステータスで開ける。
     * 「修正」（入力画面へ。10100／10201 は 10101 へ戻し、10402／10501 は全体修正）と「確定」（10100／10101 → 10201）は活性・非活性で制御する。
     */
    private void showConfirm(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        List<PhaseGroup> groups = PhaseGroup.build(d.getApplication(), d.getVersions());
        PhaseGroup g = PhaseGroup.find(groups, groupParam(req));
        if (g.isContractChange()) {
            redirect(req, res, "/emp/applications/" + id + "/change/confirm?group=" + g.getIndex());
            return;
        }
        String st = d.getApplication().getStatusCd();
        boolean live = g.isCurrent();
        ApplicationVersion view = live ? d.getCurrentVersion() : g.getDisplayVersion();
        boolean owner = user.isOwner() && d.getApplication().getOwnerEmployeeId() == user.getEmployeeId();
        ApplicationForm form = ApplicationForm.from(view);
        Validation v = form.validate(true, false);
        boolean editable = live && Set.of(StatusCd.IMPORTED, StatusCd.INPUT).contains(st);
        boolean canModify = live && owner && (d.has("input") || d.has("modify"));
        boolean fullRevise = live && owner && d.has("fullRevise");
        req.setAttribute("d", d);
        req.setAttribute("group", g);
        req.setAttribute("viewVersion", view);
        req.setAttribute("diffBase", copiedFrom(d, view));
        req.setAttribute("errors", editable ? v.getErrors() : java.util.Map.of());
        req.setAttribute("canModify", canModify);
        req.setAttribute("fullRevise", fullRevise);
        req.setAttribute("modifyNote", canModify ? (fullRevise ? "「修正」を押すと申込内容全体を修正し直すため入力中へ戻り、申込者の同意を取り直します（全体修正）。" : StatusCd.INPUT.equals(st) ? "" : "「修正」を押すと入力中（10101）に戻って申込入力画面を開きます。")
                : !live ? "完了した手続きの内容です（参照のみ）。" : !owner ? "修正・確定は担当者だけが行えます。" : StatusCd.isCanceled(st) ? "取消済みの申込は修正できません。" : StatusCd.REVIEWED.equals(st) ? "審査完了後の変更は「契約変更」から行います。" : "現在のステータスでは修正できません（承認申請中・申込者確認中・事前確認待ち・審査中）。");
        req.setAttribute("canConfirm", owner && editable && !v.hasErrors());
        req.setAttribute("confirmNote", owner && editable ? (v.hasErrors() ? "入力内容に誤りがあるため確定できません。「修正」で入力し直してください。" : "") : !owner || !live ? "" : "確定済みです。");
        render(req, res, "emp/applications/confirm.jsp");
    }

    /** 全体修正で作った版（版種別 2）の複写元。差分を赤字で示すために使う。該当しなければ null。 */
    private static ApplicationVersion copiedFrom(ApplicationDetail d, ApplicationVersion view) {
        if (view == null || !Codes.VERSION_NEW_REVISED.equals(view.getVersionType()) || view.getCopiedFromVersionNo() == null) {
            return null;
        }
        for (ApplicationVersion v : d.getVersions()) {
            if (v.getVersionNo() == view.getCopiedFromVersionNo()) {
                return v;
            }
        }
        return null;
    }

    private void postConfirm(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        String action = param(req, "action");
        if ("modify".equals(action)) {
            ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
            String st = d.getApplication().getStatusCd();
            if (StatusCd.INPUT.equals(st)) {
                redirect(req, res, "/emp/applications/" + id + "/edit");
                return;
            }
            // 10100／10201 → 10101（同じ版を編集）、10402／10501 → 10101（全体修正：確定版を複写した版を編集）
            services().getApplicationService().modify(id, rowVersion(req), user);
            if (d.has("fullRevise")) {
                flashMessage(req, "success", "I010");
            }
            redirect(req, res, "/emp/applications/" + id + "/edit");
            return;
        }
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        Validation v = ApplicationForm.from(d.getCurrentVersion()).validate(true, false);
        if (v.hasErrors()) {
            flash(req, "danger", "入力内容に誤りがあります。「修正」で入力し直してください。");
            redirect(req, res, "/emp/applications/" + id + "/confirm");
            return;
        }
        services().getApplicationService().confirm(id, rowVersion(req), user);
        flashMessage(req, "success", "I005");
        redirect(req, res, menuPath(id));
    }

    // ------------------------------------------------------------------ SC06 承認フロー

    private void showApproval(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApprovalFlowView v = services().getApprovalService().load(id, user);
        ApplicationDetail full = services().getApplicationQueryService().detail(id, user);
        List<PhaseGroup> groups = PhaseGroup.build(full.getApplication(), full.getVersions());
        PhaseGroup g = PhaseGroup.find(groups, groupParam(req));
        // 領域ごとの承認・申請履歴に絞る。過去の領域は参照のみ
        v.restrictToVersions(g.versionNos(), g.isCurrent() ? null : "完了・取消した手続きの承認・申請履歴です（参照のみ）。");
        req.setAttribute("group", g);
        req.setAttribute("viewVersion", g.isCurrent() ? full.getCurrentVersion() : g.getDisplayVersion());
        req.setAttribute("diffBase", copiedFrom(full, g.isCurrent() ? full.getCurrentVersion() : g.getDisplayVersion()));
        req.setAttribute("v", v);
        req.setAttribute("d", v.getDetail());
        req.setAttribute("initialApproverIds", v.getInitialApproverIds());
        req.setAttribute("maxSteps", ApprovalService.MAX_STEPS);
        render(req, res, "emp/applications/approval.jsp");
    }

    private void postApproval(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        String action = param(req, "action");
        int rv = rowVersion(req);
        String comment = req.getParameter("comment") == null ? "" : req.getParameter("comment").strip();
        if (comment.length() > 500) {
            throw new BusinessException("E002", "コメント", 500);
        }
        switch (action) {
            case "apply": {
                String[] ids = req.getParameterValues("approverId");
                List<Long> approverIds = new ArrayList<>();
                if (ids != null) {
                    for (String s : ids) {
                        Long v = parseId(s);
                        if (v != null) {
                            approverIds.add(v);
                        }
                    }
                }
                Long routeId = "1".equals(param(req, "templateUsed")) ? longParam(req, "routeId") : null;
                services().getApprovalService().apply(id, rv, approverIds, routeId, user);
                flashMessage(req, "success", "I002");
                break;
            }
            case "approve": {
                Long requestId = longParam(req, "approvalRequestId");
                services().getApprovalService().approve(id, rv, requestId == null ? 0 : requestId, comment, user);
                flashMessage(req, "success", "I003");
                break;
            }
            case "return": {
                Long requestId = longParam(req, "approvalRequestId");
                services().getApprovalService().returnRequest(id, rv, requestId == null ? 0 : requestId, comment, user);
                flashMessage(req, "success", "I004");
                break;
            }
            case "review": {
                Long requestId = longParam(req, "approvalRequestId");
                services().getApprovalService().requestReview(id, rv, requestId == null ? 0 : requestId, comment, user);
                flashMessage(req, "success", "I002");
                break;
            }
            default:
                res.sendError(404);
                return;
        }
        redirect(req, res, menuPath(id));
    }

    // ------------------------------------------------------------------ SC07 申込修正

    private void showRevise(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id, ApplicationForm form, Validation errors) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        String st = d.getApplication().getStatusCd();
        if (!StatusCd.REVISE.contains(st) || d.getApplication().getOwnerEmployeeId() != user.getEmployeeId()) {
            throw new TransitionNotAllowedException();
        }
        req.setAttribute("d", d);
        req.setAttribute("amountsOnly", StatusCd.FINAL_WAIT.equals(st));
        req.setAttribute("form", form == null ? ApplicationForm.from(d.getCurrentVersion()) : form);
        req.setAttribute("errors", errors == null ? java.util.Map.of() : errors.getErrors());
        req.setAttribute("newVersionNo", maxVersion(d) + 1);
        com.example.appmgmt.domain.ExternalLink latestNg = null;
        for (com.example.appmgmt.domain.ExternalLink l : d.getExternalLinks()) {
            if (Codes.EXT_RESULT_NG.equals(l.getResultCd())) {
                latestNg = l;
                break;
            }
        }
        req.setAttribute("latestNg", latestNg);
        render(req, res, "emp/applications/revise.jsp");
    }

    private static int maxVersion(ApplicationDetail d) {
        int max = 0;
        for (ApplicationVersion v : d.getVersions()) {
            max = Math.max(max, v.getVersionNo());
        }
        return max;
    }

    private void postRevise(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        boolean amountsOnly = StatusCd.FINAL_WAIT.equals(d.getApplication().getStatusCd());
        ApplicationForm form = ApplicationForm.bind(req);
        if (amountsOnly) {
            ApplicationForm base = ApplicationForm.from(d.getCurrentVersion());
            java.util.Map<String, String[]> m = new java.util.HashMap<>();
            // 一部修正は金額だけを反映する。申込者情報を含む他の項目は現行版のまま
            m.put("applicantName", new String[] {base.getApplicantName()});
            m.put("applicantKana", new String[] {base.getApplicantKana()});
            m.put("telNo", new String[] {base.getTelNo()});
            m.put("mailAddress", new String[] {base.getMailAddress()});
            m.put("address", new String[] {base.getAddress()});
            m.put("productCd", new String[] {base.getProductCd()});
            m.put("basicFee", new String[] {form.getBasicFee()});
            m.put("optionFee", new String[] {form.getOptionFee()});
            m.put("handlingFee", new String[] {form.getHandlingFee()});
            m.put("contractStartDate", new String[] {base.getContractStartDate()});
            m.put("contractEndDate", new String[] {base.getContractEndDate()});
            m.put("remarks", new String[] {base.getRemarks()});
            form = ApplicationForm.bind(new ParamRequest(m));
        }
        Validation v = form.validate(true, false);
        if (v.hasErrors()) {
            showRevise(req, res, user, id, form, v);
            return;
        }
        TransitionResult r = services().getApplicationService().revise(id, rowVersion(req), form.toVersion(), user);
        flashMessage(req, "success", "I005");
        if (r.isOverLimit()) {
            flashMessage(req, "warning", "W001");
        }
        redirect(req, res, menuPath(id));
    }

    // ------------------------------------------------------------------ SC08 契約変更入力

    private void showChangeInput(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id, ApplicationForm form, Validation errors) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        if (!StatusCd.CHG_INPUT.equals(d.getApplication().getStatusCd()) || d.getApplication().getOwnerEmployeeId() != user.getEmployeeId()) {
            throw new TransitionNotAllowedException();
        }
        req.setAttribute("d", d);
        req.setAttribute("form", form == null ? ApplicationForm.from(d.getCurrentVersion()) : form);
        req.setAttribute("errors", errors == null ? java.util.Map.of() : errors.getErrors());
        render(req, res, "emp/applications/change_input.jsp");
    }

    private void postChangeInput(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        boolean toConfirm = "confirm".equals(param(req, "action"));
        ApplicationForm form = ApplicationForm.bind(req);
        Validation v = form.validate(toConfirm, false);
        if (v.hasErrors()) {
            showChangeInput(req, res, user, id, form, v);
            return;
        }
        services().getApplicationService().saveDraft(id, rowVersion(req), form.toVersion(), user);
        if (toConfirm) {
            redirect(req, res, "/emp/applications/" + id + "/change/confirm");
        } else {
            flashMessage(req, "success", "I012");
            redirect(req, res, "/emp/applications/" + id + "/change");
        }
    }

    // ------------------------------------------------------------------ SC09 契約変更内容確認

    /**
     * SC09 契約変更内容確認。契約変更中（20101〜20601）は申込を参照できる利用者なら開ける。
     * 「修正」（20101 は入力画面へ、20201／20501 は 20101 へ戻して入力画面へ）と「確定」（20101 のみ）は活性・非活性で制御する。
     */
    private void showChangeConfirm(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
        String st = d.getApplication().getStatusCd();
        List<PhaseGroup> groups = PhaseGroup.build(d.getApplication(), d.getVersions());
        PhaseGroup g = PhaseGroup.find(groups, groupParam(req));
        if (!g.isContractChange()) {
            redirect(req, res, "/emp/applications/" + id + "/confirm?group=0");
            return;
        }
        boolean live = g.isCurrent();
        boolean owner = user.isOwner() && d.getApplication().getOwnerEmployeeId() == user.getEmployeeId();
        boolean canModify = live && owner && (d.has("inputChange") || d.has("modifyChange"));
        req.setAttribute("canModify", canModify);
        req.setAttribute("modifyNote", canModify ? (StatusCd.CHG_INPUT.equals(st) ? "" : "「修正」を押すと【契約変更】入力中（20101）に戻って契約変更入力画面を開きます。")
                : !live ? "完了・取消した契約変更の内容です（参照のみ）。" : !owner ? "修正・確定は担当者だけが行えます。" : StatusCd.CHG_FIX_WAIT.equals(st) ? "修正対応は申込メニューの「申込修正」から行います。"
                : StatusCd.CHG_REVIEWED.equals(st) ? "審査完了した契約変更です。次の変更は「契約変更」から始めます。" : "現在のステータスでは修正できません（承認申請中・申込者確認中・事前確認待ち・審査中）。");
        ApplicationVersion cur = live ? d.getCurrentVersion() : g.getDisplayVersion();
        ApplicationVersion reviewed = live && !StatusCd.CHG_REVIEWED.equals(st) ? d.getReviewedVersion() : g.getBaseline();
        java.math.BigDecimal ratioBase = reviewed == null ? null : (live && !StatusCd.CHG_REVIEWED.equals(st) && d.getBaseVersion() != null ? d.getBaseVersion().getTotalAmount() : reviewed.getTotalAmount());
        java.math.BigDecimal ratio = ratioBase == null ? null : Formats.amountRatio(cur.getTotalAmount(), ratioBase);
        boolean over = StatusTransitionService.isOverLimit(ratio, d.getCompanyDiv().getAmountRatioLimit());
        Validation v = ApplicationForm.from(cur).validate(true, false);
        req.setAttribute("d", d);
        req.setAttribute("group", g);
        req.setAttribute("viewVersion", cur);
        req.setAttribute("baselineVersion", reviewed);
        req.setAttribute("ratio", ratio);
        req.setAttribute("overLimit", over);
        req.setAttribute("noChange", reviewed != null && cur.sameContentAs(reviewed));
        req.setAttribute("errors", live ? v.getErrors() : java.util.Map.of());
        boolean canConfirm = live && owner && StatusCd.CHG_INPUT.equals(st) && !v.hasErrors();
        req.setAttribute("canConfirm", canConfirm);
        req.setAttribute("confirmNote", !owner || !live ? "" : StatusCd.CHG_INPUT.equals(st) ? (v.hasErrors() ? "入力内容に誤りがあるため確定できません。「修正」で入力し直してください。" : "") : "確定済みです。");
        String next;
        if (over) {
            next = "20201 【契約変更】一次承認申請待ち（一次承認・申込者確認を経る）";
        } else if (d.getCompanyDiv().isPreCheck()) {
            next = "20401 【契約変更】事前確認待ち（事前確認依頼を登録）";
        } else {
            next = "20501 【契約変更】最終承認申請待ち";
        }
        req.setAttribute("nextStatus", next);
        render(req, res, "emp/applications/change_confirm.jsp");
    }

    private void postChangeConfirm(HttpServletRequest req, HttpServletResponse res, LoginUser user, long id) throws ServletException, IOException {
        String action = param(req, "action");
        if ("modify".equals(action)) {
            ApplicationDetail d = services().getApplicationQueryService().detail(id, user);
            if (!StatusCd.CHG_INPUT.equals(d.getApplication().getStatusCd())) {
                services().getApplicationService().modify(id, rowVersion(req), user);
            }
            redirect(req, res, "/emp/applications/" + id + "/change");
            return;
        }
        TransitionResult r = services().getApplicationService().confirmChange(id, rowVersion(req), user);
        flashMessage(req, "success", "I005");
        if (r.isOverLimit()) {
            flashMessage(req, "warning", "W001");
        }
        redirect(req, res, menuPath(id));
    }

    /** フォーム再構築用の簡易リクエスト。 */
    private static class ParamRequest extends javax.servlet.http.HttpServletRequestWrapper {
        private final java.util.Map<String, String[]> params;

        ParamRequest(java.util.Map<String, String[]> params) {
            super(DummyRequest.create());
            this.params = params;
        }

        @Override
        public String getParameter(String name) {
            String[] v = params.get(name);
            return v == null || v.length == 0 ? null : v[0];
        }
    }

    /** HttpServletRequestWrapper の生成に必要なダミー（メソッドは使わない）。 */
    private static class DummyRequest implements java.lang.reflect.InvocationHandler {
        static HttpServletRequest create() {
            return (HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[] {HttpServletRequest.class}, new DummyRequest());
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return null;
        }
    }
}
