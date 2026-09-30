# 07. テーブル定義

| 項目 | 内容 |
| --- | --- |
| 版 | 0.1（初版・ドラフト） |
| 関連 | [06. ER図](06-er-diagram.md)、[08. コード定義](08-code-definition.md) |

テーブルはマスタ 7 本とトランザクション 10 本の計 17 本。型は論理型で記載し、SQL Server を想定した物理型は 3 章の対応表に従う（DBMS 確定後に見直す）。

## 1. テーブル一覧

| No | 論理名 | 物理名 | 種別 | 概要 |
| --- | --- | --- | --- | --- |
| 1 | 会社区分マスタ | M_COMPANY_DIV | マスタ | 会社区分と外部事前確認フローの有無、金額倍率しきい値 |
| 2 | 社員マスタ | M_EMPLOYEE | マスタ | 社員と所属会社区分・部署・権限 |
| 3 | 顧客マスタ | M_CUSTOMER | マスタ | 顧客と確認依頼の宛先 |
| 4 | ステータスマスタ | M_STATUS | マスタ | 23 ステータスと操作主体 |
| 5 | ステータス遷移マスタ | M_STATUS_TRANSITION | マスタ | 遷移元・操作・条件・遷移先 |
| 6 | 承認ルートマスタ | M_APPROVAL_ROUTE | マスタ | 会社区分・部署・承認種別ごとの回付ルート |
| 7 | 承認ルート明細 | M_APPROVAL_ROUTE_STEP | マスタ | ルート内の承認者と順序 |
| 8 | 一括取込 | T_IMPORT_BATCH | トランザクション | 取込ファイル単位の実行結果 |
| 9 | 取込エラー | T_IMPORT_ERROR | トランザクション | 取込でスキップした行と理由 |
| 10 | 申込 | T_APPLICATION | トランザクション | 申込 1 件の現在ステータスと基準金額 |
| 11 | 申込内容（版） | T_APPLICATION_VERSION | トランザクション | 版ごとの申込内容 |
| 12 | 承認申請 | T_APPROVAL_REQUEST | トランザクション | 申請 1 回分の回付状況 |
| 13 | 承認明細 | T_APPROVAL_STEP | トランザクション | 承認者ごとの承認・差戻し結果 |
| 14 | 顧客同意 | T_CUSTOMER_CONSENT | トランザクション | 顧客の同意・確定の記録と確認用トークン |
| 15 | 外部連携 | T_EXTERNAL_LINK | トランザクション | 外部事前確認・外部審査の依頼と結果 |
| 16 | 通知 | T_NOTIFICATION | トランザクション | メール通知のキューと送信結果 |
| 17 | ステータス履歴 | T_STATUS_HISTORY | トランザクション | ステータス変更の全履歴 |

元設計（15 本）からの追加：取込エラー（No.9）、通知（No.16）。追加・変更点の一覧は 5 章を参照。

## 2. 共通項目

全テーブルに以下の 5 項目を持つ。以降の各表では省略する。

| 論理名 | 物理名 | 型 | 必須 | 説明 |
| --- | --- | --- | --- | --- |
| 作成日時 | CREATED_AT | 日時 | ○ | 行の作成日時 |
| 作成者 | CREATED_BY | 文字列(20) | ○ | 社員 ID、顧客 ID、外部システム ID、バッチ名のいずれか |
| 更新日時 | UPDATED_AT | 日時 | ○ | 行の最終更新日時 |
| 更新者 | UPDATED_BY | 文字列(20) | ○ | 作成者と同じ体系 |
| 行バージョン | ROW_VERSION | 数値(9) | ○ | 楽観排他用。初期値 1、更新のたびに +1 |

## 3. 型対応表（論理型 → SQL Server 物理型、仮）

| 論理型 | 物理型 | 備考 |
| --- | --- | --- |
| 固定長(n) | CHAR(n) | 区分値・コード |
| 文字列(n) | NVARCHAR(n) | 日本語を含む文字列 |
| 数値(n)（n ≦ 9） | INT | |
| 数値(n)（10 ≦ n ≦ 18） | BIGINT | ID 列は IDENTITY(1,1) で採番 |
| 数値(13)（金額） | DECIMAL(13,0) | 円単位。小数なし |
| 数値(p,s) | DECIMAL(p,s) | |
| 日付 | DATE | |
| 日時 | DATETIME2(3) | ミリ秒まで |

