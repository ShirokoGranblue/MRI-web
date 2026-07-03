const RISK_VIEW = {
  UNKNOWN: { label: '待评估', tone: 'neutral' },
  NONE: { label: '无风险', tone: 'success' },
  LOW: { label: '低风险', tone: 'warning' },
  HIGH: { label: '高风险', tone: 'danger' },
};

export function riskView(level) {
  return RISK_VIEW[level] || RISK_VIEW.UNKNOWN;
}

export function formatScheduleRange(scheduledAt, durationMinutes = 30) {
  if (!scheduledAt) return '—';
  const start = new Date(scheduledAt);
  const duration = Number(durationMinutes) || 30;
  const end = new Date(start.getTime() + duration * 60 * 1000);
  const time = (value) => `${String(value.getHours()).padStart(2, '0')}:${String(value.getMinutes()).padStart(2, '0')}`;
  return `${time(start)}—${time(end)}`;
}
