const { chromium } = require('playwright');
const BASE = process.env.BASE || 'http://localhost:8080';
const SHOTS = __dirname + '/shots/';
let shotNo = 0;
async function shot(page, name) { shotNo++; await page.screenshot({ path: SHOTS + String(shotNo).padStart(2, '0') + '-' + name + '.png', fullPage: true }); }
async function login(page, no) {
  await page.goto(BASE + '/emp/login');
  await page.fill('#employeeNo', no); await page.fill('#password', 'password');
  await page.click('button[type=submit]'); await page.waitForURL('**/emp/applications**');
}
async function logout(page) { await page.click('form[action$="/emp/logout"] button'); await page.waitForURL('**/emp/login**'); }
async function statusOf(page) { return (await page.locator('h2 .badge').first().textContent()).trim(); }
async function expectStatus(page, code, label) {
  const s = await statusOf(page);
  if (!s.startsWith(code)) { throw new Error(`[${label}] expected status ${code} but got "${s}" at ${page.url()}`); }
  console.log(`OK  ${label}: ${s}`);
}
async function alerts(page) { return (await page.locator('.alert').allTextContents()).map(t => t.trim()).join(' | '); }
async function detailAction(page, appId, action) {
  await page.goto(BASE + '/emp/applications/' + appId);
  page.once('dialog', d => d.accept());
  await page.click(`form[action$="/${action}"] button`);
  await page.waitForLoadState('networkidle');
}
async function consentUrlFor(page, appNo) {
  await page.goto(BASE + '/emp/dev/notifications');
  const cards = page.locator('.card').filter({ hasText: appNo }).filter({ hasText: '申込者確認依頼' });
  const link = cards.first().locator('a[href*="/consent/"]');
  const href = await link.getAttribute('href');
  if (!href) throw new Error('consent URL not found for ' + appNo);
  return href.startsWith('http') ? href : BASE + href;
}
async function applicantConsent(ctx, url, { modifyFirst = false, agree = true, returnReason = null } = {}) {
  const page = await ctx.newPage();
  await page.goto(url);
  if (modifyFirst) {
    await page.click('a:has-text("内容を修正する")');
    await page.fill('#remarks', '申込者が備考を修正しました');
    await page.click('button:has-text("保存")');
    await page.waitForLoadState('networkidle');
  }
  await shot(page, 'AP01');
  await page.click('label[for=checked]'); await page.click('#confirmBtn'); await page.waitForURL('**/agree');
  await shot(page, 'AP03');
  if (returnReason) {
    await page.fill('#returnReason', returnReason);
    await page.click('button[value=return]');
  } else if (agree) {
    await page.click('label[for=agreed]'); await page.click('#agreeBtn');
  }
  await page.waitForURL('**/complete');
  await shot(page, 'AP04');
  const txt = await page.locator('main').textContent();
  await page.close();
  return txt;
}
async function mockResult(page, appNo, result, reason = '') {
  await page.goto(BASE + '/emp/dev/external-mock');
  await page.click('form[action$="/batch/run"] button');
  await page.waitForLoadState('networkidle');
  const card = page.locator('.card').filter({ hasText: appNo }).first();
  if (await card.count() === 0) throw new Error('no awaiting link for ' + appNo);
  await card.locator('select[name=result]').selectOption(result);
  if (reason) await card.locator('input[name=reason]').fill(reason);
  await card.locator('button:has-text("結果を返す")').click();
  await page.waitForLoadState('networkidle');
  console.log('    mock:', (await alerts(page)).slice(0, 160));
}
async function createApplication(page, applicantNo, basic, option) {
  await page.goto(BASE + '/emp/applications/new');
  await page.fill('#applicantNo', applicantNo); await page.fill('#productCd', 'PRD001');
  await page.fill('#basicFee', basic); await page.fill('#optionFee', option); await page.fill('#handlingFee', '0');
  await page.fill('#contractStartDate', '2026/11/01'); await page.fill('#contractEndDate', '2027/10/31'); await page.fill('#remarks', 'E2E テスト');
  await shot(page, 'SC04');
  await page.click('button[value=confirm]'); await page.waitForURL('**/confirm');
  await shot(page, 'SC05');
  await page.click('button[value=confirm]'); await page.waitForURL(/\/emp\/applications\/\d+$/);
  const appId = page.url().split('/').pop();
  const appNo = (await page.locator('h2').first().textContent()).match(/AP\d{10}/)[0];
  return { appId, appNo };
}
async function applyApproval(page, appId, { approvers = null, expectError = null } = {}) {
  await page.goto(BASE + '/emp/applications/' + appId + '/approval');
  await shot(page, 'SC06-wait');
  if (approvers !== null) {
    // 回付先を作り直す
    while (await page.locator('#routeRows button[data-remove]').count() > 0) { await page.locator('#routeRows button[data-remove]').first().click(); }
    for (const name of approvers) { await page.selectOption('#candidateSelect', { label: name }); await page.click('#addRoute'); }
  }
  await page.click('button[value=apply]'); await page.waitForLoadState('networkidle');
  console.log('    apply:', (await alerts(page)).slice(0, 120));
}
async function approve(page, appId, action, comment = '') {
  await page.goto(BASE + '/emp/applications/' + appId + '/approval');
  await shot(page, 'SC06-' + action);
  if (comment) await page.fill('#comment', comment);
  await page.click(`button[value=${action}]`); await page.waitForLoadState('networkidle');
  console.log('    ' + action + ':', (await alerts(page)).slice(0, 120));
}

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 }, locale: 'ja-JP' });
  const page = await ctx.newPage();
  page.on('pageerror', e => console.log('PAGE ERROR', e.message));
  try {
    // ---------- 会社A（事前確認なし）新規申込 ----------
    await login(page, 'A001'); await shot(page, 'SC02');
    const a = await createApplication(page, 'C0000000001', '1000000', '200000');
    await expectStatus(page, '10201', '確定後');
    await shot(page, 'SC03-10201');
    await applyApproval(page, a.appId); await expectStatus(page, '10202', '一次承認申請');
    await logout(page);

    await login(page, 'A002'); await shot(page, 'SC02-approver');
    await approve(page, a.appId, 'approve', '問題ありません'); await expectStatus(page, '10301', '一次承認（最終承認者）');
    await shot(page, 'SC03-10301');
    const url1 = await consentUrlFor(page, a.appNo);
    console.log('    consent url:', url1);
    await applicantConsent(ctx, url1, { modifyFirst: true, agree: true });
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '10501', '申込者同意（区分1）');
    await logout(page);

    // 一部修正（基準内）→ 10501 のまま、基準超 → 10201
    await login(page, 'A001');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/revise'); await shot(page, 'SC07');
    await page.fill('#optionFee', '300000'); await page.click('button:has-text("確定")'); await page.waitForLoadState('networkidle');
    await expectStatus(page, '10501', '一部修正（基準内）');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/revise');
    await page.fill('#basicFee', '2000000'); await page.click('button:has-text("確定")'); await page.waitForLoadState('networkidle');
    console.log('    ', (await alerts(page)).slice(0, 160));
    await expectStatus(page, '10201', '一部修正（基準超 → 一次承認からやり直し）');
    // 回付先なしで申請 → 10301 → 申込者差戻し → 10201 → 回付先なし申請 → 同意
    await applyApproval(page, a.appId, { approvers: [] }); await expectStatus(page, '10301', '回付先なし申請');
    const url2 = await consentUrlFor(page, a.appNo);
    await applicantConsent(ctx, url2, { returnReason: '金額に誤りがあります' });
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '10201', '申込者差戻し');
    await shot(page, 'SC03-after-applicant-return');
    await applyApproval(page, a.appId, { approvers: [] });
    const url3 = await consentUrlFor(page, a.appNo);
    await applicantConsent(ctx, url3, {});
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '10501', '再同意');
    // 最終承認：A002 → A003（審査申請）
    await applyApproval(page, a.appId); await expectStatus(page, '10502', '最終承認申請');
    await logout(page);
    await login(page, 'A002'); await approve(page, a.appId, 'approve'); await expectStatus(page, '10502', '最終承認 ステップ1 承認（遷移なし）'); await logout(page);
    await login(page, 'A003'); await approve(page, a.appId, 'review'); await expectStatus(page, '10601', '審査申請');
    await mockResult(page, a.appNo, 'RETURNED', '根拠資料が不足');
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '10501', '審査差戻し');
    await logout(page);
    await login(page, 'A001'); await applyApproval(page, a.appId); await logout(page);
    await login(page, 'A002'); await approve(page, a.appId, 'approve'); await logout(page);
    await login(page, 'A003'); await approve(page, a.appId, 'review'); await expectStatus(page, '10601', '再審査申請');
    await mockResult(page, a.appNo, 'COMPLETED');
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '10701', '審査完了'); await shot(page, 'SC03-10701');
    await logout(page);

    // ---------- 契約変更（基準内）→ 20501 → 審査差戻し → 10701 ----------
    await login(page, 'A001');
    await detailAction(page, a.appId, 'startChange');
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '20101', '契約変更開始'); await shot(page, 'SC03-20101');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/change'); await shot(page, 'SC08');
    await page.fill('#handlingFee', '50000'); await page.click('button[value=confirm]'); await page.waitForURL('**/change/confirm');
    await shot(page, 'SC09');
    await page.click('button[value=confirm]'); await page.waitForLoadState('networkidle');
    await expectStatus(page, '20501', '契約変更確定（基準内・区分1）');
    await applyApproval(page, a.appId); await logout(page);
    await login(page, 'A002'); await approve(page, a.appId, 'approve'); await logout(page);
    await login(page, 'A003'); await approve(page, a.appId, 'review'); await expectStatus(page, '20601', '契約変更審査申請');
    await mockResult(page, a.appNo, 'RETURNED', '契約変更は認められません');
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '10701', '契約変更審査差戻し（F13 復元）'); await shot(page, 'SC03-after-F13');
    await logout(page);

    // ---------- 契約変更（基準超）→ 20201 → 承認 → 同意 → 20501 → 審査完了 → 20701 ----------
    await login(page, 'A001');
    await detailAction(page, a.appId, 'startChange');
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '20101', '契約変更開始 2 回目');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/change');
    await page.fill('#basicFee', '5000000'); await page.click('button[value=confirm]'); await page.waitForURL('**/change/confirm');
    await shot(page, 'SC09-over');
    await page.click('button[value=confirm]'); await page.waitForLoadState('networkidle');
    await expectStatus(page, '20201', '契約変更確定（基準超）');
    await applyApproval(page, a.appId, { approvers: [] }); await expectStatus(page, '20301', '契約変更 回付先なし申請');
    const url4 = await consentUrlFor(page, a.appNo);
    await applicantConsent(ctx, url4, {});
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '20501', '契約変更 同意');
    await applyApproval(page, a.appId); await logout(page);
    await login(page, 'A002'); await approve(page, a.appId, 'approve'); await logout(page);
    await login(page, 'A003'); await approve(page, a.appId, 'review'); await expectStatus(page, '20601', '契約変更 審査申請');
    await mockResult(page, a.appNo, 'COMPLETED');
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '20701', '契約変更 審査完了'); await shot(page, 'SC03-20701');
    await logout(page);

    // ---------- 会社B（事前確認あり）----------
    await login(page, 'B001');
    const b = await createApplication(page, 'C0000000003', '3000000', '0');
    await applyApproval(page, b.appId, { approvers: [] }); await expectStatus(page, '10301', 'B 回付先なし');
    const urlB = await consentUrlFor(page, b.appNo);
    await applicantConsent(ctx, urlB, {});
    await page.goto(BASE + '/emp/applications/' + b.appId); await expectStatus(page, '10401', 'B 同意 → 事前確認待ち');
    await mockResult(page, b.appNo, 'NG', '契約期間を確認してください');
    await page.goto(BASE + '/emp/applications/' + b.appId); await expectStatus(page, '10402', 'B 事前確認 NG'); await shot(page, 'SC03-10402');
    await page.goto(BASE + '/emp/applications/' + b.appId + '/revise'); await shot(page, 'SC07-fix');
    await page.fill('#contractEndDate', '2027/12/31'); await page.click('button:has-text("確定")'); await page.waitForLoadState('networkidle');
    await expectStatus(page, '10401', 'B 修正対応（基準内）→ 事前確認再依頼');
    await mockResult(page, b.appNo, 'OK');
    await page.goto(BASE + '/emp/applications/' + b.appId); await expectStatus(page, '10501', 'B 事前確認 OK');
    // 引戻しの確認：全体修正 → 10101 → 確定 → 10201 → 回付先なし → 10301 → 引戻し → 10201
    await detailAction(page, b.appId, 'fullRevise');
    await page.goto(BASE + '/emp/applications/' + b.appId); await expectStatus(page, '10101', 'B 全体修正');
    await page.goto(BASE + '/emp/applications/' + b.appId + '/edit'); await page.click('button[value=confirm]'); await page.waitForURL('**/confirm');
    await page.click('button[value=confirm]'); await page.waitForLoadState('networkidle'); await expectStatus(page, '10201', 'B 再確定');
    await applyApproval(page, b.appId, { approvers: [] }); await expectStatus(page, '10301', 'B 回付先なし 2');
    await detailAction(page, b.appId, 'resendConsent'); console.log('    resend:', (await alerts(page)).slice(0, 80));
    await detailAction(page, b.appId, 'pullBack'); await expectStatus(page, '10201', 'B 引戻し');
    await shot(page, 'SC03-B-final');
    // 一括取込
    await page.goto(BASE + '/emp/import');
    await page.setInputFiles('input[name=file]', { name: 'import.csv', mimeType: 'text/csv', buffer: Buffer.from('﻿申込者番号,商品コード,基本料金,オプション料金,事務手数料,契約開始日,契約終了日,備考\nC0000000004,PRD002,500000,0,0,2026-10-01,2027-09-30,\nC0000000005,PRD001,abc,0,0,2026-10-01,2026-09-01,エラー行\nC0000000009,PRD001,1000,0,0,,,存在しない申込者\n', 'utf8') });
    await page.click('button:has-text("取込実行")'); await page.waitForLoadState('networkidle'); await shot(page, 'SC10');
    console.log('    import:', (await page.locator('.card-body').nth(1).textContent()).replace(/\s+/g, ' ').slice(0, 200));
    await logout(page);
    // 管理者：マスタ画面
    await login(page, 'A009'); await page.goto(BASE + '/emp/master/employees'); await shot(page, 'SC11');
    await page.goto(BASE + '/emp/master/company-divs'); await shot(page, 'SC12');
    await page.goto(BASE + '/emp/master/approval-routes'); await shot(page, 'SC13');
    await page.goto(BASE + '/emp/applications'); await shot(page, 'SC02-admin');
    await page.goto(BASE + '/emp/dev/notifications'); await shot(page, 'DV01');
    await page.goto(BASE + '/emp/dev/external-mock'); await shot(page, 'DV02');
    console.log('ALL FLOWS PASSED');
  } catch (e) {
    console.log('FAILED:', e.message);
    await shot(page, 'FAILED');
    console.log('alerts:', await alerts(page));
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
})();
