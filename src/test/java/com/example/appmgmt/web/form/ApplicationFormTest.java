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
        Validation v = f.validate(true, false);
        assertTrue(v.has("productCd"));
        assertTrue(v.has("basicFee"));
    }

    @Test
    void contractEndBeforeStartIsRejected() {
        ApplicationForm f = ApplicationForm.bind(request(Map.of("productCd", "PRD001", "basicFee", "1,000,000", "contractStartDate", "2026/10/01", "contractEndDate", "2026/09/30")));
        Validation v = f.validate(true, false);
        assertTrue(v.has("contractEndDate"));
        assertEquals("契約終了日は契約開始日以降の日付を入力してください。", v.getErrors().get("contractEndDate"));
    }

    @Test
    void validInputConvertsToVersionWithTotal() {
        ApplicationForm f = ApplicationForm.bind(request(Map.of("applicantName", "山田 太郎", "mailAddress", "taro@example.com",
                "productCd", "PRD001", "basicFee", "1,000,000", "optionFee", "200000", "handlingFee", "", "remarks", " 備考 ")));
        Validation v = f.validate(true, false);
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
        assertFalse(f.validate(false, false).hasErrors());
    }

    @Test
    void applicantNameAndMailAreRequiredOnConfirm() {
        ApplicationForm f = ApplicationForm.bind(request(Map.of("productCd", "PRD001", "basicFee", "1000")));
        Validation v = f.validate(true, false);
        assertTrue(v.has("applicantName"));
        assertTrue(v.has("mailAddress"));
    }

    @Test
    void applicantFormatsAreCheckedEvenOnDraft() {
        ApplicationForm f = ApplicationForm.bind(request(Map.of("applicantKana", "やまだ", "mailAddress", "not-a-mail", "telNo", "03(0000)0001")));
        Validation v = f.validate(false, false);
        assertTrue(v.has("applicantKana"));
        assertTrue(v.has("mailAddress"));
        assertTrue(v.has("telNo"));
    }

    @Test
    void amountsOnlySkipsApplicantChecks() {
        ApplicationForm f = ApplicationForm.bind(request(Map.of("basicFee", "1000")));
        assertFalse(f.validate(true, true).hasErrors());
    }

    @Test
    void companyExtraItemsAreOptionalButChecked() {
        ApplicationForm ok = ApplicationForm.bind(request(Map.of("productCd", "PRD001", "basicFee", "1000",
                "corporateNo", "1234567890123", "installPlace", "本社 3 階", "contactMemo", "平日 9 時〜17 時\r\n担当：総務")));
        assertFalse(ok.validate(false, false).hasErrors());
        ApplicationVersion ver = ok.toVersion();
        assertEquals("1234567890123", ver.getCorporateNo());
        assertEquals("平日 9 時〜17 時\n担当：総務", ver.getContactMemo());
        ApplicationForm ng = ApplicationForm.bind(request(Map.of("productCd", "PRD001", "basicFee", "1000", "corporateNo", "12345", "installPlace", "x".repeat(201))));
        Validation v = ng.validate(false, false);
        assertTrue(v.has("corporateNo"));
        assertTrue(v.has("installPlace"));
        // 未入力なら null（会社B 以外の申込は項目自体を送らない）
        assertNull(ApplicationForm.bind(request(Map.of("productCd", "PRD001", "basicFee", "1000"))).toVersion().getCorporateNo());
    }

    @Test
    void companyExtraIsKeptOnlyForCompanyBAndIgnoredByApplicantEdit() {
        ApplicationVersion stored = new ApplicationVersion();
        stored.setProductCd("PRD001");
        stored.setBasicFee(new BigDecimal("1000"));
        stored.setCorporateNo("1234567890123");
        ApplicationVersion applicantInput = stored.copyContent();
        applicantInput.setCorporateNo(null);
        // 申込者の修正（AP02）は追加項目を写さない
        ApplicationVersion cur = stored.copyContent();
        cur.applyContentFrom(applicantInput);
        assertEquals("1234567890123", cur.getCorporateNo());
        // 社員の入力は会社区分 2 のときだけ写し、それ以外は空にする
        ApplicationVersion input = stored.copyContent();
        input.setCorporateNo("9999999999999");
        cur.applyCompanyExtraFrom(input, "2");
        assertEquals("9999999999999", cur.getCorporateNo());
        cur.applyCompanyExtraFrom(input, "1");
        assertNull(cur.getCorporateNo());
        // 追加項目だけの変更も「変更あり」
        ApplicationVersion changed = stored.copyContent();
        changed.setInstallPlace("倉庫");
        assertFalse(changed.sameContentAs(stored));
    }
}
