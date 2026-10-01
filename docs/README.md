# 設計ドキュメント

申込管理システムの設計ドキュメント一覧。基本設計は `basic-design/`、図の原本は `diagrams/` に置く。

## 基本設計書

読む順序は番号順を推奨。01〜03 で全体像とステータス遷移を押さえ、06〜08 でデータ構造、09〜13 で機能・画面・IF・バッチ、14 で共通仕様を確認する。

| No | ドキュメント | 内容 |
| --- | --- | --- |
| 01 | [システム概要](basic-design/01-overview.md) | 目的、対象範囲、アクター、会社区分、設計方針、用語 |
| 02 | [システム構成・アプリケーション構成](basic-design/02-system-architecture.md) | システム構成（仮）、技術スタック（仮）、レイヤ構成、パッケージ構成案 |
| 03 | [ステータス定義・状態遷移](basic-design/03-status-transition.md) | 23 ステータス（5 桁コード）、会社区分別の遷移ルール、フロー別の状態遷移図、遷移マスタ初期データ（55 行）、版と基準版 |
| 04 | [処理フロー図](basic-design/04-process-flow.md) | 新規申込／契約変更のスイムレーン図と工程表 |
| 05 | [データフロー図](basic-design/05-data-flow.md) | コンテキスト図、レベル 1 DFD、処理・データストア・フロー一覧 |
| 06 | [ER図](basic-design/06-er-diagram.md) | ER図、リレーション一覧、データモデルの要点 |
| 07 | [テーブル定義](basic-design/07-table-definition.md) | 17 テーブルの項目定義、型対応表（SQL Server 想定）、インデックス |
| 08 | [コード定義](basic-design/08-code-definition.md) | 区分値の一覧（操作コード、条件コード、承認種別 など） |
| 09 | [機能一覧](basic-design/09-function-list.md) | F01〜F15、機能とステータスの対応、画面・IF・バッチの ID 体系 |
| 10 | [機能詳細](basic-design/10-function-detail.md) | 各機能の処理手順、F14 ステータス遷移制御、主要シーケンス図 |
| 11 | [画面設計](basic-design/11-screen-design.md) | 申込受付会社向け・申込者向けの画面一覧、画面遷移図、画面別の項目・操作・ボタン表示制御 |
| 12 | [外部インターフェース設計](basic-design/12-external-interface.md) | 審査担当部門システムとの API（IF01〜IF04）、取込ファイル（IF05） |
| 13 | [バッチ・通知設計](basic-design/13-batch-notification.md) | 通知送信バッチ（BT01）、外部連携送信バッチ（BT02）、メールテンプレート |
| 14 | [共通仕様・非機能要件](basic-design/14-common-spec.md) | 認証・認可、排他、採番、メッセージ、セキュリティ、性能・運用（仮） |
| 99 | [未決事項・確認事項](basic-design/99-open-issues.md) | 未決事項の一覧と仮置き、元設計からの変更点 |

## 図

| ファイル | 内容 | 参照元 |
| --- | --- | --- |
| [diagrams/process-flow-new-application.drawio](diagrams/process-flow-new-application.drawio) | 処理フロー図（1/2）新規申込フロー | 04 |
| [diagrams/process-flow-contract-change.drawio](diagrams/process-flow-contract-change.drawio) | 処理フロー図（2/2）契約変更フロー | 04 |
| [diagrams/status-transition.drawio](diagrams/status-transition.drawio) | ステータス遷移図（全 23 ステータス） | 03 |
| [diagrams/dfd.drawio](diagrams/dfd.drawio) | データフロー図（レベル 1） | 05 |
| [diagrams/er-diagram.drawio](diagrams/er-diagram.drawio) | ER図（17 テーブル） | 06 |
| [diagrams/png/](diagrams/png/) | 上記の PNG エクスポート（閲覧用。原本は .drawio） | |

### 図の扱い

- 簡易な図はドキュメント内に Mermaid で記述する（GitHub 上でそのまま表示される）。
- 複雑・大きな図は draw.io（`.drawio`）で作成し、閲覧用に PNG をエクスポートする。`.drawio` は [diagrams.net](https://app.diagrams.net/) または VS Code の Draw.io Integration 拡張で開ける。
- `.drawio` を更新したら PNG も更新する（draw.io の「エクスポート → PNG」）。

## 記載ルール

- 各ドキュメントの先頭に版・関連ドキュメントの表を置く。
- ステータスコード・区分値は 03／08 の定義を正とし、他のドキュメントはそれに合わせる。
- 仮置きの値・方針には「（仮）」を付け、[99. 未決事項](basic-design/99-open-issues.md) に確認事項として載せる。
- テーブル・項目名は物理名（例：T_APPLICATION.STATUS_CD）で書く。

## 改定履歴

| 版 | 日付 | 内容 |
| --- | --- | --- |
| 0.1 | 2026-09-30 | 初版（4 桁ステータス、顧客・外部システムの用語） |
| 0.2 | 2026-10-01 | 業務側のステータス一覧・会社区分別遷移表に合わせて全面改定（5 桁ステータス、申込受付会社・申込者・審査担当部門の用語、事前確認を同意後の工程に変更、申込者の修正・差戻し、審査差戻し、画面からの新規・追加申込、契約変更の複数回対応） |

## 今後の予定

1. 未決事項の確認と設計の更新（環境・外部 IF・業務ルール）。
2. 実行環境の確定（Servlet／Tomcat／SQL Server を想定）。
3. 詳細設計（画面レイアウト、メッセージ一覧、DDL、API 仕様の確定）と実装。
