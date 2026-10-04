package com.example.appmgmt.infra.pdf;

import com.example.appmgmt.common.SystemException;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

/**
 * A4 縦の簡単な帳票を作る（見出し・段落・罫線付きの表、ページ番号）。日本語は同梱の BIZ UDゴシック（OFL）をサブセットで埋め込む。
 * フォントにない文字は「?」に置き換え、改行・タブ以外の制御文字は除く。
 */
public class PdfDocumentBuilder {

    private static final String FONT_RESOURCE = "/fonts/BIZUDGothic-Regular.ttf";
    private static final float MARGIN = 40f;
    private static final float FOOTER_SPACE = 28f;
    private static final float PAD = 4f;
    private static final Color LINE = new Color(150, 150, 150);
    private static final Color SHADE = new Color(235, 238, 242);
    private static final Color SECTION = new Color(245, 246, 248);
    private static final Color CHANGED = new Color(200, 30, 30);
    private static volatile byte[] fontBytes;

    /** 同梱フォントの cmap（形式 14：異体字セレクタ）は使わないため、読み込み時の警告を抑える（JUL のロガーは弱参照のため保持する）。 */
    private static final java.util.logging.Logger CMAP_LOGGER = java.util.logging.Logger.getLogger("org.apache.fontbox.ttf.CmapSubtable");

    static {
        CMAP_LOGGER.setLevel(java.util.logging.Level.SEVERE);
    }

    /** 表の 1 行。section = true は全列をまたぐ小見出し行。changed はセルごとの強調（赤字）。 */
    public static final class Row {
        final String[] cells;
        final boolean section;
        final boolean[] changed;

        private Row(String[] cells, boolean section, boolean[] changed) {
            this.cells = cells;
            this.section = section;
            this.changed = changed;
        }

        public static Row of(String... cells) {
            return new Row(cells, false, new boolean[cells.length]);
        }

        public static Row section(String title) {
            return new Row(new String[] {title}, true, new boolean[1]);
        }

        /** 指定した列を赤字にする。 */
        public Row changed(int... columns) {
            for (int c : columns) {
                if (c >= 0 && c < changed.length) {
                    changed[c] = true;
                }
            }
            return this;
        }
    }

    private final PDDocument doc = new PDDocument();
    private final PDType0Font font;
    private final String footer;
    private final Map<Integer, Boolean> glyphCache = new HashMap<>();
    private PDPageContentStream cs;
    private float y;

    public PdfDocumentBuilder(String title, String footer) {
        this.footer = footer;
        try {
            font = PDType0Font.load(doc, new ByteArrayInputStream(fontBytes()), true);
            PDDocumentInformation info = doc.getDocumentInformation();
            info.setTitle(title);
            info.setCreator("申込管理システム");
            newPage();
        } catch (IOException e) {
            close();
            throw new SystemException("PDF の初期化に失敗しました", e);
        }
    }

    private static byte[] fontBytes() throws IOException {
        byte[] b = fontBytes;
        if (b == null) {
            try (InputStream in = PdfDocumentBuilder.class.getResourceAsStream(FONT_RESOURCE)) {
                if (in == null) {
                    throw new IOException("フォントがありません: " + FONT_RESOURCE);
                }
                b = in.readAllBytes();
            }
            fontBytes = b;
        }
        return b;
    }

    private float pageWidth() {
        return PDRectangle.A4.getWidth();
    }

    private float contentWidth() {
        return pageWidth() - MARGIN * 2;
    }

    private void newPage() throws IOException {
        if (cs != null) {
            cs.close();
        }
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        y = PDRectangle.A4.getHeight() - MARGIN;
    }

    private void ensure(float height) throws IOException {
        if (y - height < MARGIN + FOOTER_SPACE) {
            newPage();
        }
    }

    // ------------------------------------------------------------------ 文字

    /** フォントで表示できる文字だけにする。 */
    String safe(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        s.codePoints().forEach(cp -> {
            if (cp == '\n') {
                b.append('\n');
            } else if (cp == '\t') {
                b.append(' ');
            } else if (Character.isISOControl(cp)) {
                // 除く
            } else if (hasGlyph(cp)) {
                b.appendCodePoint(cp);
            } else {
                b.append('?');
            }
        });
        return b.toString();
    }

