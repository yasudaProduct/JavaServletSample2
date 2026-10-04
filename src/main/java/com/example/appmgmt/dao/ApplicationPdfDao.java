package com.example.appmgmt.dao;

import com.example.appmgmt.domain.ApplicationPdf;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** 申込内容 PDF（T_APPLICATION_PDF）。一覧では PDF 本体を読まない。 */
public class ApplicationPdfDao extends AbstractDao {

    private static final String COLS = "p.PDF_ID, p.APPLICATION_ID, p.VERSION_NO, p.PDF_TYPE, p.CONSENT_ID, p.FILE_NAME, p.FILE_SIZE, p.FILE_HASH, v.VERSION_TYPE, "
            + "p.CREATED_AT, p.CREATED_BY, p.UPDATED_AT, p.UPDATED_BY, p.ROW_VERSION";
    private static final String FROM = " FROM T_APPLICATION_PDF p JOIN T_APPLICATION_VERSION v ON v.APPLICATION_ID = p.APPLICATION_ID AND v.VERSION_NO = p.VERSION_NO";

    static ApplicationPdf map(ResultSet rs) throws SQLException {
        ApplicationPdf p = new ApplicationPdf();
        p.setPdfId(rs.getLong("PDF_ID"));
        p.setApplicationId(rs.getLong("APPLICATION_ID"));
        p.setVersionNo(rs.getInt("VERSION_NO"));
        p.setPdfType(rs.getString("PDF_TYPE"));
        p.setConsentId(longObj(rs, "CONSENT_ID"));
        p.setFileName(rs.getString("FILE_NAME"));
        p.setFileSize(rs.getInt("FILE_SIZE"));
        p.setFileHash(rs.getString("FILE_HASH"));
        p.setVersionType(rs.getString("VERSION_TYPE"));
        mapAudit(rs, p);
        return p;
    }

    /** 申込の PDF（作成の古い順）。 */
    public List<ApplicationPdf> findByApplication(Connection conn, long applicationId) {
        return query(conn, "SELECT " + COLS + FROM + " WHERE p.APPLICATION_ID = ? ORDER BY p.PDF_ID", ApplicationPdfDao::map, applicationId);
    }

    public Optional<ApplicationPdf> findWithData(Connection conn, long applicationId, long pdfId) {
        return queryOne(conn, "SELECT " + COLS + ", p.FILE_DATA" + FROM + " WHERE p.APPLICATION_ID = ? AND p.PDF_ID = ?", rs -> {
            ApplicationPdf p = map(rs);
            p.setData(rs.getBytes("FILE_DATA"));
            return p;
        }, applicationId, pdfId);
    }

    public long insert(Connection conn, ApplicationPdf p) {
        return insertAndGetKey(conn, "INSERT INTO T_APPLICATION_PDF (APPLICATION_ID, VERSION_NO, PDF_TYPE, CONSENT_ID, FILE_NAME, FILE_SIZE, FILE_HASH, FILE_DATA, " + AUDIT_COLS
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                p.getApplicationId(), p.getVersionNo(), p.getPdfType(), p.getConsentId(), p.getFileName(), p.getFileSize(), p.getFileHash(), p.getData(), now(), actor(), now(), actor());
    }
}
