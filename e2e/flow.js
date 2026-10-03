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
async function menuAction(page, appId, action, reason) {
  await page.goto(BASE + '/emp/applications/' + appId + '/menu');
  if (reason) { await page.fill(`form[action$="/${action}"] textarea[name=reason]`, reason); }
  page.once('dialog', d => d.accept());
  await page.click(`form[action$="/${action}"] button`);
  await page.waitForLoadState('networkidle');
  if (!page.url().endsWith('/menu')) { await page.goto(BASE + '/emp/applications/' + appId + '/menu'); }
}
async function consentUrlFor(page, appNo) {
  await page.goto(BASE + '/emp/dev/notifications');
  const cards = page.locator('.card').filter({ hasText: appNo }).filter({ hasText: '申込者確認依頼' });
  const href = await cards.first().locator('a[href*="/consent/"]').getAttribute('href');
  if (!href) throw new Error('consent URL not found for ' + appNo);
  return href.startsWith('http') ? href : BASE + href;
}
async function accountFor(page, appNo) {
  await page.goto(BASE + '/emp/dev/notifications');
  const card = page.locator('.card').filter({ hasText: appNo }).filter({ hasText: '申込者アカウント通知' }).first();
  if (await card.count() === 0) throw new Error('account notification not found for ' + appNo);
  const body = await card.locator('.mail-body').textContent();
  const id = body.match(/ユーザー ID：(\S+)/)[1];
  const pw = body.match(/初期パスワード：(\S+)/)[1];
  return { id, pw };
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
  console.log('    mock:', (await alerts(page)).slice(0, 100));
}
// 申込者情報（申込データ）の入力。no を渡すと発行済みの申込者アカウントに紐づける（同じ申込者の 2 件目以降）
async function fillApplicant(page, applicant) {
  await page.fill('#applicantName', applicant.name); await page.fill('#applicantKana', applicant.kana || '');
  await page.fill('#mailAddress', applicant.mail); await page.fill('#telNo', applicant.tel || ''); await page.fill('#address', applicant.address || '');
  if (applicant.no) { await page.fill('#applicantNo', applicant.no); }
}
async function applicantNoOnMenu(page) { const m = (await page.locator('.menu-head').textContent()).match(/C\d{10}/); return m ? m[0] : null; }
async function createApplication(page, applicant, basic, option) {
  await page.goto(BASE + '/emp/applications/new');
  await fillApplicant(page, applicant); await page.fill('#productCd', 'PRD001');
  await page.fill('#basicFee', basic); await page.fill('#optionFee', option); await page.fill('#handlingFee', '0');
  await page.fill('#contractStartDate', '2026/11/01'); await page.fill('#contractEndDate', '2027/10/31'); await page.fill('#remarks', 'E2E テスト');
  await shot(page, 'SC04');
  await page.click('button[value=confirm]'); await page.waitForURL('**/confirm');
  await shot(page, 'SC05');
  await page.click('button[value=confirm]'); await page.waitForURL(/\/emp\/applications\/\d+\/menu$/);
  const appId = page.url().split('/').slice(-2)[0];
  const appNo = (await page.locator('h2').first().textContent()).match(/AP\d{10}/)[0];
  return { appId, appNo };
}
async function applyApproval(page, appId, { approvers = null, expectInitial = null, label = '' } = {}) {
  await page.goto(BASE + '/emp/applications/' + appId + '/approval');
  await shot(page, 'SC06-wait');
  if (expectInitial) {
    const rows = await page.locator('#routeRows tr').allTextContents();
    const names = rows.map(r => r.replace(/\s+/g, ' ').trim());
    expectInitial.forEach((n, i) => { if (!names[i] || !names[i].includes(n)) throw new Error(`[${label}] initial route row ${i + 1} expected ${n} but got ${names[i]}`); });
    console.log(`OK  ${label}: 初期回付先 = ${names.join(' / ').slice(0, 120)}`);
    console.log('    source:', (await page.locator('p:has-text("初期値：")').textContent()).replace(/\s+/g, ' ').slice(0, 90));
  }
  if (approvers !== null) {
    while (await page.locator('#routeRows button[data-remove]').count() > 0) { await page.locator('#routeRows button[data-remove]').first().click(); }
    for (const name of approvers) { await page.selectOption('#candidateSelect', { label: name }); await page.click('#addRoute'); }
  }
  await page.click('button[value=apply]'); await page.waitForLoadState('networkidle');
  console.log('    apply:', (await alerts(page)).slice(0, 60));
}
// SC14：申込全体のタイル（契約変更・追加申込・メンテナンス）と、手続きごとの領域（新規申込、契約変更 N）。
// 各領域は常に同じ 7 タイル（活性・非活性だけが変わる）。申込入力へのタイルはなく、SC03 は開発者向けリンクだけ
const GENERAL_KEYS = ['startChange', 'additional', 'maintenance'];
const SECTION_KEYS = ['confirm', 'revise', 'approval', 'pullBack', 'cancel', 'resendConsent', 'resendExternal'];
async function checkTiles(page, appId, label, expectSections = null) {
  await page.goto(BASE + '/emp/applications/' + appId + '/menu');
  const general = await page.locator('.tile').evaluateAll(els => els.map(e => e.id.replace('tile-', '')).filter(k => !/-g\d+$/.test(k)));
  if (general.join(',') !== GENERAL_KEYS.join(',')) throw new Error(`[${label}] general tiles = ${general.join(',')}`);
  const titles = await page.locator('.menu-section .menu-section-title').allTextContents();
  if (expectSections && titles.join(',') !== expectSections.join(',')) throw new Error(`[${label}] sections = ${titles.join(',')}`);
  for (let i = 0; i < titles.length; i++) {
    const keys = await page.locator(`#sec-${i} .tile`).evaluateAll(els => els.map(e => e.id.replace('tile-', '').replace(/-g\d+$/, '')));
    if (keys.join(',') !== SECTION_KEYS.join(',')) throw new Error(`[${label}] section ${i} tiles = ${keys.join(',')}`);
  }
  if (await page.locator('#devDetailLink').count() !== 1) throw new Error(`[${label}] developer link missing`);
  if (await page.locator('#tile-detail, #tile-input, #tile-modify, #tile-fullRevise').count() !== 0) throw new Error(`[${label}] unexpected tiles`);
  const enabled = await page.locator('.tile:not(.tile-disabled)').evaluateAll(els => els.map(e => e.id.replace('tile-', '')));
  const expanded = await page.locator('.menu-section .collapse.show').evaluateAll(els => els.map(e => e.id.replace('sec-body-', '')));
  console.log(`OK  メニュー領域（${label}）: ${titles.join('／')}　展開 = ${expanded.join(',')}　活性 = ${enabled.join(',')}`);
  return titles.length;
}
async function passwordResetFor(page, appNo) {
  await page.goto(BASE + '/emp/dev/notifications');
  const card = page.locator('.card').filter({ hasText: appNo }).filter({ hasText: 'パスワード初期化通知' }).first();
  if (await card.count() === 0) throw new Error('password reset notification not found for ' + appNo);
  const body = await card.locator('.mail-body').textContent();
  return body.match(/初期パスワード：(\S+)/)[1];
}
async function approve(page, appId, action, comment = '') {
  await page.goto(BASE + '/emp/applications/' + appId + '/approval');
  await shot(page, 'SC06-' + action);
  if (comment) await page.fill('#comment', comment);
  await page.click(`button[value=${action}]`); await page.waitForLoadState('networkidle');
  console.log('    ' + action + ':', (await alerts(page)).slice(0, 60));
}
// 申込者ポータル（別のブラウザコンテキスト）
async function portalLogin(apPage, id, pw) {
  await apPage.goto(BASE + '/my/login');
  await apPage.fill('#loginId', id); await apPage.fill('#password', pw);
  await apPage.click('button[type=submit]'); await apPage.waitForURL('**/my/menu**');
}

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 }, locale: 'ja-JP' });
  const apCtx = await browser.newContext({ viewport: { width: 1280, height: 900 }, locale: 'ja-JP' });
  const page = await ctx.newPage();
  const apPage = await apCtx.newPage();
  page.on('pageerror', e => console.log('PAGE ERROR', e.message));
  apPage.on('pageerror', e => console.log('PAGE ERROR(portal)', e.message));
  try {
    // ---------- 会社A（事前確認なし）新規申込 ----------
    await login(page, 'A001'); await shot(page, 'SC02');
    const TARO = { name: '山田 太郎', kana: 'ヤマダ タロウ', mail: 'taro.yamada@example.com', tel: '03-0000-0001', address: '東京都千代田区丸の内1-1-1' };
    const a = await createApplication(page, TARO, '1000000', '200000');
    // 申込者情報は申込データ。ログイン用の申込者アカウントは一次承認まで発行しない
    if (await applicantNoOnMenu(page) !== null || !(await page.locator('.menu-head').textContent()).includes('アカウント未発行')) throw new Error('account should not exist before primary approval');
    console.log('OK  新規申込：申込者情報は申込データとして登録、申込者アカウントは未発行');
    await expectStatus(page, '10201', '確定後（メニュー）');
    await shot(page, 'SC14-10201');
    await checkTiles(page, a.appId, '10201');
    // 10201 → SC05「修正」→ 10101（申込入力）→ 確認へ → 確定 → 10201（メニューに入力画面へのタイルはない）
    await page.goto(BASE + '/emp/applications/' + a.appId + '/confirm'); await shot(page, 'SC05-10201');
    await page.click('#modifyBtn'); await page.waitForURL('**/edit');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/menu'); await expectStatus(page, '10101', 'SC05 の修正で入力中へ');
    await checkTiles(page, a.appId, '10101');
    // 申込者情報は申込内容と一緒に申込入力で修正する
    await page.goto(BASE + '/emp/applications/' + a.appId + '/edit'); await shot(page, 'SC04-edit-applicant');
    await page.fill('#telNo', '03-1111-2222'); await page.click('button[value=confirm]'); await page.waitForURL('**/confirm');
    if (!(await page.locator('.version-table').textContent()).includes('03-1111-2222')) throw new Error('SC05 should show the corrected applicant tel');
    console.log('OK  申込者情報を申込内容として修正（SC04）→ SC05 の申込内容に反映');
    await page.click('#confirmBtn'); await page.waitForURL(/\/menu$/); await expectStatus(page, '10201', 'SC05 で再確定');
    await applyApproval(page, a.appId, { expectInitial: ['一郎'], label: '一次承認 初期回付先（テンプレート）' }); await expectStatus(page, '10202', '一次承認申請');
    // 申請した担当者は申請中の承認フローを参照できる（申請ボタンは非活性、引戻しは非活性）
    await page.goto(BASE + '/emp/applications/' + a.appId + '/approval'); await shot(page, 'SC06-requester-view');
    if (await page.locator('#applyBtn:disabled').count() !== 1) throw new Error('requester view: 申請 button should be disabled');
    if (await page.locator('button[value=apply]').count() !== 0) throw new Error('requester view: apply form should not be submittable');
    console.log('OK  申請者の承認フロー参照（10202）: ' + (await page.locator('#viewNote').textContent()).trim().slice(0, 50));
    await checkTiles(page, a.appId, '10202');
    if (await page.locator('#tile-pullBack-g0.tile-disabled').count() !== 1) throw new Error('10202: 引戻し should be disabled');
    await logout(page);

    await login(page, 'A002'); await shot(page, 'SC02-approver');
    await approve(page, a.appId, 'approve', '問題ありません'); await expectStatus(page, '10301', '一次承認（最終承認者）');
    await shot(page, 'SC14-10301');
    await page.goto(BASE + '/emp/applications/' + a.appId); await shot(page, 'SC03-10301');
    // ---------- 申込者ポータル：アカウント通知 → ログイン → メニュー → 内容確認・同意 ----------
    const acct = await accountFor(page, a.appNo);
    a.applicantNo = acct.id;
    await page.goto(BASE + '/emp/applications/' + a.appId + '/menu');
    if (await applicantNoOnMenu(page) !== acct.id) throw new Error('account should be issued and linked at primary approval');
    console.log('OK  一次承認で申込者アカウントを発行して申込に紐づけ: ' + acct.id);
    await portalLogin(apPage, acct.id, acct.pw);
    await shot(apPage, 'AP07-menu');
    await apPage.goto(BASE + '/my/menu?tab=notice'); await shot(apPage, 'AP07-notice');
    const noticeCount = await apPage.locator('#pane-notice .card').count();
    console.log('OK  申込者ポータル お知らせ件数:', noticeCount);
    await apPage.goto(BASE + '/my/menu');
    await apPage.click('#tile-consent a');
    await apPage.waitForURL('**/consent');
    await apPage.click('a:has-text("内容を修正する")'); await apPage.fill('#remarks', '申込者がポータルで備考を修正しました'); await apPage.click('button:has-text("保存")'); await apPage.waitForLoadState('networkidle');
    await shot(apPage, 'AP01-portal');
    await apPage.click('label[for=checked]'); await apPage.click('#confirmBtn'); await apPage.waitForLoadState('networkidle');
    await shot(apPage, 'AP03-portal');
    await apPage.click('label[for=agreed]'); await apPage.click('#agreeBtn'); await apPage.waitForURL('**/my/menu**');
    console.log('    portal agree:', (await alerts(apPage)).slice(0, 40));
    await shot(apPage, 'AP07-after-agree');
    await apPage.goto(BASE + '/my/applications/' + a.appId); await shot(apPage, 'AP08');
    // パスワード変更
    await apPage.goto(BASE + '/my/password'); await apPage.fill('#currentPassword', acct.pw); await apPage.fill('#newPassword', 'NewPassw0rd1'); await apPage.fill('#confirmPassword', 'NewPassw0rd1');
    await apPage.click('form[action$="/my/password"] button[type=submit]'); await apPage.waitForURL('**/my/menu**'); console.log('    password:', (await alerts(apPage)).slice(0, 40));
    await apPage.click('form[action$="/my/logout"] button'); await apPage.waitForURL('**/my/login**');
    await portalLogin(apPage, acct.id, 'NewPassw0rd1'); console.log('OK  申込者ポータル 変更後パスワードでログイン');
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '10501', '申込者同意（区分1・ポータル）');
    await logout(page);

    // ---------- SC15 メンテナンス：通知履歴とパスワードの初期化 ----------
    await login(page, 'A001');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/maintenance'); await shot(page, 'SC15');
    // SC15 はログイン用のデータ（申込者アカウント）だけを扱い、申込者情報は変更しない
    if (await page.locator('#applicantName, #updateApplicantBtn').count() !== 0) throw new Error('SC15 should not edit applicant data');
    if (!(await page.locator('#accountTable').textContent()).includes(acct.id)) throw new Error('SC15 should show the applicant account');
    const noticeCards = await page.locator('.notice-card').count();
    if (noticeCards < 2) throw new Error('maintenance: expected applicant notices, got ' + noticeCards);
    page.once('dialog', d => d.accept());
    await page.click('#resetPasswordBtn'); await page.waitForLoadState('networkidle');
    console.log('OK  パスワード初期化:', (await alerts(page)).slice(0, 40), '／ 通知履歴', noticeCards, '件');
    if (await page.locator('.notice-card').filter({ hasText: 'パスワード初期化通知' }).count() !== 1) throw new Error('maintenance: reset notice missing');
    const resetPw = await passwordResetFor(page, a.appNo);
    await apPage.click('form[action$="/my/logout"] button'); await apPage.waitForURL('**/my/login**');
    await apPage.goto(BASE + '/my/login'); await apPage.fill('#loginId', acct.id); await apPage.fill('#password', 'NewPassw0rd1'); await apPage.click('button[type=submit]'); await apPage.waitForLoadState('networkidle');
    if (!apPage.url().includes('/my/login')) throw new Error('old password should be rejected after reset');
    await portalLogin(apPage, acct.id, resetPw); console.log('OK  申込者ポータル 初期化後のパスワードでログイン');
    await logout(page);

    // 一部修正（基準内）→ 10501 のまま、基準超 → 10201
    await login(page, 'A001');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/revise'); await shot(page, 'SC07');
    await page.fill('#optionFee', '300000'); await page.click('button:has-text("確定")'); await page.waitForLoadState('networkidle');
    await expectStatus(page, '10501', '一部修正（基準内）');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/revise');
    await page.fill('#basicFee', '2000000'); await page.click('button:has-text("確定")'); await page.waitForLoadState('networkidle');
    await expectStatus(page, '10201', '一部修正（基準超 → 一次承認からやり直し）');
    // 回付先なしで申請 → 10301 → 申込者差戻し（トークン経由）→ 10201 → 回付先なし申請 → 同意（トークン経由）
    await applyApproval(page, a.appId, { approvers: [] }); await expectStatus(page, '10301', '回付先なし申請');
    const url2 = await consentUrlFor(page, a.appNo);
    await applicantConsent(ctx, url2, { returnReason: '金額に誤りがあります' });
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '10201', '申込者差戻し');
    await applyApproval(page, a.appId, { approvers: [] });
    const url3 = await consentUrlFor(page, a.appNo);
    await applicantConsent(ctx, url3, {});
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '10501', '再同意（トークン）');
    // 最終承認：テンプレートと逆順（二郎 → 一郎）で申請し、次回の初期値が前回の回付先になることを確認する
    await applyApproval(page, a.appId, { approvers: ['会社A 承認 二郎（100）', '会社A 承認 一郎（100）'] }); await expectStatus(page, '10502', '最終承認申請（逆順の回付先）');
    await logout(page);
    await login(page, 'A003'); await approve(page, a.appId, 'approve'); await expectStatus(page, '10502', '最終承認 ステップ1 承認（遷移なし）'); await logout(page);
    await login(page, 'A002'); await approve(page, a.appId, 'review'); await expectStatus(page, '10601', '審査申請');
    await mockResult(page, a.appNo, 'RETURNED', '根拠資料が不足');
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '10501', '審査差戻し');
    await logout(page);
    await login(page, 'A001'); await applyApproval(page, a.appId, { expectInitial: ['二郎', '一郎'], label: '最終承認 初期回付先（前回の申請）' }); await logout(page);
    await login(page, 'A003'); await approve(page, a.appId, 'approve'); await logout(page);
    await login(page, 'A002'); await approve(page, a.appId, 'review'); await expectStatus(page, '10601', '再審査申請');
    await mockResult(page, a.appNo, 'COMPLETED');
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '10701', '審査完了'); await shot(page, 'SC03-10701');
    await logout(page);

    // ---------- 契約変更（基準内）→ 20501 → 審査差戻し → 10701、契約変更の取消 ----------
    await login(page, 'A001');
    await menuAction(page, a.appId, 'startChange'); await expectStatus(page, '20101', '契約変更開始');
    await shot(page, 'SC14-20101');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/change'); await shot(page, 'SC08');
    await page.fill('#handlingFee', '50000'); await page.click('button[value=confirm]'); await page.waitForURL('**/change/confirm');
    await shot(page, 'SC09');
    await page.click('button[value=confirm]'); await page.waitForLoadState('networkidle');
    await expectStatus(page, '20501', '契約変更確定（基準内・区分1）');
    await applyApproval(page, a.appId, { expectInitial: ['一郎', '二郎'], label: '契約変更最終承認 初期回付先（テンプレート）' }); await logout(page);
    await login(page, 'A002'); await approve(page, a.appId, 'approve'); await logout(page);
    await login(page, 'A003'); await approve(page, a.appId, 'review'); await expectStatus(page, '20601', '契約変更審査申請');
    await mockResult(page, a.appNo, 'RETURNED', '契約変更は認められません');
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '10701', '契約変更審査差戻し（F13 復元）'); await shot(page, 'SC03-after-F13');
    await logout(page);
    await login(page, 'A001');
    await menuAction(page, a.appId, 'startChange'); await expectStatus(page, '20101', '契約変更開始（取消テスト）');
    // 20101 → 確定 → 20501 → SC09「修正」→ 20101 → 契約変更の取消
    await page.goto(BASE + '/emp/applications/' + a.appId + '/change');
    await page.fill('#handlingFee', '10000'); await page.click('button[value=confirm]'); await page.waitForURL('**/change/confirm');
    await page.click('#confirmBtn'); await page.waitForLoadState('networkidle'); await expectStatus(page, '20501', '契約変更確定（取消テスト）');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/change/confirm'); await shot(page, 'SC09-20501');
    await page.click('#modifyBtn'); await page.waitForURL('**/change');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/menu'); await expectStatus(page, '20101', 'SC09 の修正で契約変更入力中へ');
    await menuAction(page, a.appId, 'cancel', '契約変更は不要になりました'); await expectStatus(page, '10701', '契約変更の取消 → 審査完了へ復元');
    console.log('    ', (await alerts(page)).slice(0, 40));

    // ---------- 契約変更（基準超）→ 20201 → 承認 → 同意 → 20501 → 審査完了 → 20701 ----------
    await menuAction(page, a.appId, 'startChange'); await expectStatus(page, '20101', '契約変更開始 2 回目');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/change');
    await page.fill('#basicFee', '5000000'); await page.click('button[value=confirm]'); await page.waitForURL('**/change/confirm');
    await shot(page, 'SC09-over');
    await page.click('button[value=confirm]'); await page.waitForLoadState('networkidle');
    await expectStatus(page, '20201', '契約変更確定（基準超）');
    await applyApproval(page, a.appId, { approvers: [] }); await expectStatus(page, '20301', '契約変更 回付先なし申請');
    // 契約変更の同意はポータルから
    await apPage.goto(BASE + '/my/menu'); await apPage.click('#tile-consent a'); await apPage.waitForURL('**/consent'); await shot(apPage, 'AP01-portal-change');
    await apPage.click('label[for=checked]'); await apPage.click('#confirmBtn'); await apPage.waitForLoadState('networkidle');
    await apPage.click('label[for=agreed]'); await apPage.click('#agreeBtn'); await apPage.waitForURL('**/my/menu**');
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '20501', '契約変更 同意（ポータル）');
    await applyApproval(page, a.appId); await logout(page);
    await login(page, 'A002'); await approve(page, a.appId, 'approve'); await logout(page);
    await login(page, 'A003'); await approve(page, a.appId, 'review'); await expectStatus(page, '20601', '契約変更 審査申請');
    await mockResult(page, a.appNo, 'COMPLETED');
    await page.goto(BASE + '/emp/applications/' + a.appId); await expectStatus(page, '20701', '契約変更 審査完了'); await shot(page, 'SC03-20701');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/menu'); await shot(page, 'SC14-20701');
    await checkTiles(page, a.appId, '20701', ['新規申込', '契約変更1', '契約変更2', '契約変更3']);
    if (await page.locator('#sec-body-3.show').count() !== 1 || await page.locator('#sec-body-0.show').count() !== 0) throw new Error('20701: only the latest section should be expanded');
    await page.click('#sec-0 .section-toggle'); await page.waitForSelector('#sec-body-0.show'); await shot(page, 'SC14-20701-expanded');
    console.log('OK  領域の折りたたみ切替: 新規申込を展開 →', (await page.locator('#sec-0 .section-toggle').textContent()).trim());
    await page.goto(BASE + '/emp/applications/' + a.appId + '/change/confirm?group=1'); await shot(page, 'SC09-group1');
    if (await page.locator('#modifyBtn:disabled').count() !== 1 || !(await page.title()).includes('契約変更1')) throw new Error('group 1 should be read-only');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/confirm?group=0');
    if (await page.locator('#modifyBtn:disabled').count() !== 1) throw new Error('group 0 should be read-only after contract change');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/approval?group=3'); await shot(page, 'SC06-group3');
    const histRows = await page.locator('table').last().locator('tbody tr').count();
    if (histRows < 1) throw new Error('group 3 approval history should not be empty');
    console.log('OK  過去の領域の参照（SC05／SC09／SC06）: 契約変更3 の履歴行 =', histRows);
    await logout(page);

    // ---------- 申込取消 ----------
    await login(page, 'A001');
    // 同じ申込者の 2 件目：申込者番号なしで同じメールアドレスを入れると W003（アカウント発行済み）→ 申込者番号を入れて既存のアカウントに紐づける
    await page.goto(BASE + '/emp/applications/new');
    await fillApplicant(page, { name: '山田 太郎', mail: 'TARO.YAMADA@example.com' }); await page.fill('#productCd', 'PRD001'); await page.fill('#basicFee', '800000');
    await page.click('button[value=save]'); await page.waitForLoadState('networkidle');
    if (await page.locator('#duplicateWarning').count() !== 1) throw new Error('duplicate mail should show W003');
    const dupNo = (await page.locator('#duplicateWarning code').first().textContent()).trim();
    if (dupNo !== a.applicantNo) throw new Error('W003 should list ' + a.applicantNo + ' but got ' + dupNo);
    await shot(page, 'SC04-duplicate');
    console.log('OK  同じメールアドレスの発行済みアカウントを警告（W003）: ' + dupNo);
    // 別の申込者として一時保存（未紐づけ）→ 再表示で紐づけ候補を案内 → 申込者番号を入れて紐づける
    await page.locator('label[for=allowDuplicate]').click();
    await page.click('button[value=save]'); await page.waitForURL(/\/edit$/);
    if (await page.locator('#duplicateWarning').count() !== 0) throw new Error('allowDuplicate should save without W003');
    if (!(await page.locator('#linkCandidates').textContent()).includes(dupNo)) throw new Error('unlinked application should show link candidate ' + dupNo);
    console.log('OK  未紐づけの申込の再表示で紐づけ候補を案内: ' + dupNo);
    await page.fill('#applicantNo', dupNo); await page.fill('#optionFee', '0'); await page.fill('#handlingFee', '0');
    await page.click('button[value=confirm]'); await page.waitForURL('**/confirm');
    await page.click('#confirmBtn'); await page.waitForURL(/\/menu$/);
    const c = { appId: page.url().split('/').slice(-2)[0] };
    if (await applicantNoOnMenu(page) !== a.applicantNo) throw new Error('second application should be linked to the existing account');
    console.log('OK  2 件目の申込を既存の申込者アカウントに紐づけ: ' + a.applicantNo);
    await menuAction(page, c.appId, 'cancel', '申込者の都合により取消'); await expectStatus(page, '90101', '申込取消');
    await shot(page, 'SC14-90101');
    await logout(page);

    // ---------- 会社B（事前確認あり）----------
    await login(page, 'B001');
    const b = await createApplication(page, { name: '佐藤 次郎', kana: 'サトウ ジロウ', mail: 'jiro.sato@example.com', tel: '06-0000-0003', address: '大阪府大阪市北区梅田3-3-3' }, '3000000', '0');
    await applyApproval(page, b.appId, { approvers: [] }); await expectStatus(page, '10301', 'B 回付先なし');
    await page.goto(BASE + '/emp/applications/' + b.appId + '/menu'); b.applicantNo = await applicantNoOnMenu(page);
    if (!b.applicantNo) throw new Error('B account should be issued when reaching 10301');
    const urlB = await consentUrlFor(page, b.appNo);
    await applicantConsent(ctx, urlB, {});
    await page.goto(BASE + '/emp/applications/' + b.appId); await expectStatus(page, '10401', 'B 同意 → 事前確認待ち');
    await mockResult(page, b.appNo, 'NG', '契約期間を確認してください');
    await page.goto(BASE + '/emp/applications/' + b.appId); await expectStatus(page, '10402', 'B 事前確認 NG');
    await page.goto(BASE + '/emp/applications/' + b.appId + '/revise'); await shot(page, 'SC07-fix');
    await page.fill('#contractEndDate', '2027/12/31'); await page.click('button:has-text("確定")'); await page.waitForLoadState('networkidle');
    await expectStatus(page, '10401', 'B 修正対応（基準内）→ 事前確認再依頼');
    await mockResult(page, b.appNo, 'OK');
    await page.goto(BASE + '/emp/applications/' + b.appId); await expectStatus(page, '10501', 'B 事前確認 OK');
    // 全体修正は SC05「修正（全体修正）」から（メニューに入力画面への遷移はない）
    await page.goto(BASE + '/emp/applications/' + b.appId + '/confirm'); await shot(page, 'SC05-10501');
    page.once('dialog', d => d.accept());
    await page.click('#modifyBtn'); await page.waitForURL('**/edit');
    await page.goto(BASE + '/emp/applications/' + b.appId + '/menu'); await expectStatus(page, '10101', 'B 全体修正（SC05 の修正）');
    await page.goto(BASE + '/emp/applications/' + b.appId + '/edit');
    // アカウント発行後も申込者情報は申込データとして全体修正で変更できる（アカウントは紐づいたまま）
    if (await page.locator('#applicantName').count() !== 1 || !(await page.locator('#accountNote').textContent()).includes(b.applicantNo)) throw new Error('applicant data should be editable as application data');
    await page.fill('#address', '大阪府大阪市北区梅田9-9-9'); await page.fill('#remarks', '全体修正で備考を変更'); await page.click('button[value=confirm]'); await page.waitForURL('**/confirm');
    const changedCells = await page.locator('td.changed').count();
    if (changedCells < 2) throw new Error('full revise: changed applicant address and remarks should be red, got ' + changedCells);
    console.log('OK  全体修正の変更項目を赤字表示: ' + changedCells + ' 項目'); await shot(page, 'SC05-fullrevise-diff');
    await page.click('button[value=confirm]'); await page.waitForLoadState('networkidle'); await expectStatus(page, '10201', 'B 再確定');
    await applyApproval(page, b.appId, { approvers: [] }); await expectStatus(page, '10301', 'B 回付先なし 2');
    await menuAction(page, b.appId, 'resendConsent'); console.log('    resend:', (await alerts(page)).slice(0, 40));
    await menuAction(page, b.appId, 'pullBack'); await expectStatus(page, '10201', 'B 引戻し');
    await shot(page, 'SC14-B-final');
    // 一括取込
    await page.goto(BASE + '/emp/import');
    const csv = '\ufeff申込者番号,申込者名,申込者名カナ,メールアドレス,電話番号,住所,商品コード,基本料金,オプション料金,事務手数料,契約開始日,契約終了日,備考\n'
      + ',高橋 美咲,タカハシ ミサキ,misaki.takahashi@example.com,052-000-0004,愛知県名古屋市中区栄4-4-4,PRD002,500000,0,0,2026-10-01,2027-09-30,新規の申込者\n'
      + b.applicantNo + ',佐藤 次郎,サトウ ジロウ,jiro.sato@example.com,,,PRD001,700000,0,0,2026-10-01,2027-09-30,発行済みのアカウントに紐づけ\n'
      + ',高橋 美咲,タカハシ ミサキ,misaki.takahashi@example.com,,,PRD001,300000,0,0,,,同じファイルの同じ申込者（アカウントは一次承認で発行）\n'
      + ',田中 健一,タナカ ケンイチ,kenichi.tanaka@example.com,,,PRD001,abc,0,0,2026-10-01,2026-09-01,エラー行\n'
      + 'C0000009999,,,,,,PRD001,1000,0,0,,,存在しない申込者\n'
      + ',別人 花子,,jiro.sato@example.com,,,PRD001,1000,0,0,,,登録済みと同じメールアドレス\n';
    await page.setInputFiles('input[name=file]', { name: 'import.csv', mimeType: 'text/csv', buffer: Buffer.from(csv, 'utf8') });
    await page.click('button:has-text("取込実行")'); await page.waitForLoadState('networkidle'); await shot(page, 'SC10');
    const importText = (await page.locator('.card-body').nth(1).textContent()).replace(/\s+/g, ' ');
    console.log('    import:', importText.slice(0, 120));
    if (!/成功：\s*3/.test(importText) || !/エラー：\s*3/.test(importText)) throw new Error('import should succeed 3 and fail 3: ' + importText.slice(0, 200));
    console.log('OK  一括取込：申込者情報は申込データとして取込、申込者番号の行は既存アカウントに紐づけ、エラー 3 行（形式・存在しない番号・発行済みと同じメール）');
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
    await shot(page, 'FAILED'); await shot(apPage, 'FAILED-portal');
    console.log('alerts:', await alerts(page));
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
})();