    private boolean hasGlyph(int cp) {
        return glyphCache.computeIfAbsent(cp, k -> {
            try {
                font.encode(new String(Character.toChars(k)));
                return true;
            } catch (IOException | IllegalArgumentException e) {
                return false;
            }
        });
    }

    private float width(String s, float size) throws IOException {
        return font.getStringWidth(s) / 1000f * size;
    }

    /** 幅に収まるように 1 文字単位で折り返す（改行は保つ）。空なら 1 行の空文字。 */
    List<String> wrap(String text, float size, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String para : safe(text).split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            int[] cps = para.codePoints().toArray();
            for (int cp : cps) {
                String next = line.toString() + new String(Character.toChars(cp));
                if (line.length() > 0 && width(next, size) > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.appendCodePoint(cp);
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private void showText(String s, float x, float baseline, float size, Color color) throws IOException {
        if (s.isEmpty()) {
            return;
        }
        cs.beginText();
        cs.setNonStrokingColor(color);
        cs.setFont(font, size);
        cs.newLineAtOffset(x, baseline);
        cs.showText(s);
        cs.endText();
    }

    // ------------------------------------------------------------------ 部品

    /** 表題（下線付き）。 */
    public PdfDocumentBuilder title(String text) {
        try {
            float size = 16f;
            for (String line : wrap(text, size, contentWidth())) {
                ensure(size + 6);
                y -= size + 2;
                showText(line, MARGIN, y, size, Color.BLACK);
            }
            y -= 6;
            cs.setStrokingColor(Color.DARK_GRAY);
            cs.setLineWidth(1f);
            cs.moveTo(MARGIN, y);
            cs.lineTo(pageWidth() - MARGIN, y);
            cs.stroke();
            y -= 10;
            return this;
        } catch (IOException e) {
            throw new SystemException("PDF の作成に失敗しました", e);
        }
    }

    /** 小見出し。 */
    public PdfDocumentBuilder heading(String text) {
        try {
            float size = 11f;
            ensure(size + 14);
            y -= 6;
            cs.setNonStrokingColor(new Color(40, 80, 160));
            cs.addRect(MARGIN, y - size - 3, 3, size + 4);
            cs.fill();
            y -= size;
            showText(safe(text).replace('\n', ' '), MARGIN + 8, y, size, Color.BLACK);
            y -= 8;
            return this;
        } catch (IOException e) {
            throw new SystemException("PDF の作成に失敗しました", e);
        }
    }

    /** 段落（折り返し）。 */
    public PdfDocumentBuilder text(String text, float size) {
        return text(text, size, Color.BLACK);
    }

    public PdfDocumentBuilder note(String text) {
        return text(text, 8.5f, Color.DARK_GRAY);
    }

    private PdfDocumentBuilder text(String text, float size, Color color) {
        try {
            float lh = size * 1.45f;
            for (String line : wrap(text, size, contentWidth())) {
                ensure(lh);
                y -= lh;
                showText(line, MARGIN, y + (lh - size) / 2, size, color);
            }
            y -= 4;
            return this;
        } catch (IOException e) {
            throw new SystemException("PDF の作成に失敗しました", e);
        }
    }

    public PdfDocumentBuilder space(float h) {
        y -= h;
        return this;
    }

    /**
     * 罫線付きの表。headers が null なら見出し行なし。widths は列幅の比率。1 列目は網掛けの項目名列として描く。
     * ページをまたぐ場合は改ページ後に見出し行を描き直す。
     */
    public PdfDocumentBuilder table(String[] headers, List<Row> rows, float[] widths) {
        try {
            float size = 9f;
            float lh = size * 1.4f;
            float total = 0;
            for (float w : widths) {
                total += w;
            }
            float[] cols = new float[widths.length];
            for (int i = 0; i < widths.length; i++) {
                cols[i] = contentWidth() * widths[i] / total;
            }
            if (headers != null) {
                drawRow(Row.of(headers), cols, size, lh, true);
            }
            for (Row r : rows) {
                float h = rowHeight(r, cols, size, lh);
                if (y - h < MARGIN + FOOTER_SPACE) {
                    newPage();
                    if (headers != null) {
                        drawRow(Row.of(headers), cols, size, lh, true);
                    }
                }
                drawRow(r, cols, size, lh, false);
            }
            y -= 8;
            return this;
        } catch (IOException e) {
            throw new SystemException("PDF の作成に失敗しました", e);
        }
    }

    private float rowHeight(Row r, float[] cols, float size, float lh) throws IOException {
        if (r.section) {
            return lh + PAD;
        }
        int max = 1;
        for (int i = 0; i < cols.length && i < r.cells.length; i++) {
            max = Math.max(max, wrap(r.cells[i], size, cols[i] - PAD * 2).size());
        }
        return max * lh + PAD * 2;
    }

    private void drawRow(Row r, float[] cols, float size, float lh, boolean header) throws IOException {
        float h = rowHeight(r, cols, size, lh);
        ensure(h);
        float top = y;
        float bottom = y - h;
        cs.setLineWidth(0.5f);
        cs.setStrokingColor(LINE);
        if (r.section) {
            float w = contentWidth();
            cs.setNonStrokingColor(SECTION);
            cs.addRect(MARGIN, bottom, w, h);
            cs.fill();
            cs.addRect(MARGIN, bottom, w, h);
            cs.stroke();
            showText(safe(r.cells[0]).replace('\n', ' '), MARGIN + PAD, bottom + (h - size) / 2 + 1, size - 0.5f, Color.DARK_GRAY);
            y = bottom;
            return;
        }
        float x = MARGIN;
        for (int i = 0; i < cols.length; i++) {
            if (header || i == 0) {
                cs.setNonStrokingColor(SHADE);
                cs.addRect(x, bottom, cols[i], h);
                cs.fill();
            }
            cs.addRect(x, bottom, cols[i], h);
            cs.stroke();
            String cell = i < r.cells.length ? r.cells[i] : "";
            Color color = r.changed.length > i && r.changed[i] ? CHANGED : Color.BLACK;
            float ly = top - PAD;
            for (String line : wrap(cell, size, cols[i] - PAD * 2)) {
                ly -= lh;
                showText(line, x + PAD, ly + (lh - size) / 2 + 1, size, color);
            }
            x += cols[i];
        }
        y = bottom;
    }

    // ------------------------------------------------------------------ 出力

    /** フッター（左：footer、右：ページ番号）を全ページに描いて PDF のバイト列を返す。 */
    public byte[] toBytes() {
        try {
            cs.close();
            cs = null;
            int n = doc.getNumberOfPages();
            for (int i = 0; i < n; i++) {
                PDPage p = doc.getPage(i);
                try (PDPageContentStream f = new PDPageContentStream(doc, p, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    float size = 7.5f;
                    f.setStrokingColor(LINE);
                    f.setLineWidth(0.5f);
                    f.moveTo(MARGIN, MARGIN + 12);
                    f.lineTo(pageWidth() - MARGIN, MARGIN + 12);
                    f.stroke();
                    String left = safe(footer).replace('\n', ' ');
                    String right = (i + 1) + " / " + n;
                    f.beginText();
                    f.setNonStrokingColor(Color.DARK_GRAY);
                    f.setFont(font, size);
                    f.newLineAtOffset(MARGIN, MARGIN);
                    f.showText(left);
                    f.endText();
                    f.beginText();
                    f.setFont(font, size);
                    f.newLineAtOffset(pageWidth() - MARGIN - width(right, size), MARGIN);
                    f.showText(right);
                    f.endText();
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new SystemException("PDF の出力に失敗しました", e);
        } finally {
            close();
        }
    }

    private void close() {
        try {
            if (cs != null) {
                cs.close();
            }
            doc.close();
        } catch (IOException e) {
            // 閉じるときの失敗は無視する
        }
    }
}
