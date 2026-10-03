package com.example.appmgmt.web.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        ApplicationForm f = ApplicationForm.bind(request(Map.of("applicantName", "山田 太郎", "mailAddress", "taro@example.com",
                "productCd", "PRD001", "basicFee", "1,000,000", "optionFee", "200000", "handlingFee", "", "remarks", " 備考 ")));
        Validation v = f.validate(true, false, false);
        assertFalse(v.hasErrors(), v.getErrors().toString());
        ApplicationVersion ver = f.toVersion();
        assertEquals(new BigDecimal("1200000"), ver.getTotalAmount());
        assertEquals(BigDecimal.ZERO, ver.getHandlingFee());
        assertEquals("備考", ver.getRemarks());
        // 申込者情報は申込データとして版に入る（空の任意項目は null）
        assertEquals("山田 太郎", ver.getApplicantName());
        assertEquals("taro@example.com", ver.getMailAddress());
        assertNull(ver.getTelNo());
        assertNull(ver.getApplicantKana());
    }

    @Test
    void draftValidationSkipsRequiredChecks() {
        ApplicationForm f = ApplicationForm.bind(request(Map.of("productCd", "", "basicFee", "")));
        assertFalse(f.validate(false, false, false).hasErrors());
    }

    @Test
    void applicantNameAndMailAreRequiredOnConfirm() {
        ApplicationForm f = ApplicationForm.bind(request(Map.of("productCd", "PRD001", "basicFee", "1000")));
        Validation v = f.validate(true, false, false);
        assertTrue(v.has("applicantName"));
        assertTrue(v.has("mailAddress"));
    }

    @Test
    void applicantFormatsAreCheckedEvenOnDraft() {
        ApplicationForm f = ApplicationForm.bind(request(Map.of("applicantKana", "やまだ", "mailAddress", "not-a-mail", "telNo", "03(0000)0001")));
        Validation v = f.validate(false, false, false);
        assertTrue(v.has("applicantKana"));
        assertTrue(v.has("mailAddress"));
        assertTrue(v.has("telNo"));
    }

    @Test
    void accountNumberIsOptionalButFormatChecked() {
        Map<String, String> ok = Map.of("applicantName", "山田 太郎", "mailAddress", "taro@example.com", "productCd", "PRD001", "basicFee", "1000");
        assertFalse(ApplicationForm.bind(request(ok)).validate(true, true, false).has("applicantNo"));
        Map<String, String> bad = new java.util.HashMap<>(ok);
        bad.put("applicantNo", "C-0001/x");
        assertTrue(ApplicationForm.bind(request(bad)).validate(true, true, false).has("applicantNo"));
    }

    @Test
    void amountsOnlySkipsApplicantChecks() {
        ApplicationForm f = ApplicationForm.bind(request(Map.of("basicFee", "1000")));
        assertFalse(f.validate(true, false, true).hasErrors());
    }
}
