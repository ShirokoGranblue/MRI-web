import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, clearSession, getStoredSession, saveSession } from './api.js';
import { isPatientRole } from './role-utils.js';

export const statusLabel = {
  REQUESTED: '待检查',
  IN_PROGRESS: '检查中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REPORT_PUBLISHED: '已出报告',
  ARCHIVED: '已归档',
  DRAFT: '草稿',
  SUBMITTED: '待审核',
  APPROVED: '已审核',
  REJECTED: '已驳回',
  PUBLISHED: '已发布',
};

export const statusTone = {
  COMPLETED: 'success',
  ARCHIVED: 'success',
  PUBLISHED: 'success',
  APPROVED: 'success',
  REPORT_PUBLISHED: 'success',
  REQUESTED: 'warning',
  DRAFT: 'neutral',
  IN_PROGRESS: 'info',
  SUBMITTED: 'info',
  CANCELLED: 'danger',
  REJECTED: 'danger',
};

const AppContext = createContext(null);

export function AppProvider({ children }) {
  const stored = getStoredSession();
  const [token, setToken] = useState(stored.token);
  const [username, setUsername] = useState(stored.username);
  const [displayName, setDisplayName] = useState(stored.displayName);
  const [roles, setRoles] = useState(stored.roles);
  const [busyKey, setBusyKey] = useState('');
  const [notice, setNotice] = useState(null);
  const [patients, setPatients] = useState([]);
  const [patientProfile, setPatientProfile] = useState(null);
  const [exams, setExams] = useState([]);
  const [studies, setStudies] = useState([]);
  const [reports, setReports] = useState([]);
  const [logs, setLogs] = useState([
    { id: 'init', type: 'info', title: '系统已就绪', detail: '可开始登记患者与检查申请', time: '当前' },
  ]);

  const loggedIn = Boolean(token);
  const isPatient = isPatientRole(roles);

  const notify = useCallback((type, title, detail) => {
    setNotice({
      id: Date.now(),
      type,
      title,
      detail,
    });
  }, []);

  useEffect(() => {
    if (!notice) return undefined;
    const delay = notice.type === 'error' ? 5000 : notice.type === 'warning' ? 4000 : 3500;
    const timer = window.setTimeout(() => setNotice(null), delay);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const log = useCallback((type, title, detail) => {
    setLogs((current) => [
      {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
        type,
        title,
        detail,
        time: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
      },
      ...current.slice(0, 49),
    ]);
  }, []);

  const runAction = useCallback(
    async (key, title, fn, onSuccess, options = {}) => {
      setBusyKey(key);
      try {
        const data = await fn();
        onSuccess?.(data);
        if (options.log !== false && !isPatient) {
          log('success', title, options.successMessage || '操作成功');
        }
        if (options.notify !== false) {
          notify('success', title, options.successMessage || '操作成功');
        }
        return data;
      } catch (error) {
        if (error.status === 401) {
          clearSession();
          setToken('');
          setUsername('');
          setDisplayName('');
          setRoles([]);
          setPatients([]);
          setPatientProfile(null);
          setExams([]);
          setStudies([]);
          setReports([]);
        }
        if (options.log !== false && !isPatient) {
          log('error', title, error.message || '操作失败');
        }
        if (options.notify !== false) {
          notify('error', title, error.message || '操作失败');
        }
        return null;
      } finally {
        setBusyKey('');
      }
    },
    [isPatient, log, notify],
  );

  const login = useCallback(
    async (form) => {
      const data = await runAction(
        'login',
        '登录',
        () => api.login(form),
        (result) => {
          saveSession(result);
          setToken(result.token);
          setUsername(result.username);
          setDisplayName(result.displayName);
          setRoles(result.roles || []);
        },
        { log: false },
      );
      return data;
    },
    [runAction],
  );

  const register = useCallback(
    (form) => runAction('register', '患者注册', () => api.register(form), undefined, {
      log: false,
      successMessage: '注册成功，请使用新账号登录',
    }),
    [runAction],
  );

  const logout = useCallback(async () => {
    setBusyKey('logout');
    try {
      if (token) {
        await api.logout();
      }
      log('success', '退出登录', '已安全退出');
      notify('success', '退出登录', '已安全退出');
    } catch (error) {
      log('warning', '退出登录', error.message || '本地会话已清理');
      notify('warning', '退出登录', error.message || '本地会话已清理');
    } finally {
      clearSession();
      setToken('');
      setUsername('');
      setDisplayName('');
      setRoles([]);
      setPatientProfile(null);
      setBusyKey('');
    }
  }, [token, log, notify]);

  const refreshPatients = useCallback(
    (keyword = '', notifyUser = true) =>
      runAction('patients', '加载患者列表', () => api.patients(1, 10, keyword), (records) => {
        setPatients(records);
      }, { notify: notifyUser }),
    [runAction],
  );
  const refreshExams = useCallback(
    (status = '', notifyUser = true) =>
      runAction('exams', '加载检查列表', () => api.exams(1, 10, status), (records) => {
        setExams(records);
      }, { notify: notifyUser }),
    [runAction],
  );
  const refreshStudies = useCallback(
    (keyword = '', notifyUser = true) =>
      runAction('studies', '加载影像列表', () => api.studies(1, 10, keyword), (records) => {
        setStudies(records);
      }, { notify: notifyUser }),
    [runAction],
  );
  const refreshReports = useCallback(
    (status = '', notifyUser = true) =>
      runAction('reports', '加载报告列表', () => api.reports(1, 10, status), (records) => {
        setReports(records);
      }, { notify: notifyUser }),
    [runAction],
  );

  const refreshMyProfile = useCallback(
    (notifyUser = false) =>
      runAction('myProfile', '加载本人资料', () => api.myProfile(), setPatientProfile, {
        notify: notifyUser,
        log: false,
      }),
    [runAction],
  );

  const refreshPatientData = useCallback(
    (notifyUser = false) =>
      runAction(
        'patientData',
        '刷新个人进度',
        () => Promise.all([api.myExams(), api.myStudies(), api.myReports()]),
        ([nextExams, nextStudies, nextReports]) => {
          setExams(nextExams);
          setStudies(nextStudies);
          setReports(nextReports);
        },
        { notify: notifyUser, log: false },
      ),
    [runAction],
  );

  useEffect(() => {
    if (!loggedIn) {
      return;
    }
    if (isPatient) {
      refreshMyProfile().then((profile) => {
        if (profile?.profileComplete) {
          refreshPatientData();
        }
      });
      return;
    }
    refreshPatients('', false);
    refreshExams('', false);
    refreshStudies('', false);
    refreshReports('', false);
  }, [loggedIn, isPatient, refreshPatients, refreshExams, refreshStudies, refreshReports, refreshMyProfile, refreshPatientData]);

  useEffect(() => {
    if (!loggedIn || !isPatient || !patientProfile?.profileComplete) {
      return undefined;
    }
    let running = false;
    let timer;
    const refresh = async () => {
      if (running || document.visibilityState === 'hidden') return;
      running = true;
      await refreshPatientData(false);
      running = false;
    };
    const start = () => {
      window.clearInterval(timer);
      timer = window.setInterval(refresh, 5000);
    };
    const onVisibility = () => {
      if (document.visibilityState === 'hidden') {
        window.clearInterval(timer);
      } else {
        refresh();
        start();
      }
    };
    start();
    document.addEventListener('visibilitychange', onVisibility);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener('visibilitychange', onVisibility);
    };
  }, [loggedIn, isPatient, patientProfile?.profileComplete, refreshPatientData]);

  const patientName = useCallback(
    (id) => patients.find((p) => p.id === Number(id))?.name || `患者${id}`,
    [patients],
  );
  const examLabel = useCallback(
    (id) => {
      const e = exams.find((x) => x.id === Number(id));
      return e ? `${patientName(e.patientId)} · ${e.examItem}` : `检查${id}`;
    },
    [exams, patientName],
  );
  const studyLabel = useCallback(
    (id) => {
      const s = studies.find((x) => x.id === Number(id));
      return s ? `${examLabel(s.examOrderId)} · ${s.description || ''}` : `影像${id}`;
    },
    [studies, examLabel],
  );

  const value = useMemo(
    () => ({
      token,
      username,
      displayName,
      roles,
      isPatient,
      loggedIn,
      busyKey,
      patients,
      patientProfile,
      exams,
      studies,
      reports,
      logs,
      setPatients,
      setExams,
      setStudies,
      setReports,
      log,
      notify,
      runAction,
      login,
      register,
      logout,
      refreshPatients,
      refreshExams,
      refreshStudies,
      refreshReports,
      refreshMyProfile,
      refreshPatientData,
      patientName,
      examLabel,
      studyLabel,
    }),
    [token, username, displayName, roles, isPatient, loggedIn, busyKey, patients, patientProfile, exams, studies, reports, logs, log, notify, runAction, login, register, logout, refreshPatients, refreshExams, refreshStudies, refreshReports, refreshMyProfile, refreshPatientData, patientName, examLabel, studyLabel],
  );

  return (
    <AppContext.Provider value={value}>
      {children}
      {notice ? (
        <div className={`global-notice ${notice.type}`} role="status" aria-live="polite">
          <div>
            <strong>{notice.title}</strong>
            <span>{notice.detail}</span>
          </div>
          <button type="button" aria-label="关闭提示" onClick={() => setNotice(null)}>×</button>
        </div>
      ) : null}
    </AppContext.Provider>
  );
}

export function useApp() {
  const ctx = useContext(AppContext);
  if (!ctx) {
    throw new Error('useApp must be used within AppProvider');
  }
  return ctx;
}
