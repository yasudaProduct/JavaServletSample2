package com.example.appmgmt.dao;

import com.example.appmgmt.domain.ImportBatch;
import com.example.appmgmt.domain.ImportError;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ImportBatchDao extends AbstractDao {

    private static final String SELECT = "SELECT b.IMPORT_BATCH_ID, b.FILE_NAME, b.IMPORT_EMPLOYEE_ID, e.EMPLOYEE_NAME, b.IMPORTED_AT, b.TOTAL_COUNT, b.SUCCESS_COUNT, b.ERROR_COUNT, b.CREATED_AT, b.CREATED_BY, b.UPDATED_AT, b.UPDATED_BY, b.ROW_VERSION "
            + "FROM T_IMPORT_BATCH b JOIN M_EMPLOYEE e ON e.EMPLOYEE_ID = b.IMPORT_EMPLOYEE_ID";

    static ImportBatch map(ResultSet rs) throws SQLException {
        ImportBatch b = new ImportBatch();
        b.setImportBatchId(rs.getLong("IMPORT_BATCH_ID"));
        b.setFileName(rs.getString("FILE_NAME"));
        b.setImportEmployeeId(rs.getLong("IMPORT_EMPLOYEE_ID"));
        b.setImportEmployeeName(rs.getString("EMPLOYEE_NAME"));
        b.setImportedAt(ts(rs, "IMPORTED_AT"));
        b.setTotalCount(rs.getInt("TOTAL_COUNT"));
        b.setSuccessCount(rs.getInt("SUCCESS_COUNT"));
        b.setErrorCount(rs.getInt("ERROR_COUNT"));
        mapAudit(rs, b);
        return b;
    }

    static ImportError mapError(ResultSet rs) throws SQLException {
        ImportError e = new ImportError();
        e.setImportBatchId(rs.getLong("IMPORT_BATCH_ID"));
        e.setLineNo(rs.getInt("LINE_NO"));
        e.setErrorMessage(rs.getString("ERROR_MESSAGE"));
        e.setRawLine(rs.getString("RAW_LINE"));
        mapAudit(rs, e);
        return e;
    }

    public long insert(Connection conn, ImportBatch b) {
        return insertAndGetKey(conn, "INSERT INTO T_IMPORT_BATCH (FILE_NAME, IMPORT_EMPLOYEE_ID, IMPORTED_AT, TOTAL_COUNT, SUCCESS_COUNT, ERROR_COUNT, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                b.getFileName(), b.getImportEmployeeId(), b.getImportedAt(), b.getTotalCount(), b.getSuccessCount(), b.getErrorCount(), now(), actor(), now(), actor());
    }

    public void updateCounts(Connection conn, long id, int total, int success, int error) {
        update(conn, "UPDATE T_IMPORT_BATCH SET TOTAL_COUNT = ?, SUCCESS_COUNT = ?, ERROR_COUNT = ?, UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE IMPORT_BATCH_ID = ?",
                total, success, error, now(), actor(), id);
    }

    public Optional<ImportBatch> findById(Connection conn, long id) {
        return queryOne(conn, SELECT + " WHERE b.IMPORT_BATCH_ID = ?", ImportBatchDao::map, id);
    }

    public List<ImportBatch> findByEmployee(Connection conn, long employeeId, int limit) {
        return query(conn, SELECT + " WHERE b.IMPORT_EMPLOYEE_ID = ? ORDER BY b.IMPORTED_AT DESC, b.IMPORT_BATCH_ID DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY", ImportBatchDao::map, employeeId, limit);
    }

    public void insertError(Connection conn, ImportError e) {
        update(conn, "INSERT INTO T_IMPORT_ERROR (IMPORT_BATCH_ID, LINE_NO, ERROR_MESSAGE, RAW_LINE, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
                e.getImportBatchId(), e.getLineNo(), truncate(e.getErrorMessage(), 500), truncate(e.getRawLine(), 2000), now(), actor(), now(), actor());
    }

    public List<ImportError> findErrors(Connection conn, long importBatchId) {
        return query(conn, "SELECT IMPORT_BATCH_ID, LINE_NO, ERROR_MESSAGE, RAW_LINE, " + AUDIT_COLS + " FROM T_IMPORT_ERROR WHERE IMPORT_BATCH_ID = ? ORDER BY LINE_NO", ImportBatchDao::mapError, importBatchId);
    }
}
