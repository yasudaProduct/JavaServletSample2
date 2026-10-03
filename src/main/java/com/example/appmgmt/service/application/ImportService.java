package com.example.appmgmt.service.application;

import com.example.appmgmt.common.BusinessException;
import com.example.appmgmt.common.ForbiddenException;
import com.example.appmgmt.common.Formats;
import com.example.appmgmt.common.Messages;
import com.example.appmgmt.common.Tx;
import com.example.appmgmt.common.Validation;
import com.example.appmgmt.dao.ApplicantDao;
import com.example.appmgmt.dao.ApplicationDao;
import com.example.appmgmt.dao.ApplicationVersionDao;
import com.example.appmgmt.dao.ImportBatchDao;
import com.example.appmgmt.dao.StatusHistoryDao;
import com.example.appmgmt.domain.Applicant;
import com.example.appmgmt.domain.Application;
import com.example.appmgmt.domain.ApplicationVersion;
import com.example.appmgmt.domain.Codes;
import com.example.appmgmt.domain.ImportBatch;
import com.example.appmgmt.domain.ImportError;
import com.example.appmgmt.domain.LoginUser;
import com.example.appmgmt.domain.StatusCd;
import com.example.appmgmt.domain.StatusHistory;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** F01 申込一括取込（IF05 の CSV）。 */
public class ImportService {

    /**
     * 列：申込者（番号・名・カナ・メールアドレス・電話番号・住所）＋申込内容。
     * 申込者番号が空の行は新規の申込者として登録・採番する（F18）。指定した行は登録済みの申込者に紐づけ、申込者情報の列は空にする。
     */
    public static final String[] HEADER = {"申込者番号", "申込者名", "申込者名カナ", "メールアドレス", "電話番号", "住所",
            "商品コード", "基本料金", "オプション料金", "事務手数料", "契約開始日", "契約終了日", "備考"};
    private static final int C_PRODUCT = 6;
    public static final int MAX_ROWS = 1000;
    public static final long MAX_SIZE = 5L * 1024 * 1024;

    private final ApplicationDao applicationDao;
    private final ApplicationVersionDao versionDao;
    private final ApplicantDao applicantDao;
    private final ImportBatchDao importBatchDao;
    private final StatusHistoryDao historyDao;

    public ImportService(ApplicationDao applicationDao, ApplicationVersionDao versionDao, ApplicantDao applicantDao, ImportBatchDao importBatchDao, StatusHistoryDao historyDao) {
        this.applicationDao = applicationDao;
        this.versionDao = versionDao;
        this.applicantDao = applicantDao;
        this.importBatchDao = importBatchDao;
        this.historyDao = historyDao;
    }

