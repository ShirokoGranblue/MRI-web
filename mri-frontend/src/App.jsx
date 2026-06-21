import { useMemo, useState } from 'react';
import {
  Activity,
  Archive,
  CalendarDays,
  CheckCircle2,
  ClipboardList,
  Database,
  FileText,
  Image,
  KeyRound,
  LogOut,
  Play,
  RefreshCw,
  Send,
  Server,
  Settings2,
  ShieldCheck,
  UploadCloud,
  Users,
} from 'lucide-react';
import { api, clearSession, getStoredUser, getToken, saveSession } from './lib/api.js';
import { seedExams, seedManifest, seedPatients, seedReports, seedStudies } from './lib/seed.js';

const navItems = [
  { id: 'overview', label: '工作台', icon: Activity },
  { id: 'patients', label: '患者档案', icon: Users },
  { id: 'exams', label: '检查申请', icon: ClipboardList },
  { id: 'images', label: '影像归档', icon: Image },
  { id: 'reports', label: '报告审核', icon: FileText },
  { id: 'demo', label: '缓存配置', icon: Database },
];

const statusTone = {
  已完成: 'success',
  已归档: 'success',
  已发布: 'success',
  已审核: 'success',
  待检查: 'warning',
  草稿: 'neutral',
  已提交: 'info',
  已开始: 'info',
  已取消: 'danger',
};

function nextPatientNo() {
  return `P${new Date().toISOString().slice(0, 10).replaceAll('-', '')}${String(Date.now()).slice(-4)}`;
}

function shortToken(token) {
  if (!token) {
    return '未获取';
  }
  return `${token.slice(0, 12)}...${token.slice(-8)}`;
}

function toNumber(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : value;
}

function toRecords(pageResult) {
  if (Array.isArray(pageResult)) {
    return pageResult;
  }
  return pageResult?.records || [];
}

function StatusTag({ children, tone = 'neutral' }) {
  return <span className={`status-tag ${tone}`}>{children}</span>;
}

function SectionHeader({ icon: Icon, title, actions }) {
  return (
    <div className="section-header">
      <div className="section-title">
        <Icon size={18} aria-hidden="true" />
        <h2>{title}</h2>
      </div>
      {actions ? <div className="section-actions">{actions}</div> : null}
    </div>
  );
}

function TextField({ label, name, value, onChange, type = 'text', placeholder }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input name={name} value={value} type={type} placeholder={placeholder} onChange={onChange} />
    </label>
  );
}

function SelectField({ label, name, value, onChange, options }) {
  return (
    <label className="field">
      <span>{label}</span>
      <select name={name} value={value} onChange={onChange}>
        {options.map((item) => (
          <option key={item} value={item}>
            {item}
          </option>
        ))}
      </select>
    </label>
  );
}

function TextAreaField({ label, name, value, onChange, rows = 3 }) {
  return (
    <label className="field wide">
      <span>{label}</span>
      <textarea name={name} value={value} rows={rows} onChange={onChange} />
    </label>
  );
}

function Button({ children, icon: Icon, variant = 'primary', busy, ...props }) {
  return (
    <button className={`button ${variant}`} disabled={busy || props.disabled} {...props}>
      {Icon ? <Icon size={16} aria-hidden="true" /> : null}
      <span>{busy ? '处理中' : children}</span>
    </button>
  );
}

