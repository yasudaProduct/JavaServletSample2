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
async function applicantConsent(ctx, url, { modifyFirst = false, agree = true, returnReason = null, expectTexts = [] } = {}) {
  const page = await ctx.newPage();
  await page.goto(url);
  const expectOnPage = async (when) => {
    const t = await page.locator('.version-table').first().textContent();
    for (const x of expectTexts) { if (!t.includes(x)) throw new Error(`AP01 (${when}) should show ${x}`); }
    // 担当（申込受付会社）は社員向けの情報。社員が同じブラウザで開いても申込者向け画面には出さない
    if (t.includes('担当（申込受付会社）')) throw new Error(`AP01 (${when}) must not show the employee-side assignment`);
  };
  await expectOnPage('表示');
  if (modifyFirst) {
    await page.click('a:has-text("内容を修正する")');
    // 会社B の追加項目は申込者には表示だけ（AP02 に入力欄はない）
    if (await page.locator('#corporateNo, #installPlace, #contactMemo').count() !== 0) throw new Error('AP02 must not show company B extra inputs');
    await page.fill('#remarks', '申込者が備考を修正しました');
    await page.click('button:has-text("保存")');
    await page.waitForLoadState('networkidle');
    await expectOnPage('AP02 保存後');
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
// 申込者情報（申込データ）の入力。新規申込は同じ氏名・メールアドレスでも別の申込者になる
async function fillApplicant(page, applicant) {
  await page.fill('#applicantName', applicant.name); await page.fill('#applicantKana', applicant.kana || '');
  await page.fill('#mailAddress', applicant.mail); await page.fill('#telNo', applicant.tel || ''); await page.fill('#address', applicant.address || '');
}
async function applicantNoOnMenu(page) { const m = (await page.locator('.menu-head').textContent()).match(/C\d{10}/); return m ? m[0] : null; }
async function createApplication(page, applicant, basic, option, extra = null) {
  await page.goto(BASE + '/emp/applications/new');
  await fillApplicant(page, applicant); await page.fill('#productCd', 'PRD001');
  if (extra) {
    if (!(await page.locator('#companyExtra').isVisible())) throw new Error('SC04: company B extra items should be visible for company B');
    await page.fill('#corporateNo', extra.corporateNo); await page.fill('#installPlace', extra.installPlace); await page.fill('#contactMemo', extra.contactMemo);
  } else if (await page.locator('#companyExtra').isVisible()) {
    throw new Error('SC04: company B extra items should be hidden for company A');
  }
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
// SC16 部署マスタに部署を追加する
async function addDepartment(page, companyDiv, deptCd, deptName) {
  await page.goto(BASE + '/emp/master/departments');
  await page.selectOption('#newDepartment [name=companyDiv]', companyDiv); await page.fill('#newDepartment [name=deptCd]', deptCd); await page.fill('#newDepartment [name=deptName]', deptName);
  await page.click('#newDepartment button[type=submit]'); await page.waitForLoadState('networkidle');
  if (!(await alerts(page)).includes('登録しました') || await page.locator(`#departmentTable input[name=deptName][value="${deptName}"]`).count() !== 1) throw new Error('SC16: add department failed ' + deptCd);
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
    if (await page.locator('#tile-additional.tile-disabled').count() !== 1) throw new Error('10201: 追加申込 should be disabled until 審査完了');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/additional');
    if (page.url().endsWith('/additional') || !(await alerts(page)).includes('現在のステータスではこの操作はできません')) throw new Error('10201: direct access to 追加申込 should be rejected (E101)');
    console.log('OK  追加申込は審査完了まで不可（タイル非活性、URL 直接指定は E101）');
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
    if (await page.locator('#companyExtra, #corporateNo').count() !== 0) throw new Error('SC07: company A must not show company B extra items');
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
    await applyApproval(page, a.appId, { approvers: ['会社A 承認 二郎（営業部）', '会社A 承認 一郎（営業部）'] }); await expectStatus(page, '10502', '最終承認申請（逆順の回付先）');
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

    // ---------- 追加申込（同じ申込者）：元の申込が審査完了のときだけ。アカウントを引き継ぎ、申込者情報は複写した別データ ----------
    await login(page, 'A001');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/menu');
    if (await page.locator('#tile-additional.tile-disabled').count() !== 0) throw new Error('20701: 追加申込 should be enabled');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/additional');
    if ((await page.inputValue('#applicantName')) !== TARO.name || (await page.inputValue('#mailAddress')) !== TARO.mail) throw new Error('additional: applicant data should be copied from the source');
    if (!(await page.locator('#accountNote').textContent()).includes(a.applicantNo)) throw new Error('additional: should show the inherited account ' + a.applicantNo);
    await page.fill('#address', '東京都港区芝公園4-2-8'); await page.fill('#remarks', '追加申込（住所を変更）');
    await shot(page, 'SC04-additional');
    await page.click('button[value=confirm]'); await page.waitForURL('**/confirm');
    await page.click('#confirmBtn'); await page.waitForURL(/\/menu$/);
    const add = { appId: page.url().split('/').slice(-2)[0] };
    if (await applicantNoOnMenu(page) !== a.applicantNo) throw new Error('additional application should inherit the account ' + a.applicantNo);
    await shot(page, 'SC14-additional');
    await page.goto(BASE + '/emp/applications/' + a.appId + '/change/confirm?group=3');
    const srcText = await page.locator('.version-table').first().textContent();
    if (srcText.includes('芝公園') || !srcText.includes('丸の内')) throw new Error('editing the additional application must not change the source application data');
    console.log('OK  追加申込：アカウント ' + a.applicantNo + ' を引き継ぎ、申込者情報は複写した別データ（元の申込の住所は変わらない）');

    // ---------- 同じ氏名・メールアドレスの新規申込は別の申込者 → 申込取消 ----------
    const c = await createApplication(page, { name: TARO.name, mail: 'TARO.YAMADA@example.com' }, '800000', '0');
    if (await applicantNoOnMenu(page) !== null) throw new Error('new application should not be linked to an existing account');
    await applyApproval(page, c.appId, { approvers: [] }); await expectStatus(page, '10301', '同じ氏名・メールの新規申込 回付先なし');
    await page.goto(BASE + '/emp/applications/' + c.appId + '/menu'); c.applicantNo = await applicantNoOnMenu(page);
    if (!c.applicantNo || c.applicantNo === a.applicantNo) throw new Error('same name/mail new application should get its own account, got ' + c.applicantNo);
    console.log('OK  同じ氏名・メールアドレスの新規申込は別の申込者（新しいアカウント ' + c.applicantNo + '。元は ' + a.applicantNo + '）');
    await menuAction(page, c.appId, 'cancel', '申込者の都合により取消'); await expectStatus(page, '90101', '申込取消');
    await shot(page, 'SC14-90101');
    if (await page.locator('#tile-additional.tile-disabled').count() !== 1) throw new Error('90101: 追加申込 should be disabled');
    await logout(page);

    // ---------- 担当（申込受付会社の会社 > 部署 > 担当者）：部署マスタ（SC16）と、担当者を自分以外にした申込 ----------
    await login(page, 'A009');
    await addDepartment(page, '1', '110', '営業第二部');
    await addDepartment(page, '2', '210', '法人営業部');
    await page.selectOption('#newDepartment [name=companyDiv]', '1'); await page.fill('#newDepartment [name=deptCd]', '110'); await page.fill('#newDepartment [name=deptName]', '重複');
    await page.click('#newDepartment button[type=submit]'); await page.waitForLoadState('networkidle');
    if (!(await page.locator('.invalid-feedback:visible').allTextContents()).join().includes('すでに登録')) throw new Error('SC16: duplicate department code should be rejected');
    await page.goto(BASE + '/emp/master/departments'); await shot(page, 'SC16');
    // 社員の所属部署は部署マスタから選ぶ（会社で絞り込み）
    await page.goto(BASE + '/emp/master/employees/new');
    await page.fill('[name=employeeNo]', 'A004'); await page.fill('[name=employeeName]', '会社A 担当 四葉'); await page.fill('[name=password]', 'password');
    await page.selectOption('#companyDiv', '2');
    if ((await page.locator('#deptCd option:not([disabled])').allTextContents()).join() !== '営業部（200）,法人営業部（210）') throw new Error('SC11: departments should be filtered by company');
    await page.selectOption('#companyDiv', '1'); await page.selectOption('#deptCd', '110'); await page.selectOption('[name=roleCd]', '01');
    await page.fill('[name=mailAddress]', 'a004@example.com'); await page.check('#validFlg');
    await page.click('button:has-text("保存")'); await page.waitForURL('**/emp/master/employees');
    if (!(await page.locator('tr', { hasText: 'A004' }).textContent()).includes('営業第二部（110）')) throw new Error('SC11: A004 should belong to 営業第二部');
    console.log('OK  SC16 部署マスタ：部署を追加（重複は E006）、社員の部署は会社で絞り込んだ部署マスタから選択');
    await logout(page);
    // 担当者 A001 が、担当部署を 営業第二部、担当者を A004 にして新規申込を保存する（確認ダイアログ → I020 → 一覧へ。以降は A004 だけが操作できる）
    await login(page, 'A001');
    await page.goto(BASE + '/emp/applications/new');
    const defaults = [await page.inputValue('#companyDiv'), await page.inputValue('#deptCd'), await page.locator('#ownerEmployeeId option:checked').textContent()];
    if (defaults[0] !== '1' || defaults[1] !== '100' || !defaults[2].includes('会社A 担当 太郎')) throw new Error('SC04: assignment should default to the login user, got ' + defaults.join(' / '));
    await fillApplicant(page, { name: '伊藤 さくら', kana: 'イトウ サクラ', mail: 'sakura.ito@example.com' }); await page.fill('#productCd', 'PRD001');
    await page.fill('#basicFee', '600000'); await page.fill('#optionFee', '0'); await page.fill('#handlingFee', '0');
    // 会社B の追加項目は担当会社を会社B にしたときだけ表示・送信する
    await page.selectOption('#companyDiv', '2');
    if (!(await page.locator('#companyExtra').isVisible()) || await page.locator('#corporateNo').isDisabled()) throw new Error('SC04: extra items should appear when company B is selected');
    await page.selectOption('#companyDiv', '1');
    if (await page.locator('#companyExtra').isVisible() || !(await page.locator('#corporateNo').isDisabled())) throw new Error('SC04: extra items should be hidden and disabled for company A');
    console.log('OK  SC04：会社B の追加項目は担当会社の選択で表示・非表示（会社A では送信しない）');
    await page.selectOption('#deptCd', '110'); await page.selectOption('#ownerEmployeeId', { label: '会社A 担当 四葉（営業第二部）' });
    await page.locator('#assignCard').scrollIntoViewIfNeeded(); await shot(page, 'SC04-assign');
    let dialogText = '';
    page.once('dialog', d => { dialogText = d.message(); d.accept(); });
    await page.click('button[value=save]'); await page.waitForURL('**/emp/applications');
    if (!dialogText.includes('会社A 担当 四葉')) throw new Error('SC04: confirm dialog expected when the owner is someone else, got ' + dialogText);
    const handOver = await alerts(page);
    const d4No = (handOver.match(/AP\d{10}/) || [])[0];
    if (!d4No || !handOver.includes('会社A 担当 四葉 さん')) throw new Error('I020 expected, got ' + handOver);
    if (await page.locator('a', { hasText: d4No }).count() !== 0) throw new Error('the inputter should no longer see the application in the list');
    console.log('OK  担当者を自分以外にして保存：確認ダイアログ → ' + handOver.slice(0, 60));
    await logout(page);
    await login(page, 'A004');
    await page.goto(BASE + '/emp/applications?search=1');
    const d4Href = await page.locator('a', { hasText: d4No }).getAttribute('href');
    const d4 = { appId: d4Href.split('/').slice(-2)[0], appNo: d4No };
    await page.goto(BASE + '/emp/applications/' + d4.appId + '/menu'); await expectStatus(page, '10101', '担当者 A004 の申込（入力中）');
    if ((await page.locator('#assignHead').textContent()).replace(/\s+/g, ' ').trim() !== '会社A ／ 営業第二部 ／ 会社A 担当 四葉') throw new Error('SC14 assignment header: ' + await page.locator('#assignHead').textContent());
    await page.goto(BASE + '/emp/applications/' + d4.appId + '/edit');
    await page.click('button[value=confirm]'); await page.waitForURL('**/confirm');
    const assignRows = (await page.locator('.version-table tr', { hasText: '部署' }).first().textContent()).replace(/\s+/g, ' ');
    if (!assignRows.includes('営業第二部（110）')) throw new Error('SC05 should show the assigned department: ' + assignRows);
    await page.click('button[value=confirm]'); await page.waitForLoadState('networkidle'); await expectStatus(page, '10201', '担当者 A004 が確定');
    await logout(page);
    await login(page, 'A001');
    await page.goto(BASE + '/emp/applications/' + d4.appId + '/menu');
    if (page.url().includes('/' + d4.appId + '/') || !(await alerts(page)).includes('権限')) throw new Error('A001 should not be able to open the application owned by A004: ' + page.url() + ' ' + await alerts(page));
    console.log('OK  担当者 A004 が操作（10101 → 10201）、入力した A001 は開けない（' + (await alerts(page)).slice(0, 40) + '）');
    await logout(page);

    // ---------- 会社B（事前確認あり）----------
    await login(page, 'B001');
    const B_EXTRA = { corporateNo: '1234567890123', installPlace: '大阪支店 3 階 サーバ室', contactMemo: '平日 9 時〜17 時\n総務部 経由' };
    const b = await createApplication(page, { name: '佐藤 次郎', kana: 'サトウ ジロウ', mail: 'jiro.sato@example.com', tel: '06-0000-0003', address: '大阪府大阪市北区梅田3-3-3' }, '3000000', '0', B_EXTRA);
    await page.goto(BASE + '/emp/applications/' + b.appId + '/confirm');
    const bView = await page.locator('.version-table').first().textContent();
    if (!bView.includes('会社B 追加項目') || !bView.includes(B_EXTRA.corporateNo) || !bView.includes(B_EXTRA.installPlace)) throw new Error('SC05: company B extra items should be shown');
    console.log('OK  会社B の新規申込：追加項目（法人番号・設置場所・窓口メモ）を入力して SC05 に表示');
    await applyApproval(page, b.appId, { approvers: [] }); await expectStatus(page, '10301', 'B 回付先なし');
    await page.goto(BASE + '/emp/applications/' + b.appId + '/menu'); b.applicantNo = await applicantNoOnMenu(page);
    if (!b.applicantNo) throw new Error('B account should be issued when reaching 10301');
    const urlB = await consentUrlFor(page, b.appNo);
    await applicantConsent(ctx, urlB, { modifyFirst: true, expectTexts: [B_EXTRA.corporateNo, B_EXTRA.installPlace] });
    console.log('OK  申込者画面：会社B の追加項目は表示だけ（AP02 に入力欄なし、申込者の保存後も値は残る）');
    await page.goto(BASE + '/emp/applications/' + b.appId); await expectStatus(page, '10401', 'B 同意 → 事前確認待ち');
    await mockResult(page, b.appNo, 'NG', '契約期間を確認してください');
    await page.goto(BASE + '/emp/applications/' + b.appId); await expectStatus(page, '10402', 'B 事前確認 NG');
    await page.goto(BASE + '/emp/applications/' + b.appId + '/revise'); await shot(page, 'SC07-fix');
    if (await page.inputValue('#corporateNo') !== B_EXTRA.corporateNo) throw new Error('SC07: company B extra items should be editable with current values');
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
    await page.fill('#address', '大阪府大阪市北区梅田9-9-9'); await page.fill('#remarks', '全体修正で備考を変更');
    // 担当部署も入力中（全体修正後の再入力）なら変えられる
    await page.selectOption('#deptCd', '210');
    // 会社B の追加項目も入力中に変えられる（書式の誤りは E003）
    await page.fill('#corporateNo', '12345'); await page.click('button[value=confirm]'); await page.waitForLoadState('networkidle');
    if (!(await page.locator('#corporateNo.is-invalid').count())) throw new Error('SC04: invalid corporate number should be rejected');
    await page.fill('#corporateNo', '9876543210987');
    await page.click('button[value=confirm]'); await page.waitForURL('**/confirm');
    const changedCells = await page.locator('td.changed').count();
    if (changedCells < 4) throw new Error('full revise: changed department, applicant address, remarks and corporate number should be red, got ' + changedCells);
    if (!(await page.locator('td.changed', { hasText: '9876543210987' }).count())) throw new Error('full revise: the changed corporate number should be red');
    if (!(await page.locator('td.changed', { hasText: '法人営業部（210）' }).count())) throw new Error('full revise: the changed department should be red');
    console.log('OK  全体修正の変更項目を赤字表示: ' + changedCells + ' 項目'); await shot(page, 'SC05-fullrevise-diff');
    await page.click('button[value=confirm]'); await page.waitForLoadState('networkidle'); await expectStatus(page, '10201', 'B 再確定');
    await applyApproval(page, b.appId, { approvers: [] }); await expectStatus(page, '10301', 'B 回付先なし 2');
    await menuAction(page, b.appId, 'resendConsent'); console.log('    resend:', (await alerts(page)).slice(0, 40));
    await menuAction(page, b.appId, 'pullBack'); await expectStatus(page, '10201', 'B 引戻し');
    await shot(page, 'SC14-B-final');
    // 一括取込
    await page.goto(BASE + '/emp/import');
    const csv = '\ufeff申込者名,申込者名カナ,メールアドレス,電話番号,住所,商品コード,基本料金,オプション料金,事務手数料,契約開始日,契約終了日,備考\n'
      + '高橋 美咲,タカハシ ミサキ,misaki.takahashi@example.com,052-000-0004,愛知県名古屋市中区栄4-4-4,PRD002,500000,0,0,2026-10-01,2027-09-30,新規申込\n'
      + '佐藤 次郎,サトウ ジロウ,jiro.sato@example.com,,,PRD001,700000,0,0,2026-10-01,2027-09-30,既存の申込と同じ氏名・メールアドレス（別の申込者）\n'
      + '高橋 美咲,タカハシ ミサキ,misaki.takahashi@example.com,,,PRD001,300000,0,0,,,同じファイルの同じ氏名・メールアドレス（別の申込者）\n'
      + '田中 健一,タナカ ケンイチ,kenichi.tanaka@example.com,,,PRD001,abc,0,0,2026-10-01,2026-09-01,エラー行\n'
      + '鈴木 一郎,,,,,PRD001,1000,0,0,,,メールアドレスなし\n';
    await page.setInputFiles('input[name=file]', { name: 'import.csv', mimeType: 'text/csv', buffer: Buffer.from(csv, 'utf8') });
    await page.click('button:has-text("取込実行")'); await page.waitForLoadState('networkidle'); await shot(page, 'SC10');
    const importText = (await page.locator('.card-body').nth(1).textContent()).replace(/\s+/g, ' ');
    console.log('    import:', importText.slice(0, 120));
    if (!/成功：\s*3/.test(importText) || !/エラー：\s*2/.test(importText)) throw new Error('import should succeed 3 and fail 2: ' + importText.slice(0, 200));
    console.log('OK  一括取込：申込者情報は申込データとして取込、同じ氏名・メールアドレスの行も別の申込者として登録、エラー 2 行（形式・メールアドレスなし）');
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
