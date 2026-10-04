package com.example.appmgmt.dao;

import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.Codes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ApplicationVersionDao extends AbstractDao {

    /** 担当の表示名（会社区分名・部署名・担当社員名）も結合して取る。 */
    private static final String SELECT = "SELECT v.APPLICATION_ID, v.VERSION_NO, v.VERSION_TYPE, v.COPIED_FROM_VERSION_NO, v.APPLICANT_NAME, v.APPLICANT_KANA, v.TEL_NO, v.MAIL_ADDRESS, v.ADDRESS, "
            + "v.PRODUCT_CD, v.BASIC_FEE, v.OPTION_FEE, v.HANDLING_FEE, v.TOTAL_AMOUNT, v.CONTRACT_START_DATE, v.CONTRACT_END_DATE, "
            + "v.AMOUNT_RATIO, v.REMARKS, v.CONFIRMED_AT, v.FIXED_FLG, v.CANCELED_FLG, v.COMPANY_DIV, v.DEPT_CD, v.OWNER_EMPLOYEE_ID, "
            + "c.COMPANY_DIV_NAME, d.DEPT_NAME, e.EMPLOYEE_NAME AS OWNER_NAME, "
            + "v.CREATED_AT, v.CREATED_BY, v.UPDATED_AT, v.UPDATED_BY, v.ROW_VERSION FROM T_APPLICATION_VERSION v "
            + "LEFT JOIN M_COMPANY_DIV c ON c.COMPANY_DIV = v.COMPANY_DIV "
            + "LEFT JOIN M_DEPARTMENT d ON d.COMPANY_DIV = v.COMPANY_DIV AND d.DEPT_CD = v.DEPT_CD "
            + "LEFT JOIN M_EMPLOYEE e ON e.EMPLOYEE_ID = v.OWNER_EMPLOYEE_ID";

    static ApplicationVersion map(ResultSet rs) throws SQLException {
        ApplicationVersion v = new ApplicationVersion();
        v.setApplicationId(rs.getLong("APPLICATION_ID"));
        v.setVersionNo(rs.getInt("VERSION_NO"));
        v.setVersionType(rs.getString("VERSION_TYPE"));
        v.setCopiedFromVersionNo(intObj(rs, "COPIED_FROM_VERSION_NO"));
        v.setApplicantName(rs.getString("APPLICANT_NAME"));
        v.setApplicantKana(rs.getString("APPLICANT_KANA"));
        v.setTelNo(rs.getString("TEL_NO"));
        v.setMailAddress(rs.getString("MAIL_ADDRESS"));
        v.setAddress(rs.getString("ADDRESS"));
        v.setProductCd(rs.getString("PRODUCT_CD"));
        v.setBasicFee(rs.getBigDecimal("BASIC_FEE"));
        v.setOptionFee(rs.getBigDecimal("OPTION_FEE"));
        v.setHandlingFee(rs.getBigDecimal("HANDLING_FEE"));
        v.setTotalAmount(rs.getBigDecimal("TOTAL_AMOUNT"));
        v.setContractStartDate(dt(rs, "CONTRACT_START_DATE"));
        v.setContractEndDate(dt(rs, "CONTRACT_END_DATE"));
        v.setAmountRatio(rs.getBigDecimal("AMOUNT_RATIO"));
        v.setRemarks(rs.getString("REMARKS"));
        v.setConfirmedAt(ts(rs, "CONFIRMED_AT"));
        v.setFixedFlg(rs.getString("FIXED_FLG"));
        v.setCanceledFlg(rs.getString("CANCELED_FLG"));
        v.setCompanyDiv(rs.getString("COMPANY_DIV"));
        v.setDeptCd(rs.getString("DEPT_CD"));
        v.setOwnerEmployeeId(longObj(rs, "OWNER_EMPLOYEE_ID"));
        v.setCompanyDivName(rs.getString("COMPANY_DIV_NAME"));
        v.setDeptName(rs.getString("DEPT_NAME"));
        v.setOwnerName(rs.getString("OWNER_NAME"));
        mapAudit(rs, v);
        return v;
    }

    public Optional<ApplicationVersion> find(Connection conn, long applicationId, int versionNo) {
        return queryOne(conn, SELECT + " WHERE v.APPLICATION_ID = ? AND v.VERSION_NO = ?", ApplicationVersionDao::map, applicationId, versionNo);
    }

    public ApplicationVersion get(Connection conn, long applicationId, int versionNo) {
        return find(conn, applicationId, versionNo).orElseThrow(() -> new IllegalStateException("版が存在しません: " + applicationId + "/" + versionNo));
    }

    public List<ApplicationVersion> findAll(Connection conn, long applicationId) {
        return query(conn, SELECT + " WHERE v.APPLICATION_ID = ? ORDER BY v.VERSION_NO DESC", ApplicationVersionDao::map, applicationId);
    }

    public void insert(Connection conn, ApplicationVersion v) {
        v.recalcTotal();
        update(conn, "INSERT INTO T_APPLICATION_VERSION (APPLICATION_ID, VERSION_NO, VERSION_TYPE, COPIED_FROM_VERSION_NO, APPLICANT_NAME, APPLICANT_KANA, TEL_NO, MAIL_ADDRESS, ADDRESS, "
                + "PRODUCT_CD, BASIC_FEE, OPTION_FEE, HANDLING_FEE, TOTAL_AMOUNT, CONTRACT_START_DATE, CONTRACT_END_DATE, "
                + "AMOUNT_RATIO, REMARKS, CONFIRMED_AT, FIXED_FLG, CANCELED_FLG, COMPANY_DIV, DEPT_CD, OWNER_EMPLOYEE_ID, " + AUDIT_COLS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                v.getApplicationId(), v.getVersionNo(), v.getVersionType(), v.getCopiedFromVersionNo(),
                v.getApplicantName(), v.getApplicantKana(), v.getTelNo(), v.getMailAddress(), v.getAddress(),
                v.getProductCd(), v.getBasicFee(), v.getOptionFee(), v.getHandlingFee(), v.getTotalAmount(),
                v.getContractStartDate(), v.getContractEndDate(), v.getAmountRatio(), v.getRemarks(), v.getConfirmedAt(),
                v.getFixedFlg() == null ? Codes.FLG_OFF : v.getFixedFlg(), v.getCanceledFlg() == null ? Codes.FLG_OFF : v.getCanceledFlg(),
                v.getCompanyDiv(), v.getDeptCd(), v.getOwnerEmployeeId(), now(), actor(), now(), actor());
    }

    /** 版の担当（会社区分・部署・担当社員）の更新。申込入力（SC04）の保存で、申込の担当と一緒に更新する。 */
    public void updateAssignment(Connection conn, long applicationId, int versionNo, String companyDiv, String deptCd, long ownerEmployeeId) {
        update(conn, "UPDATE T_APPLICATION_VERSION SET COMPANY_DIV = ?, DEPT_CD = ?, OWNER_EMPLOYEE_ID = ?, UPDATED_AT = ?, UPDATED_BY = ? WHERE APPLICATION_ID = ? AND VERSION_NO = ?",
                companyDiv, deptCd, ownerEmployeeId, now(), actor(), applicationId, versionNo);
    }

    /** 申込内容（申込者情報・商品・金額・契約期間・備考・倍率・確定日時）の更新。 */
    public void updateContent(Connection conn, ApplicationVersion v) {
        v.recalcTotal();
        update(conn, "UPDATE T_APPLICATION_VERSION SET APPLICANT_NAME = ?, APPLICANT_KANA = ?, TEL_NO = ?, MAIL_ADDRESS = ?, ADDRESS = ?, "
                + "PRODUCT_CD = ?, BASIC_FEE = ?, OPTION_FEE = ?, HANDLING_FEE = ?, TOTAL_AMOUNT = ?, CONTRACT_START_DATE = ?, CONTRACT_END_DATE = ?, AMOUNT_RATIO = ?, REMARKS = ?, CONFIRMED_AT = ?, "
                + "UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE APPLICATION_ID = ? AND VERSION_NO = ?",
                v.getApplicantName(), v.getApplicantKana(), v.getTelNo(), v.getMailAddress(), v.getAddress(),
                v.getProductCd(), v.getBasicFee(), v.getOptionFee(), v.getHandlingFee(), v.getTotalAmount(), v.getContractStartDate(), v.getContractEndDate(), v.getAmountRatio(), v.getRemarks(), v.getConfirmedAt(),
                now(), actor(), v.getApplicationId(), v.getVersionNo());
    }

    public void updateFixed(Connection conn, long applicationId, int versionNo) {
        update(conn, "UPDATE T_APPLICATION_VERSION SET FIXED_FLG = '1', UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE APPLICATION_ID = ? AND VERSION_NO = ?",
                now(), actor(), applicationId, versionNo);
    }

    /** 指定の版より後の版を取消にする（F13）。 */
    public int cancelAfter(Connection conn, long applicationId, int versionNo) {
        return update(conn, "UPDATE T_APPLICATION_VERSION SET CANCELED_FLG = '1', UPDATED_AT = ?, UPDATED_BY = ?, ROW_VERSION = ROW_VERSION + 1 WHERE APPLICATION_ID = ? AND VERSION_NO > ? AND CANCELED_FLG = '0'",
                now(), actor(), applicationId, versionNo);
    }

    /** 現行版を複写した新しい版を作る。 */
    public ApplicationVersion copyToNew(Connection conn, ApplicationVersion source, int newVersionNo, String versionType) {
        ApplicationVersion v = source.copyContent();
        v.setApplicationId(source.getApplicationId());
        v.setVersionNo(newVersionNo);
        v.setVersionType(versionType);
        v.setCopiedFromVersionNo(source.getVersionNo());
        v.setAmountRatio(null);
        v.setConfirmedAt(null);
        v.setFixedFlg(Codes.FLG_OFF);
        v.setCanceledFlg(Codes.FLG_OFF);
        insert(conn, v);
        return v;
    }
}
