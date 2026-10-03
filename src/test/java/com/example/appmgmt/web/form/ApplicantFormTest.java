package com.example.appmgmt.web.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.appmgmt.common.Validation;
import com.example.appmgmt.domain.Applicant;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class ApplicantFormTest {

    private static HttpServletRequest request(Map<String, String> params) {
        InvocationHandler h = (proxy, method, args) -> "getParameter".equals(method.getName()) ? params.get((String) args[0]) : null;
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[] {HttpServletRequest.class}, h);
    }

    private static Map<String, String> valid() {
        Map<String, String> m = new HashMap<>();
        m.put("applicantMode", "new");
        m.put("applicantName", "山田 太郎");
        m.put("applicantKana", "ヤマダ タロウ");
        m.put("mailAddress", "taro@example.com");
        m.put("telNo", "03-0000-0001");
        m.put("address", "東京都千代田区1-1-1");
        return m;
    }

    @Test
    void validNewApplicantHasNoErrors() {
        ApplicantForm f = ApplicantForm.bind(request(valid()));
        Validation v = new Validation();
        f.validateProfile(v);
        assertFalse(v.hasErrors(), v.getErrors().toString());
        assertTrue(f.isNewApplicant());
    }

    @Test
    void nameAndMailAreRequired() {
        Map<String, String> m = valid();
        m.put("applicantName", "");
        m.put("mailAddress", "");
        Validation v = new Validation();
        ApplicantForm.bind(request(m)).validateProfile(v);
        assertTrue(v.has("applicantName"));
        assertTrue(v.has("mailAddress"));
    }

    @Test
    void formatsAreChecked() {
        Map<String, String> m = valid();
        m.put("applicantKana", "やまだ");
        m.put("mailAddress", "not-a-mail");
        m.put("telNo", "03(0000)0001");
        Validation v = new Validation();
        ApplicantForm.bind(request(m)).validateProfile(v);
        assertTrue(v.has("applicantKana"));
        assertTrue(v.has("mailAddress"));
        assertTrue(v.has("telNo"));
    }

    @Test
    void existingModeAndEmptyOptionalFieldsBecomeNull() {
        Map<String, String> m = valid();
        m.put("applicantMode", "existing");
        m.put("applicantKana", "");
        m.put("telNo", "");
        m.put("address", "");
        ApplicantForm f = ApplicantForm.bind(request(m));
        assertFalse(f.isNewApplicant());
        Applicant a = f.toApplicant();
        assertEquals("山田 太郎", a.getApplicantName());
        assertNull(a.getApplicantKana());
        assertNull(a.getTelNo());
        assertNull(a.getAddress());
        assertNull(a.getApplicantNo());
    }
}
