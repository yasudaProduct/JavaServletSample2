# 画面の一括動作確認（Playwright）

`flow.js` は起動中のアプリに対してブラウザ操作で主要フローを一通り実行し、各ステータスへの遷移を確認する。

```bash
npm install playwright
npx playwright install chromium
BASE=http://localhost:8080 node e2e/flow.js
```

- 会社A：新規申込 → 一次承認 → 申込者の修正・確定・同意 → 一部修正（基準内／基準超）→ 申込者差戻し → 最終承認 → 審査申請 → 審査差戻し／審査完了 → 契約変更（基準内／基準超）→ 契約変更の審査差戻し（復元）／審査完了
- 会社B：回付先なし申請 → 同意 → 事前確認 NG → 修正対応 → 事前確認 OK → 全体修正 → 引戻し
- 一括取込（正常行とエラー行）、管理者のマスタ画面、開発支援画面

スクリーンショットは `e2e/shots/` に保存される（git 管理外）。開発支援画面（`app.dev-tools.enabled=true`）と `extapi.mode=mock` を前提にしている。
