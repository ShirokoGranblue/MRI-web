import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { Activity, ArrowRight, ClipboardList, Download, FileText, Image, Plus, RefreshCw, Save, Send, Trash2, UserRound } from 'lucide-react';
import { api } from '../lib/api.js';
import { statusLabel, useApp } from '../lib/app-context.jsx';
import { patientLandingPath } from '../lib/role-utils.js';
import { Button, DataTable, EmptyState, Metric, PageHeader, SectionHeader, SelectField, StatusTag, TextField } from '../components/ui.jsx';

const emptyContraindication = () => ({ type: '金属植入物', description: '', severity: 'HIGH' });

const patientFlow = ['提交申请', '待审核', '排程检查', '检查执行', '报告发布'];

export function PatientDashboardPage() {
  const { patientProfile, exams, studies, reports } = useApp();
  const navigate = useNavigate();
  const landingPath = patientLandingPath(patientProfile);
  if (landingPath === '/patients') {
    return <Navigate to="/patients" replace />;
  }

  const pendingExams = exams.filter((e) => e.status === 'REQUESTED').length;
  const inProgressExams = exams.filter((e) => e.status === 'IN_PROGRESS').length;
  const publishedReports = reports.filter((r) => r.status === 'PUBLISHED').length;

  return (
    <div className="page-stack">
      <PageHeader
        title="我的工作台"
        subtitle="查看本人检查、影像与诊断报告处理进度"
        actions={<Button icon={Plus} onClick={() => navigate('/exams/request')}>申请检查</Button>}
      />
      {!patientProfile?.profileComplete ? <ProfileRequired /> : (
        <>
          <section className="metric-grid">
            <Metric label="资料状态" value="已完成" detail={patientProfile.patient?.patientNo} tone="success" />
            <Metric label="检查申请" value={exams.length} detail={statusLabel[exams[0]?.status] || '暂无'} tone="info" />
            <Metric label="影像记录" value={studies.length} detail={studies.some((s) => s.reportPublished) ? '可查看' : '处理中'} tone="warning" />
            <Metric label="诊断报告" value={reports.length} detail={reports.length > 0 ? `${publishedReports} 已发布` : '暂无'} tone="neutral" />
          </section>

          <section className="panel">
            <SectionHeader icon={Activity} title="检查流程" />
            <div className="workflow">
              {patientFlow.map((item, index) => (
                <div className="workflow-step" key={item}>
                  <span>{index + 1}</span>
                  <strong>{item}</strong>
                  {index < patientFlow.length - 1 ? <ArrowRight size={14} className="workflow-arrow" aria-hidden="true" /> : null}
                </div>
              ))}
            </div>
          </section>

          <section className="panel">
            <SectionHeader icon={ClipboardList} title="最近检查" actions={<Link to="/exams" className="link">查看全部</Link>} />
            {exams.length ? (
              <div className="todo-grid">
                <div className="todo-card">
                  <strong>{pendingExams}</strong>
                  <span>待检查</span>
                  <StatusTag tone="warning">等待医生处理</StatusTag>
                </div>
                <div className="todo-card">
                  <strong>{inProgressExams}</strong>
                  <span>检查中</span>
                  <StatusTag tone="info">正在执行</StatusTag>
                </div>
                <div className="todo-card">
                  <strong>{publishedReports}</strong>
                  <span>已出报告</span>
                  <StatusTag tone="success">可查看</StatusTag>
                </div>
              </div>
            ) : (
              <p className="muted">暂无检查记录，点击上方「申请检查」提交新的检查申请。</p>
            )}
          </section>
        </>
      )}
    </div>
  );
}