function DataTable({ columns, rows, emptyText }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key}>{column.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length ? (
            rows.map((row, index) => (
              <tr key={row.id || `${columns[0].key}-${index}`}>
                {columns.map((column) => (
                  <td key={column.key}>{column.render ? column.render(row) : row[column.key] || '-'}</td>
                ))}
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={columns.length} className="empty-cell">
                {emptyText}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

function Metric({ label, value, detail, tone }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
      <StatusTag tone={tone}>{detail}</StatusTag>
    </div>
  );
}

function App() {
  const [active, setActive] = useState('overview');
  const [token, setToken] = useState(getToken());
  const [username, setUsername] = useState(getStoredUser());
  const [busyKey, setBusyKey] = useState('');
  const [patients, setPatients] = useState(seedPatients);
  const [exams, setExams] = useState(seedExams);
  const [schedules, setSchedules] = useState([]);
  const [studies, setStudies] = useState(seedStudies);
  const [seriesList, setSeriesList] = useState(seedManifest.series.map(({ files, ...series }) => series));
  const [reports, setReports] = useState(seedReports);
  const [manifest, setManifest] = useState(seedManifest);
  const [config, setConfig] = useState({ watermark: '医院MRI影像系统', downloadEnabled: true });
  const [logs, setLogs] = useState([
    { id: 1, type: 'info', title: '前端已就绪', detail: '默认通过 /api/** 访问网关', time: '当前' },
  ]);

  const [loginForm, setLoginForm] = useState({ username: 'admin', password: 'admin123' });
  const [patientForm, setPatientForm] = useState({
    patientNo: nextPatientNo(),
    name: '演示患者',
    gender: '男',
    birthDate: '1988-06-21',
    phone: '13800000000',
  });
  const [examForm, setExamForm] = useState({
    patientId: '1',
    examItem: '头颅MRI平扫',
    clinicalDiagnosis: '头痛待查',
    priority: '普通',
  });
  const [scheduleForm, setScheduleForm] = useState({
    examOrderId: '1',
    scannerRoom: 'MRI-01',
    scheduledAt: '2026-06-21T10:00',
    technologist: '技师A',
  });
  const [studyForm, setStudyForm] = useState({
    examOrderId: '1',
    studyInstanceUid: `1.2.840.113619.20260621.${String(Date.now()).slice(-5)}`,
    description: '头颅MRI平扫',
  });
  const [seriesForm, setSeriesForm] = useState({ studyId: '1', seriesName: 'T1_AX', bodyPosition: '头部' });
  const [fileForm, setFileForm] = useState({
    studyId: '1',
    seriesId: '1',
    fileName: 'T1_AX_001.dcm',
    storagePath: '',
    checksum: 'demo-checksum-001',
  });
  const [reportForm, setReportForm] = useState({
    examOrderId: '1',
    studyId: '1',
    findings: '头颅MRI平扫示脑实质未见明显异常信号。',
    impression: '请结合临床随访。',
  });
  const [manifestStudyId, setManifestStudyId] = useState('1');

  const loggedIn = Boolean(token);
  const counts = useMemo(
    () => ({
      patient: patients.length,
      exam: exams.length,
      study: studies.length,
      report: reports.length,
    }),
    [patients.length, exams.length, studies.length, reports.length],
  );

  function log(type, title, detail) {
    setLogs((current) => [
      {
        id: Date.now(),
        type,
        title,
        detail,
        time: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
      },
      ...current.slice(0, 8),
    ]);
  }

  function updateForm(setter) {
    return (event) => {
      const { name, value } = event.target;
      setter((current) => ({ ...current, [name]: value }));
    };
  }

  async function runAction(key, successTitle, action, onSuccess) {
    setBusyKey(key);
    try {
      const data = await action();
      onSuccess?.(data);
      log('success', successTitle, '网关请求成功');
      return data;
    } catch (error) {
      log('error', successTitle, error.message || '接口调用失败');
      return null;
    } finally {
      setBusyKey('');
    }
  }

  function jumpTo(id) {
    setActive(id);
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  async function handleLogin(event) {
    event.preventDefault();
    const data = await runAction(
      'login',
      '登录认证',
      () => api.login(loginForm),
      (result) => {
        saveSession({ ...result, username: loginForm.username });
        setToken(result.token);
        setUsername(loginForm.username);
      },
    );
    if (data?.token) {
      setActive('overview');
    }
  }

  async function handleLogout() {
    setBusyKey('logout');
    try {
      if (token) {
        await api.logout();
      }
      log('success', '退出登录', 'token 已加入后端黑名单');
    } catch (error) {
      log('warning', '退出登录', error.message || '本地会话已清理');
    } finally {
      clearSession();
      setToken('');
      setUsername('未登录');
      setBusyKey('');
    }
  }

  async function refreshPatients() {
    await runAction('patients', '患者分页查询', api.patients, (result) => {
      const records = toRecords(result);
      if (records.length) {
        setPatients(records);
      }
    });
  }

  async function createPatient(event) {
    event.preventDefault();
    await runAction('createPatient', '新增患者档案', () => api.createPatient(patientForm), (created) => {
      setPatients((current) => [created, ...current.filter((item) => item.id !== created.id)]);
      setExamForm((current) => ({ ...current, patientId: String(created.id || current.patientId) }));
      setPatientForm((current) => ({ ...current, patientNo: nextPatientNo(), name: '' }));
    });
  }

  async function createExam(event) {
    event.preventDefault();
    const body = { ...examForm, patientId: toNumber(examForm.patientId) };
    await runAction('createExam', '创建 MRI 检查申请', () => api.createExam(body), (created) => {
      const row = { ...created, priority: examForm.priority };
      setExams((current) => [row, ...current.filter((item) => item.id !== row.id)]);
      setScheduleForm((current) => ({ ...current, examOrderId: String(row.id || current.examOrderId) }));
      setStudyForm((current) => ({ ...current, examOrderId: String(row.id || current.examOrderId) }));
      setReportForm((current) => ({ ...current, examOrderId: String(row.id || current.examOrderId) }));
    });
  }

  async function createSchedule(event) {
    event.preventDefault();
    const body = { ...scheduleForm, examOrderId: toNumber(scheduleForm.examOrderId) };
    await runAction('createSchedule', '新增检查排程', () => api.createSchedule(body), (created) => {
      setSchedules((current) => [created, ...current.filter((item) => item.id !== created.id)]);
    });
  }

  async function updateExamStatus(id, action, label) {
    await runAction(`exam-${label}-${id}`, label, () => action(id), (updated) => {
      setExams((current) => current.map((item) => (item.id === updated.id ? { ...item, ...updated } : item)));
    });
  }

  async function archiveStudy(event) {
    event.preventDefault();
    const body = { ...studyForm, examOrderId: toNumber(studyForm.examOrderId) };
    await runAction('archiveStudy', '归档 MRI Study', () => api.archiveStudy(body), (created) => {
      setStudies((current) => [created, ...current.filter((item) => item.id !== created.id)]);
      setSeriesForm((current) => ({ ...current, studyId: String(created.id || current.studyId) }));
      setFileForm((current) => ({ ...current, studyId: String(created.id || current.studyId) }));
      setReportForm((current) => ({ ...current, studyId: String(created.id || current.studyId) }));
      setManifestStudyId(String(created.id || manifestStudyId));
    });
  }

  async function createSeries(event) {
    event.preventDefault();
    const studyId = toNumber(seriesForm.studyId);
    const body = { seriesName: seriesForm.seriesName, bodyPosition: seriesForm.bodyPosition };
    await runAction('createSeries', '新增 MRI Series', () => api.createSeries(studyId, body), (created) => {
      setSeriesList((current) => [created, ...current.filter((item) => item.id !== created.id)]);
      setFileForm((current) => ({ ...current, studyId: String(studyId), seriesId: String(created.id || current.seriesId) }));
    });
  }

  async function createFile(event) {
    event.preventDefault();
    const studyId = toNumber(fileForm.studyId);
    const body = {
      seriesId: toNumber(fileForm.seriesId),
      fileName: fileForm.fileName,
      storagePath: fileForm.storagePath || null,
      checksum: fileForm.checksum,
    };
    await runAction('createFile', '登记影像文件', () => api.createFile(studyId, body), () => {
      setManifestStudyId(String(studyId));
    });
  }

  async function loadManifest() {
    const studyId = toNumber(manifestStudyId);
    await runAction('manifest', '读取影像预览清单', () => api.viewerManifest(studyId), setManifest);
  }

  async function loadCacheDemo() {
    const studyId = toNumber(manifestStudyId);
    await runAction('cacheDemo', 'Redis 缓存演示查询', () => api.cacheDemo(studyId), (result) => {
      if (result?.study) {
        setManifest((current) => ({ ...current, study: result.study, watermark: result.watermark || current.watermark }));
      }
    });
  }

  async function loadConfig() {
    await runAction('config', '读取 Nacos 动态配置', api.demoConfig, setConfig);
  }

  async function createReport(event) {
    event.preventDefault();
    const body = {
      ...reportForm,
      examOrderId: toNumber(reportForm.examOrderId),
      studyId: toNumber(reportForm.studyId),
    };
    await runAction('createReport', '新增诊断报告', () => api.createReport(body), (created) => {
      setReports((current) => [created, ...current.filter((item) => item.id !== created.id)]);
    });
  }

  async function changeReportStatus(id, action, label) {
    await runAction(`report-${label}-${id}`, label, () => action(id), (updated) => {
      setReports((current) => current.map((item) => (item.id === updated.id ? { ...item, ...updated } : item)));
    });
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">MRI</div>
          <div>
            <strong>MRI影像工作台</strong>
            <span>医院核磁共振图像信息管理</span>
          </div>
        </div>
        <nav className="nav-list" aria-label="主导航">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <button key={item.id} className={active === item.id ? 'active' : ''} onClick={() => jumpTo(item.id)}>
                <Icon size={17} aria-hidden="true" />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>
        <div className="sidebar-foot">
          <StatusTag tone={loggedIn ? 'success' : 'warning'}>{loggedIn ? '已认证' : '未登录'}</StatusTag>
          <span>{shortToken(token)}</span>
        </div>
      </aside>

      <main className="workspace">
        <header className="topbar">
          <div>
            <h1>医院MRI图像信息管理系统</h1>
            <div className="topbar-status">
              <StatusTag tone="info">网关 8080</StatusTag>
              <StatusTag tone="success">Redis缓存</StatusTag>
              <StatusTag tone="success">Nacos注册/配置</StatusTag>
              <StatusTag tone="neutral">Feign远程调用</StatusTag>
            </div>
          </div>
          <div className="user-box">
            <ShieldCheck size={18} aria-hidden="true" />
            <span>{username}</span>
            {loggedIn ? (
              <Button icon={LogOut} variant="ghost" onClick={handleLogout} busy={busyKey === 'logout'}>
                登出
              </Button>
            ) : null}
          </div>
        </header>

        <div className="content-grid">
          <div className="main-column">
            <section className="panel auth-panel" id="overview">
              <SectionHeader icon={KeyRound} title="登录认证" />
              <form className="login-form" onSubmit={handleLogin}>
                <TextField label="账号" name="username" value={loginForm.username} onChange={updateForm(setLoginForm)} />
                <TextField label="密码" name="password" type="password" value={loginForm.password} onChange={updateForm(setLoginForm)} />
                <Button icon={KeyRound} busy={busyKey === 'login'}>登录</Button>
              </form>
              <div className="token-line">
                <span>Bearer Token</span>
                <code>{shortToken(token)}</code>
              </div>
            </section>

            <section className="metric-grid" aria-label="业务数量">
              <Metric label="患者档案" value={counts.patient} detail="档案" tone="info" />
              <Metric label="检查申请" value={counts.exam} detail="申请单" tone="warning" />
              <Metric label="MRI Study" value={counts.study} detail="归档" tone="success" />
              <Metric label="诊断报告" value={counts.report} detail="报告" tone="neutral" />
            </section>

            <section className="panel workflow-panel">
              <SectionHeader icon={Server} title="分布式流程" />
              <div className="workflow">
                {['患者服务', '检查服务', '影像服务', '报告服务', 'API网关'].map((item, index) => (
                  <div className="workflow-step" key={item}>
                    <span>{index + 1}</span>
                    <strong>{item}</strong>
                  </div>
                ))}
              </div>
            </section>

            <section className="panel" id="patients">
              <SectionHeader
                icon={Users}
                title="患者档案"
                actions={
                  <Button icon={RefreshCw} variant="secondary" onClick={refreshPatients} busy={busyKey === 'patients'}>
                    刷新
                  </Button>
                }
              />
              <form className="form-grid" onSubmit={createPatient}>
                <TextField label="患者编号" name="patientNo" value={patientForm.patientNo} onChange={updateForm(setPatientForm)} />
                <TextField label="姓名" name="name" value={patientForm.name} onChange={updateForm(setPatientForm)} />
                <SelectField label="性别" name="gender" value={patientForm.gender} onChange={updateForm(setPatientForm)} options={['男', '女']} />
                <TextField label="出生日期" name="birthDate" type="date" value={patientForm.birthDate} onChange={updateForm(setPatientForm)} />
                <TextField label="联系电话" name="phone" value={patientForm.phone} onChange={updateForm(setPatientForm)} />
                <div className="form-submit">
                  <Button icon={Send} busy={busyKey === 'createPatient'}>新增患者</Button>
                </div>
              </form>
              <DataTable
                rows={patients}
                emptyText="暂无患者档案"
                columns={[
                  { key: 'patientNo', label: '患者编号' },
                  { key: 'name', label: '姓名' },
                  { key: 'gender', label: '性别' },
                  { key: 'birthDate', label: '出生日期' },
                  { key: 'phone', label: '联系电话' },
                ]}
              />
            </section>

            <section className="panel" id="exams">
              <SectionHeader icon={ClipboardList} title="检查申请与排程" />
              <form className="form-grid" onSubmit={createExam}>
                <TextField label="患者ID" name="patientId" value={examForm.patientId} onChange={updateForm(setExamForm)} />
                <TextField label="检查项目" name="examItem" value={examForm.examItem} onChange={updateForm(setExamForm)} />
                <TextField label="临床诊断" name="clinicalDiagnosis" value={examForm.clinicalDiagnosis} onChange={updateForm(setExamForm)} />
                <SelectField label="优先级" name="priority" value={examForm.priority} onChange={updateForm(setExamForm)} options={['普通', '加急']} />
                <div className="form-submit">
                  <Button icon={Send} busy={busyKey === 'createExam'}>创建申请</Button>
                </div>
              </form>
              <form className="form-grid compact" onSubmit={createSchedule}>
                <TextField label="申请ID" name="examOrderId" value={scheduleForm.examOrderId} onChange={updateForm(setScheduleForm)} />
                <TextField label="检查室" name="scannerRoom" value={scheduleForm.scannerRoom} onChange={updateForm(setScheduleForm)} />
                <TextField label="排程时间" name="scheduledAt" type="datetime-local" value={scheduleForm.scheduledAt} onChange={updateForm(setScheduleForm)} />
                <TextField label="技师" name="technologist" value={scheduleForm.technologist} onChange={updateForm(setScheduleForm)} />
                <div className="form-submit">
                  <Button icon={CalendarDays} variant="secondary" busy={busyKey === 'createSchedule'}>新增排程</Button>
                </div>
              </form>
              <DataTable
                rows={exams}
                emptyText="暂无检查申请"
                columns={[
                  { key: 'id', label: '申请ID' },
                  { key: 'patientId', label: '患者ID' },
                  { key: 'examItem', label: '检查项目' },
                  { key: 'priority', label: '优先级' },
                  { key: 'status', label: '状态', render: (row) => <StatusTag tone={statusTone[row.status]}>{row.status || '待检查'}</StatusTag> },
                  {
                    key: 'actions',
                    label: '操作',
                    render: (row) => (
                      <div className="table-actions">
                        <Button icon={Play} variant="ghost" onClick={() => updateExamStatus(row.id, api.startExam, '开始检查')}>
                          开始
                        </Button>
                        <Button icon={CheckCircle2} variant="ghost" onClick={() => updateExamStatus(row.id, api.completeExam, '完成检查')}>
                          完成
                        </Button>
                      </div>
                    ),
                  },
                ]}
              />
              {schedules.length ? (
                <div className="schedule-strip">
                  {schedules.map((item) => (
                    <span key={item.id || `${item.examOrderId}-${item.scheduledAt}`}>
                      {item.scannerRoom} / 申请{item.examOrderId} / {String(item.scheduledAt).replace('T', ' ')}
                    </span>
                  ))}
                </div>
              ) : null}
            </section>

            <section className="panel" id="images">
              <SectionHeader icon={Archive} title="影像归档" />
              <form className="form-grid" onSubmit={archiveStudy}>
                <TextField label="检查申请ID" name="examOrderId" value={studyForm.examOrderId} onChange={updateForm(setStudyForm)} />
                <TextField label="Study UID" name="studyInstanceUid" value={studyForm.studyInstanceUid} onChange={updateForm(setStudyForm)} />
                <TextField label="Study 描述" name="description" value={studyForm.description} onChange={updateForm(setStudyForm)} />
                <div className="form-submit">
                  <Button icon={Archive} busy={busyKey === 'archiveStudy'}>归档Study</Button>
                </div>
              </form>
              <form className="form-grid compact" onSubmit={createSeries}>
                <TextField label="Study ID" name="studyId" value={seriesForm.studyId} onChange={updateForm(setSeriesForm)} />
                <TextField label="Series 名称" name="seriesName" value={seriesForm.seriesName} onChange={updateForm(setSeriesForm)} />
                <TextField label="体位" name="bodyPosition" value={seriesForm.bodyPosition} onChange={updateForm(setSeriesForm)} />
                <div className="form-submit">
                  <Button icon={Image} variant="secondary" busy={busyKey === 'createSeries'}>新增Series</Button>
                </div>
              </form>
              <form className="form-grid compact" onSubmit={createFile}>
                <TextField label="Study ID" name="studyId" value={fileForm.studyId} onChange={updateForm(setFileForm)} />
                <TextField label="Series ID" name="seriesId" value={fileForm.seriesId} onChange={updateForm(setFileForm)} />
                <TextField label="文件名" name="fileName" value={fileForm.fileName} onChange={updateForm(setFileForm)} />
                <TextField label="校验值" name="checksum" value={fileForm.checksum} onChange={updateForm(setFileForm)} />
                <div className="form-submit">
                  <Button icon={UploadCloud} variant="secondary" busy={busyKey === 'createFile'}>登记文件</Button>
                </div>
              </form>
              <DataTable
                rows={studies}
                emptyText="暂无 Study"
                columns={[
                  { key: 'id', label: 'Study ID' },
                  { key: 'examOrderId', label: '申请ID' },
                  { key: 'studyInstanceUid', label: 'Study UID' },
                  { key: 'description', label: '描述' },
                  { key: 'status', label: '状态', render: (row) => <StatusTag tone={statusTone[row.status]}>{row.status || '已归档'}</StatusTag> },
                ]}
              />
            </section>

            <section className="panel" id="reports">
              <SectionHeader icon={FileText} title="报告审核发布" />
              <form className="form-grid" onSubmit={createReport}>
                <TextField label="申请ID" name="examOrderId" value={reportForm.examOrderId} onChange={updateForm(setReportForm)} />
                <TextField label="Study ID" name="studyId" value={reportForm.studyId} onChange={updateForm(setReportForm)} />
                <TextAreaField label="影像所见" name="findings" value={reportForm.findings} onChange={updateForm(setReportForm)} />
                <TextAreaField label="诊断意见" name="impression" value={reportForm.impression} onChange={updateForm(setReportForm)} />
                <div className="form-submit">
                  <Button icon={Send} busy={busyKey === 'createReport'}>新增报告</Button>
                </div>
              </form>
              <DataTable
                rows={reports}
                emptyText="暂无报告"
                columns={[
                  { key: 'id', label: '报告ID' },
                  { key: 'examOrderId', label: '申请ID' },
                  { key: 'studyId', label: 'Study ID' },
                  { key: 'findings', label: '影像所见' },
                  { key: 'status', label: '状态', render: (row) => <StatusTag tone={statusTone[row.status]}>{row.status || '草稿'}</StatusTag> },
                  {
                    key: 'actions',
                    label: '操作',
                    render: (row) => (
                      <div className="table-actions">
                        <Button icon={Send} variant="ghost" onClick={() => changeReportStatus(row.id, api.submitReport, '提交审核')}>
                          提交
                        </Button>
                        <Button icon={CheckCircle2} variant="ghost" onClick={() => changeReportStatus(row.id, api.approveReport, '审核通过')}>
                          审核
                        </Button>
                        <Button icon={UploadCloud} variant="ghost" onClick={() => changeReportStatus(row.id, api.publishReport, '发布报告')}>
                          发布
                        </Button>
                      </div>
                    ),
                  },
                ]}
              />
            </section>
          </div>

          <aside className="inspector" id="demo">
            <section className="panel">
              <SectionHeader icon={Database} title="缓存/配置演示" />
              <div className="inline-controls">
                <TextField label="Study ID" name="manifestStudyId" value={manifestStudyId} onChange={(event) => setManifestStudyId(event.target.value)} />
                <Button icon={RefreshCw} variant="secondary" onClick={loadManifest} busy={busyKey === 'manifest'}>
                  预览清单
                </Button>
              </div>
              <div className="demo-actions">
                <Button icon={Database} variant="secondary" onClick={loadCacheDemo} busy={busyKey === 'cacheDemo'}>
                  Redis查询
                </Button>
                <Button icon={Settings2} variant="secondary" onClick={loadConfig} busy={busyKey === 'config'}>
                  动态配置
                </Button>
              </div>
              <div className="config-box">
                <div>
                  <span>水印</span>
                  <strong>{config.watermark || manifest.watermark}</strong>
                </div>
                <div>
                  <span>下载开关</span>
                  <StatusTag tone={config.downloadEnabled ? 'success' : 'danger'}>
                    {config.downloadEnabled ? '开启' : '关闭'}
                  </StatusTag>
                </div>
              </div>
            </section>

            <section className="panel manifest-panel">
              <SectionHeader icon={Image} title="影像预览清单" />
              <div className="study-card">
                <span>Study #{manifest?.study?.id || '-'}</span>
                <strong>{manifest?.study?.description || '-'}</strong>
                <small>{manifest?.study?.studyInstanceUid || '-'}</small>
              </div>
              <div className="scan-grid">
                {(manifest?.series || []).flatMap((series) =>
                  (series.files || []).map((file, index) => (
                    <div className="scan-item" key={`${series.seriesId}-${file.id || file.fileName}`}>
                      <div className={`scan-thumb thumb-${index % 3}`} />
                      <strong>{series.seriesName}</strong>
                      <span>{file.fileName}</span>
                    </div>
                  )),
                )}
              </div>
              <div className="series-list">
                {(manifest?.series || []).map((series) => (
                  <div key={series.seriesId} className="series-row">
                    <span>{series.seriesName}</span>
                    <strong>{series.files?.length || 0} 张</strong>
                  </div>
                ))}
              </div>
            </section>

            <section className="panel log-panel">
              <SectionHeader icon={Activity} title="操作日志" />
              <div className="log-list">
                {logs.map((item) => (
                  <div className={`log-row ${item.type}`} key={item.id}>
                    <span>{item.time}</span>
                    <strong>{item.title}</strong>
                    <p>{item.detail}</p>
                  </div>
                ))}
              </div>
            </section>
          </aside>
        </div>
      </main>
    </div>
  );
}

export default App;

