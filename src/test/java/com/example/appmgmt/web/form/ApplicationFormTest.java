package com.example.appmgmt.web.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.appmgmt.common.Validation;
import com.example.appmgmt.domain.ApplicationVersion;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class ApplicationFormTest {

    private static HttpServletRequest request(Map<String, String> params) {
        InvocationHandler h = (proxy, method, args) -> {
            if ("getParameter".equals(method.getName())) {
                return params.get((String) args[0]);
            }
            return null;
        };
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[] {HttpServletRequest.class}, h);
    }

    @Test
    void fullValidationRequiresProductAndPositiveTotal() {
        ApplicationForm f = ApplicationForm.bind(request(Map.of("productCd", "", "basicFee", "0", "optionFee", "", "handlingFee", "")));
        Validation v = f.validate(true, false, false);
        assertTrue(v.has("productCd"));
        assertTrue(v.has("basicFee"));
    }

    @Test
    void contractEndBeforeStartIsRejected() {
        ApplicationForm f = ApplicationForm.bind(request(Map.of("productCd", "PRD001", "basicFee", "1,000,000", "contractStartDate", "2026/10/01", "contractEndDate", "2026/09/30")));
        Validation v = f.validate(true, false, false);
        assertTrue(v.has("contractEndDate"));
        assertEquals("契約終了日は契約開始日以降の日付を入力してください。", v.getErrors().get("contractEndDate"));
    }

    @Test
    void validInputConvertsToVersionWithTotal() {
        ApplicationForm f = ApplicationForm.bind(request(Map.of("productCd", "PRD001", "basicFee", "1,000,000", "optionFee", "200000", "handlingFee", "", "remarks", " 備考 ")));
        Validation v = f.validate(true, false, false);
        assertFalse(v.hasErrors());
        ApplicationVersion ver = f.toVersion();
        assertEquals(new BigDecimal("1200000"), ver.getTotalAmount());
        assertEquals(BigDecimal.ZERO, ver.getHandlingFee());
        assertEquals("備考", ver.getRemarks());
    }

    @Test
    void draftValidationSkipsRequiredChecks() {
        ApplicationForm f = ApplicationForm.bind(request(Map.of("productCd", "", "basicFee", "")));
        assertFalse(f.validate(false, false, false).hasErrors());
    }
}
