package com.example.appmgmt.service.external;

import com.example.appmgmt.common.AppConfig;
import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.dao.ExternalLinkDao;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.ExternalLink;
import com.example.appmgmt.domain.StatusCd;
import com.example.appmgmt.service.transition.StatusTransitionService;
import com.example.appmgmt.service.transition.TransitionRequest;
import com.example.appmgmt.service.transition.TransitionResult;
import java.time.LocalDateTime;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** F09 審査結果受信（IF02 事前確認結果、IF04 審査結果）。12. 4.3 節の手順に従う。 */
public class ExternalResultService {

    private static final Logger log = LoggerFactory.getLogger(ExternalResultService.class);

    private final ExternalLinkDao externalLinkDao;
    private final ApplicationDao applicationDao;
    private final StatusTransitionService transitionService;

    /** 処理を中断して応答を返すための内部例外（DB は更新しない）。 */
    private static class Abort extends RuntimeException {
        final ReceiveResult result;
        Abort(ReceiveResult r) {
            super(r.getMessage());
            this.result = r;
        }
    }

    public ExternalResultService(ExternalLinkDao externalLinkDao, ApplicationDao applicationDao, StatusTransitionService transitionService) {
        this.externalLinkDao = externalLinkDao;
        this.applicationDao = applicationDao;
        this.transitionService = transitionService;
    }

    /**
     * @param precheck true = IF02（事前確認結果）、false = IF04（審査結果）
     */
    public ReceiveResult receive(boolean precheck, String receiptNo, Long requestId, String result, String reason) {
        // 2. 電文検証
        if (receiptNo == null || receiptNo.isBlank() || receiptNo.length() > 30 || requestId == null || result == null) {
            return ReceiveResult.error(400, "E400", null, null, "電文が不正です（externalReceiptNo、requestId、result は必須）。");
        }
        Set<String> allowed = precheck ? Set.of("OK", "NG") : Set.of("COMPLETED", "RETURNED");
        if (!allowed.contains(result)) {
            return ReceiveResult.error(400, "E400", null, null, "result の値が不正です: " + result);
        }
        boolean negative = "NG".equals(result) || "RETURNED".equals(result);
        if (negative && (reason == null || reason.isBlank())) {
            return ReceiveResult.error(400, "E400", null, null, "NG／RETURNED では reason が必須です。");
        }
        if (reason != null && reason.length() > 1000) {
            return ReceiveResult.error(400, "E400", null, null, "reason は 1000 桁以内です。");
        }
        String resultCd;
        String actionCd;
        switch (result) {
            case "OK": resultCd = Codes.EXT_RESULT_OK; actionCd = Codes.ACTION_PRECHECK_OK; break;
            case "NG": resultCd = Codes.EXT_RESULT_NG; actionCd = Codes.ACTION_PRECHECK_NG; break;
            case "COMPLETED": resultCd = Codes.EXT_RESULT_COMPLETED; actionCd = Codes.ACTION_REVIEW_COMPLETED; break;
            default: resultCd = Codes.EXT_RESULT_RETURNED; actionCd = Codes.ACTION_REVIEW_RETURNED; break;
        }
        String externalSystemId = AppConfig.get().getString("app.external-system-id", "EXT01");
        try {
            return Tx.execute(conn -> {
                // 3. 外部連携の特定
                ExternalLink link = externalLinkDao.findByReceiptNo(conn, receiptNo.trim()).orElse(null);
                if (link == null || link.isPrecheck() != precheck) {
                    throw new Abort(ReceiveResult.error(404, "E404", null, null, "受付番号に該当する依頼がありません。"));
                }
                if (link.getExternalLinkId() != requestId) {
                    throw new Abort(ReceiveResult.error(400, "E400", link.getApplicationNo(), null, "requestId が依頼と一致しません。"));
                }
                Application app = applicationDao.findById(conn, link.getApplicationId()).orElseThrow();
                // 4. 重複判定
                if (link.getResultCd() != null) {
                    throw new Abort(ReceiveResult.duplicate(app.getApplicationNo(), app.getStatusCd()));
                }
                // 5. ステータス確認
                Set<String> acceptable = precheck ? StatusCd.PRECHECK_RESULT_ACCEPTABLE : StatusCd.REVIEW_RESULT_ACCEPTABLE;
                if (!acceptable.contains(app.getStatusCd())) {
                    throw new Abort(ReceiveResult.error(409, "E409", app.getApplicationNo(), app.getStatusCd(), "受信できるステータスではありません。"));
                }
                // 6. 版確認
                if (link.getVersionNo() != app.getCurrentVersionNo()) {
                    throw new Abort(ReceiveResult.error(410, "E410", app.getApplicationNo(), app.getStatusCd(), "依頼の版が現行版ではありません。"));
                }
                // 7. 結果保存
                externalLinkDao.updateResult(conn, link.getExternalLinkId(), resultCd, LocalDateTime.now().withNano(0), negative ? reason : null);
                // 8. F14
                TransitionResult tr;
                try {
                    tr = transitionService.transition(conn, TransitionRequest.of(app.getApplicationId(), app.getRowVersion(), actionCd, Codes.ACTOR_REVIEWER, externalSystemId)
                            .externalLinkId(link.getExternalLinkId()).comment(negative ? reason : null));
                } catch (BusinessException e) {
                    throw new Abort(ReceiveResult.error(409, "E409", app.getApplicationNo(), app.getStatusCd(), "ステータス不整合または排他エラーです: " + e.getMessage()));
                }
                String msg = Codes.label("EXT_RESULT", resultCd) + "を受け付けました。";
                return ReceiveResult.ok(app.getApplicationNo(), tr.getToStatusCd(), msg);
            });
        } catch (Abort a) {
            log.info("外部結果受信を中断 receiptNo={} code={} {}", receiptNo, a.result.getErrorCode(), a.result.getMessage());
            return a.result;
        }
    }
}
