package com.example.appmgmt.dao;

import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.StatusTransition;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class StatusTransitionDao extends AbstractDao {

    private static final String SELECT = "SELECT TRANSITION_ID, FROM_STATUS_CD, ACTION_CD, CONDITION_CD, EVAL_ORDER, TO_STATUS_CD, ACTOR_TYPE, PRE_CHECK_COND, REF_NO, " + AUDIT_COLS + " FROM M_STATUS_TRANSITION";

    static StatusTransition map(ResultSet rs) throws SQLException {
        StatusTransition t = new StatusTransition();
        t.setTransitionId(rs.getInt("TRANSITION_ID"));
        t.setFromStatusCd(rs.getString("FROM_STATUS_CD"));
        t.setActionCd(rs.getString("ACTION_CD"));
        t.setConditionCd(rs.getString("CONDITION_CD"));
        t.setEvalOrder(rs.getInt("EVAL_ORDER"));
        t.setToStatusCd(rs.getString("TO_STATUS_CD"));
        t.setActorType(rs.getString("ACTOR_TYPE"));
        t.setPreCheckCond(rs.getString("PRE_CHECK_COND"));
        t.setRefNo(rs.getString("REF_NO"));
        mapAudit(rs, t);
        return t;
    }

    /** 遷移候補：遷移元・操作コードが一致し、事前確認区分が共通（9）または会社区分のフラグと一致する行を評価順に返す。 */
    public List<StatusTransition> findCandidates(Connection conn, String fromStatusCd, String actionCd, String preCheckFlg) {
        return query(conn, SELECT + " WHERE FROM_STATUS_CD = ? AND ACTION_CD = ? AND (PRE_CHECK_COND = ? OR PRE_CHECK_COND = ?) ORDER BY EVAL_ORDER, TRANSITION_ID",
                StatusTransitionDao::map, fromStatusCd, actionCd, Codes.PRE_CHECK_COMMON, preCheckFlg);
    }

    /** 遷移元ステータスで有効な操作コードの一覧（画面のボタン表示の補助）。 */
    public List<StatusTransition> findByFrom(Connection conn, String fromStatusCd, String preCheckFlg) {
        return query(conn, SELECT + " WHERE FROM_STATUS_CD = ? AND (PRE_CHECK_COND = ? OR PRE_CHECK_COND = ?) ORDER BY ACTION_CD, EVAL_ORDER",
                StatusTransitionDao::map, fromStatusCd, Codes.PRE_CHECK_COMMON, preCheckFlg);
    }

    public List<StatusTransition> findAll(Connection conn) {
        return query(conn, SELECT + " ORDER BY TRANSITION_ID", StatusTransitionDao::map);
    }
}
