package com.example.appmgmt.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 区分値の定数と表示名（08. コード定義）。 */
public final class Codes {

    private Codes() {
    }

    // 会社区分
    public static final String COMPANY_DIV_A = "1";
    public static final String COMPANY_DIV_B = "2";

    /** 会社B（会社区分 2）だけの申込の追加項目（法人番号・設置場所・窓口メモ）を入力する会社区分か。項目を使った制御はない。 */
    public static boolean hasCompanyExtra(String companyDiv) {
        return COMPANY_DIV_B.equals(companyDiv);
    }

    // 権限
    public static final String ROLE_OWNER = "01";
    public static final String ROLE_APPROVER = "02";
    public static final String ROLE_ADMIN = "09";

    // 操作主体
    public static final String ACTOR_OWNER = "1";
    public static final String ACTOR_APPROVER = "2";
    public static final String ACTOR_APPLICANT = "3";
    public static final String ACTOR_REVIEWER = "4";
    public static final String ACTOR_NONE = "9";

    // 操作コード
    public static final String ACTION_CREATE = "00";
    public static final String ACTION_MODIFY = "01";
    public static final String ACTION_CONFIRM = "02";
    public static final String ACTION_REQUEST = "03";
    public static final String ACTION_APPROVE = "04";
    public static final String ACTION_RETURN = "05";
    public static final String ACTION_PULL_BACK = "06";
    public static final String ACTION_CONSENT = "07";
    public static final String ACTION_APPLICANT_RETURN = "08";
    public static final String ACTION_REVIEW_REQUEST = "09";
    public static final String ACTION_PRECHECK_OK = "10";
    public static final String ACTION_PRECHECK_NG = "11";
    public static final String ACTION_REVIEW_COMPLETED = "12";
    public static final String ACTION_REVIEW_RETURNED = "13";
    public static final String ACTION_CHANGE_START = "14";
    public static final String ACTION_CANCEL = "15";

    // 条件コード
    public static final String COND_NONE = "00";
    public static final String COND_ROUTE_EXISTS = "11";
    public static final String COND_ROUTE_NONE = "12";
    public static final String COND_OVER_LIMIT = "21";
    public static final String COND_FIRST_CHANGE = "41";
    public static final String COND_LATER_CHANGE = "42";

    // 事前確認区分（遷移マスタ）
    public static final String PRE_CHECK_COMMON = "9";

    // 承認種別
    public static final String APPROVAL_PRIMARY = "01";
    public static final String APPROVAL_FINAL = "02";
    public static final String APPROVAL_CHANGE_PRIMARY = "03";
    public static final String APPROVAL_CHANGE_FINAL = "04";

    // 申請状態
    public static final String REQUEST_IN_PROGRESS = "1";
    public static final String REQUEST_APPROVED = "2";
    public static final String REQUEST_RETURNED = "3";

    // 承認結果
    public static final String RESULT_PENDING = "0";
    public static final String RESULT_APPROVED = "1";
    public static final String RESULT_RETURNED = "2";
    public static final String RESULT_REVIEW_REQUESTED = "3";

    // 版種別
    public static final String VERSION_NEW = "1";
    public static final String VERSION_NEW_REVISED = "2";
    public static final String VERSION_CHANGE = "3";
    public static final String VERSION_CHANGE_REVISED = "4";

    // 登録区分
    public static final String REG_IMPORT = "1";
    public static final String REG_SCREEN = "2";
    public static final String REG_ADDITIONAL = "3";

    // 同意種別
    public static final String CONSENT_TYPE_NEW = "1";
    public static final String CONSENT_TYPE_CHANGE = "2";

    // 同意状態
    public static final String CONSENT_REQUESTED = "1";
    public static final String CONSENT_AGREED = "2";
    public static final String CONSENT_RETURNED = "3";
    public static final String CONSENT_INVALID = "9";

    // 同意事項の対象（M_CONSENT_DOCUMENT.TARGET_TYPE）。申込者同意の同意種別と同じ値 + 共通
    public static final String DOC_TARGET_NEW = "1";
    public static final String DOC_TARGET_CHANGE = "2";
    public static final String DOC_TARGET_COMMON = "9";

    // 申込内容 PDF の種別（T_APPLICATION_PDF.PDF_TYPE）
    public static final String PDF_CONSENTED = "1";
    public static final String PDF_REVIEWED = "2";

    // 連携種別
    public static final String LINK_PRECHECK = "1";
    public static final String LINK_REVIEW = "2";
    public static final String LINK_CHANGE_PRECHECK = "3";
    public static final String LINK_CHANGE_REVIEW = "4";

    // 送信状態
    public static final String SEND_PENDING = "0";
    public static final String SEND_SENT = "1";
    public static final String SEND_ERROR = "2";

    // 連携結果
    public static final String EXT_RESULT_OK = "1";
    public static final String EXT_RESULT_NG = "2";
    public static final String EXT_RESULT_COMPLETED = "3";
    public static final String EXT_RESULT_RETURNED = "4";

