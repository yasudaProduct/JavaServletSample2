package com.example.appmgmt.dao;

import com.example.appmgmt.domain.Status;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StatusDao extends AbstractDao {

    private static final String SELECT = "SELECT STATUS_CD, STATUS_NAME, APPLICANT_STATUS_NAME, CATEGORY_L, CATEGORY_M, CATEGORY_S, ACTOR_TYPE, EDITABLE_FLG, DISPLAY_ORDER, DESCRIPTION, " + AUDIT_COLS + " FROM M_STATUS";

    static Status map(ResultSet rs) throws SQLException {
        Status s = new Status();
        s.setStatusCd(rs.getString("STATUS_CD"));
        s.setStatusName(rs.getString("STATUS_NAME"));
        s.setApplicantStatusName(rs.getString("APPLICANT_STATUS_NAME"));
        s.setCategoryL(rs.getString("CATEGORY_L"));
        s.setCategoryM(rs.getString("CATEGORY_M"));
        s.setCategoryS(rs.getString("CATEGORY_S"));
        s.setActorType(rs.getString("ACTOR_TYPE"));
        s.setEditableFlg(rs.getString("EDITABLE_FLG"));
        s.setDisplayOrder(rs.getInt("DISPLAY_ORDER"));
        s.setDescription(rs.getString("DESCRIPTION"));
        mapAudit(rs, s);
        return s;
    }

    public List<Status> findAll(Connection conn) {
        return query(conn, SELECT + " ORDER BY DISPLAY_ORDER", StatusDao::map);
    }

    public Map<String, Status> findAllAsMap(Connection conn) {
        Map<String, Status> m = new LinkedHashMap<>();
        for (Status s : findAll(conn)) {
            m.put(s.getStatusCd(), s);
        }
        return m;
    }

    public Optional<Status> find(Connection conn, String statusCd) {
        return queryOne(conn, SELECT + " WHERE STATUS_CD = ?", StatusDao::map, statusCd);
    }
}
