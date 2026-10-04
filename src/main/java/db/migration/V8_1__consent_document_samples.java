package db.migration;

import com.example.appmgmt.common.TokenUtil;
import com.example.appmgmt.service.document.SampleConsentDocuments;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * V8_1 サンプルの同意事項（新規申込 3 件・契約変更 2 件。利用規約は共通）と第 1 版の PDF を登録する。
 * PDF はサンプル文言から生成する（バイナリをリポジトリに置かないため Java マイグレーションにしている）。
 */
public class V8_1__consent_document_samples extends BaseJavaMigration {

    private static final Timestamp AT = Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0));

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        insert(conn, "TERMS", "利用規約", "9", 10, "terms_v1.pdf", SampleConsentDocuments.terms());
        insert(conn, "IMPORTANT_NEW", "重要事項説明（新規申込）", "1", 20, "important_new_v1.pdf", SampleConsentDocuments.importantNew());
        insert(conn, "PRIVACY", "個人情報の取扱いについて", "1", 30, "privacy_v1.pdf", SampleConsentDocuments.privacy());
        insert(conn, "IMPORTANT_CHANGE", "重要事項説明（契約変更）", "2", 20, "important_change_v1.pdf", SampleConsentDocuments.importantChange());
    }

    private static void insert(Connection conn, String cd, String name, String target, int order, String fileName, byte[] pdf) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO M_CONSENT_DOCUMENT (DOCUMENT_CD, DOCUMENT_NAME, TARGET_TYPE, DISPLAY_ORDER, VALID_FLG, "
                + "CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, ROW_VERSION) VALUES (?, ?, ?, ?, '1', ?, 'SYSTEM', ?, 'SYSTEM', 1)")) {
            ps.setString(1, cd);
            ps.setString(2, name);
            ps.setString(3, target);
            ps.setInt(4, order);
            ps.setTimestamp(5, AT);
            ps.setTimestamp(6, AT);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO M_CONSENT_DOCUMENT_VERSION (DOCUMENT_CD, VERSION_NO, EFFECTIVE_FROM, FILE_NAME, FILE_SIZE, FILE_HASH, FILE_DATA, REMARKS, "
                + "CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, ROW_VERSION) VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, 'SYSTEM', ?, 'SYSTEM', 1)")) {
            ps.setString(1, cd);
            ps.setTimestamp(2, AT);
            ps.setString(3, fileName);
            ps.setInt(4, pdf.length);
            ps.setString(5, TokenUtil.sha256Hex(pdf));
            ps.setBytes(6, pdf);
            ps.setString(7, "初版（サンプル）");
            ps.setTimestamp(8, AT);
            ps.setTimestamp(9, AT);
            ps.executeUpdate();
        }
    }
}
