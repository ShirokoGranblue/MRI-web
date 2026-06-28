const doctorNavigation = [
  { to: '/', label: '工作台', icon: 'dashboard', end: true },
  { to: '/patients', label: '患者档案', icon: 'patients' },
  { to: '/exams', label: '检查申请', icon: 'exams' },
  { to: '/images', label: '影像归档', icon: 'images' },
  { to: '/reports', label: '诊断报告', icon: 'reports' },
  { to: '/settings', label: '系统设置', icon: 'settings' },
  { to: '/activity', label: '操作记录', icon: 'activity' },
];

const patientNavigation = [
  { to: '/', label: '工作台', icon: 'dashboard', end: true },
  { to: '/patients', label: '我的资料', icon: 'patients' },
  { to: '/exams/request', label: '申请检查', icon: 'exams' },
  { to: '/exams', label: '我的检查', icon: 'exams', end: true },
  { to: '/images', label: '我的影像', icon: 'images' },
  { to: '/reports', label: '我的报告', icon: 'reports' },
];

export function isPatientRole(roles) {
  return Array.isArray(roles) && roles.includes('PATIENT');
}

export function visibleNavigation(roles) {
  return isPatientRole(roles) ? patientNavigation : doctorNavigation;
}

export function nextRegistrationUsername(displayName, usernameEdited, currentUsername) {
  return usernameEdited ? currentUsername : displayName;
}

export function patientLandingPath(profile) {
  if (!profile) return null;
  return profile.profileComplete ? '/' : '/patients';
}
