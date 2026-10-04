# 07. テーブル定義

| 項目 | 内容 |
| --- | --- |
| 版 | 0.10（同意事項マスタ・同意事項の版・申込者同意の同意事項・申込内容 PDF の 4 テーブルを追加（V8）。版 0.9：申込内容（版）に会社B（会社区分 2）だけの追加項目を追加。版 0.8：部署マスタ M_DEPARTMENT を追加し、申込内容（版）に担当（会社区分・部署・担当社員）を記録。版 0.7：申込者アカウントの紐づけは一次承認での発行と追加申込の引き継ぎだけにする。版 0.6：申込者情報を申込内容（版）へ移し、申込者マスタをログイン用の申込者アカウント（M_APPLICANT_ACCOUNT）に変更） |
| 関連 | [06. ER図](06-er-diagram.md)、[08. コード定義](08-code-definition.md)、[03. ステータス定義・状態遷移](03-status-transition.md) |

テーブルはマスタ 10 本とトランザクション 12 本の計 22 本。型は論理型で記載し、SQL Server を想定した物理型は 3 章の対応表に従う（DBMS 確定後に見直す）。

## 1. テーブル一覧

| No | 論理名 | 物理名 | 種別 | 概要 |
| --- | --- | --- | --- | --- |
| 1 | 会社区分マスタ | M_COMPANY_DIV | マスタ | 会社区分と外部事前確認の有無、金額倍率しきい値 |
| 2 | 部署マスタ | M_DEPARTMENT | マスタ | 会社区分ごとの部署コードと部署名（申込受付会社の会社 > 部署 > 担当者の部署） |
| 3 | 社員マスタ | M_EMPLOYEE | マスタ | 申込受付会社の社員と所属会社区分・部署・権限 |
| 4 | 申込者アカウント | M_APPLICANT_ACCOUNT | マスタ | 申込者ページのログインに必要なデータ（ユーザー ID・パスワード）。申込者名・連絡先は申込内容（版）が持つ |
| 5 | ステータスマスタ | M_STATUS | マスタ | 23 ステータスと分類・操作主体・表示名 |
| 6 | ステータス遷移マスタ | M_STATUS_TRANSITION | マスタ | 遷移元・操作・条件・遷移先・会社区分による有効範囲 |
| 7 | 承認ルートマスタ | M_APPROVAL_ROUTE | マスタ | 会社区分・部署・承認種別ごとの回付先テンプレート |
| 8 | 承認ルート明細 | M_APPROVAL_ROUTE_STEP | マスタ | テンプレート内の承認者と順序 |
| 9 | 一括取込 | T_IMPORT_BATCH | トランザクション | 取込ファイル単位の実行結果 |
| 10 | 取込エラー | T_IMPORT_ERROR | トランザクション | 取込でスキップした行と理由 |
| 11 | 申込 | T_APPLICATION | トランザクション | 申込 1 件の現在ステータス、現行版・基準版・審査完了版 |
| 12 | 申込内容（版） | T_APPLICATION_VERSION | トランザクション | 版ごとの申込内容 |
| 13 | 承認申請 | T_APPROVAL_REQUEST | トランザクション | 申請 1 回分の回付状況 |
| 14 | 承認明細 | T_APPROVAL_STEP | トランザクション | 承認者ごとの承認・差戻し・審査申請の結果 |
| 15 | 申込者同意 | T_APPLICANT_CONSENT | トランザクション | 申込者の確定・同意・差戻しの記録と確認用トークン |
| 16 | 外部連携 | T_EXTERNAL_LINK | トランザクション | 審査担当部門システムへの事前確認依頼・審査依頼と結果 |
| 17 | 通知 | T_NOTIFICATION | トランザクション | メール通知のキューと送信結果 |
| 18 | ステータス履歴 | T_STATUS_HISTORY | トランザクション | ステータス変更の全履歴 |
| 19 | 同意事項マスタ | M_CONSENT_DOCUMENT | マスタ | 申込同意確認画面（AP03）に表示する同意事項の文書と対象（新規申込／契約変更／共通） |
| 20 | 同意事項の版 | M_CONSENT_DOCUMENT_VERSION | マスタ | 同意事項の版ごとの PDF 本体・ハッシュ・適用開始日時（追加のみ） |
| 21 | 申込者同意の同意事項 | T_APPLICANT_CONSENT_DOCUMENT | トランザクション | 同意ごとに開いた同意事項の版と、同意した版 |
| 22 | 申込内容 PDF | T_APPLICATION_PDF | トランザクション | 同意時・審査完了時に作成した申込内容の PDF（作り直さない） |

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
| バイナリ | VARBINARY(MAX) | PDF などのファイル本体 |

