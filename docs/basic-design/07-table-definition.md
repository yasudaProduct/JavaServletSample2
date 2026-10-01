# 07. テーブル定義

| 項目 | 内容 |
| --- | --- |
| 版 | 0.2（ステータス体系の改定を反映） |
| 関連 | [06. ER図](06-er-diagram.md)、[08. コード定義](08-code-definition.md)、[03. ステータス定義・状態遷移](03-status-transition.md) |

テーブルはマスタ 7 本とトランザクション 10 本の計 17 本。型は論理型で記載し、SQL Server を想定した物理型は 3 章の対応表に従う（DBMS 確定後に見直す）。

## 1. テーブル一覧

| No | 論理名 | 物理名 | 種別 | 概要 |
| --- | --- | --- | --- | --- |
| 1 | 会社区分マスタ | M_COMPANY_DIV | マスタ | 会社区分と外部事前確認の有無、金額倍率しきい値 |
| 2 | 社員マスタ | M_EMPLOYEE | マスタ | 申込受付会社の社員と所属会社区分・部署・権限 |
| 3 | 申込者マスタ | M_APPLICANT | マスタ | 申込者と確認依頼の宛先 |
| 4 | ステータスマスタ | M_STATUS | マスタ | 23 ステータスと分類・操作主体・表示名 |
| 5 | ステータス遷移マスタ | M_STATUS_TRANSITION | マスタ | 遷移元・操作・条件・遷移先・会社区分による有効範囲 |
| 6 | 承認ルートマスタ | M_APPROVAL_ROUTE | マスタ | 会社区分・部署・承認種別ごとの回付先テンプレート |
| 7 | 承認ルート明細 | M_APPROVAL_ROUTE_STEP | マスタ | テンプレート内の承認者と順序 |
| 8 | 一括取込 | T_IMPORT_BATCH | トランザクション | 取込ファイル単位の実行結果 |
| 9 | 取込エラー | T_IMPORT_ERROR | トランザクション | 取込でスキップした行と理由 |
| 10 | 申込 | T_APPLICATION | トランザクション | 申込 1 件の現在ステータス、現行版・基準版・審査完了版 |
| 11 | 申込内容（版） | T_APPLICATION_VERSION | トランザクション | 版ごとの申込内容 |
| 12 | 承認申請 | T_APPROVAL_REQUEST | トランザクション | 申請 1 回分の回付状況 |
| 13 | 承認明細 | T_APPROVAL_STEP | トランザクション | 承認者ごとの承認・差戻し・審査申請の結果 |
| 14 | 申込者同意 | T_APPLICANT_CONSENT | トランザクション | 申込者の確定・同意・差戻しの記録と確認用トークン |
| 15 | 外部連携 | T_EXTERNAL_LINK | トランザクション | 審査担当部門システムへの事前確認依頼・審査依頼と結果 |
| 16 | 通知 | T_NOTIFICATION | トランザクション | メール通知のキューと送信結果 |
| 17 | ステータス履歴 | T_STATUS_HISTORY | トランザクション | ステータス変更の全履歴 |

## 2. 共通項目

全テーブルに以下の 5 項目を持つ。以降の各表では省略する。

| 論理名 | 物理名 | 型 | 必須 | 説明 |
| --- | --- | --- | --- | --- |
| 作成日時 | CREATED_AT | 日時 | ○ | 行の作成日時 |
| 作成者 | CREATED_BY | 文字列(20) | ○ | 社員 ID、申込者 ID、外部システム ID、バッチ名のいずれか |
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
| 会社区分 | COMPANY_DIV | 固定長(1) | ○ | PK | 1：事前確認なし、2：事前確認あり |
| 会社区分名 | COMPANY_DIV_NAME | 文字列(50) | ○ | | |
| 外部事前確認フラグ | PRE_CHECK_FLG | 固定長(1) | ○ | | 1：あり、0：なし |
| 金額倍率しきい値 | AMOUNT_RATIO_LIMIT | 数値(3,2) | ○ | | 既定値 1.50。これを超えると変更基準超 |