- ID 列（〜_ID）は主キーとして IDENTITY で採番する。申込番号などの業務キーは別途採番する（[14. 共通仕様](14-common-spec.md)）。
- 照合順序は Japanese_CI_AS を想定。

## 4. テーブル定義

### 4.1 会社区分マスタ（M_COMPANY_DIV）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 会社区分 | COMPANY_DIV | 固定長(1) | ○ | PK | 1／2（値は仮） |
| 会社区分名 | COMPANY_DIV_NAME | 文字列(50) | ○ | | |
| 外部事前確認フラグ | PRE_CHECK_FLG | 固定長(1) | ○ | | 1：あり、0：なし |
| 金額倍率しきい値 | AMOUNT_RATIO_LIMIT | 数値(3,2) | ○ | | 既定値 1.50。これを超えると事前確認対象 |

### 4.2 社員マスタ（M_EMPLOYEE）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 社員ID | EMPLOYEE_ID | 数値(10) | ○ | PK | |
| 社員番号 | EMPLOYEE_NO | 文字列(10) | ○ | UK | ログイン ID |
| 氏名 | EMPLOYEE_NAME | 文字列(50) | ○ | | |
| パスワードハッシュ | PASSWORD_HASH | 文字列(100) | ○ | | ID／パスワード認証用。ソルト付きハッシュ。社内認証基盤と連携する場合は未使用 |
| 会社区分 | COMPANY_DIV | 固定長(1) | ○ | FK | M_COMPANY_DIV |
| 部署コード | DEPT_CD | 文字列(10) | ○ | | 承認ルートの判定に使う |
| 権限 | ROLE_CD | 固定長(2) | ○ | | 01：担当者、02：承認者、09：管理者 |
| メールアドレス | MAIL_ADDRESS | 文字列(254) | ○ | | 承認依頼通知の宛先 |
| 有効フラグ | VALID_FLG | 固定長(1) | ○ | | 1：有効、0：無効。無効な社員はログインできず、承認ルートにも設定できない |

インデックス：IX_M_EMPLOYEE_01（COMPANY_DIV, DEPT_CD）

### 4.3 顧客マスタ（M_CUSTOMER）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 顧客ID | CUSTOMER_ID | 数値(10) | ○ | PK | |
| 顧客番号 | CUSTOMER_NO | 文字列(12) | ○ | UK | 取込ファイルで申込と顧客を突き合わせるキー |
| 顧客名 | CUSTOMER_NAME | 文字列(100) | ○ | | |
| 顧客名カナ | CUSTOMER_KANA | 文字列(100) | | | |
| メールアドレス | MAIL_ADDRESS | 文字列(254) | ○ | | 確認依頼メールの宛先 |
| 電話番号 | TEL_NO | 文字列(15) | | | |
| 住所 | ADDRESS | 文字列(200) | | | |

### 4.4 ステータスマスタ（M_STATUS）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| ステータスコード | STATUS_CD | 固定長(4) | ○ | PK | 0101〜1202 |
| ステータス名 | STATUS_NAME | 文字列(30) | ○ | | |
| フロー区分 | FLOW_TYPE | 固定長(1) | ○ | | 1：新規申込、2：外部事前確認、3：契約変更、4：契約変更時の事前確認 |
| 操作主体 | ACTOR_TYPE | 固定長(1) | ○ | | 1：担当者、2：承認者、3：顧客、4：外部システム、9：なし |
| 編集可フラグ | EDITABLE_FLG | 固定長(1) | ○ | | 社員が申込内容を編集できるステータスは 1 |
| 表示順 | DISPLAY_ORDER | 数値(3) | ○ | | 一覧の検索条件・並び順に使う |

