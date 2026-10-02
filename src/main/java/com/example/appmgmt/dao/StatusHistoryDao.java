package com.example.appmgmt.dao;

import com.example.appmgmt.domain.StatusHistory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class StatusHistoryDao extends AbstractDao {

    private static final String SELECT = "SELECT h.STATUS_HISTORY_ID, h.APPLICATION_ID, h.VERSION_NO, h.FROM_STATUS_CD, h.TO_STATUS_CD, h.TRANSITION_ID, h.ACTION_CD, h.ACTOR_TYPE, h.ACTOR_ID, h.COMMENT, h.CHANGED_AT, "
            + "h.CREATED_AT, h.CREATED_BY, h.UPDATED_AT, h.UPDATED_BY, h.ROW_VERSION FROM T_STATUS_HISTORY h";

    static StatusHistory map(ResultSet rs) throws SQLException {
        StatusHistory h = new StatusHistory();
        h.setStatusHistoryId(rs.getLong("STATUS_HISTORY_ID"));
        h.setApplicationId(rs.getLong("APPLICATION_ID"));
        h.setVersionNo(rs.getInt("VERSION_NO"));
        h.setFromStatusCd(rs.getString("FROM_STATUS_CD"));
        h.setToStatusCd(rs.getString("TO_STATUS_CD"));
        h.setTransitionId(intObj(rs, "TRANSITION_ID"));
        h.setActionCd(rs.getString("ACTION_CD"));
        h.setActorType(rs.getString("ACTOR_TYPE"));
        h.setActorId(rs.getString("ACTOR_ID"));
        h.setComment(rs.getString("COMMENT"));
        h.setChangedAt(ts(rs, "CHANGED_AT"));
        mapAudit(rs, h);
        return h;
    }

    public long insert(Connection conn, StatusHistory h) {
        return insertAndGetKey(conn, "INSERT INTO T_STATUS_HISTORY (APPLICATION_ID, VERSION_NO, FROM_STATUS_CD, TO_STATUS_CD, TRANSITION_ID, ACTION_CD, ACTOR_TYPE, ACTOR_ID, COMMENT, CHANGED_AT, " + AUDIT_COLS
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                h.getApplicationId(), h.getVersionNo(), h.getFromStatusCd(), h.getToStatusCd(), h.getTransitionId(), h.getActionCd(), h.getActorType(), h.getActorId(), truncate(h.getComment(), 500), h.getChangedAt(),
                now(), actor(), now(), actor());
    }

    public List<StatusHistory> findByApplication(Connection conn, long applicationId) {
        return query(conn, SELECT + " WHERE h.APPLICATION_ID = ? ORDER BY h.CHANGED_AT DESC, h.STATUS_HISTORY_ID DESC", StatusHistoryDao::map, applicationId);
    }
}
