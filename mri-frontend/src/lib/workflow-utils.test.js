import assert from 'node:assert/strict';
import { test } from 'node:test';
import { formatScheduleRange, riskView } from './workflow-utils.js';

test('风险级别映射为医生可理解的标签', () => {
  assert.deepEqual(riskView('UNKNOWN'), { label: '待评估', tone: 'neutral' });
  assert.deepEqual(riskView('NONE'), { label: '无风险', tone: 'success' });
  assert.deepEqual(riskView('LOW'), { label: '低风险', tone: 'warning' });
  assert.deepEqual(riskView('HIGH'), { label: '高风险', tone: 'danger' });
});

test('排程结束时间按检查时长计算且默认三十分钟', () => {
  assert.equal(formatScheduleRange('2026-07-01T10:00:00', 45), '10:00—10:45');
  assert.equal(formatScheduleRange('2026-07-01T10:00:00'), '10:00—10:30');
});