### 4.5 ステータス遷移マスタ（M_STATUS_TRANSITION）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 遷移ID | TRANSITION_ID | 数値(5) | ○ | PK | |
| 遷移元ステータス | FROM_STATUS_CD | 固定長(4) | ○ | FK | M_STATUS |
| 操作コード | ACTION_CD | 固定長(2) | ○ | | 01：修正、02：確定、03：申請、04：承認、05：差戻し、06：引戻し、07：同意、08：審査完了、09：確認OK、10：確認NG、11：契約変更開始 |
| 条件コード | CONDITION_CD | 固定長(2) | ○ | | 00：なし、11：回付先あり、12：回付先なし、21：最終承認者以外、22：最終承認者、32：倍率しきい値超、33：その他修正あり |
| 評価順 | EVAL_ORDER | 数値(2) | ○ | | 同一の遷移元・操作の中で条件を評価する順。若い順に評価し、最初に成立した行を採用する |
| 遷移先ステータス | TO_STATUS_CD | 固定長(4) | ○ | FK | M_STATUS |
| 操作主体 | ACTOR_TYPE | 固定長(1) | ○ | | M_STATUS と同じ区分値 |
| 事前確認限定フラグ | PRE_CHECK_ONLY_FLG | 固定長(1) | ○ | | 1：外部事前確認ありの会社区分だけで有効 |

