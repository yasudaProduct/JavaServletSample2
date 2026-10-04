package com.example.appmgmt.infra.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfDocumentBuilderTest {

    private static String textOf(byte[] pdf) throws Exception {
        try (PDDocument d = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(d);
        }
    }

    @Test
    void writesJapaneseTextTablesAndPageNumbers() throws Exception {
        PdfDocumentBuilder b = new PdfDocumentBuilder("テスト", "申込管理システム");
        b.title("お申込内容（ご同意時）").text("申込番号 AP0000000001　山田 太郎 様", 10).heading("申込者情報");
        List<PdfDocumentBuilder.Row> rows = new ArrayList<>();
        rows.add(PdfDocumentBuilder.Row.section("申込内容"));
        rows.add(PdfDocumentBuilder.Row.of("基本料金", "1,000,000 円", "1,500,000 円").changed(2));
        for (int i = 0; i < 60; i++) {
            rows.add(PdfDocumentBuilder.Row.of("備考 " + i, "長い文章の折り返しを確認します。".repeat(4), "😀 絵文字はフォントにないので置き換える"));
        }
        b.table(new String[] {"項目", "変更前", "変更後"}, rows, new float[] {1, 2, 2});
        byte[] pdf = b.toBytes();
        assertTrue(new String(pdf, 0, 5).startsWith("%PDF-"));
        String text = textOf(pdf);
        assertTrue(text.contains("お申込内容（ご同意時）"), text);
        assertTrue(text.contains("1,500,000 円"));
        assertTrue(text.contains("? 絵文字"));
        try (PDDocument d = PDDocument.load(pdf)) {
            assertTrue(d.getNumberOfPages() >= 2);
            assertTrue(textOf(pdf).contains("1 / " + d.getNumberOfPages()));
        }
    }

    @Test
    void wrapsByWidthAndKeepsLineBreaks() throws Exception {
        PdfDocumentBuilder b = new PdfDocumentBuilder("t", "f");
        List<String> lines = b.wrap("あいうえお\nかきくけこ", 10f, 25f);
        assertEquals("あい", lines.get(0));
        assertEquals("かき", lines.get(3));
        assertEquals(6, lines.size());
        b.toBytes();
    }
}
