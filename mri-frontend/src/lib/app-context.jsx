import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, clearSession, getToken, getStoredUser, saveSession } from './api.js';

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
  const [token, setToken] = useState(getToken());
  const [username, setUsername] = useState(getStoredUser());
  const [busyKey, setBusyKey] = useState('');
  const [patients, setPatients] = useState([]);
  const [exams, setExams] = useState([]);
  const [studies, setStudies] = useState([]);
  const [reports, setReports] = useState([]);
  const [logs, setLogs] = useState([
    { id: 'init', type: 'info', title: '系统已就绪', detail: '可开始登记患者与检查申请', time: '当前' },
  ]);

  const loggedIn = Boolean(token);

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
    async (key, title, fn, onSuccess) => {
      setBusyKey(key);
      try {
        const data = await fn();
        onSuccess?.(data);
        log('success', title, '操作成功');
        return data;
      } catch (error) {
        if (error.status === 401) {
          clearSession();
          setToken('');
          setUsername('');
          setPatients([]);
          setExams([]);
          setStudies([]);
          setReports([]);
        }
        log('error', title, error.message || '操作失败');
        return null;
      } finally {
        setBusyKey('');
      }
    },
    [log],
  );

  const login = useCallback(
    async (form) => {
      const data = await runAction(
        'login',
        '登录',
        () => api.login(form),
        (result) => {
          saveSession({ ...result, username: form.username });
          setToken(result.token);
          setUsername(form.username);
        },
      );
      return data;
    },
    [runAction],
  );

  const logout = useCallback(async () => {
    setBusyKey('logout');
    try {
      if (token) {
        await api.logout();
      }
      log('success', '退出登录', '已安全退出');
    } catch (error) {
      log('warning', '退出登录', error.message || '本地会话已清理');
    } finally {
      clearSession();
      setToken('');
      setUsername('');
      setBusyKey('');
    }
  }, [token, log]);

  const refreshPatients = useCallback(
    (keyword = '') =>
      runAction('patients', '加载患者列表', () => api.patients(1, 10, keyword), (records) => {
        setPatients(records);
      }),
    [runAction],
  );
  const refreshExams = useCallback(
    (status = '') =>
      runAction('exams', '加载检查列表', () => api.exams(1, 10, status), (records) => {
        setExams(records);
      }),
    [runAction],
  );
  const refreshStudies = useCallback(
    (keyword = '') =>
      runAction('studies', '加载影像列表', () => api.studies(1, 10, keyword), (records) => {
        setStudies(records);
      }),
    [runAction],
  );
  const refreshReports = useCallback(
    (status = '') =>
      runAction('reports', '加载报告列表', () => api.reports(1, 10, status), (records) => {
        setReports(records);
      }),
    [runAction],
  );

  useEffect(() => {
    if (!loggedIn) {
      return;
    }
    refreshPatients();
    refreshExams();
    refreshStudies();
    refreshReports();
  }, [loggedIn, refreshPatients, refreshExams, refreshStudies, refreshReports]);

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
      loggedIn,
      busyKey,
      patients,
      exams,
      studies,
      reports,
      logs,
      setPatients,
      setExams,
      setStudies,
      setReports,
      log,
      runAction,
      login,
      logout,
      refreshPatients,
      refreshExams,
      refreshStudies,
      refreshReports,
      patientName,
      examLabel,
      studyLabel,
    }),
    [token, username, loggedIn, busyKey, patients, exams, studies, reports, logs, log, runAction, login, logout, refreshPatients, refreshExams, refreshStudies, refreshReports, patientName, examLabel, studyLabel],
  );

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useApp() {
  const ctx = useContext(AppContext);
  if (!ctx) {
    throw new Error('useApp must be used within AppProvider');
  }
  return ctx;
}
