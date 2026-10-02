package com.example.appmgmt.dao;

import com.example.appmgmt.common.AuditContext;
import com.example.appmgmt.common.SystemException;
import com.example.appmgmt.domain.AuditedEntity;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO 共通の JDBC ヘルパ。SQL は各 DAO に閉じ、PreparedStatement のみ使う。 */
public abstract class AbstractDao {

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    protected static final String AUDIT_COLS = "CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, ROW_VERSION";

    protected LocalDateTime now() {
        return LocalDateTime.now().withNano((LocalDateTime.now().getNano() / 1_000_000) * 1_000_000);
    }

    protected String actor() {
        return AuditContext.get();
    }

    protected <T> List<T> query(Connection conn, String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapper.map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new SystemException("SQL の実行に失敗しました: " + sql, e);
        }
    }

    protected <T> Optional<T> queryOne(Connection conn, String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(conn, sql, mapper, params);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    protected long queryLong(Connection conn, String sql, Object... params) {
        return queryOne(conn, sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    protected int update(Connection conn, String sql, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new SystemException("SQL の実行に失敗しました: " + sql, e);
        }
    }

    protected long insertAndGetKey(Connection conn, String sql, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new SystemException("採番した ID を取得できません: " + sql);
        } catch (SQLException e) {
            throw new SystemException("SQL の実行に失敗しました: " + sql, e);
        }
    }

    protected void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int idx = i + 1;
            if (p == null) {
                ps.setNull(idx, Types.NULL);
            } else if (p instanceof String) {
                ps.setString(idx, (String) p);
            } else if (p instanceof Integer) {
                ps.setInt(idx, (Integer) p);
            } else if (p instanceof Long) {
                ps.setLong(idx, (Long) p);
            } else if (p instanceof BigDecimal) {
                ps.setBigDecimal(idx, (BigDecimal) p);
            } else if (p instanceof LocalDate) {
                ps.setDate(idx, Date.valueOf((LocalDate) p));
            } else if (p instanceof LocalDateTime) {
                ps.setTimestamp(idx, Timestamp.valueOf((LocalDateTime) p));
            } else if (p instanceof Boolean) {
                ps.setBoolean(idx, (Boolean) p);
            } else {
                ps.setObject(idx, p);
            }
        }
    }

    protected static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    protected static LocalDate dt(ResultSet rs, String col) throws SQLException {
        Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    protected static Integer intObj(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    protected static Long longObj(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    protected static void mapAudit(ResultSet rs, AuditedEntity e) throws SQLException {
        e.setCreatedAt(ts(rs, "CREATED_AT"));
        e.setCreatedBy(rs.getString("CREATED_BY"));
        e.setUpdatedAt(ts(rs, "UPDATED_AT"));
        e.setUpdatedBy(rs.getString("UPDATED_BY"));
        e.setRowVersion(rs.getInt("ROW_VERSION"));
    }

    /** 文字列を最大桁で切り詰める（履歴コメント等）。 */
    protected static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
