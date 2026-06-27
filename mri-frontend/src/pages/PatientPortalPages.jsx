import { useEffect, useMemo, useState } from 'react';
import { ClipboardList, FileText, Image, Plus, RefreshCw, Save, Trash2, UserRound } from 'lucide-react';
import { api } from '../lib/api.js';
import { statusLabel, useApp } from '../lib/app-context.jsx';
import { Button, DataTable, EmptyState, PageHeader, SectionHeader, SelectField, StatusTag, TextField } from '../components/ui.jsx';

const emptyContraindication = () => ({ type: '金属植入物', description: '', severity: 'HIGH' });

export function PatientDashboardPage() {
  const { patientProfile, exams, studies, reports } = useApp();
  return (
    <div className="page-stack">
      <PageHeader title="我的进度" subtitle="实时查看本人检查、影像与诊断报告处理状态" />
      {!patientProfile?.profileComplete ? <ProfileRequired /> : (
        <div className="stat-grid">
          <Stat title="资料状态" value="已完成" detail={patientProfile.patient?.patientNo} />
          <Stat title="检查申请" value={exams.length} detail={latestStatus(exams)} />
          <Stat title="影像检查" value={studies.length} detail={studies.some((item) => item.reportPublished) ? '已有可查看影像' : '处理中'} />
          <Stat title="诊断报告" value={reports.length} detail={reports.some((item) => item.status === 'PUBLISHED') ? '已有已发布报告' : '处理中'} />
        </div>
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
      contraindications: patientProfile.contraindications.map((item) => ({
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
          {form.hasContraindications ? (
            <div className="contra-editor wide">
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
          ) : null}
          <div className="form-submit"><Button icon={Save} busy={busyKey === 'saveMyProfile'}>{patientProfile?.profileComplete ? '保存资料' : '提交建档'}</Button></div>
        </form>
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
          { key: 'schedule', label: '检查排程', render: (item) => item.schedules?.length ? `${formatTime(item.schedules[0].scheduledAt)} · ${item.schedules[0].scannerRoom}` : '待排程' },
          { key: 'status', label: '状态', render: (item) => <StatusTag status={item.status} /> },
        ]} />
      </section>
    </div>
  );
}

export function PatientImagesPage() {
  const { patientProfile, studies, refreshPatientData, busyKey } = useApp();
  const [manifest, setManifest] = useState(null);
  if (!patientProfile?.profileComplete) return <ProfileRequired />;
  async function openStudy(study) {
    if (!study.reportPublished) return;
    const data = await api.myViewerManifest(study.id);
    setManifest(data);
  }
  return (
    <div className="page-stack">
      <PageHeader title="我的影像" subtitle="报告发布后可查看本人相关影像" actions={<Button icon={RefreshCw} variant="secondary" onClick={() => refreshPatientData(true)} busy={busyKey === 'patientData'}>刷新</Button>} />
      <section className="panel">
        <DataTable rows={studies} emptyText="暂无影像记录" columns={[
          { key: 'description', label: '影像检查' },
          { key: 'status', label: '归档状态', render: (item) => <StatusTag status={item.status} /> },
          { key: 'fileCount', label: '文件数量' },
          { key: 'visibility', label: '查看权限', render: (item) => item.reportPublished ? <Button variant="ghost" icon={Image} onClick={() => openStudy(item)}>查看影像</Button> : <span className="muted">报告发布后开放</span> },
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

function PatientManifest({ manifest, onClose }) {
  const files = useMemo(() => manifest.series?.flatMap((series) => series.files || []) || [], [manifest]);
  return (
    <div className="dialog-overlay" role="dialog" aria-modal="true">
      <div className="dialog-card dialog-wide">
        <div className="dialog-head dialog-head-row"><strong>{manifest.study.description || 'MRI 影像'}</strong><Button variant="ghost" onClick={onClose}>关闭</Button></div>
        <div className="scan-grid">
          {files.map((file) => <PatientImageFile file={file} key={file.id} />)}
        </div>
      </div>
    </div>
  );
}

function PatientImageFile({ file }) {
  const [url, setUrl] = useState('');
  useEffect(() => {
    let active = true;
    api.myFileContent(file.id).then((blob) => {
      if (!active) return;
      setUrl(URL.createObjectURL(blob));
    });
    return () => {
      active = false;
      if (url) URL.revokeObjectURL(url);
    };
  }, [file.id]);
  return <div className="scan-item">{url ? <img src={url} alt={file.fileName} /> : <span>影像加载中</span>}<strong>{file.fileName}</strong></div>;
}

function ProfileRequired() {
  return <section className="panel profile-required"><UserRound size={28} /><strong>请先完成患者资料</strong><p>完成本人资料和 MRI 禁忌症登记后，即可查看检查、影像和报告进度。</p></section>;
}

function Stat({ title, value, detail }) {
  return <section className="stat-card"><span>{title}</span><strong>{value}</strong><p>{detail || '暂无记录'}</p></section>;
}

function latestStatus(items) {
  return items.length ? statusLabel[items[0].status] || items[0].status : '暂无记录';
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—';
}