    /** ファイル単位のエラーは BusinessException（E008 または固定文言）。戻り値は取込 ID。 */
    public long importCsv(LoginUser user, String fileName, byte[] bytes) {
        if (!user.isOwner()) {
            throw new ForbiddenException();
        }
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv") || bytes == null || bytes.length == 0 || bytes.length > MAX_SIZE) {
            throw new BusinessException("E008");
        }
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new BusinessException("E008");
        }
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        List<List<String>> records = CsvParser.parse(text);
        if (records.isEmpty()) {
            throw new BusinessException("E008");
        }
        List<String> header = records.get(0);
        for (int i = 0; i < HEADER.length; i++) {
            String h = i < header.size() ? header.get(i).trim() : "";
            if (!HEADER[i].equals(h)) {
                throw new BusinessException("E008.header", i + 1, HEADER[i]) {
                    @Override
                    public String getMessage() {
                        return "ヘッダ行が正しくありません。" + (getArgs()[0]) + " 列目は「" + getArgs()[1] + "」である必要があります。";
                    }
                };
            }
        }
        List<List<String>> dataRows = new ArrayList<>();
        List<Integer> lineNos = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> r = records.get(i);
            boolean blank = r.stream().allMatch(s -> s == null || s.isBlank());
            if (blank) {
                continue;
            }
            dataRows.add(r);
            lineNos.add(i + 1);
        }
        if (dataRows.isEmpty()) {
            throw new BusinessException("E008.norows") {
                @Override
                public String getMessage() {
                    return "取込対象の行がありません。";
                }
            };
        }
        if (dataRows.size() > MAX_ROWS) {
            throw new BusinessException("E008.toomany") {
                @Override
                public String getMessage() {
                    return "取込できる行数は 1,000 行までです。";
                }
            };
        }
        return Tx.execute(conn -> {
            ImportBatch batch = new ImportBatch();
            batch.setFileName(fileName);
            batch.setImportEmployeeId(user.getEmployeeId());
            batch.setImportedAt(LocalDateTime.now().withNano(0));
            long batchId = importBatchDao.insert(conn, batch);
            int success = 0;
            int error = 0;
            // 同じファイル内で新規登録した申込者（メールアドレスの小文字 → 申込者と登録した行番号）
            java.util.Map<String, Object[]> createdInFile = new java.util.HashMap<>();
            for (int i = 0; i < dataRows.size(); i++) {
                List<String> row = dataRows.get(i);
                int lineNo = lineNos.get(i);
                List<String> errors = new ArrayList<>();
                ApplicationVersion v = new ApplicationVersion();
                Applicant applicant = null;
                Applicant newApplicant = null;
                if (row.size() != HEADER.length) {
                    errors.add("列数が正しくありません（" + HEADER.length + " 列必要）。");
                } else {
                    String applicantNo = row.get(0).trim();
                    Applicant profile = profileOf(row);
                    boolean profileGiven = row.subList(1, C_PRODUCT).stream().anyMatch(c -> !c.isBlank());
                    if (!applicantNo.isEmpty()) {
                        // 登録済みの申込者を指定
                        if (applicantNo.length() > 12) {
                            errors.add(Messages.get("E002", "申込者番号", 12));
                        } else {
                            applicant = applicantDao.findByNo(conn, applicantNo).orElse(null);
                            if (applicant == null) {
                                errors.add("申込者番号 " + applicantNo + " は申込者マスタに存在しません。");
                            }
                        }
                        if (profileGiven) {
                            errors.add("申込者番号を指定した行は申込者情報の列を空にしてください（登録済みの申込者の情報は取込で変更しません）。");
                        }
                    } else {
                        // 新規の申込者：申込者情報を検証し、同じメールアドレスの申込者がいないことを確認する
                        int before = errors.size();
                        validateProfile(profile, errors);
                        if (errors.size() == before) {
                            String key = profile.getMailAddress().toLowerCase();
                            Object[] created = createdInFile.get(key);
                            if (created != null) {
                                Applicant c = (Applicant) created[0];
                                if (c.getApplicantName().equals(profile.getApplicantName())) {
                                    applicant = c;
                                } else {
                                    errors.add("同じファイルの " + created[1] + " 行目と同じメールアドレスで申込者名が異なります。");
                                }
                            } else {
                                List<Applicant> dup = applicantDao.findByMail(conn, profile.getMailAddress(), null);
                                if (!dup.isEmpty()) {
                                    errors.add("メールアドレス " + profile.getMailAddress() + " は登録済みの申込者（" + dup.get(0).getApplicantNo() + " " + dup.get(0).getApplicantName()
                                            + "）と同じです。同じ申込者なら申込者番号を指定してください。");
                                } else {
                                    newApplicant = profile;
                                }
                            }
                        }
                    }
                    String productCd = row.get(C_PRODUCT).trim();
                    if (productCd.isEmpty()) {
                        errors.add(Messages.get("E001", "商品コード"));
                    } else if (productCd.length() > 10) {
                        errors.add(Messages.get("E002", "商品コード", 10));
                    } else if (!Validation.isAlnum(productCd)) {
                        errors.add(Messages.get("E003", "商品コード"));
                    }
                    v.setProductCd(productCd);
                    v.setBasicFee(amount(row.get(C_PRODUCT + 1), "基本料金", true, errors));
                    v.setOptionFee(amount(row.get(C_PRODUCT + 2), "オプション料金", false, errors));
                    v.setHandlingFee(amount(row.get(C_PRODUCT + 3), "事務手数料", false, errors));
                    v.setContractStartDate(date(row.get(C_PRODUCT + 4), "契約開始日", errors));
                    v.setContractEndDate(date(row.get(C_PRODUCT + 5), "契約終了日", errors));
                    String remarks = row.get(C_PRODUCT + 6).trim();
                    if (remarks.length() > 1000) {
                        errors.add(Messages.get("E002", "備考", 1000));
                    } else if (Validation.hasControlChars(remarks)) {
                        errors.add(Messages.get("E003", "備考"));
                    }
                    v.setRemarks(remarks.isEmpty() ? null : remarks);
                    if (v.getContractStartDate() != null && v.getContractEndDate() != null && v.getContractEndDate().isBefore(v.getContractStartDate())) {
                        errors.add(Messages.get("E004"));
                    }
                    if (v.getBasicFee() != null && v.getOptionFee() != null && v.getHandlingFee() != null) {
                        v.recalcTotal();
                        if (v.getTotalAmount().signum() <= 0) {
                            errors.add(Messages.get("E005", "申込金額合計", 1));
                        }
                    }
                }
                if (errors.isEmpty() && newApplicant != null) {
                    applicant = ApplicantService.register(conn, applicantDao, newApplicant);
                    createdInFile.put(newApplicant.getMailAddress().toLowerCase(), new Object[] {applicant, lineNo});
                }
                if (!errors.isEmpty()) {
                    ImportError e = new ImportError();
                    e.setImportBatchId(batchId);
                    e.setLineNo(lineNo);
                    e.setErrorMessage(String.join("／", errors));
                    e.setRawLine(String.join(",", row));
                    importBatchDao.insertError(conn, e);
                    error++;
                    continue;
                }
                Application app = new Application();
                app.setApplicationNo(applicationDao.nextApplicationNo(conn));
                app.setApplicantId(applicant.getApplicantId());
                app.setOwnerEmployeeId(user.getEmployeeId());
                app.setCompanyDiv(user.getCompanyDiv());
                app.setDeptCd(user.getDeptCd());
                app.setStatusCd(StatusCd.IMPORTED);
                app.setCurrentVersionNo(1);
                app.setRegistrationType(Codes.REG_IMPORT);
                app.setImportBatchId(batchId);
                long appId = applicationDao.insert(conn, app);
                v.setApplicationId(appId);
                v.setVersionNo(1);
                v.setVersionType(Codes.VERSION_NEW);
                v.setFixedFlg(Codes.FLG_OFF);
                v.setCanceledFlg(Codes.FLG_OFF);
                versionDao.insert(conn, v);
                StatusHistory h = new StatusHistory();
                h.setApplicationId(appId);
                h.setVersionNo(1);
                h.setToStatusCd(StatusCd.IMPORTED);
                h.setActionCd(Codes.ACTION_CREATE);
                h.setActorType(Codes.ACTOR_OWNER);
                h.setActorId(String.valueOf(user.getEmployeeId()));
                h.setChangedAt(LocalDateTime.now().withNano(0));
                historyDao.insert(conn, h);
                success++;
            }
            importBatchDao.updateCounts(conn, batchId, dataRows.size(), success, error);
            return batchId;
        });
    }

    private static BigDecimal amount(String s, String name, boolean required, List<String> errors) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            if (required) {
                errors.add(Messages.get("E001", name));
                return null;
            }
            return BigDecimal.ZERO;
        }
        if (!t.matches("[0-9]{1,13}")) {
            errors.add(Messages.get("E003", name));
            return null;
        }
        return new BigDecimal(t);
    }

    private static LocalDate date(String s, String name, List<String> errors) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (!t.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            errors.add(Messages.get("E003", name));
            return null;
        }
        LocalDate d = Formats.parseDate(t);
        if (d == null) {
            errors.add(Messages.get("E003", name));
        }
        return d;
    }

    public Optional<ImportBatch> findBatch(long id) {
        return Tx.execute(conn -> importBatchDao.findById(conn, id));
    }

    public List<ImportError> findErrors(long id) {
        return Tx.execute(conn -> importBatchDao.findErrors(conn, id));
    }

    public List<ImportBatch> history(LoginUser user) {
        return Tx.execute(conn -> importBatchDao.findByEmployee(conn, user.getEmployeeId(), 20));
    }

    private static Applicant profileOf(List<String> row) {
        Applicant a = new Applicant();
        a.setApplicantName(row.get(1).trim());
        a.setApplicantKana(row.get(2).trim().isEmpty() ? null : row.get(2).trim());
        a.setMailAddress(row.get(3).trim());
        a.setTelNo(row.get(4).trim().isEmpty() ? null : row.get(4).trim());
        a.setAddress(row.get(5).trim().isEmpty() ? null : row.get(5).trim());
        return a;
    }

    /** 新規の申込者の列チェック（SC04 の申込者情報と同じ）。 */
    private static void validateProfile(Applicant a, List<String> errors) {
        String name = a.getApplicantName();
        if (name.isEmpty()) {
            errors.add(Messages.get("E001", "申込者名"));
        } else if (name.length() > 100) {
            errors.add(Messages.get("E002", "申込者名", 100));
        }
        String kana = a.getApplicantKana() == null ? "" : a.getApplicantKana();
        if (kana.length() > 100) {
            errors.add(Messages.get("E002", "申込者名カナ", 100));
        } else if (!kana.matches("[\\u30A0-\\u30FF\\u3000 ]*")) {
            errors.add(Messages.get("E003", "申込者名カナ"));
        }
        String mail = a.getMailAddress();
        if (mail.isEmpty()) {
            errors.add(Messages.get("E001", "メールアドレス"));
        } else if (mail.length() > 254 || !Validation.isMail(mail)) {
            errors.add(Messages.get("E003", "メールアドレス"));
        }
        String tel = a.getTelNo() == null ? "" : a.getTelNo();
        if (tel.length() > 15 || !Validation.isTel(tel)) {
            errors.add(Messages.get("E003", "電話番号"));
        }
        String addr = a.getAddress() == null ? "" : a.getAddress();
        if (addr.length() > 200) {
            errors.add(Messages.get("E002", "住所", 200));
        }
    }
}
