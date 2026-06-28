#!/usr/bin/env node

/**
 * MRI 全流程自动化演示脚本（视频录制版）
 *
 * 运行时长约 10-20 分钟，操作节奏模拟真人：
 *   - 每一步之间有自然停顿
 *   - 表单逐字输入，速度接近真人键盘
 *   - 点击前先 hover，停顿后点击
 *
 * 流程：
 *   1. 患者注册
 *   2. 患者登录
 *   3. 患者建档（含禁忌症）
 *   4. 患者提交检查申请
 *   5. Admin 登录
 *   6. Admin 排程
 *   7. Admin 开始 → 完成检查
 *   8. Admin 创建影像检查
 *   9. Admin 创建序列 + 上传真实图片（可重复上传）
 *  10. Admin 编写报告 → 提交 → 审核 → 发布
 *  11. 患者重新登录 → 工作台
 *  12. 患者查看报告
 *  13. 患者查看影像
 *
 * 用法：
 *   node demo-playwright.js
 *   node demo-playwright.js --headless
 */

import { chromium } from 'playwright';
import { mkdirSync, readFileSync } from 'fs';
import { resolve } from 'path';

const OUT_DIR = resolve('target-demo-output');
mkdirSync(OUT_DIR, { recursive: true });

const BASE  = 'http://localhost:5173';
const ARGS   = process.argv.slice(2);
const HEADLESS = ARGS.includes('--headless');

// ── 真实图片 ──────────────────────────────────────────
const REAL_IMAGE = 'C:/Users/l2653/Pictures/HobbyTown_USA_Oshkosh_interior_under_construction_2002_(The_Backrooms).jpg';

// ── 测试账号 ──────────────────────────────────────────
const USER = `demo${Date.now()}`;
const NAME = '张小明';
const PASS = 'test123';

// ═══════════════════════════════════════════════════════
//  人类化操作层
// ═══════════════════════════════════════════════════════

// 短暂思考/观看停顿（秒）—— 总时长约 12-15 分钟
const ST = {
  quick:  () => 1.0 + Math.random() * 1.5,   // 1.0-2.5s  敲键盘间隔
  pause:  () => 3.0 + Math.random() * 3.0,   // 3.0-6.0s  看一眼屏幕
  read:   () => 5.0 + Math.random() * 5.0,   // 5.0-10.0s 通读内容
  think:  () => 7.0 + Math.random() * 7.0,   // 7.0-14.0s 思考/决策
  scene:  () => 10.0 + Math.random() * 10.0, // 10.0-20.0s 切场景/深呼吸
};

async function sec(page, fn) {
  await page.waitForTimeout(fn() * 1000);
}

// 逐字输入 —— 中英文通用，速度接近真人
async function type(page, selector, text) {
  const el = page.locator(selector);
  await el.click();
  await el.fill('');
  for (const ch of text) {
    await page.keyboard.type(ch);
    await page.waitForTimeout(60 + Math.random() * 140); // 60-200ms/字
  }
}

// 人类化点击：先 hover → 停顿 → 点击
async function click(page, locator) {
  await locator.waitFor({ state: 'visible', timeout: 8000 });
  await locator.hover();
  await page.waitForTimeout(150 + Math.random() * 300);
  await locator.click();
}

// ── 高级原语 ──────────────────────────────────────────

async function navTo(page, label) {
  const lnk = page.locator('.nav-list a').filter({ hasText: label });
  await sec(page, ST.quick);
  await click(page, lnk);
}

async function btnClick(page, text) {
  const b = page.locator('button').filter({ hasText: text }).first();
  await sec(page, ST.quick);
  await click(page, b);
}

async function loginAs(page, username, password) {
  await sec(page, ST.scene);
  await page.locator('.login-tabs button').filter({ hasText: '登录' }).click();
  await sec(page, ST.pause);
  await type(page, 'input[name="username"]', username);
  await sec(page, ST.quick);
  await type(page, 'input[name="password"]', password);
  await sec(page, ST.pause);
  await click(page, page.locator('form.login-card .button'));
  await page.waitForSelector('.sidebar', { timeout: 15000 });
  await sec(page, ST.scene);
}