export function PatientProfilePage() {
  const { patientProfile, runAction, refreshMyProfile, refreshPatientData, busyKey } = useApp();
  const [form, setForm] = useState({
    name: '',
    gender: '男',
    birthDate: '1990-01-01',
    phone: '',
    hasContraindications: false,
    contraindications: [],
  });

  useEffect(() => {
    if (!patientProfile?.profileComplete) return;
    setForm({
      name: patientProfile.patient.name || '',
      gender: patientProfile.patient.gender || '男',
      birthDate: patientProfile.patient.birthDate || '1990-01-01',
      phone: patientProfile.patient.phone || '',
      hasContraindications: patientProfile.hasContraindications,
      contraindications: (patientProfile.contraindications || []).map((item) => ({
        type: item.type,
        description: item.description || '',
        severity: item.severity || 'HIGH',
      })),
    });
  }, [patientProfile]);

  function update(event) {
    const { name, value } = event.target;
    if (name === 'hasContraindications') {
      const enabled = value === 'true';
      setForm((current) => ({
        ...current,
        hasContraindications: enabled,
        contraindications: enabled && !current.contraindications.length ? [emptyContraindication()] : enabled ? current.contraindications : [],
      }));
      return;
    }
    setForm((current) => ({ ...current, [name]: value }));
  }

  function updateContraindication(index, event) {
    const { name, value } = event.target;
    setForm((current) => ({
      ...current,
      contraindications: current.contraindications.map((item, itemIndex) => (
        itemIndex === index ? { ...item, [name]: value } : item
      )),
    }));
  }

  async function submit(event) {
    event.preventDefault();
    const creating = !patientProfile?.profileComplete;
    await runAction(
      'saveMyProfile',
      creating ? '完成患者建档' : '保存本人资料',
      () => creating ? api.createMyProfile(form) : api.updateMyProfile(form),
      async () => {
        await refreshMyProfile(false);
        await refreshPatientData(false);
      },
      { log: false },
    );
  }

  return (
    <div className="page-stack">
      <PageHeader title="我的资料" subtitle="患者本人维护基本资料与 MRI 禁忌症信息" actions={
        <Button icon={RefreshCw} variant="secondary" onClick={() => refreshMyProfile(true)} busy={busyKey === 'myProfile'}>刷新</Button>
      } />
      <section className="panel">
        <SectionHeader icon={UserRound} title={patientProfile?.profileComplete ? '患者资料' : '首次患者建档'} />
        {patientProfile?.profileComplete ? <p className="profile-number">患者编号：<strong>{patientProfile.patient.patientNo}</strong></p> : null}
        <form className="form-grid" onSubmit={submit}>
          <TextField label="姓名" name="name" value={form.name} onChange={update} />
          <SelectField label="性别" name="gender" value={form.gender} onChange={update} options={['男', '女']} />
          <TextField label="出生日期" name="birthDate" type="date" value={form.birthDate} onChange={update} />
          <TextField label="联系电话" name="phone" value={form.phone} onChange={update} />
          <SelectField
            label="是否存在 MRI 禁忌症"
            name="hasContraindications"
            value={String(form.hasContraindications)}
            onChange={update}
            options={[{ value: 'false', label: '无禁忌症' }, { value: 'true', label: '有禁忌症' }]}
          />
        </form>
        {form.hasContraindications ? (
          <section className="contra-section">
            <SectionHeader icon={Plus} title="禁忌症明细" />
            <div className="contra-editor">
              {form.contraindications.map((item, index) => (
                <div className="contra-editor-row" key={index}>
                  <SelectField label="类型" name="type" value={item.type} onChange={(event) => updateContraindication(index, event)} options={['心脏起搏器', '金属植入物', '幽闭恐惧', '肾功能不全', '妊娠']} />
                  <TextField label="说明" name="description" value={item.description} onChange={(event) => updateContraindication(index, event)} />
                  <SelectField label="严重程度" name="severity" value={item.severity} onChange={(event) => updateContraindication(index, event)} options={[{ value: 'HIGH', label: '高' }, { value: 'LOW', label: '低' }]} />
                  <Button type="button" icon={Trash2} variant="ghost" onClick={() => setForm((current) => ({
                    ...current,
                    contraindications: current.contraindications.filter((_, itemIndex) => itemIndex !== index),
                  }))}>移除</Button>
                </div>
              ))}
              <Button type="button" icon={Plus} variant="secondary" onClick={() => setForm((current) => ({
                ...current,
                contraindications: [...current.contraindications, emptyContraindication()],
              }))}>增加禁忌症</Button>
            </div>
          </section>
        ) : null}
        <div className="form-submit" style={{ marginTop: 18 }}><Button icon={Save} onClick={submit} busy={busyKey === 'saveMyProfile'}>{patientProfile?.profileComplete ? '保存资料' : '提交建档'}</Button></div>
      </section>
    </div>
  );
}