ユニーク制約：UK_M_STATUS_TRANSITION_01（FROM_STATUS_CD, ACTION_CD, EVAL_ORDER）
初期データは [03. ステータス定義・状態遷移 4 章](03-status-transition.md#4-ステータス遷移マスタ-初期データ) を参照。

### 4.6 承認ルートマスタ（M_APPROVAL_ROUTE）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| ルートID | ROUTE_ID | 数値(10) | ○ | PK | |
| 会社区分 | COMPANY_DIV | 固定長(1) | ○ | FK | M_COMPANY_DIV |
| 部署コード | DEPT_CD | 文字列(10) | ○ | | |
| 承認種別 | APPROVAL_TYPE | 固定長(2) | ○ | | 01：一次承認、02：最終承認、03：契約変更承認、04：契約変更審査依頼承認 |
| ルート名 | ROUTE_NAME | 文字列(50) | ○ | | |
| 適用開始日 | VALID_FROM | 日付 | ○ | | |
| 適用終了日 | VALID_TO | 日付 | | | 空は無期限 |

インデックス：IX_M_APPROVAL_ROUTE_01（COMPANY_DIV, DEPT_CD, APPROVAL_TYPE, VALID_FROM）

### 4.7 承認ルート明細（M_APPROVAL_ROUTE_STEP）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| ルートID | ROUTE_ID | 数値(10) | ○ | PK、FK | M_APPROVAL_ROUTE |
| ステップ番号 | STEP_NO | 数値(2) | ○ | PK | 1 から連番。最大値が最終承認者 |
| 承認者社員ID | APPROVER_EMPLOYEE_ID | 数値(10) | ○ | FK | M_EMPLOYEE |

### 4.8 一括取込（T_IMPORT_BATCH）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 取込ID | IMPORT_BATCH_ID | 数値(10) | ○ | PK | |
| ファイル名 | FILE_NAME | 文字列(255) | ○ | | アップロード時のファイル名 |
| 取込社員ID | IMPORT_EMPLOYEE_ID | 数値(10) | ○ | FK | M_EMPLOYEE |
| 取込日時 | IMPORTED_AT | 日時 | ○ | | |
| 総件数 | TOTAL_COUNT | 数値(6) | ○ | | ヘッダ行を除いた行数 |
| 成功件数 | SUCCESS_COUNT | 数値(6) | ○ | | 0101 で登録した件数 |
| エラー件数 | ERROR_COUNT | 数値(6) | ○ | | スキップした件数 |

### 4.9 取込エラー（T_IMPORT_ERROR）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 取込ID | IMPORT_BATCH_ID | 数値(10) | ○ | PK、FK | T_IMPORT_BATCH |
| 行番号 | LINE_NO | 数値(6) | ○ | PK | ファイル内の行番号（ヘッダ行を 1 とする） |
| エラー内容 | ERROR_MESSAGE | 文字列(500) | ○ | | 複数エラーは区切り文字で連結 |
| 行データ | RAW_LINE | 文字列(2000) | | | 元の行をそのまま保持 |

### 4.10 申込（T_APPLICATION）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | PK | |
| 申込番号 | APPLICATION_NO | 文字列(12) | ○ | UK | 画面・帳票に表示する番号。採番規則は [14. 共通仕様](14-common-spec.md) |
| 顧客ID | CUSTOMER_ID | 数値(10) | ○ | FK | M_CUSTOMER |
| 担当社員ID | OWNER_EMPLOYEE_ID | 数値(10) | ○ | FK | M_EMPLOYEE。取込時は取込社員 |
| 会社区分 | COMPANY_DIV | 固定長(1) | ○ | FK | 起票時点の担当社員の会社区分を保持 |
| 部署コード | DEPT_CD | 文字列(10) | ○ | | 起票時点の担当社員の部署 |
| ステータスコード | STATUS_CD | 固定長(4) | ○ | FK | M_STATUS。現在のステータス |
| 現行版番号 | CURRENT_VERSION_NO | 数値(3) | ○ | | 編集中または承認・審査中の版 |
| 審査完了版番号 | REVIEWED_VERSION_NO | 数値(3) | | | 直近で審査完了した版 |
| 基準金額 | BASE_AMOUNT | 数値(13) | | | 外部審査を依頼した時点の申込金額。倍率判定の分母 |
| 取込ID | IMPORT_BATCH_ID | 数値(10) | | FK | T_IMPORT_BATCH |
| 審査完了日時 | REVIEWED_AT | 日時 | | | 直近の審査完了日時 |

インデックス：IX_T_APPLICATION_01（STATUS_CD）、IX_T_APPLICATION_02（OWNER_EMPLOYEE_ID, STATUS_CD）、IX_T_APPLICATION_03（CUSTOMER_ID）

### 4.11 申込内容（版）（T_APPLICATION_VERSION）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | PK、FK | T_APPLICATION |
| 版番号 | VERSION_NO | 数値(3) | ○ | PK | 1 から連番 |
| 版種別 | VERSION_TYPE | 固定長(1) | ○ | | 1：新規申込、2：審査中修正、3：契約変更、4：契約変更審査中修正 |
| 商品コード | PRODUCT_CD | 文字列(10) | ○ | | |
| 申込金額 | APPLICATION_AMOUNT | 数値(13) | ○ | | 円 |
| 契約開始日 | CONTRACT_START_DATE | 日付 | | | |
| 契約終了日 | CONTRACT_END_DATE | 日付 | | | |
| 変更金額倍率 | AMOUNT_RATIO | 数値(7,4) | | | 申込金額 ÷ 基準金額。審査中修正の版（版種別 2／4）で設定 |
| その他修正フラグ | OTHER_MODIFIED_FLG | 固定長(1) | ○ | | 金額以外の項目を修正した版は 1。第 1 版・契約変更版は 0 |
| 備考 | REMARKS | 文字列(1000) | | | |
| 確定日時 | CONFIRMED_AT | 日時 | | | 社員が確定した日時。差戻し後の再確定で上書き |

### 4.12 承認申請（T_APPROVAL_REQUEST）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 承認申請ID | APPROVAL_REQUEST_ID | 数値(10) | ○ | PK | |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | FK | T_APPLICATION_VERSION |
| 版番号 | VERSION_NO | 数値(3) | ○ | FK | T_APPLICATION_VERSION |
| 承認種別 | APPROVAL_TYPE | 固定長(2) | ○ | | M_APPROVAL_ROUTE と同じ区分値 |
| ルートID | ROUTE_ID | 数値(10) | | FK | 申請時に使ったルート。回付先なしの場合は空 |
| 申請社員ID | REQUEST_EMPLOYEE_ID | 数値(10) | ○ | FK | M_EMPLOYEE。引戻しできるのはこの社員 |
| 申請日時 | REQUESTED_AT | 日時 | ○ | | |
| 申請状態 | REQUEST_STATUS | 固定長(1) | ○ | | 1：申請中、2：承認済、3：差戻し、4：引戻し |
| 現在ステップ | CURRENT_STEP_NO | 数値(2) | | | 次に承認するステップ。回付先なしの場合は空 |
| 最終ステップ | FINAL_STEP_NO | 数値(2) | | | 申請時点のルートの最大ステップ。回付先なしの場合は空 |
| 完了日時 | COMPLETED_AT | 日時 | | | 承認済・差戻し・引戻しになった日時 |

インデックス：IX_T_APPROVAL_REQUEST_01（APPLICATION_ID, REQUEST_STATUS）

### 4.13 承認明細（T_APPROVAL_STEP）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 承認申請ID | APPROVAL_REQUEST_ID | 数値(10) | ○ | PK、FK | T_APPROVAL_REQUEST |
| ステップ番号 | STEP_NO | 数値(2) | ○ | PK | 申請時に承認ルート明細から複写 |
| 承認者社員ID | APPROVER_EMPLOYEE_ID | 数値(10) | ○ | FK | M_EMPLOYEE |
| 結果 | RESULT_CD | 固定長(1) | ○ | | 0：未処理、1：承認、2：差戻し |
| コメント | COMMENT | 文字列(500) | | | 差戻し時は必須 |
| 処理日時 | ACTED_AT | 日時 | | | |

インデックス：IX_T_APPROVAL_STEP_01（APPROVER_EMPLOYEE_ID, RESULT_CD）

### 4.14 顧客同意（T_CUSTOMER_CONSENT）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 同意ID | CONSENT_ID | 数値(10) | ○ | PK | |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | FK | T_APPLICATION_VERSION |
| 版番号 | VERSION_NO | 数値(3) | ○ | FK | 同意対象の版 |
| 同意種別 | CONSENT_TYPE | 固定長(1) | ○ | | 1：新規申込、2：契約変更 |
| 確認用トークン | ACCESS_TOKEN_HASH | 文字列(64) | ○ | UK | 確認 URL のトークンを SHA-256 でハッシュ化した 16 進文字列 |
| トークン有効期限 | TOKEN_EXPIRES_AT | 日時 | ○ | | 発行から 14 日後（仮） |
| 内容同意日時 | CONTENT_AGREED_AT | 日時 | | | 0301→0302、0901→0902 の日時 |
| 同意確定日時 | CONSENT_CONFIRMED_AT | 日時 | | | 0302→0401、0902→1001 の日時 |
| 接続元IP | CLIENT_IP | 文字列(45) | | | 同意・確定時の証跡。IPv6 対応 |
| 無効フラグ | INVALID_FLG | 固定長(1) | ○ | | 差戻し等で対象の版が再編集になった場合に 1。無効なトークンは使えない |

### 4.15 外部連携（T_EXTERNAL_LINK）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 外部連携ID | EXTERNAL_LINK_ID | 数値(10) | ○ | PK | |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | FK | T_APPLICATION_VERSION |
| 版番号 | VERSION_NO | 数値(3) | ○ | FK | 依頼対象の版 |
| 連携種別 | LINK_TYPE | 固定長(1) | ○ | | 1：外部事前確認、2：外部審査、3：契約変更事前確認、4：契約変更審査 |
| 送信状態 | SEND_STATUS | 固定長(1) | ○ | | 0：未送信、1：送信済、2：送信エラー |
| 再送回数 | RETRY_COUNT | 数値(2) | ○ | | 送信に失敗した回数。成功時は更新しない。上限は [13. バッチ・通知設計](13-batch-notification.md) |
| 送信日時 | SENT_AT | 日時 | | | 送信に成功した日時 |
| 送信エラー内容 | ERROR_MESSAGE | 文字列(1000) | | | 直近の送信エラー |
| 外部受付番号 | EXTERNAL_RECEIPT_NO | 文字列(30) | | UK | 外部システムが採番。結果受信時の照合キー |
| 結果 | RESULT_CD | 固定長(1) | | | 1：確認OK、2：確認NG、3：審査完了 |
| 結果受信日時 | RESULT_RECEIVED_AT | 日時 | | | |
| NG理由 | NG_REASON | 文字列(1000) | | | 確認 NG の場合に保持 |

インデックス：IX_T_EXTERNAL_LINK_01（SEND_STATUS）、IX_T_EXTERNAL_LINK_02（APPLICATION_ID, VERSION_NO）
外部受付番号の UK は NULL を除外したフィルタ付きユニークインデックスとする。

### 4.16 通知（T_NOTIFICATION）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 通知ID | NOTIFICATION_ID | 数値(12) | ○ | PK | |
| 通知種別 | NOTIFICATION_TYPE | 固定長(2) | ○ | | 01：顧客確認依頼、02：承認依頼、03：差戻し通知、04：確認NG通知、05：審査完了通知、06：送信エラー通知 |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | FK | T_APPLICATION |
| 宛先アドレス | TO_ADDRESS | 文字列(254) | ○ | | 登録時点の宛先を保持 |
| 件名 | SUBJECT | 文字列(200) | ○ | | |
| 本文 | BODY | 文字列(4000) | ○ | | 確認用 URL などを埋め込んだ本文。顧客確認依頼では平文トークンを含むため画面に表示せず、ログにも出さない |
| 送信状態 | SEND_STATUS | 固定長(1) | ○ | | 0：未送信、1：送信済、2：送信エラー |
| 再送回数 | RETRY_COUNT | 数値(2) | ○ | | 送信に失敗した回数。成功時は更新しない |
| 送信日時 | SENT_AT | 日時 | | | |
| 送信エラー内容 | ERROR_MESSAGE | 文字列(1000) | | | |

インデックス：IX_T_NOTIFICATION_01（SEND_STATUS, CREATED_AT）

### 4.17 ステータス履歴（T_STATUS_HISTORY）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 履歴ID | STATUS_HISTORY_ID | 数値(12) | ○ | PK | |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | FK | T_APPLICATION |
| 版番号 | VERSION_NO | 数値(3) | ○ | | 変更後の現行版 |
| 遷移元ステータス | FROM_STATUS_CD | 固定長(4) | | | 取込時は空 |
| 遷移先ステータス | TO_STATUS_CD | 固定長(4) | ○ | | |
| 遷移ID | TRANSITION_ID | 数値(5) | | FK | 採用した遷移マスタの行。取込時は空 |
| 操作コード | ACTION_CD | 固定長(2) | ○ | | 00：取込 を含む。M_STATUS_TRANSITION と同じ区分値 |
| 操作主体 | ACTOR_TYPE | 固定長(1) | ○ | | 1：担当者、2：承認者、3：顧客、4：外部システム |
| 操作者ID | ACTOR_ID | 文字列(20) | ○ | | 社員 ID、顧客 ID、外部システム ID のいずれか |
| コメント | COMMENT | 文字列(500) | | | 差戻し理由、NG 理由など |
| 変更日時 | CHANGED_AT | 日時 | ○ | | |

インデックス：IX_T_STATUS_HISTORY_01（APPLICATION_ID, CHANGED_AT）

## 5. 元設計からの追加・変更点

| 対象 | 変更 | 理由 |
| --- | --- | --- |
| T_IMPORT_ERROR | テーブル追加 | 取込エラーを画面表示だけでなく後から確認できるようにする |
| T_NOTIFICATION | テーブル追加 | メール送信を画面トランザクションから切り離し、バッチで再送できるようにする |
| M_EMPLOYEE.PASSWORD_HASH | 列追加 | 社内認証基盤が未確定のため、ID／パスワード認証を実装できるようにする |
| M_STATUS_TRANSITION.EVAL_ORDER | 列追加 | 同一遷移元・操作の条件を評価する順序を明示する。条件コード 31 は廃止 |
| T_APPROVAL_REQUEST.COMPLETED_AT | 列追加 | 承認完了・差戻し・引戻しの日時を持つ |
| T_CUSTOMER_CONSENT.INVALID_FLG | 列追加 | 差戻しで版が再編集になった際に旧トークンを無効化する |
| T_EXTERNAL_LINK.RETRY_COUNT、ERROR_MESSAGE | 列追加 | 再送制御とエラー内容の保持 |
| T_EXTERNAL_LINK.EXTERNAL_RECEIPT_NO | UK 追加 | 結果受信時の照合キーとして一意にする |
| T_STATUS_HISTORY.TRANSITION_ID | 列追加 | どの遷移定義で遷移したかを証跡に残す |