// ── 工具 ──────────────────────────────────────────────

const log = (s, m) => console.log(`\x1b[1m[${new Date().toLocaleTimeString('zh-CN', { hour12: false })}] [${s}]\x1b[0m ${m}`);

async function snap(page, name) {
  await page.screenshot({ path: resolve(OUT_DIR, `${name}.png`), fullPage: true });
  console.log(`  📸 ${name}.png`);
}

// ═══════════════════════════════════════════════════════
//  主流程
// ═══════════════════════════════════════════════════════

async function main() {
  console.log('\n╔══════════════════════════════════════════════╗');
  console.log('║  🏥 MRI 影像管理系统 · 全流程自动化演示   ║');
  console.log('╚══════════════════════════════════════════════╝\n');

  const browser = await chromium.launch({
    headless: HEADLESS,
    slowMo: HEADLESS ? 0 : 40,
  });
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' });

  try {
    // ── 1. 患者注册 ──────────────────────────────────
    log('01/13', '患者注册');
    const pp = await ctx.newPage();
    await pp.goto(`${BASE}/#/login`, { waitUntil: 'networkidle' });
    await sec(pp, ST.scene);

    await pp.locator('.login-tabs button').filter({ hasText: '患者注册' }).click();
    await sec(pp, ST.pause);

    await type(pp, 'input[name="displayName"]', NAME);
    await sec(pp, ST.quick);
    // 手动覆写用户名
    await pp.locator('input[name="username"]').click();
    await pp.locator('input[name="username"]').selectText();
    await type(pp, 'input[name="username"]', USER);
    await type(pp, 'input[name="password"]', PASS);
    await type(pp, 'input[name="confirmPassword"]', PASS);
    await sec(pp, ST.pause);

    await click(pp, pp.locator('form.login-card .button:not(:disabled)'));
    await sec(pp, ST.scene);
    log('01/13', '注册成功 ✅');

    // ── 2. 患者登录 ──────────────────────────────────
    log('02/13', '患者登录');
    await pp.goto(`${BASE}/#/login`, { waitUntil: 'networkidle' });
    await loginAs(pp, USER, PASS);
    await snap(pp, '02-patient-logged-in');
    log('02/13', '患者登录成功 ✅');

    // ── 3. 患者建档 ──────────────────────────────────
    log('03/13', '患者建档');
    try {
      await pp.waitForSelector('text=首次患者建档', { timeout: 3000 });
    } catch {
      await navTo(pp, '我的资料');
    }
    await sec(pp, ST.read);

    await type(pp, 'input[name="name"]', NAME);
    await pp.locator('select[name="gender"]').selectOption({ label: '男' });
    await sec(pp, ST.quick);
    await pp.locator('input[name="birthDate"]').fill('1995-03-15');
    await type(pp, 'input[name="phone"]', '13800138000');
    await pp.locator('select[name="hasContraindications"]').selectOption({ label: '有禁忌症' });
    await sec(pp, ST.think);

    // 禁忌症明细
    await sec(pp, ST.pause);
    await pp.locator('select[name="type"]').first().selectOption({ label: '心脏起搏器' });
    await type(pp, 'input[name="description"]', 'Medtronic A2DR01 2023年植入');
    await sec(pp, ST.read);

    await btnClick(pp, '提交建档');
    await sec(pp, ST.scene);
    await snap(pp, '03-patient-profile');
    log('03/13', '建档完成 ✅');

    // ── 4. 患者提交检查申请 ──────────────────────────
    log('04/13', '患者提交检查申请');
    await navTo(pp, '申请检查');
    await pp.waitForSelector('text=填写检查申请', { timeout: 5000 });
    await sec(pp, ST.read);

    await type(pp, 'input[name="examItem"]', '头颅MRI平扫');
    await sec(pp, ST.quick);
    await type(pp, 'input[name="clinicalDiagnosis"]', '反复头痛3月，疑似颅内占位');
    await sec(pp, ST.quick);
    await pp.locator('select[name="priority"]').selectOption({ label: '加急' });
    await sec(pp, ST.think);

    await btnClick(pp, '提交申请');
    await pp.waitForSelector('text=我的检查', { timeout: 5000 });
    await sec(pp, ST.scene);
    await snap(pp, '04-patient-exam-submitted');

    // 获取真实 patientId
    const pToken = await pp.evaluate(() => localStorage.getItem('mri.frontend.token'));
    const meResp = await fetch('http://localhost:8080/api/patients/me', {
      headers: { Authorization: `Bearer ${pToken}` },
    }).then(r => r.json()).catch(() => ({}));
    const PATIENT_ID = meResp?.data?.patient?.id;
    console.log(`  → 患者真实 ID: ${PATIENT_ID}`);
    await pp.close();

    // ── 5. Admin 登录 ─────────────────────────────────
    log('05/13', 'Admin 登录');
    const ap = await ctx.newPage();
    await ap.goto(`${BASE}/#/login`, { waitUntil: 'domcontentloaded' });
    await ap.evaluate(() => localStorage.clear());
    await ap.goto(`${BASE}/#/login`, { waitUntil: 'networkidle' });
    await loginAs(ap, 'admin', 'admin123');

    // reload 拿到患者刚注册的数据
    await ap.reload({ waitUntil: 'networkidle' });
    await ap.waitForSelector('.sidebar', { timeout: 15000 });
    await sec(ap, ST.scene);

    const token = await ap.evaluate(() => localStorage.getItem('mri.frontend.token'));
    const raw = await fetch(`http://localhost:8080/api/exams/by-patient/${PATIENT_ID}`, {
      headers: { Authorization: `Bearer ${token}` },
    }).then(r => r.json()).catch(() => ({}));
    const allExams = raw?.data || [];
    const reqExam = allExams.find(e => e.status === 'REQUESTED');
    const PATIENT_EXAM_ID = (reqExam || allExams[allExams.length - 1])?.id;
    allExams.forEach(e => console.log(`    id=${e.id} status=${e.status} item=${e.examItem}`));
    console.log(`  → target examId=${PATIENT_EXAM_ID}`);

    await snap(ap, '05-admin-dashboard');
    log('05/13', `Admin 登录 ✅ (examId=${PATIENT_EXAM_ID})`);

    // ── 6. Admin 排程 ─────────────────────────────────
    log('06/13', 'Admin 查看检查申请并排程');
    if (!PATIENT_EXAM_ID) throw new Error('找不到患者提交的检查申请');

    await navTo(ap, '检查申请');
    await ap.waitForSelector('text=检查申请列表', { timeout: 8000 });
    await ap.locator('table tbody td').first().waitFor({ state: 'visible', timeout: 15000 });
    await sec(ap, ST.read);

    // 选中排程
    const examSelect = ap.locator('select[name="examId"]');
    if (await examSelect.count() > 0) {
      const opts = await examSelect.locator('option').all();
      for (let i = 0; i < opts.length; i++) {
        if (await opts[i].getAttribute('value') === String(PATIENT_EXAM_ID)) {
          await examSelect.selectOption({ index: i });
          break;
        }
      }
    }
    await sec(ap, ST.pause);

    await ap.locator('input[name="scheduledAt"]').fill('2026-07-01T09:00');
    await type(ap, 'input[name="technologist"]', '李技师');
    await sec(ap, ST.pause);

    await btnClick(ap, '新增排程');
    await sec(ap, ST.scene);
    await snap(ap, '06-admin-scheduled');
    log('06/13', '排程完成 ✅');

    // reload 确保排程后的页面数据同步
    await ap.reload({ waitUntil: 'networkidle' });
    await ap.waitForSelector('.sidebar', { timeout: 15000 });
    await sec(ap, ST.scene);
    // 重新导航到检查申请页面
    await navTo(ap, '检查申请');
    await ap.waitForSelector('text=检查申请列表', { timeout: 8000 });
    // 等待异步 exam 数据加载（表格中不再显示"暂无检查申请"）
    await ap.waitForFunction(() => {
      const tbody = document.querySelector('table tbody');
      return tbody && tbody.textContent && !tbody.textContent.includes('暂无检查申请');
    }, { timeout: 20000 });
    await sec(ap, ST.read);

    // ── 7. Admin 开始 → 完成检查 ──────────────────────
    log('07/13', 'Admin 开始检查 → 完成检查');
    await sec(ap, ST.read);

    const cell = ap.locator('table tbody tr td:first-child').filter({ hasText: String(PATIENT_EXAM_ID) });
    if (await cell.count() === 0) throw new Error(`表格未找到 examId=${PATIENT_EXAM_ID}`);
    const row = cell.first().locator('xpath=..');
    await row.waitFor({ state: 'visible', timeout: 8000 });

    await click(ap, row.locator('button:has-text("开始")'));
    await sec(ap, ST.scene);

    await click(ap, row.locator('button:has-text("完成")'));
    await sec(ap, ST.scene);
    await snap(ap, '07-admin-exam-done');
    log('07/13', '检查执行完成 ✅');

    // 完成检查后 reload 确保 exam 状态同步到 COMPLETED
    await ap.reload({ waitUntil: 'networkidle' });
    await ap.waitForSelector('.sidebar', { timeout: 15000 });
    await sec(ap, ST.scene);

    // ── 8. Admin 创建影像检查 ─────────────────────────
    log('08/13', 'Admin 影像归档 - 创建影像检查');
    await navTo(ap, '影像归档');
    await ap.waitForSelector('text=保存影像检查', { timeout: 5000 });
    await sec(ap, ST.read);

    const se = ap.locator('select[name="examOrderId"]');
    if (await se.count() > 0) {
      const opts = await se.locator('option').all();
      if (opts.length > 1) await se.selectOption({ index: 1 });
    }
    await type(ap, 'input[name="description"]', '头颅MRI平扫 T1+T2');
    await sec(ap, ST.pause);

    await btnClick(ap, '保存影像');
    await sec(ap, ST.scene);
    log('08/13', '影像检查创建完成 ✅');

    // ── 9. Admin 创建序列 + 上传影像 ──────────────────
    log('09/13', 'Admin 创建序列并上传影像（可重复上传同一图片）');

    await click(ap, ap.locator('button').filter({ hasText: '影像管理' }).first());
    await ap.waitForSelector('text=添加序列', { timeout: 5000 });
    await sec(ap, ST.read);

    await type(ap, 'input[name="seriesName"]', 'T1_AX');
    await type(ap, 'input[name="bodyPosition"]', '头部仰卧');
    await sec(ap, ST.pause);

    await btnClick(ap, '添加序列');
    await sec(ap, ST.scene);

    // 选中刚创建的序列
    const ss = ap.locator('select[name="uploadSeriesId"]');
    if (await ss.count() > 0) {
      const opts = await ss.locator('option').all();
      if (opts.length > 1) await ss.selectOption({ index: 1 });
    }
    await sec(ap, ST.pause);

    // 上传真实图片
    const realFile = readFileSync(REAL_IMAGE);
    const fileInput = ap.locator('input[type="file"]').first();
    await fileInput.setInputFiles([
      { name: 'HobbyTown_USA_Oshkosh_interior_under_construction_2002_(The_Backrooms).jpg',
        mimeType: 'image/jpeg',
        buffer: realFile },
    ]);
    await sec(ap, ST.pause);
    await btnClick(ap, '上传影像');
    await sec(ap, ST.scene);
    await snap(ap, '09a-first-upload');

    // 重复上传同一张图片
    log('  ↳ 重复上传同一张图片');
    await fileInput.setInputFiles([
      { name: 'HobbyTown_USA_Oshkosh_interior_under_construction_2002_(The_Backrooms).jpg',
        mimeType: 'image/jpeg',
        buffer: realFile },
    ]);
    await fileInput.evaluate(el => { el.value = ''; });
    await sec(ap, ST.pause);
    await btnClick(ap, '上传影像');
    await sec(ap, ST.scene);
    await snap(ap, '09-admin-images');
    log('09/13', '影像上传完成 ✅');

    // ── 10. Admin 报告 → 提交 → 审核 → 发布 ──────────
    log('10/13', 'Admin 编写诊断报告');
    await navTo(ap, '诊断报告');
    await ap.waitForSelector('text=新增报告', { timeout: 5000 });
    await sec(ap, ST.read);

    const re = ap.locator('select[name="examOrderId"]');
    const rs = ap.locator('select[name="studyId"]');
    if (await re.count() > 0) {
      const o = await re.locator('option').all();
      if (o.length > 1) await re.selectOption({ index: 1 });
    }
    await sec(ap, ST.quick);
    if (await rs.count() > 0) {
      const o = await rs.locator('option').all();
      if (o.length > 1) await rs.selectOption({ index: 1 });
    }
    await sec(ap, ST.read);

    await type(ap, 'textarea[name="findings"]',
      '颅脑形态对称，脑灰白质分界清楚。双侧大脑半球、小脑及脑干未见明确异常信号。脑室系统未见扩大，脑沟裂未见增宽。中线结构居中。');
    await sec(ap, ST.think);
    await type(ap, 'textarea[name="impression"]',
      '头颅MRI平扫未见明确异常。建议结合临床，必要时增强扫描。');
    await sec(ap, ST.think);

    await btnClick(ap, '新增报告');
    await sec(ap, ST.scene);
    log('    ↳ 草稿已创建');

    // 等待表格刷新，然后依次提交→审核→发布
    await sec(ap, ST.scene);
    const rows = ap.locator('.table-actions');
    const actionBtn = async (label) => {
      for (let i = 0, n = await rows.count(); i < n; i++) {
        const b = rows.nth(i).locator('button').filter({ hasText: label });
        if (await b.count() > 0 && await b.isEnabled()) {
          await click(ap, b);
          return true;
        }
      }
      return false;
    };

    await actionBtn('提交');
    await sec(ap, ST.scene);
    log('    ↳ 已提交，等待审核');

    await actionBtn('审核');
    await sec(ap, ST.scene);
    log('    ↳ 审核通过，发布报告');

    await actionBtn('发布');
    await sec(ap, ST.scene);
    await snap(ap, '10-admin-report-published');
    log('10/13', '报告已发布 ✅');
    await ap.close();

    // ── 11. 患者重新登录 ──────────────────────────────
    log('11/13', '患者重新登录查看进度');
    const ctx2 = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' });
    const pp2 = await ctx2.newPage();
    await pp2.goto(`${BASE}/#/login`, { waitUntil: 'networkidle' });
    await loginAs(pp2, USER, PASS);
    await snap(pp2, '11-patient-dashboard');
    log('11/13', '患者工作台 ✅');

    // ── 12. 患者查看报告 ──────────────────────────────
    log('12/13', '患者查看诊断报告');
    await navTo(pp2, '我的报告');
    await sec(pp2, ST.scene);
    await snap(pp2, '12-patient-reports');
    log('12/13', '报告查看完成 ✅');

    // ── 13. 患者查看影像 ──────────────────────────────
    log('13/13', '患者查看影像');
    await navTo(pp2, '我的影像');
    await pp2.waitForSelector('text=我的影像', { timeout: 5000 });
    await sec(pp2, ST.scene);

    const vbtn = pp2.locator('button').filter({ hasText: '查看影像' });
    if (await vbtn.count() > 0 && await vbtn.first().isEnabled()) {
      await click(pp2, vbtn.first());
      await sec(pp2, ST.scene);
    }
    await snap(pp2, '13-patient-images');
    await pp2.close();

    // ── 完成 ──────────────────────────────────────────
    console.log('\n╔══════════════════════════════════════════════╗');
    console.log('║  🎉 全流程演示完成！                        ║');
    console.log('╠══════════════════════════════════════════════╣');
    console.log(`║  患者账号: ${USER}`);
    console.log(`║  密码    : ${PASS}`);
    console.log(`║  Admin   : admin / admin123`);
    console.log(`║  截图目录: ${OUT_DIR}`);
    console.log('╚══════════════════════════════════════════════╝\n');

  } catch (e) {
    console.error('\n❌', e.message);
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
}

main();
