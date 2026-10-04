package com.example.appmgmt.service.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.ConsentDocumentRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class ApplicationPdfRendererTest {

    private static ApplicationVersion version(int no, String type, String basic) {
        ApplicationVersion v = new ApplicationVersion();
        v.setVersionNo(no);
        v.setVersionType(type);
        v.setApplicantName("山田 太郎");
        v.setMailAddress("taro@example.com");
        v.setProductCd("PRD001");
        v.setBasicFee(new BigDecimal(basic));
        v.setOptionFee(BigDecimal.ZERO);
        v.setHandlingFee(BigDecimal.ZERO);
        v.setContractStartDate(LocalDate.of(2026, 11, 1));
        v.setContractEndDate(LocalDate.of(2027, 10, 31));
        v.setRemarks("備考\n2 行目");
        v.recalcTotal();
        return v;
    }

    private static String text(byte[] pdf) throws Exception {
        try (PDDocument d = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(d);
        }
    }

    @Test
    void consentPdfListsContentAndAgreedDocuments() throws Exception {
        ApplicationPdfRenderer.Input in = new ApplicationPdfRenderer.Input();
        in.title = "お申込内容（ご同意時）";
        in.applicationNo = "AP0000000001";
        in.applicantName = "山田 太郎";
        in.version = version(1, "1", "1000000");
        in.timeLabel = "ご同意日時";
        in.time = LocalDateTime.of(2026, 10, 4, 10, 30);
        ConsentDocumentRecord r = new ConsentDocumentRecord();
        r.setDocumentName("利用規約");
        r.setVersionNo(2);
        r.setEffectiveFrom(LocalDateTime.of(2026, 10, 1, 0, 0));
        r.setViewedAt(LocalDateTime.of(2026, 10, 4, 10, 29));
        r.setFileHash("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        in.agreedDocuments.add(r);
        String t = text(ApplicationPdfRenderer.render(in));
        assertTrue(t.contains("お申込内容（ご同意時）"), t);
        assertTrue(t.contains("AP0000000001"));
        assertTrue(t.contains("1,000,000 円"));
        assertTrue(t.contains("2026/11/01 〜 2027/10/31"));
        assertTrue(t.contains("利用規約") && t.contains("第 2 版") && t.contains("0123456789abcdef"));
        assertTrue(t.contains("2026/10/04 10:30"));
        // 会社B の追加項目は会社B の版だけ
        assertFalse(t.contains("会社B 追加項目"));
    }

    @Test
    void reviewedPdfComparesWithConsentedVersion() throws Exception {
        ApplicationVersion agreed = version(2, "2", "1000000");
        ApplicationVersion reviewed = version(3, "2", "1200000");
        reviewed.setCompanyDiv("2");
        reviewed.setCorporateNo("1234567890123");
        assertEquals(List.of("基本料金", "申込金額合計", "法人番号"), ApplicationPdfRenderer.changedLabels(agreed, reviewed));
        ApplicationPdfRenderer.Input in = new ApplicationPdfRenderer.Input();
        in.title = "お申込内容（審査完了時）";
        in.applicationNo = "AP0000000002";
        in.applicantName = "山田 太郎";
        in.version = reviewed;
        in.versionLabel = "審査完了時";
        in.compare = agreed;
        in.compareLabel = "ご同意時（第 2 版）";
        in.notes.add("ご同意後に変更された項目があります。");
        String t = text(ApplicationPdfRenderer.render(in));
        assertTrue(t.contains("ご同意時（第 2 版）") && t.contains("審査完了時"), t);
        assertTrue(t.contains("1,000,000 円") && t.contains("1,200,000 円"));
        assertTrue(t.contains("会社B 追加項目") && t.contains("1234567890123"));
        assertTrue(t.contains("ご同意後に変更された項目があります。"));
    }
}
