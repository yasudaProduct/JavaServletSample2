package com.example.appmgmt.service;

import com.example.appmgmt.common.AppConfig;
import com.example.appmgmt.dao.ApplicantConsentDao;
import com.example.appmgmt.dao.ApplicantDao;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.dao.ApplicationVersionDao;
import com.example.appmgmt.dao.ApprovalRequestDao;
import com.example.appmgmt.dao.ApprovalRouteDao;
import com.example.appmgmt.dao.CompanyDivDao;
import com.example.appmgmt.dao.EmployeeDao;
import com.example.appmgmt.dao.ExternalLinkDao;
import com.example.appmgmt.dao.ImportBatchDao;
import com.example.appmgmt.dao.NotificationDao;
import com.example.appmgmt.dao.StatusDao;
import com.example.appmgmt.dao.StatusHistoryDao;
import com.example.appmgmt.dao.StatusTransitionDao;
import com.example.appmgmt.infra.extapi.ExternalApiClient;
import com.example.appmgmt.infra.mail.MailSender;
import com.example.appmgmt.infra.scheduler.ExternalLinkSendJob;
import com.example.appmgmt.infra.scheduler.NotificationSendJob;
import com.example.appmgmt.service.application.ApplicationQueryService;
import com.example.appmgmt.service.application.ApplicationService;
import com.example.appmgmt.service.application.ImportService;
import com.example.appmgmt.service.approval.ApprovalService;
import com.example.appmgmt.service.auth.AuthService;
import com.example.appmgmt.service.consent.ConsentIssuer;
import com.example.appmgmt.service.consent.ConsentService;
import com.example.appmgmt.service.external.ExternalRequestBuilder;
import com.example.appmgmt.service.external.ExternalResultService;
import com.example.appmgmt.service.master.MasterService;
import com.example.appmgmt.service.notification.NotificationService;
import com.example.appmgmt.service.transition.StatusTransitionService;

/** サービスと DAO の組み立て（手動 DI）。Web アプリ起動時に 1 回だけ作る。 */
public final class Services {

    private static volatile Services instance;

    private final CompanyDivDao companyDivDao = new CompanyDivDao();
    private final EmployeeDao employeeDao = new EmployeeDao();
    private final ApplicantDao applicantDao = new ApplicantDao();
    private final StatusDao statusDao = new StatusDao();
    private final StatusTransitionDao statusTransitionDao = new StatusTransitionDao();
    private final ApprovalRouteDao approvalRouteDao = new ApprovalRouteDao();
    private final ImportBatchDao importBatchDao = new ImportBatchDao();
    private final ApplicationDao applicationDao = new ApplicationDao();
    private final ApplicationVersionDao applicationVersionDao = new ApplicationVersionDao();
    private final ApprovalRequestDao approvalRequestDao = new ApprovalRequestDao();
    private final ApplicantConsentDao applicantConsentDao = new ApplicantConsentDao();
    private final ExternalLinkDao externalLinkDao = new ExternalLinkDao();
    private final NotificationDao notificationDao = new NotificationDao();
    private final StatusHistoryDao statusHistoryDao = new StatusHistoryDao();

    private final NotificationService notificationService;
    private final ConsentIssuer consentIssuer;
    private final StatusTransitionService statusTransitionService;
    private final ApplicationQueryService applicationQueryService;
    private final ApplicationService applicationService;
    private final ImportService importService;
    private final ApprovalService approvalService;
    private final ConsentService consentService;
    private final ExternalRequestBuilder externalRequestBuilder;
    private final ExternalResultService externalResultService;
    private final AuthService authService;
    private final MasterService masterService;
    private final MailSender mailSender;
    private final ExternalApiClient externalApiClient;
    private final NotificationSendJob notificationSendJob;
    private final ExternalLinkSendJob externalLinkSendJob;