    // 通知種別
    public static final String NOTIFY_CONSENT_REQUEST = "01";
    public static final String NOTIFY_APPROVAL_REQUEST = "02";
    public static final String NOTIFY_RETURNED = "03";
    public static final String NOTIFY_APPLICANT_RETURNED = "04";
    public static final String NOTIFY_PRECHECK_RESULT = "05";
    public static final String NOTIFY_REVIEW_RESULT = "06";
    public static final String NOTIFY_SEND_ERROR = "07";
    public static final String NOTIFY_APPLICANT_ACCOUNT = "08";
    public static final String NOTIFY_PASSWORD_RESET = "09";

    public static final String FLG_ON = "1";
    public static final String FLG_OFF = "0";

    private static final Map<String, Map<String, String>> LABELS = new LinkedHashMap<>();

    static {
        put("COMPANY_DIV", "1", "会社A", "2", "会社B");
        put("ROLE", "01", "担当者", "02", "承認者", "09", "管理者");
        put("ACTOR", "1", "担当者", "2", "承認者", "3", "申込者", "4", "審査担当部門", "9", "なし");
        put("ACTION", "00", "新規作成", "01", "修正", "02", "確定", "03", "申請", "04", "承認", "05", "差戻し", "06", "引戻し",
                "07", "同意", "08", "申込者差戻し", "09", "審査申請", "10", "事前確認OK", "11", "事前確認NG", "12", "審査完了",
                "13", "審査差戻し", "14", "契約変更開始", "15", "取消");
        put("CONDITION", "00", "なし", "11", "回付先あり", "12", "回付先なし", "21", "変更基準超", "41", "初回の契約変更", "42", "2回目以降の契約変更");
        put("APPROVAL_TYPE", "01", "一次承認", "02", "最終承認", "03", "契約変更一次承認", "04", "契約変更最終承認");
        put("REQUEST_STATUS", "1", "申請中", "2", "承認済", "3", "差戻し");
        put("APPROVAL_RESULT", "0", "未処理", "1", "承認", "2", "差戻し", "3", "審査申請");
        put("VERSION_TYPE", "1", "新規申込", "2", "新規申込の修正", "3", "契約変更", "4", "契約変更の修正");
        put("REGISTRATION_TYPE", "1", "一括取込", "2", "画面入力", "3", "追加申込");
        put("CONSENT_TYPE", "1", "新規申込", "2", "契約変更");
        put("CONSENT_STATUS", "1", "確認依頼中", "2", "同意済", "3", "申込者差戻し", "9", "無効");
        put("DOC_TARGET", "1", "新規申込", "2", "契約変更", "9", "共通");
        put("PDF_TYPE", "1", "ご同意時", "2", "審査完了時");
        put("LINK_TYPE", "1", "事前確認依頼", "2", "審査依頼", "3", "契約変更事前確認依頼", "4", "契約変更審査依頼");
        put("SEND_STATUS", "0", "未送信", "1", "送信済", "2", "送信エラー");
        put("EXT_RESULT", "1", "問題なし", "2", "修正必要", "3", "審査完了", "4", "審査差戻し");
        put("NOTIFICATION_TYPE", "01", "申込者確認依頼", "02", "承認依頼", "03", "差戻し通知", "04", "申込者差戻し通知",
                "05", "事前確認結果通知", "06", "審査結果通知", "07", "送信エラー通知", "08", "申込者アカウント通知", "09", "パスワード初期化通知");
        put("FLG", "1", "あり", "0", "なし");
    }

    private static void put(String group, String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        LABELS.put(group, Collections.unmodifiableMap(m));
    }

    public static String label(String group, String code) {
        if (code == null) {
            return "";
        }
        Map<String, String> m = LABELS.get(group);
        if (m == null) {
            return code;
        }
        return m.getOrDefault(code, code);
    }

    public static Map<String, String> labels(String group) {
        return LABELS.getOrDefault(group, Collections.emptyMap());
    }

    /** 承認種別 → 申請待ちステータス、申請中ステータスなどの対応（08. 9 章）。 */
    public static String approvalTypeOf(String statusCd) {
        switch (statusCd) {
            case StatusCd.PRIMARY_WAIT: case StatusCd.PRIMARY_IN_PROGRESS: return APPROVAL_PRIMARY;
            case StatusCd.FINAL_WAIT: case StatusCd.FINAL_IN_PROGRESS: return APPROVAL_FINAL;
            case StatusCd.CHG_PRIMARY_WAIT: case StatusCd.CHG_PRIMARY_IN_PROGRESS: return APPROVAL_CHANGE_PRIMARY;
            case StatusCd.CHG_FINAL_WAIT: case StatusCd.CHG_FINAL_IN_PROGRESS: return APPROVAL_CHANGE_FINAL;
            default: return null;
        }
    }

    public static boolean isFinalApprovalType(String approvalType) {
        return APPROVAL_FINAL.equals(approvalType) || APPROVAL_CHANGE_FINAL.equals(approvalType);
    }
}