export function PatientExamsPage() {
  const { patientProfile, exams, refreshPatientData, busyKey } = useApp();
  if (!patientProfile?.profileComplete) return <ProfileRequired />;
  return (
    <div className="page-stack">
      <PageHeader title="我的检查" subtitle="只读查看检查申请、排程和执行进度" actions={<Button icon={RefreshCw} variant="secondary" onClick={() => refreshPatientData(true)} busy={busyKey === 'patientData'}>刷新</Button>} />
      <section className="panel">
        <DataTable rows={exams} emptyText="暂无检查申请" columns={[
          { key: 'examItem', label: '检查项目' },
          { key: 'clinicalDiagnosis', label: '临床诊断' },
          { key: 'priority', label: '优先级' },
          { key: 'schedule', label: '检查排程', render: (item) => item.schedules?.length && item.schedules[0].scheduledAt ? `${formatTime(item.schedules[0].scheduledAt)} · ${item.schedules[0].scannerRoom}` : '待排程' },
          { key: 'status', label: '状态', render: (item) => <StatusTag status={item.status} /> },
        ]} />
      </section>
    </div>
  );
}

export function PatientImagesPage() {
  const { patientProfile, studies, refreshPatientData, runAction, busyKey } = useApp();
  const [manifest, setManifest] = useState(null);
  if (!patientProfile?.profileComplete) return <ProfileRequired />;
  async function openStudy(study) {
    if (!study.reportPublished) return;
    await runAction(
      `open-my-study-${study.id}`,
      '打开本人影像',
      () => api.myViewerManifest(study.id),
      setManifest,
      { log: false },
    );
  }
  return (
    <div className="page-stack">
      <PageHeader title="我的影像" subtitle="报告发布后可查看并下载本人相关影像" actions={<Button icon={RefreshCw} variant="secondary" onClick={() => refreshPatientData(true)} busy={busyKey === 'patientData'}>刷新</Button>} />
      <section className="panel">
        <DataTable rows={studies} emptyText="暂无影像记录" columns={[
          { key: 'description', label: '影像检查' },
          { key: 'status', label: '归档状态', render: (item) => <StatusTag status={item.status} /> },
          { key: 'fileCount', label: '文件数量' },
          { key: 'visibility', label: '影像', render: (item) => item.reportPublished ? <Button variant="ghost" icon={Image} onClick={() => openStudy(item)}>查看影像</Button> : <span className="muted">报告发布后开放</span> },
        ]} />
      </section>
      {manifest ? <PatientManifest manifest={manifest} onClose={() => setManifest(null)} /> : null}
    </div>
  );
}

export function PatientReportsPage() {
  const { patientProfile, reports, refreshPatientData, busyKey } = useApp();
  if (!patientProfile?.profileComplete) return <ProfileRequired />;
  return (
    <div className="page-stack">
      <PageHeader title="我的报告" subtitle="查看报告处理进度和已发布诊断内容" actions={<Button icon={RefreshCw} variant="secondary" onClick={() => refreshPatientData(true)} busy={busyKey === 'patientData'}>刷新</Button>} />
      {reports.length ? reports.map((report) => (
        <section className="panel" key={report.id}>
          <SectionHeader icon={FileText} title={`诊断报告 #${report.id}`} actions={<StatusTag status={report.status} />} />
          {report.status === 'PUBLISHED' ? (
            <div className="report-readonly">
              <div><strong>影像所见</strong><p>{report.findings || '—'}</p></div>
              <div><strong>诊断意见</strong><p>{report.impression || '—'}</p></div>
            </div>
          ) : <p className="muted">报告当前处于“{statusLabel[report.status] || report.status}”，发布后可查看完整内容。</p>}
        </section>
      )) : <EmptyState icon={FileText} text="暂无诊断报告" />}
    </div>
  );
}