    private Services(AppConfig config) {
        notificationService = new NotificationService(notificationDao, employeeDao, applicantDao, statusDao);
        consentIssuer = new ConsentIssuer(applicantConsentDao, notificationService);
        statusTransitionService = new StatusTransitionService(applicationDao, applicationVersionDao, companyDivDao, statusTransitionDao, statusHistoryDao,
                approvalRequestDao, applicantConsentDao, externalLinkDao, notificationService, consentIssuer);
        applicationQueryService = new ApplicationQueryService(applicationDao, applicationVersionDao, applicantDao, employeeDao, statusDao, companyDivDao,
                approvalRequestDao, applicantConsentDao, externalLinkDao, statusHistoryDao);
        applicationService = new ApplicationService(applicationDao, applicationVersionDao, applicantDao, statusHistoryDao, externalLinkDao, statusTransitionService, consentIssuer);
        importService = new ImportService(applicationDao, applicationVersionDao, applicantDao, importBatchDao, statusHistoryDao);
        approvalService = new ApprovalService(applicationDao, approvalRequestDao, approvalRouteDao, employeeDao, applicationQueryService, statusTransitionService, notificationService);
        consentService = new ConsentService(applicantConsentDao, applicationDao, applicationVersionDao, applicantDao, statusDao, statusTransitionService);
        externalRequestBuilder = new ExternalRequestBuilder(applicationDao, applicationVersionDao, applicantDao);
        externalResultService = new ExternalResultService(externalLinkDao, applicationDao, statusTransitionService);
        authService = new AuthService(employeeDao, companyDivDao);
        masterService = new MasterService(employeeDao, companyDivDao, approvalRouteDao);
        mailSender = MailSender.create(config);
        externalApiClient = ExternalApiClient.create(config);
        notificationSendJob = new NotificationSendJob(notificationDao, mailSender, config);
        externalLinkSendJob = new ExternalLinkSendJob(externalLinkDao, applicationDao, externalRequestBuilder, externalApiClient, notificationService, config);
    }

    public static synchronized void init(AppConfig config) {
        instance = new Services(config);
    }

    public static Services get() {
        Services s = instance;
        if (s == null) {
            throw new IllegalStateException("Services が初期化されていません");
        }
        return s;
    }

    public CompanyDivDao getCompanyDivDao() { return companyDivDao; }
    public EmployeeDao getEmployeeDao() { return employeeDao; }
    public ApplicantDao getApplicantDao() { return applicantDao; }
    public StatusDao getStatusDao() { return statusDao; }
    public StatusTransitionDao getStatusTransitionDao() { return statusTransitionDao; }
    public ApprovalRouteDao getApprovalRouteDao() { return approvalRouteDao; }
    public ImportBatchDao getImportBatchDao() { return importBatchDao; }
    public ApplicationDao getApplicationDao() { return applicationDao; }
    public ApplicationVersionDao getApplicationVersionDao() { return applicationVersionDao; }
    public ApprovalRequestDao getApprovalRequestDao() { return approvalRequestDao; }
    public ApplicantConsentDao getApplicantConsentDao() { return applicantConsentDao; }
    public ExternalLinkDao getExternalLinkDao() { return externalLinkDao; }
    public NotificationDao getNotificationDao() { return notificationDao; }
    public StatusHistoryDao getStatusHistoryDao() { return statusHistoryDao; }
    public NotificationService getNotificationService() { return notificationService; }
    public ConsentIssuer getConsentIssuer() { return consentIssuer; }
    public StatusTransitionService getStatusTransitionService() { return statusTransitionService; }
    public ApplicationQueryService getApplicationQueryService() { return applicationQueryService; }
    public ApplicationService getApplicationService() { return applicationService; }
    public ImportService getImportService() { return importService; }
    public ApprovalService getApprovalService() { return approvalService; }
    public ConsentService getConsentService() { return consentService; }
    public ExternalRequestBuilder getExternalRequestBuilder() { return externalRequestBuilder; }
    public ExternalResultService getExternalResultService() { return externalResultService; }
    public AuthService getAuthService() { return authService; }
    public MasterService getMasterService() { return masterService; }
    public MailSender getMailSender() { return mailSender; }
    public ExternalApiClient getExternalApiClient() { return externalApiClient; }
    public NotificationSendJob getNotificationSendJob() { return notificationSendJob; }
    public ExternalLinkSendJob getExternalLinkSendJob() { return externalLinkSendJob; }
}
