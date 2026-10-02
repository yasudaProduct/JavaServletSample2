package com.example.appmgmt.service.external;

import com.example.appmgmt.dao.ApplicantDao;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.dao.ApplicationVersionDao;
import com.example.appmgmt.domain.Applicant;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.ExternalLink;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** IF01／IF03 送信電文の組み立て（12. 外部インターフェース設計 3.2 節）。 */
public class ExternalRequestBuilder {

    private final ApplicationDao applicationDao;
    private final ApplicationVersionDao versionDao;
    private final ApplicantDao applicantDao;

    public ExternalRequestBuilder(ApplicationDao applicationDao, ApplicationVersionDao versionDao, ApplicantDao applicantDao) {
        this.applicationDao = applicationDao;
        this.versionDao = versionDao;
        this.applicantDao = applicantDao;
    }

    public Map<String, Object> build(Connection conn, ExternalLink link) {
        Application app = applicationDao.findById(conn, link.getApplicationId()).orElseThrow();
        ApplicationVersion v = versionDao.get(conn, app.getApplicationId(), link.getVersionNo());
        Applicant ap = applicantDao.findById(conn, app.getApplicantId()).orElseThrow();
        boolean precheck = link.isPrecheck();
        boolean changeReview = Codes.LINK_CHANGE_REVIEW.equals(link.getLinkType());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("requestId", link.getExternalLinkId());
        m.put("linkType", link.getLinkType());
        m.put("applicationNo", app.getApplicationNo());
        m.put("versionNo", v.getVersionNo());
        m.put("versionType", v.getVersionType());
        m.put("companyDiv", app.getCompanyDiv());
        Map<String, Object> applicant = new LinkedHashMap<>();
        applicant.put("applicantNo", ap.getApplicantNo());
        applicant.put("applicantName", ap.getApplicantName());
        applicant.put("applicantKana", ap.getApplicantKana());
        applicant.put("mailAddress", ap.getMailAddress());
        applicant.put("telNo", ap.getTelNo());
        applicant.put("address", ap.getAddress());
        m.put("applicant", applicant);
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("productCd", v.getProductCd());
        m.put("product", product);
        Map<String, Object> amounts = new LinkedHashMap<>();
        amounts.put("basicFee", v.getBasicFee());
        amounts.put("optionFee", v.getOptionFee());
        amounts.put("handlingFee", v.getHandlingFee());
        amounts.put("totalAmount", v.getTotalAmount());
        m.put("amounts", amounts);
        m.put("contractStartDate", v.getContractStartDate() == null ? null : v.getContractStartDate().toString());
        m.put("contractEndDate", v.getContractEndDate() == null ? null : v.getContractEndDate().toString());
        m.put("remarks", v.getRemarks());

        Integer baseNo = precheck ? app.getBaseVersionNo() : (changeReview ? app.getReviewedVersionNo() : null);
        if (baseNo != null) {
            ApplicationVersion b = versionDao.get(conn, app.getApplicationId(), baseNo);
            Map<String, Object> base = new LinkedHashMap<>();
            base.put("versionNo", b.getVersionNo());
            base.put("productCd", b.getProductCd());
            base.put("totalAmount", b.getTotalAmount());
            base.put("contractStartDate", b.getContractStartDate() == null ? null : b.getContractStartDate().toString());
            base.put("contractEndDate", b.getContractEndDate() == null ? null : b.getContractEndDate().toString());
            base.put("remarks", b.getRemarks());
            m.put("baseVersion", base);
        }
        if (precheck) {
            BigDecimal ratio = v.getAmountRatio();
            if (ratio == null || (baseNo != null && baseNo == v.getVersionNo())) {
                ratio = BigDecimal.ONE.setScale(4);
            }
            m.put("amountRatio", ratio);
        } else if (v.getAmountRatio() != null) {
            m.put("amountRatio", v.getAmountRatio());
        }
        int changeCount = 0;
        if (Codes.LINK_CHANGE_PRECHECK.equals(link.getLinkType()) || changeReview) {
            changeCount = 1;
            if (app.getReviewedVersionNo() != null) {
                ApplicationVersion reviewed = versionDao.get(conn, app.getApplicationId(), app.getReviewedVersionNo());
                changeCount = com.example.appmgmt.service.transition.StatusTransitionService.isContractChangeVersion(reviewed.getVersionType()) ? 2 : 1;
            }
        }
        m.put("contractChangeCount", changeCount);
        m.put("requestedAt", OffsetDateTime.now().withNano(0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return m;
    }
}
