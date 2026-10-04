package com.example.appmgmt.service.transition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** 変更基準（条件 21）の判定：倍率 ≧ しきい値で基準超、減額・未設定は基準内。 */
class StatusTransitionServiceTest {

    private static final BigDecimal LIMIT = new BigDecimal("1.50");

    @Test
    void ratioEqualToLimitIsOver() {
        assertTrue(StatusTransitionService.isOverLimit(new BigDecimal("1.5000"), LIMIT));
    }

    @Test
    void ratioAboveLimitIsOver() {
        assertTrue(StatusTransitionService.isOverLimit(new BigDecimal("1.8333"), LIMIT));
    }

    @Test
    void ratioBelowLimitIsWithin() {
        assertFalse(StatusTransitionService.isOverLimit(new BigDecimal("1.4999"), LIMIT));
    }

    @Test
    void decreaseIsAlwaysWithin() {
        assertFalse(StatusTransitionService.isOverLimit(new BigDecimal("0.2000"), LIMIT));
    }

    @Test
    void contractChangeVersionTypes() {
        assertFalse(StatusTransitionService.isContractChangeVersion("1"));
        assertFalse(StatusTransitionService.isContractChangeVersion("2"));
        assertTrue(StatusTransitionService.isContractChangeVersion("3"));
        assertTrue(StatusTransitionService.isContractChangeVersion("4"));
    }

    @Test
    void explicitOverLimitDecisionIsUsedWithoutReadingTheVersion() {
        // 修正対応（10402／20402）を変更なしで確定するときは、現行版に残っている倍率ではなく呼出元の判定（基準内）を使う
        StatusTransitionService s = new StatusTransitionService(null, null, null, null, null, null, null, null, null, null, null);
        TransitionRequest within = TransitionRequest.of(1L, 0, "02", "1", "1").overLimit(false);
        TransitionRequest over = TransitionRequest.of(1L, 0, "02", "1", "1").overLimit(true);
        assertFalse(s.evaluate(null, "21", null, null, within));
        assertTrue(s.evaluate(null, "21", null, null, over));
    }

    @Test
    void nullRatioIsWithin() {
        assertFalse(StatusTransitionService.isOverLimit(null, LIMIT));
    }
}
