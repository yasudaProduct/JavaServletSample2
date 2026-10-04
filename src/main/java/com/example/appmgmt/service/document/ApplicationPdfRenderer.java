package com.example.appmgmt.service.document;

import com.example.appmgmt.common.Formats;
import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.ConsentDocumentRecord;
import com.example.appmgmt.infra.pdf.PdfDocumentBuilder;
import com.example.appmgmt.infra.pdf.PdfDocumentBuilder.Row;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 申込内容 PDF（同意時・審査完了時）の書式。申込者向けの書面のため、担当（申込受付会社）・承認・審査の情報は載せない。
 * 比較する版（compare）を渡すと「比較元 ／ この版」の 2 列で表示し、異なる項目を赤字にする。
 */
public final class ApplicationPdfRenderer {

    /** 表示内容。 */
    public static final class Input {
        public String title;
        public String applicationNo;
        public String applicantName;
        public ApplicationVersion version;
        public String versionLabel = "内容";
        public ApplicationVersion compare;
        public String compareLabel;
        public String timeLabel;
        public LocalDateTime time;
        public LocalDateTime createdAt = LocalDateTime.now().withNano(0);
        public List<ConsentDocumentRecord> agreedDocuments = new ArrayList<>();
        public List<String> notes = new ArrayList<>();
    }

    private static final class Field {
        final String label;
        final Function<ApplicationVersion, String> value;

        Field(String label, Function<ApplicationVersion, String> value) {
            this.label = label;
            this.value = value;
        }
    }

    private static final List<Field> APPLICANT = List.of(
            new Field("申込者名", v -> v.getApplicantName()),
            new Field("申込者名カナ", v -> v.getApplicantKana()),
            new Field("電話番号", v -> v.getTelNo()),
            new Field("メールアドレス", v -> v.getMailAddress()),
            new Field("住所", v -> v.getAddress()));
    private static final List<Field> CONTENT = List.of(
            new Field("商品コード", v -> v.getProductCd()),
            new Field("基本料金", v -> yen(v.getBasicFee())),
            new Field("オプション料金", v -> yen(v.getOptionFee())),
            new Field("事務手数料", v -> yen(v.getHandlingFee())),
            new Field("申込金額合計", v -> yen(v.getTotalAmount())),
            new Field("契約期間", v -> Formats.date(v.getContractStartDate()) + " 〜 " + Formats.date(v.getContractEndDate())),
            new Field("備考", v -> v.getRemarks()));
    private static final List<Field> COMPANY_EXTRA = List.of(
            new Field("法人番号", v -> v.getCorporateNo()),
            new Field("設置場所", v -> v.getInstallPlace()),
            new Field("窓口メモ", v -> v.getContactMemo()));

    private ApplicationPdfRenderer() {
    }

    private static String yen(java.math.BigDecimal v) {
        return v == null ? "" : Formats.amount(v) + " 円";
    }

    private static String text(String s) {
        return s == null || s.isEmpty() ? "－" : s;
    }

    public static byte[] render(Input in) {
        PdfDocumentBuilder b = new PdfDocumentBuilder(in.title + " " + in.applicationNo, "申込管理システム　" + in.title + "　申込番号 " + in.applicationNo);
        b.title(in.title);
        List<Row> meta = new ArrayList<>();
        meta.add(Row.of("申込番号", in.applicationNo));
        meta.add(Row.of("お名前", text(in.applicantName) + " 様"));
        meta.add(Row.of("申込内容の版", "第 " + in.version.getVersionNo() + " 版（" + in.version.getVersionTypeName() + "）"));
        if (in.time != null) {
            meta.add(Row.of(in.timeLabel, Formats.dateTime(in.time)));
        }
        meta.add(Row.of("作成日時", Formats.dateTime(in.createdAt)));
        b.table(null, meta, new float[] {1, 3});

        b.heading("お申込内容");
        boolean twoColumns = in.compare != null;
        List<Row> rows = new ArrayList<>();
        section(rows, "申込者情報", APPLICANT, in, twoColumns);
        section(rows, "申込内容", CONTENT, in, twoColumns);
        if (in.version.isCompanyExtraTarget() || (in.compare != null && in.compare.isCompanyExtraTarget())) {
            section(rows, "会社B 追加項目", COMPANY_EXTRA, in, twoColumns);
        }
        if (twoColumns) {
            b.table(new String[] {"項目", in.compareLabel, in.versionLabel}, rows, new float[] {1.1f, 2, 2});
        } else {
            b.table(null, rows, new float[] {1, 3});
        }

        if (!in.agreedDocuments.isEmpty()) {
            b.heading("ご同意いただいた同意事項");
            List<Row> docs = new ArrayList<>();
            for (ConsentDocumentRecord r : in.agreedDocuments) {
                docs.add(Row.of(r.getDocumentName(), "第 " + r.getVersionNo() + " 版", Formats.dateTime(r.getEffectiveFrom()), Formats.dateTime(r.getViewedAt()),
                        r.getFileHash() == null ? "" : r.getFileHash().substring(0, 16)));
            }
            b.table(new String[] {"同意事項", "版", "適用開始", "確認日時", "SHA-256（先頭 16 桁）"}, docs, new float[] {2.4f, 0.8f, 1.4f, 1.4f, 1.7f});
        }
        for (String n : in.notes) {
            b.note("※ " + n);
        }
        return b.toBytes();
    }

    private static void section(List<Row> rows, String title, List<Field> fields, Input in, boolean twoColumns) {
        rows.add(Row.section(title));
        for (Field f : fields) {
            String cur = text(f.value.apply(in.version));
            if (twoColumns) {
                String before = text(f.value.apply(in.compare));
                Row r = Row.of(f.label, before, cur);
                if (!Objects.equals(before, cur)) {
                    r.changed(2);
                }
                rows.add(r);
            } else {
                rows.add(Row.of(f.label, cur));
            }
        }
    }

    /** 2 つの版で表示が異なる項目名（審査完了時 PDF の注記用）。 */
    public static List<String> changedLabels(ApplicationVersion a, ApplicationVersion b) {
        List<String> out = new ArrayList<>();
        List<Field> all = new ArrayList<>(APPLICANT);
        all.addAll(CONTENT);
        all.addAll(COMPANY_EXTRA);
        for (Field f : all) {
            if (!Objects.equals(text(f.value.apply(a)), text(f.value.apply(b)))) {
                out.add(f.label);
            }
        }
        return out;
    }
}