### 4.2 社員マスタ（M_EMPLOYEE）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 社員ID | EMPLOYEE_ID | 数値(10) | ○ | PK | |
| 社員番号 | EMPLOYEE_NO | 文字列(10) | ○ | UK | ログイン ID |
| 氏名 | EMPLOYEE_NAME | 文字列(50) | ○ | | |
| パスワードハッシュ | PASSWORD_HASH | 文字列(100) | ○ | | ID／パスワード認証用。ソルト付きハッシュ。社内認証基盤と連携する場合は未使用 |
| 会社区分 | COMPANY_DIV | 固定長(1) | ○ | FK | M_COMPANY_DIV |
| 部署コード | DEPT_CD | 文字列(10) | ○ | | 承認ルートテンプレートの検索に使う |
| 権限 | ROLE_CD | 固定長(2) | ○ | | 01：担当者、02：承認者、09：管理者 |
| メールアドレス | MAIL_ADDRESS | 文字列(254) | ○ | | 承認依頼通知などの宛先 |
| 有効フラグ | VALID_FLG | 固定長(1) | ○ | | 1：有効、0：無効。無効な社員はログインできず、回付先にも設定できない |

インデックス：IX_M_EMPLOYEE_01（COMPANY_DIV, DEPT_CD）

### 4.3 申込者マスタ（M_APPLICANT）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 申込者ID | APPLICANT_ID | 数値(10) | ○ | PK | |
| 申込者番号 | APPLICANT_NO | 文字列(12) | ○ | UK | 取込ファイル・画面で申込者を指定するキー |
| 申込者名 | APPLICANT_NAME | 文字列(100) | ○ | | |
| 申込者名カナ | APPLICANT_KANA | 文字列(100) | | | |
| メールアドレス | MAIL_ADDRESS | 文字列(254) | ○ | | 確認依頼メールの宛先 |
| 電話番号 | TEL_NO | 文字列(15) | | | |
| 住所 | ADDRESS | 文字列(200) | | | |

### 4.4 ステータスマスタ（M_STATUS）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| ステータスコード | STATUS_CD | 固定長(5) | ○ | PK | 10100〜20701。大分類(1)＋中分類(2)＋小分類(2) |
| ステータス名 | STATUS_NAME | 文字列(40) | ○ | | 申込受付会社向け表示名 |
| 申込者向けステータス名 | APPLICANT_STATUS_NAME | 文字列(30) | | | 申込者向け画面の表示名。申込者が関与しない段階は空 |
| 大分類 | CATEGORY_L | 固定長(1) | ○ | | 1：新規申込、2：契約変更 |
| 中分類 | CATEGORY_M | 固定長(2) | ○ | | 01：入力中〜07：審査完了 |
| 小分類 | CATEGORY_S | 固定長(2) | ○ | | 00〜02 |
| 操作主体 | ACTOR_TYPE | 固定長(1) | ○ | | 1：担当者、2：承認者、3：申込者、4：審査担当部門、9：なし |
| 編集可フラグ | EDITABLE_FLG | 固定長(1) | ○ | | 0：編集不可、1：担当者が編集可、2：申込者が編集可 |
| 表示順 | DISPLAY_ORDER | 数値(3) | ○ | | 一覧の検索条件・並び順に使う |
| 状態説明 | DESCRIPTION | 文字列(200) | | | 状態の説明文 |