export function PatientExamRequestPage() {
  const { patientProfile, refreshPatientData, runAction, busyKey } = useApp();
  const navigate = useNavigate();
  const [form, setForm] = useState({ examItem: '', clinicalDiagnosis: '', priority: '普通' });

  if (!patientProfile?.profileComplete) return <ProfileRequired />;

  function update(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    if (!form.examItem.trim()) return;
    const body = {
      patientId: patientProfile.patient.id,
      examItem: form.examItem.trim(),
      clinicalDiagnosis: form.clinicalDiagnosis.trim(),
      priority: form.priority,
    };
    const result = await runAction(
      'requestExam',
      '提交检查申请',
      () => api.createExam(body),
      async () => { await refreshPatientData(false); },
      { log: false },
    );
    if (result) {
      navigate('/exams');
    }
  }

  return (
    <div className="page-stack">
      <PageHeader title="申请检查" subtitle="提交新的 MRI 检查申请，医生将根据您的信息安排检查" />
      <section className="panel">
        <SectionHeader icon={ClipboardList} title="填写检查申请" />
        <form className="form-grid" onSubmit={submit}>
          <TextField label="检查项目" name="examItem" value={form.examItem} onChange={update} placeholder="如：头颅MRI平扫" />
          <TextField label="临床诊断" name="clinicalDiagnosis" value={form.clinicalDiagnosis} onChange={update} placeholder="如：头痛待查" />
          <SelectField label="优先级" name="priority" value={form.priority} onChange={update} options={['普通', '加急']} />
          <div className="form-submit">
            <Button icon={Send} busy={busyKey === 'requestExam'} disabled={!form.examItem.trim()}>
              提交申请
            </Button>
          </div>
        </form>
        <p className="muted" style={{ marginTop: 16 }}>提交后申请状态为「待检查」，由医生处理排程。您可在「我的检查」中查看申请进度。</p>
      </section>
    </div>
  );
}

function PatientManifest({ manifest, onClose }) {
  const { runAction, busyKey } = useApp();
  const files = useMemo(() => manifest.series?.flatMap((series) => series.files || []) || [], [manifest]);
  const downloadEnabled = manifest.downloadEnabled !== false;

  async function downloadAll() {
    await runAction(
      `my-download-study-${manifest.study.id}`,
      '下载整组影像',
      () => api.myDownloadStudy(manifest.study.id),
      undefined,
      { log: false, successMessage: '整组影像已保存到本地' },
    );
  }

  return (
    <div className="dialog-overlay" role="dialog" aria-modal="true">
      <div className="dialog-card dialog-wide">
        <div className="dialog-head dialog-head-row">
          <strong>{manifest.study.description || 'MRI 影像'}</strong>
          <div className="table-actions">
            <Button icon={Download} disabled={!downloadEnabled || !files.length} busy={busyKey === `my-download-study-${manifest.study.id}`} onClick={downloadAll}>下载全部</Button>
            <Button variant="ghost" onClick={onClose}>关闭</Button>
          </div>
        </div>
        {!downloadEnabled ? <p className="inline-notice">当前配置已关闭影像下载。</p> : null}
        <div className="scan-grid">
          {files.map((file) => <PatientImageFile file={file} downloadEnabled={downloadEnabled} key={file.id} />)}
        </div>
      </div>
    </div>
  );
}

function PatientImageFile({ file, downloadEnabled }) {
  const { notify, runAction, busyKey } = useApp();
  const urlRef = useRef('');
  const [url, setUrl] = useState('');
  useEffect(() => {
    let active = true;
    api.myFileContent(file.id)
      .then((blob) => {
        if (!active) return;
        const objUrl = URL.createObjectURL(blob);
        urlRef.current = objUrl;
        setUrl(objUrl);
      })
      .catch((error) => {
        if (active) notify('error', '加载影像', error.message || '影像加载失败');
      });
    return () => {
      active = false;
      if (urlRef.current) URL.revokeObjectURL(urlRef.current);
    };
  }, [file.id, notify]);
  return (
    <div className="scan-item">
      {url ? <img src={url} alt={file.fileName} className="scan-thumb-img" /> : <span>影像加载中</span>}
      <strong>{file.fileName}</strong>
      <Button
        icon={Download}
        variant="secondary"
        disabled={!downloadEnabled}
        busy={busyKey === `my-download-file-${file.id}`}
        onClick={() => runAction(
          `my-download-file-${file.id}`,
          '下载影像',
          () => api.myDownloadFile(file.id),
          undefined,
          { log: false, successMessage: '影像已保存到本地' },
        )}
      >
        下载
      </Button>
    </div>
  );
}

function ProfileRequired() {
  return <section className="panel profile-required"><UserRound size={28} /><strong>请先完成患者资料</strong><p>完成本人资料和 MRI 禁忌症登记后，即可查看检查、影像和报告进度。</p></section>;
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—';
}
