package com.example.appmgmt.service.application;

import com.example.appmgmt.common.ForbiddenException;
import com.example.appmgmt.common.OptimisticLockException;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.dao.ApplicantDao;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.domain.Applicant;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.LoginUser;
import java.sql.Connection;
import java.util.List;

/**
 * F18 申込者情報の管理。
 * 申込者は申込受付会社が新規申込を登録するとき（SC04 の画面入力、SC10 の一括取込）に、申込の作成と同じトランザクションで登録・採番する。
 * 申込者が関与する前（アカウント未発行で、この申込だけで使われている間）は SC04 で補正でき、それ以降の変更は SC15 で行う。
 */
public class ApplicantService {

    private final ApplicantDao applicantDao;
    private final ApplicationDao applicationDao;

    public ApplicantService(ApplicantDao applicantDao, ApplicationDao applicationDao) {
        this.applicantDao = applicantDao;
        this.applicationDao = applicationDao;
    }

    /** 新規の申込者を登録する（申込者番号を採番）。呼び出し側のトランザクションで実行する。 */
    public static Applicant register(Connection conn, ApplicantDao dao, Applicant input) {
        input.setApplicantNo(dao.nextApplicantNo(conn));
        input.setApplicantId(dao.insert(conn, input));
        return input;
    }

    /** 同じメールアドレスの登録済み申込者（excludeId は除く）。 */
    public List<Applicant> duplicates(String mailAddress, Long excludeId) {
        return Tx.execute(conn -> applicantDao.findByMail(conn, mailAddress, excludeId));
    }

    /** 申込入力（SC04）で申込者情報を補正できるか：申込者ページのアカウントが未発行で、この申込だけで使われている。 */
    public boolean editableInInput(Connection conn, Applicant a) {
        return a != null && !a.isAccountIssued() && applicantDao.countApplications(conn, a.getApplicantId()) <= 1;
    }

    public boolean editableInInput(Applicant a) {
        return Tx.execute(conn -> editableInInput(conn, a));
    }

    public int countApplications(long applicantId) {
        return Tx.execute(conn -> applicantDao.countApplications(conn, applicantId));
    }

    /** SC15 申込者情報の変更。申込の担当者または管理者だけが行える。申込者の行バージョンで楽観ロックする。 */
    public void updateProfile(long applicationId, Applicant input, int applicantRowVersion, LoginUser user) {
        Tx.executeVoid(conn -> {
            Application app = applicationDao.findById(conn, applicationId).orElseThrow(ForbiddenException::new);
            if (!user.isAdmin() && !(user.isOwner() && app.getOwnerEmployeeId() == user.getEmployeeId())) {
                throw new ForbiddenException();
            }
            input.setApplicantId(app.getApplicantId());
            if (applicantDao.updateProfile(conn, input, applicantRowVersion) != 1) {
                throw new OptimisticLockException();
            }
        });
    }
}
