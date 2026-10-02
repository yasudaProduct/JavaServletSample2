package com.example.appmgmt.common;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * トランザクション境界。1 回の execute が 1 トランザクション（14. 共通仕様 3 章）。
 * 正常終了でコミット、例外でロールバックする。
 */
public final class Tx {

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection conn) throws Exception;
    }

    @FunctionalInterface
    public interface VoidWork {
        void run(Connection conn) throws Exception;
    }

    private Tx() {
    }

    public static <T> T execute(Work<T> work) {
        return execute(DataSourceProvider.get(), work);
    }

    public static void executeVoid(VoidWork work) {
        execute(conn -> {
            work.run(conn);
            return null;
        });
    }

    public static <T> T execute(DataSource ds, Work<T> work) {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            T result = work.run(conn);
            conn.commit();
            return result;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignore) {
                    // ロールバック失敗は元の例外を優先する
                }
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new SystemException("トランザクション中にエラーが発生しました", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignore) {
                    // close 失敗は無視
                }
            }
        }
    }
}
