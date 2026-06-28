import assert from 'node:assert/strict';
import test from 'node:test';
import { isPatientRole, nextRegistrationUsername, patientLandingPath, visibleNavigation } from './role-utils.js';

test('患者角色识别和导航不包含设置与操作记录', () => {
  assert.equal(isPatientRole(['PATIENT']), true);
  assert.deepEqual(
    visibleNavigation(['PATIENT']).map((item) => item.to),
    ['/', '/patients', '/exams/request', '/exams', '/images', '/reports'],
  );
});

test('注册用户名在未手动修改时跟随姓名', () => {
  assert.equal(nextRegistrationUsername('张三', false, ''), '张三');
  assert.equal(nextRegistrationUsername('李四', true, 'patient01'), 'patient01');
});

test('患者首次登录未建档时跳转我的资料', () => {
  assert.equal(patientLandingPath({ profileComplete: false }), '/patients');
  assert.equal(patientLandingPath({ profileComplete: true }), '/');
  assert.equal(patientLandingPath(null), null);
});