- ID 列（〜_ID）は主キーとして IDENTITY で採番する。申込番号などの業務キーは別途採番する（[14. 共通仕様](14-common-spec.md)）。
- 照合順序は Japanese_CI_AS を想定。

## 4. テーブル定義

### 4.1 会社区分マスタ（M_COMPANY_DIV）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 会社区分 | COMPANY_DIV | 固定長(1) | ○ | PK | 1：会社A（事前確認なし）、2：会社B（事前確認あり） |
| 会社区分名 | COMPANY_DIV_NAME | 文字列(50) | ○ | | |
| 外部事前確認フラグ | PRE_CHECK_FLG | 固定長(1) | ○ | | 1：あり、0：なし |
| 金額倍率しきい値 | AMOUNT_RATIO_LIMIT | 数値(3,2) | ○ | | 既定値 1.50。申込金額合計の基準版に対する倍率がこの値以上なら変更基準超。減額は常に基準内 |

### 4.2 部署マスタ（M_DEPARTMENT）

申込受付会社の「会社 > 部署 > 担当者」の部署。社員の所属部署、承認ルートの部署、申込の担当部署はこのマスタから選ぶ。保守は管理者が SC16 で行う（[11. 4.16 節](11-screen-design.md#416-sc16-部署マスタ)）。部署は削除せず、使わなくなったら無効にする。

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 会社区分 | COMPANY_DIV | 固定長(1) | ○ | PK、FK | M_COMPANY_DIV |
| 部署コード | DEPT_CD | 文字列(10) | ○ | PK | 会社区分の中で一意。半角英数字。登録後は変更しない |
| 部署名 | DEPT_NAME | 文字列(50) | ○ | | 画面の表示名 |
| 有効フラグ | VALID_FLG | 固定長(1) | ○ | | 1：有効、0：無効。無効な部署は新しく選べない（設定済みの社員・承認ルート・申込はそのまま） |

### 4.3 社員マスタ（M_EMPLOYEE）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 社員ID | EMPLOYEE_ID | 数値(10) | ○ | PK | |
| 社員番号 | EMPLOYEE_NO | 文字列(10) | ○ | UK | ログイン ID |
| 氏名 | EMPLOYEE_NAME | 文字列(50) | ○ | | |
| パスワードハッシュ | PASSWORD_HASH | 文字列(100) | ○ | | ID／パスワード認証用。ソルト付きハッシュ。社内認証基盤と連携する場合は未使用 |
| 会社区分 | COMPANY_DIV | 固定長(1) | ○ | FK | M_COMPANY_DIV |
| 部署コード | DEPT_CD | 文字列(10) | ○ | FK | 所属部署。M_DEPARTMENT（COMPANY_DIV, DEPT_CD）。申込の担当部署の既定値 |
| 権限 | ROLE_CD | 固定長(2) | ○ | | 01：担当者、02：承認者、09：管理者 |
| メールアドレス | MAIL_ADDRESS | 文字列(254) | ○ | | 承認依頼通知などの宛先 |
| 有効フラグ | VALID_FLG | 固定長(1) | ○ | | 1：有効、0：無効。無効な社員はログインできず、回付先にも設定できない |

インデックス：IX_M_EMPLOYEE_01（COMPANY_DIV, DEPT_CD）

### 4.4 申込者アカウント（M_APPLICANT_ACCOUNT）

申込者ページにログインするためのデータだけを持つ。申込の一次承認が通って申込内容確認待ちになったとき、申込がまだアカウントに紐づいていなければ発行し、申込（T_APPLICATION.APPLICANT_ID）に紐づける（F14 後続処理、[10. 17.2 節](10-function-detail.md#172-アカウント発行f14-後続処理)）。初期データには持たない。申込者名・メールアドレスなどの申込者情報は申込データとして申込内容（版）に持つ（4.12 節、[10. 18 章](10-function-detail.md#18-申込者情報と申込者アカウント)）。

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 申込者ID | APPLICANT_ID | 数値(10) | ○ | PK | |
| 申込者番号 | APPLICANT_NO | 文字列(12) | ○ | UK | 発行時にシステムが採番（C ＋ 10 桁、SEQ_APPLICANT_NO）。申込者ページのユーザー ID。変更不可 |
| パスワードハッシュ | PASSWORD_HASH | 文字列(100) | ○ | | 発行時は初期パスワードのハッシュ |
| アカウント発行日時 | ACCOUNT_ISSUED_AT | 日時 | ○ | | |
| パスワード変更日時 | PASSWORD_CHANGED_AT | 日時 | | | 申込者がパスワードを変更した日時。空は初期パスワードのまま（発行時、SC15 の初期化後） |

PASSWORD_HASH・ACCOUNT_ISSUED_AT は発行時に必ず設定する（V3 で追加した列のため、DB 上は NULL 可のまま）。

### 4.5 ステータスマスタ（M_STATUS）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| ステータスコード | STATUS_CD | 固定長(5) | ○ | PK | 10100〜20701 と 90101（申込取消）。大分類(1)＋中分類(2)＋小分類(2) |
| ステータス名 | STATUS_NAME | 文字列(40) | ○ | | 申込受付会社向け表示名 |
| 申込者向けステータス名 | APPLICANT_STATUS_NAME | 文字列(30) | | | 申込者向け画面の表示名。申込者が関与しない段階は空 |
| 大分類 | CATEGORY_L | 固定長(1) | ○ | | 1：新規申込、2：契約変更、9：取消 |
| 中分類 | CATEGORY_M | 固定長(2) | ○ | | 01：入力中〜07：審査完了 |
| 小分類 | CATEGORY_S | 固定長(2) | ○ | | 00〜02 |
| 操作主体 | ACTOR_TYPE | 固定長(1) | ○ | | 1：担当者、2：承認者、3：申込者、4：審査担当部門、9：なし |
| 編集可フラグ | EDITABLE_FLG | 固定長(1) | ○ | | 0：編集不可、1：担当者が編集可、2：申込者が編集可 |
| 表示順 | DISPLAY_ORDER | 数値(3) | ○ | | 一覧の検索条件・並び順に使う |
| 状態説明 | DESCRIPTION | 文字列(200) | | | 状態の説明文 |

### 4.6 ステータス遷移マスタ（M_STATUS_TRANSITION）

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

### 4.7 承認ルートマスタ（M_APPROVAL_ROUTE）

承認フロー画面で回付先の初期値として使うテンプレート。申請者は画面で回付先を変更できる（仮）。

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| ルートID | ROUTE_ID | 数値(10) | ○ | PK | |
| 会社区分 | COMPANY_DIV | 固定長(1) | ○ | FK | M_COMPANY_DIV |
| 部署コード | DEPT_CD | 文字列(10) | ○ | FK | M_DEPARTMENT（COMPANY_DIV, DEPT_CD）。申込の担当部署と一致するテンプレートを使う |
| 承認種別 | APPROVAL_TYPE | 固定長(2) | ○ | | 01：一次承認、02：最終承認、03：契約変更一次承認、04：契約変更最終承認 |
| ルート名 | ROUTE_NAME | 文字列(50) | ○ | | |
| 適用開始日 | VALID_FROM | 日付 | ○ | | |
| 適用終了日 | VALID_TO | 日付 | | | 空は無期限 |

インデックス：IX_M_APPROVAL_ROUTE_01（COMPANY_DIV, DEPT_CD, APPROVAL_TYPE, VALID_FROM）

### 4.8 承認ルート明細（M_APPROVAL_ROUTE_STEP）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| ルートID | ROUTE_ID | 数値(10) | ○ | PK、FK | M_APPROVAL_ROUTE |
| ステップ番号 | STEP_NO | 数値(2) | ○ | PK | 1 から連番。最大値が最終承認者 |
| 承認者社員ID | APPROVER_EMPLOYEE_ID | 数値(10) | ○ | FK | M_EMPLOYEE |

### 4.9 一括取込（T_IMPORT_BATCH）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 取込ID | IMPORT_BATCH_ID | 数値(10) | ○ | PK | |
| ファイル名 | FILE_NAME | 文字列(255) | ○ | | アップロード時のファイル名 |
| 取込社員ID | IMPORT_EMPLOYEE_ID | 数値(10) | ○ | FK | M_EMPLOYEE |
| 取込日時 | IMPORTED_AT | 日時 | ○ | | |
| 総件数 | TOTAL_COUNT | 数値(6) | ○ | | ヘッダ行を除いた行数 |
| 成功件数 | SUCCESS_COUNT | 数値(6) | ○ | | 10100 で登録した件数 |
| エラー件数 | ERROR_COUNT | 数値(6) | ○ | | スキップした件数 |

### 4.10 取込エラー（T_IMPORT_ERROR）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 取込ID | IMPORT_BATCH_ID | 数値(10) | ○ | PK、FK | T_IMPORT_BATCH |
| 行番号 | LINE_NO | 数値(6) | ○ | PK | ファイル内の行番号（ヘッダ行を 1 とする） |
| エラー内容 | ERROR_MESSAGE | 文字列(500) | ○ | | 複数エラーは区切り文字で連結 |
| 行データ | RAW_LINE | 文字列(2000) | | | 元の行をそのまま保持 |

### 4.11 申込（T_APPLICATION）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | PK | |
| 申込番号 | APPLICATION_NO | 文字列(12) | ○ | UK | 画面・帳票に表示する番号。採番規則は [14. 共通仕様](14-common-spec.md) |
| 申込者ID | APPLICANT_ID | 数値(10) | | FK | M_APPLICANT_ACCOUNT。新規申込は申込者アカウントの発行（一次承認が通ったとき）で設定し、追加申込は元の申込の値を引き継ぐ。空はアカウント未発行 |
| 担当社員ID | OWNER_EMPLOYEE_ID | 数値(10) | ○ | FK | M_EMPLOYEE。申込の担当者（操作できる担当者権限の社員）。取込時は取込社員、画面入力時は SC04 で選んだ社員（既定は入力した社員） |
| 会社区分 | COMPANY_DIV | 固定長(1) | ○ | FK | 申込の担当会社。事前確認の有無・変更基準・承認者の候補を決める。SC04 で選ぶ（既定は入力した社員の会社区分）。取込時は取込社員の会社区分 |
| 部署コード | DEPT_CD | 文字列(10) | ○ | FK | 申込の担当部署。M_DEPARTMENT（COMPANY_DIV, DEPT_CD）。承認ルートテンプレートの検索と承認者の参照範囲に使う。担当者の所属部署と異なってよい |
| ステータスコード | STATUS_CD | 固定長(5) | ○ | FK | M_STATUS。現在のステータス |
| 現行版番号 | CURRENT_VERSION_NO | 数値(3) | ○ | | 編集中または承認・確認・審査中の版 |
| 基準版番号 | BASE_VERSION_NO | 数値(3) | | | 変更基準の比較元。申込者同意時と契約変更開始時に更新 |
| 審査完了版番号 | REVIEWED_VERSION_NO | 数値(3) | | | 直近で審査完了した版 |
| 登録区分 | REGISTRATION_TYPE | 固定長(1) | ○ | | 1：一括取込、2：画面入力、3：追加申込 |
| 追加申込元申込ID | SOURCE_APPLICATION_ID | 数値(10) | | FK | 追加申込の元になった申込（T_APPLICATION） |
| 取込ID | IMPORT_BATCH_ID | 数値(10) | | FK | T_IMPORT_BATCH。一括取込の場合のみ |
| 審査完了日時 | REVIEWED_AT | 日時 | | | 直近の審査完了日時 |

会社区分・部署コード・担当社員ID は申込の担当（会社 > 部署 > 担当者）で、変更できるのは申込入力中（10101）だけ。現行版（T_APPLICATION_VERSION）にも同じ値を記録する。

インデックス：IX_T_APPLICATION_01（STATUS_CD）、IX_T_APPLICATION_02（OWNER_EMPLOYEE_ID, STATUS_CD）、IX_T_APPLICATION_03（APPLICANT_ID）

### 4.12 申込内容（版）（T_APPLICATION_VERSION）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | PK、FK | T_APPLICATION |
| 版番号 | VERSION_NO | 数値(3) | ○ | PK | 1 から連番 |
| 版種別 | VERSION_TYPE | 固定長(1) | ○ | | 1：新規申込、2：新規申込の修正、3：契約変更、4：契約変更の修正 |
| 複写元版番号 | COPIED_FROM_VERSION_NO | 数値(3) | | | 複写して作った版の複写元。第 1 版は空 |
| 会社区分 | COMPANY_DIV | 固定長(1) | | | この版の担当会社（申込の COMPANY_DIV の記録。SC04 で選ぶ）。外部キーなし |
| 部署コード | DEPT_CD | 文字列(10) | | | この版の担当部署（申込の DEPT_CD の記録）。外部キーなし |
| 担当社員ID | OWNER_EMPLOYEE_ID | 数値(10) | | | この版の担当者（申込の OWNER_EMPLOYEE_ID の記録）。外部キーなし |
| 申込者名 | APPLICANT_NAME | 文字列(100) | | | 申込データ。確認へ・確定・取込で必須（一時保存では空を許す） |
| 申込者名カナ | APPLICANT_KANA | 文字列(100) | | | 全角カナ |
| 電話番号 | TEL_NO | 文字列(15) | | | 数字とハイフン |
| メールアドレス | MAIL_ADDRESS | 文字列(254) | | | 確認へ・確定・取込で必須。現行版のメールアドレスが確認依頼メール・アカウント通知・パスワード初期化通知の宛先。一意制約は持たない（同じメールアドレスでも新規申込は別の申込者） |
| 住所 | ADDRESS | 文字列(200) | | | |
| 商品コード | PRODUCT_CD | 文字列(10) | ○ | | |
| 基本料金 | BASIC_FEE | 数値(13) | ○ | | 円。金額項目の名称・個数はサンプル（業務確認済み。実装時に実際の項目へ差し替える） |
| オプション料金 | OPTION_FEE | 数値(13) | ○ | | 円。未入力は 0 |
| 事務手数料 | HANDLING_FEE | 数値(13) | ○ | | 円。未入力は 0 |
| 申込金額合計 | TOTAL_AMOUNT | 数値(13) | ○ | | 基本料金・オプション料金・事務手数料 の合計。保存時に計算して保持する。変更基準の判定対象 |
| 契約開始日 | CONTRACT_START_DATE | 日付 | | | |
| 契約終了日 | CONTRACT_END_DATE | 日付 | | | |
| 変更金額倍率 | AMOUNT_RATIO | 数値(7,4) | | | 申込金額合計 ÷ 基準版の申込金額合計。変更基準を判定した版に設定 |
| 備考 | REMARKS | 文字列(1000) | | | |
| 法人番号 | CORPORATE_NO | 文字列(13) | | | 会社B（会社区分 2）だけの追加項目。半角数字 13 桁。項目名・桁はサンプル。会社区分 2 以外の申込では空 |
| 設置場所 | INSTALL_PLACE | 文字列(200) | | | 会社B だけの追加項目（同上） |
| 窓口メモ | CONTACT_MEMO | 文字列(500) | | | 会社B だけの追加項目（同上）。改行可 |
| 確定日時 | CONFIRMED_AT | 日時 | | | 担当者または申込者が確定した日時。再確定で上書き |
| 確定版フラグ | FIXED_FLG | 固定長(1) | ○ | | 1：申込者が同意した版または審査完了した版。以降は変更しない |
| 取消フラグ | CANCELED_FLG | 固定長(1) | ○ | | 1：契約変更の審査差戻しで取り消した版 |

### 4.13 承認申請（T_APPROVAL_REQUEST）

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

### 4.14 承認明細（T_APPROVAL_STEP）

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 承認申請ID | APPROVAL_REQUEST_ID | 数値(10) | ○ | PK、FK | T_APPROVAL_REQUEST |
| ステップ番号 | STEP_NO | 数値(2) | ○ | PK | 承認フロー画面で設定した順序 |
| 承認者社員ID | APPROVER_EMPLOYEE_ID | 数値(10) | ○ | FK | M_EMPLOYEE |
| 結果 | RESULT_CD | 固定長(1) | ○ | | 0：未処理、1：承認、2：差戻し、3：審査申請 |
| コメント | COMMENT | 文字列(500) | | | 差戻し時は必須 |
| 処理日時 | ACTED_AT | 日時 | | | |

インデックス：IX_T_APPROVAL_STEP_01（APPROVER_EMPLOYEE_ID, RESULT_CD）

### 4.15 申込者同意（T_APPLICANT_CONSENT）

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

### 4.16 外部連携（T_EXTERNAL_LINK）

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

### 4.17 通知（T_NOTIFICATION）

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

### 4.18 ステータス履歴（T_STATUS_HISTORY）

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

### 4.19 同意事項マスタ（M_CONSENT_DOCUMENT）

申込同意確認画面（AP03）で申込者に開いてもらう同意事項の文書。保守は管理者が SC17 で行う（[10. 21 章](10-function-detail.md#21-f20-同意事項)）。

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 文書コード | DOCUMENT_CD | 文字列(20) | ○ | PK | 半角英大文字・数字・_。登録後は変更しない |
| 文書名 | DOCUMENT_NAME | 文字列(100) | ○ | | AP03・PDF に表示する名称 |
| 対象 | TARGET_TYPE | 固定長(1) | ○ | | 1：新規申込、2：契約変更、9：共通（[08. 20 章](08-code-definition.md#20-同意事項の対象target_type同意事項マスタ)） |
| 表示順 | DISPLAY_ORDER | 数値(3) | ○ | | |
| 有効フラグ | VALID_FLG | 固定長(1) | ○ | | 0：新しい同意では表示しない（記録済みの同意には影響しない） |

### 4.20 同意事項の版（M_CONSENT_DOCUMENT_VERSION）

同意事項の版ごとの PDF。版は追加するだけで、登録済みの版は変更・削除しない。表示する版は、適用開始日時が現在以前で最も新しい版。

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 文書コード | DOCUMENT_CD | 文字列(20) | ○ | PK、FK | M_CONSENT_DOCUMENT |
| 版番号 | VERSION_NO | 数値(5) | ○ | PK | 1 から連番 |
| 適用開始日時 | EFFECTIVE_FROM | 日時 | ○ | | 登録済みの版より後 |
| ファイル名 | FILE_NAME | 文字列(200) | ○ | | アップロード時のファイル名 |
| ファイルサイズ | FILE_SIZE | 数値(9) | ○ | | バイト。10MB 以内 |
| ハッシュ | FILE_HASH | 固定長(64) | ○ | | PDF の SHA-256（16 進） |
| PDF | FILE_DATA | バイナリ | ○ | | 先頭が %PDF- のファイル |
| 改定内容 | REMARKS | 文字列(200) | | | |

### 4.21 申込者同意の同意事項（T_APPLICANT_CONSENT_DOCUMENT）

申込者同意（確認依頼）ごとに、どの同意事項のどの版を開き、どの版に同意したかを記録する。

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| 同意ID | CONSENT_ID | 数値(10) | ○ | PK、FK | T_APPLICANT_CONSENT |
| 文書コード | DOCUMENT_CD | 文字列(20) | ○ | PK、FK | M_CONSENT_DOCUMENT_VERSION |
| 版番号 | VERSION_NO | 数値(5) | ○ | PK、FK | 同上 |
| 閲覧日時 | VIEWED_AT | 日時 | ○ | | AP03 でその版の PDF を最初に開いた日時 |
| 同意日時 | AGREED_AT | 日時 | | | 「同意する」で同意した版に設定。改定前に開いた版は空のまま |

### 4.22 申込内容 PDF（T_APPLICATION_PDF）

申込者の同意時と審査完了時に作成した申込内容の PDF。作成後は変更しない（[10. 20 章](10-function-detail.md#20-f19-申込内容-pdf)）。

| 論理名 | 物理名 | 型 | 必須 | キー | 説明 |
| --- | --- | --- | --- | --- | --- |
| PDF ID | PDF_ID | 数値(10) | ○ | PK | |
| 申込ID | APPLICATION_ID | 数値(10) | ○ | FK | T_APPLICATION_VERSION（APPLICATION_ID, VERSION_NO） |
| 版番号 | VERSION_NO | 数値(3) | ○ | FK | 載せた申込内容の版 |
| 種別 | PDF_TYPE | 固定長(1) | ○ | | 1：ご同意時、2：審査完了時（[08. 21 章](08-code-definition.md#21-申込内容-pdf-の種別pdf_type)） |
| 同意ID | CONSENT_ID | 数値(10) | | FK | T_APPLICANT_CONSENT。種別 1 のみ |
| ファイル名 | FILE_NAME | 文字列(200) | ○ | | 申込番号_v版_種類_日時.pdf |
| ファイルサイズ | FILE_SIZE | 数値(9) | ○ | | バイト |
| ハッシュ | FILE_HASH | 固定長(64) | ○ | | PDF の SHA-256（16 進） |
| PDF | FILE_DATA | バイナリ | ○ | | |

インデックス：IX_T_APPLICATION_PDF_01（APPLICATION_ID）

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
| T_APPLICATION_VERSION | APPLICATION_AMOUNT を金額項目（BASIC_FEE・OPTION_FEE・HANDLING_FEE）と申込金額合計（TOTAL_AMOUNT）に分割。OTHER_MODIFIED_FLG を削除 | 変更基準を金額項目の合計の倍率だけで判定する（業務確認済み） |
| T_APPROVAL_REQUEST | REQUEST_STATUS から 4（引戻し）を削除。ROUTE_ID をテンプレートの参照に変更 | 申請中の引戻しは遷移表にない。回付先は申請時に設定する |
| T_APPROVAL_STEP | RESULT_CD に 3（審査申請）を追加 | 最終承認者の操作が審査申請になる |
| T_EXTERNAL_LINK | NG_REASON を RESULT_REASON に変更。RESULT_CD に 4（審査差戻し）を追加 | 審査差戻しに対応 |
| T_NOTIFICATION | 通知種別を 7 種に変更 | 申込者差戻し・審査結果の通知を追加 |

## 6. 版 0.3 からの変更点（実装 V3）

| 対象 | 変更 | 理由 |
| --- | --- | --- |
| M_APPLICANT | PASSWORD_HASH、ACCOUNT_ISSUED_AT、PASSWORD_CHANGED_AT を追加 | 申込者ページのログイン（ログイン ID は申込者番号） |
| M_STATUS | 90101 申込取消 を追加 | 申込メニューからの申込取消 |
| M_STATUS_TRANSITION | 操作コード 15（取消）の遷移 56〜74 を追加 | 同上。契約変更中の取消は契約変更前へ復元 |
| T_NOTIFICATION | 通知種別 08（申込者アカウント通知）を追加 | ユーザー ID と初期パスワードの連携 |

## 7. 実装 V4 の変更点

| 対象 | 変更 | 理由 |
| --- | --- | --- |
| SEQ_APPLICANT_NO | 申込者番号の採番用シーケンスを追加 | 申込者を新規申込の登録時に採番して登録するため |
| M_APPLICANT | 初期データのサンプル申込者（C0000000001〜C0000000005）のうち申込から参照されていないものを削除 | 申込者データの起点を申込受付会社の新規申込にするため |
| T_NOTIFICATION | 通知種別 09（パスワード初期化通知）を追加（コードのみ） | SC15 のパスワード初期化 |

## 8. 実装 V5 の変更点

| 対象 | 変更 | 理由 |
| --- | --- | --- |
| T_APPLICATION_VERSION | APPLICANT_NAME、APPLICANT_KANA、TEL_NO、MAIL_ADDRESS、ADDRESS を追加。既存の申込は申込者マスタの値を全版へ複写 | 申込者情報を申込データとして版で持つ（全体修正・契約変更・申込者の修正の対象、差分表示） |
| T_APPLICATION | APPLICANT_ID を任意（NULL 可）に変更。アカウント未発行の申込者を参照していた申込は空にする | 申込者アカウントは一次承認が通ったときに発行して紐づける |
| M_APPLICANT → M_APPLICANT_ACCOUNT | 名称変更。APPLICANT_NAME、APPLICANT_KANA、MAIL_ADDRESS、TEL_NO、ADDRESS を削除。アカウント未発行（PASSWORD_HASH が空）の行を削除 | 申込データとログインに必要なデータを分ける |

## 9. 実装 V6 の変更点

| 対象 | 変更 | 理由 |
| --- | --- | --- |
| M_DEPARTMENT | 部署マスタを追加。既存の社員・承認ルート・申込の部署コードから作成（100：営業部、900：管理部、200：営業部、それ以外は「部署 + コード」の仮の名称） | 申込の担当（会社 > 部署 > 担当者）を部署名で選び、表示するため |
| M_EMPLOYEE、M_APPROVAL_ROUTE、T_APPLICATION | （COMPANY_DIV, DEPT_CD）に M_DEPARTMENT への外部キーを追加（V6_1） | 部署はマスタから選ぶ |
| T_APPLICATION_VERSION | COMPANY_DIV、DEPT_CD、OWNER_EMPLOYEE_ID を追加。既存の版には申込の値を複写（V6_1） | 版ごとに担当を記録し、全体修正で変えた担当を差分（赤字）で表示する |

## 10. 実装 V7 の変更点

| 対象 | 変更 | 理由 |
| --- | --- | --- |
| T_APPLICATION_VERSION | CORPORATE_NO、INSTALL_PLACE、CONTACT_MEMO を追加（いずれも NULL 可） | 会社B（会社区分 2）だけの申込の入力項目。入力・表示するだけで、項目を使った制御（遷移・判定・連携）はない。項目名・桁はサンプル |

## 11. 実装 V8 の変更点

| 対象 | 変更 | 理由 |
| --- | --- | --- |
| M_CONSENT_DOCUMENT、M_CONSENT_DOCUMENT_VERSION | 同意事項マスタと版を追加。サンプルの同意事項（新規申込 3 件・契約変更 2 件。利用規約は共通）と第 1 版の PDF を V8_1（Java マイグレーション。PDF はサンプル文言から生成）で登録 | AP03 に同意事項を PDF で表示し、改定を版で管理する |
| T_APPLICANT_CONSENT_DOCUMENT | 同意ごとに開いた版・同意した版を記録するテーブルを追加 | 申込者がどの版の同意事項に同意したかを把握する |
| T_APPLICATION_PDF | 申込内容 PDF（同意時・審査完了時）を保存するテーブルを追加 | 申込者がメニューから同意時・審査完了時の申込内容を閲覧する |