### 4.5 ステータス遷移マスタ（M_STATUS_TRANSITION）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 遷移ID | TRANSITION_ID | 数値(5) | ○ | PK | |
| 遷移元ステータス | FROM_STATUS_CD | 固定長(5) | ○ | FK | M_STATUS |
| 操作コード | ACTION_CD | 固定長(2) | ○ | | 01〜14（[08. 6 章](08-code-definition.md#6-操作コードaction_cd)） |
| 条件コード | CONDITION_CD | 固定長(2) | ○ | | 00：なし、11：回付先あり、12：回付先なし、21：変更基準超、41：初回の契約変更、42：2回目以降の契約変更 |
| 評価順 | EVAL_ORDER | 数値(2) | ○ | | 同一の遷移元・操作の中で条件を評価する順。若い順に評価し、最初に成立した行を採用する |
| 遷移先ステータス | TO_STATUS_CD | 固定長(5) | ○ | FK | M_STATUS |
| 操作主体 | ACTOR_TYPE | 固定長(1) | ○ | | M_STATUS と同じ区分値 |
| 事前確認区分 | PRE_CHECK_COND | 固定長(1) | ○ | | 9：共通、0：事前確認なしの会社区分のみ、1：事前確認ありの会社区分のみ |
| 遷移表No | REF_NO | 文字列(20) | | | 業務側の遷移表の No（区分 1／区分 2） |

ユニーク制約：UK_M_STATUS_TRANSITION_01（FROM_STATUS_CD, ACTION_CD, PRE_CHECK_COND, EVAL_ORDER）
初期データは [03. ステータス定義・状態遷移 6 章](03-status-transition.md#6-ステータス遷移マスタ-初期データ) を参照。

### 4.6 承認ルートマスタ（M_APPROVAL_ROUTE）

承認フロー画面で回付先の初期値として使うテンプレート。申請者は画面で回付先を変更できる（仮）。

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| ルートID | ROUTE_ID | 数値(10) | ○ | PK | |
| 会社区分 | COMPANY_DIV | 固定長(1) | ○ | FK | M_COMPANY_DIV |
| 部署コード | DEPT_CD | 文字列(10) | ○ | | |
| 承認種別 | APPROVAL_TYPE | 固定長(2) | ○ | | 01：一次承認、02：最終承認、03：契約変更一次承認、04：契約変更最終承認 |
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
| 成功件数 | SUCCESS_COUNT | 数値(6) | ○ | | 10100 で登録した件数 |
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
| 申込者ID | APPLICANT_ID | 数値(10) | ○ | FK | M_APPLICANT |
| 担当社員ID | OWNER_EMPLOYEE_ID | 数値(10) | ○ | FK | M_EMPLOYEE。取込時は取込社員、画面入力時は入力社員 |
| 会社区分 | COMPANY_DIV | 固定長(1) | ○ | FK | 起票時点の担当社員の会社区分を保持 |
| 部署コード | DEPT_CD | 文字列(10) | ○ | | 起票時点の担当社員の部署 |
| ステータスコード | STATUS_CD | 固定長(5) | ○ | FK | M_STATUS。現在のステータス |
| 現行版番号 | CURRENT_VERSION_NO | 数値(3) | ○ | | 編集中または承認・確認・審査中の版 |
| 基準版番号 | BASE_VERSION_NO | 数値(3) | | | 変更基準の比較元。申込者同意時と契約変更開始時に更新 |
| 審査完了版番号 | REVIEWED_VERSION_NO | 数値(3) | | | 直近で審査完了した版 |
| 登録区分 | REGISTRATION_TYPE | 固定長(1) | ○ | | 1：一括取込、2：画面入力、3：追加申込 |
| 追加申込元申込ID | SOURCE_APPLICATION_ID | 数値(10) | | FK | 追加申込の元になった申込（T_APPLICATION） |
| 取込ID | IMPORT_BATCH_ID | 数値(10) | | FK | T_IMPORT_BATCH。一括取込の場合のみ |
| 審査完了日時 | REVIEWED_AT | 日時 | | | 直近の審査完了日時 |

インデックス：IX_T_APPLICATION_01（STATUS_CD）、IX_T_APPLICATION_02（OWNER_EMPLOYEE_ID, STATUS_CD）、IX_T_APPLICATION_03（APPLICANT_ID）

### 4.11 申込内容（版）（T_APPLICATION_VERSION）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | PK、FK | T_APPLICATION |
| 版番号 | VERSION_NO | 数値(3) | ○ | PK | 1 から連番 |
| 版種別 | VERSION_TYPE | 固定長(1) | ○ | | 1：新規申込、2：新規申込の修正、3：契約変更、4：契約変更の修正 |
| 複写元版番号 | COPIED_FROM_VERSION_NO | 数値(3) | | | 複写して作った版の複写元。第 1 版は空 |
| 商品コード | PRODUCT_CD | 文字列(10) | ○ | | |
| 申込金額 | APPLICATION_AMOUNT | 数値(13) | ○ | | 円 |
| 契約開始日 | CONTRACT_START_DATE | 日付 | | | |
| 契約終了日 | CONTRACT_END_DATE | 日付 | | | |
| 変更金額倍率 | AMOUNT_RATIO | 数値(7,4) | | | 申込金額 ÷ 基準版の申込金額。変更基準を判定した版に設定 |
| その他修正フラグ | OTHER_MODIFIED_FLG | 固定長(1) | ○ | | 基準版と比べて金額以外の項目を変更した版は 1 |
| 備考 | REMARKS | 文字列(1000) | | | |
| 確定日時 | CONFIRMED_AT | 日時 | | | 担当者または申込者が確定した日時。再確定で上書き |
| 確定版フラグ | FIXED_FLG | 固定長(1) | ○ | | 1：申込者が同意した版または審査完了した版。以降は変更しない |
| 取消フラグ | CANCELED_FLG | 固定長(1) | ○ | | 1：契約変更の審査差戻しで取り消した版 |

### 4.12 承認申請（T_APPROVAL_REQUEST）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 承認申請ID | APPROVAL_REQUEST_ID | 数値(10) | ○ | PK | |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | FK | T_APPLICATION_VERSION |
| 版番号 | VERSION_NO | 数値(3) | ○ | FK | T_APPLICATION_VERSION。申請対象の版 |
| 承認種別 | APPROVAL_TYPE | 固定長(2) | ○ | | M_APPROVAL_ROUTE と同じ区分値 |
| ルートID | ROUTE_ID | 数値(10) | | FK | 回付先の初期値に使ったテンプレート。使わなかった場合は空 |
| 申請社員ID | REQUEST_EMPLOYEE_ID | 数値(10) | ○ | FK | M_EMPLOYEE |
| 申請日時 | REQUESTED_AT | 日時 | ○ | | |
| 申請状態 | REQUEST_STATUS | 固定長(1) | ○ | | 1：申請中、2：承認済、3：差戻し |
| 現在ステップ | CURRENT_STEP_NO | 数値(2) | | | 次に承認するステップ。回付先なしの場合は空 |
| 最終ステップ | FINAL_STEP_NO | 数値(2) | | | 申請時点の回付先の最大ステップ。回付先なしの場合は空 |
| 完了日時 | COMPLETED_AT | 日時 | | | 承認済・差戻しになった日時 |

インデックス：IX_T_APPROVAL_REQUEST_01（APPLICATION_ID, REQUEST_STATUS）

### 4.13 承認明細（T_APPROVAL_STEP）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 承認申請ID | APPROVAL_REQUEST_ID | 数値(10) | ○ | PK、FK | T_APPROVAL_REQUEST |
| ステップ番号 | STEP_NO | 数値(2) | ○ | PK | 承認フロー画面で設定した順序 |
| 承認者社員ID | APPROVER_EMPLOYEE_ID | 数値(10) | ○ | FK | M_EMPLOYEE |
| 結果 | RESULT_CD | 固定長(1) | ○ | | 0：未処理、1：承認、2：差戻し、3：審査申請 |
| コメント | COMMENT | 文字列(500) | | | 差戻し時は必須 |
| 処理日時 | ACTED_AT | 日時 | | | |

インデックス：IX_T_APPROVAL_STEP_01（APPROVER_EMPLOYEE_ID, RESULT_CD）

### 4.14 申込者同意（T_APPLICANT_CONSENT）

確認依頼（確認用 URL の発行）1 回につき 1 行。

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 同意ID | CONSENT_ID | 数値(10) | ○ | PK | |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | FK | T_APPLICATION_VERSION |
| 版番号 | VERSION_NO | 数値(3) | ○ | FK | 確認対象の版 |
| 同意種別 | CONSENT_TYPE | 固定長(1) | ○ | | 1：新規申込、2：契約変更 |
| 確認用トークン | ACCESS_TOKEN_HASH | 文字列(64) | ○ | UK | 確認 URL のトークンを SHA-256 でハッシュ化した 16 進文字列 |
| トークン有効期限 | TOKEN_EXPIRES_AT | 日時 | ○ | | 発行から 14 日後（仮） |
| 同意状態 | CONSENT_STATUS | 固定長(1) | ○ | | 1：確認依頼中、2：同意済、3：申込者差戻し、9：無効 |
| 内容確定日時 | CONTENT_CONFIRMED_AT | 日時 | | | 申込者が内容確認で「確定」した日時（10301→10302、20301→20302）。修正で戻って再確定した場合は上書き |
| 同意日時 | CONSENTED_AT | 日時 | | | 申込者が「同意する」を押した日時 |
| 差戻し日時 | RETURNED_AT | 日時 | | | 申込者が「差戻し」を押した日時 |
| 差戻し理由 | RETURN_REASON | 文字列(500) | | | 申込者差戻しの理由 |
| 接続元IP | CLIENT_IP | 文字列(45) | | | 確定・同意・差戻し時の証跡。IPv6 対応 |

インデックス：IX_T_APPLICANT_CONSENT_01（APPLICATION_ID, CONSENT_STATUS）

### 4.15 外部連携（T_EXTERNAL_LINK）

審査担当部門システムへの依頼 1 回につき 1 行。

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 外部連携ID | EXTERNAL_LINK_ID | 数値(10) | ○ | PK | |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | FK | T_APPLICATION_VERSION |
| 版番号 | VERSION_NO | 数値(3) | ○ | FK | 依頼対象の版 |
| 連携種別 | LINK_TYPE | 固定長(1) | ○ | | 1：事前確認依頼、2：審査依頼、3：契約変更事前確認依頼、4：契約変更審査依頼 |
| 送信状態 | SEND_STATUS | 固定長(1) | ○ | | 0：未送信、1：送信済、2：送信エラー |
| 再送回数 | RETRY_COUNT | 数値(2) | ○ | | 送信に失敗した回数。成功時は更新しない |
| 送信日時 | SENT_AT | 日時 | | | 送信に成功した日時 |
| 送信エラー内容 | ERROR_MESSAGE | 文字列(1000) | | | 直近の送信エラー |
| 外部受付番号 | EXTERNAL_RECEIPT_NO | 文字列(30) | | UK | 審査担当部門システムが採番。結果受信時の照合キー |
| 結果 | RESULT_CD | 固定長(1) | | | 1：問題なし、2：修正必要、3：審査完了、4：審査差戻し |
| 結果受信日時 | RESULT_RECEIVED_AT | 日時 | | | |
| 結果理由 | RESULT_REASON | 文字列(1000) | | | 修正必要の指摘内容、審査差戻しの理由 |

インデックス：IX_T_EXTERNAL_LINK_01（SEND_STATUS）、IX_T_EXTERNAL_LINK_02（APPLICATION_ID, VERSION_NO）
外部受付番号の UK は NULL を除外したフィルタ付きユニークインデックスとする。

### 4.16 通知（T_NOTIFICATION）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 通知ID | NOTIFICATION_ID | 数値(12) | ○ | PK | |
| 通知種別 | NOTIFICATION_TYPE | 固定長(2) | ○ | | 01：申込者確認依頼、02：承認依頼、03：差戻し通知、04：申込者差戻し通知、05：事前確認結果通知、06：審査結果通知、07：送信エラー通知 |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | FK | T_APPLICATION |
| 宛先アドレス | TO_ADDRESS | 文字列(254) | ○ | | 登録時点の宛先を保持 |
| 件名 | SUBJECT | 文字列(200) | ○ | | |
| 本文 | BODY | 文字列(4000) | ○ | | 確認用 URL などを埋め込んだ本文。申込者確認依頼では平文トークンを含むため画面に表示せず、ログにも出さない |
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
| 遷移元ステータス | FROM_STATUS_CD | 固定長(5) | | | 新規作成時は空 |
| 遷移先ステータス | TO_STATUS_CD | 固定長(5) | ○ | | |
| 遷移ID | TRANSITION_ID | 数値(5) | | FK | 採用した遷移マスタの行。新規作成時は空 |
| 操作コード | ACTION_CD | 固定長(2) | ○ | | 00：新規作成 を含む。M_STATUS_TRANSITION と同じ区分値 |
| 操作主体 | ACTOR_TYPE | 固定長(1) | ○ | | 1：担当者、2：承認者、3：申込者、4：審査担当部門 |
| 操作者ID | ACTOR_ID | 文字列(20) | ○ | | 社員 ID、申込者 ID、外部システム ID のいずれか |
| コメント | COMMENT | 文字列(500) | | | 差戻し理由、指摘内容など |
| 変更日時 | CHANGED_AT | 日時 | ○ | | |

インデックス：IX_T_STATUS_HISTORY_01（APPLICATION_ID, CHANGED_AT）

## 5. 版 0.1 からの変更点

| 対象 | 変更 | 理由 |
| --- | --- | --- |
| M_CUSTOMER → M_APPLICANT | 名称変更（顧客 → 申込者） | 業務用語に合わせる |
| T_CUSTOMER_CONSENT → T_APPLICANT_CONSENT | 名称変更。INVALID_FLG を CONSENT_STATUS に統合し、内容確定日時・差戻し日時・差戻し理由を追加 | 申込者の確定・同意・差戻しを記録する |
| M_STATUS | STATUS_CD を 5 桁に変更。申込者向けステータス名、大分類・中分類・小分類、状態説明を追加。フロー区分を廃止 | 新しいステータス体系 |
| M_STATUS_TRANSITION | PRE_CHECK_ONLY_FLG を PRE_CHECK_COND（9／0／1）に変更。遷移表 No を追加 | 会社区分ごとに遷移先が異なる行を表す |
| M_COMPANY_DIV | 区分値の意味を 1 = 事前確認なし、2 = 事前確認あり に変更 | 業務側の遷移表に合わせる |
| T_APPLICATION | BASE_AMOUNT を BASE_VERSION_NO（基準版番号）に変更。REGISTRATION_TYPE、SOURCE_APPLICATION_ID を追加 | 変更基準の比較元を版で持つ。画面からの新規申込・追加申込に対応 |
| T_APPLICATION_VERSION | COPIED_FROM_VERSION_NO、FIXED_FLG、CANCELED_FLG を追加。版種別の意味を変更 | 確定版の保護、契約変更の審査差戻しによる取消 |
| T_APPROVAL_REQUEST | REQUEST_STATUS から 4（引戻し）を削除。ROUTE_ID をテンプレートの参照に変更 | 申請中の引戻しは遷移表にない。回付先は申請時に設定する |
| T_APPROVAL_STEP | RESULT_CD に 3（審査申請）を追加 | 最終承認者の操作が審査申請になる |
| T_EXTERNAL_LINK | NG_REASON を RESULT_REASON に変更。RESULT_CD に 4（審査差戻し）を追加 | 審査差戻しに対応 |
| T_NOTIFICATION | 通知種別を 7 種に変更 | 申込者差戻し・審査結果の通知を追加 |
