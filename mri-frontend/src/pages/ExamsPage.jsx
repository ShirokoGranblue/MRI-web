import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { CalendarDays, ClipboardList, Pencil, Play, RefreshCw, Send, Trash2, CheckCircle2, Ban } from 'lucide-react';
import { api } from '../lib/api.js';
import { statusLabel, statusTone, useApp } from '../lib/app-context.jsx';
import { formatScheduleRange, riskView } from '../lib/workflow-utils.js';
import { Button, DataTable, PageHeader, SectionHeader, SelectField, StatusTag, TextField, useConfirm } from '../components/ui.jsx';

const STATUS_OPTIONS = [
  { value: '', label: '全部' },
  { value: 'REQUESTED', label: '待检查' },
  { value: 'IN_PROGRESS', label: '检查中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'REPORT_PUBLISHED', label: '已出报告' },
];

export default function ExamsPage() {
  const { exams, setExams, patients, runAction, refreshExams, patientName, busyKey } = useApp();
  const [params, setParams] = useSearchParams();
  const [status, setStatus] = useState(params.get('status') || '');
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({ patientId: '', examItem: '', clinicalDiagnosis: '', priority: '普通' });
  const { ask, dialog } = useConfirm();

  const patientOptions = patients.map((p) => ({ value: String(p.id), label: `${p.name}（${p.patientNo}）` }));

  function update(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }
  function onStatusChange(event) {
    const value = event.target.value;
    setStatus(value);
    setParams(value ? { status: value } : {});
    refreshExams(value);
  }
  function startEdit(e) {
    setEditingId(e.id);
    setForm({ patientId: String(e.patientId), examItem: e.examItem, clinicalDiagnosis: e.clinicalDiagnosis || '', priority: e.priority || '普通' });
  }
  function resetForm() {
    setEditingId(null);
    setForm({ patientId: '', examItem: '', clinicalDiagnosis: '', priority: '普通' });
  }

  async function submit(event) {
    event.preventDefault();
    const body = { ...form, patientId: Number(form.patientId) };
    if (editingId) {
      await runAction('saveExam', '保存检查申请', () => api.updateExam(editingId, body), (updated) => {
        setExams((current) => current.map((x) => (x.id === updated.id ? { ...x, ...updated } : x)));
        resetForm();
      });
    } else {
      await runAction('createExam', '创建检查申请', () => api.createExam(body), (created) => {
        setExams((current) => [created, ...current.filter((x) => x.id !== created.id)]);
        resetForm();
      });
    }
  }

  async function act(e, action, label, key) {
    await runAction(`${key}-${e.id}`, label, () => action(e.id), (updated) => {
      setExams((current) => current.map((x) => (x.id === updated.id ? { ...x, ...updated } : x)));
    });
  }

  async function start(e) {
    const risk = await runAction(
      `risk-${e.id}`,
      '复核安全风险',
      () => api.examRisk(e.id),
      undefined,
      { log: false, notify: false },
    );
    if (!risk) return;
    let confirmed = false;
    if (risk.level === 'HIGH') {
      const details = (risk.items || []).map((item) =>
        `${item.type || '未命名禁忌症'}：${item.description || '未填写说明'}（${item.severity === 'LOW' ? '低风险' : '高风险'}）`,
      );
      confirmed = await ask({
        title: '高风险检查确认',
        message: `开始检查前发现以下 MRI 安全风险：\n${details.join('\n')}\n\n请确认已完成风险评估。`,
        confirmText: '确认已评估风险并开始检查',
        tone: 'warning',
      });
      if (!confirmed) return;
    }
    await act(e, (id) => api.startExam(id, confirmed), '开始检查', 'start');
  }

  async function cancel(e) {
    const ok = await ask({ message: `确定取消「${patientName(e.patientId)}」的检查申请吗？` });
    if (!ok) return;
    await act(e, api.cancelExam, '取消检查申请', 'cancel');
  }

  async function remove(e) {
    const ok = await ask({ message: `确定删除「${patientName(e.patientId)}」的检查申请吗？此操作不可撤销，相关排程将一并删除。` });
    if (!ok) return;
    await runAction(`del-exam-${e.id}`, '删除检查申请', () => api.deleteExam(e.id), () => {
      setExams((current) => current.filter((x) => x.id !== e.id));
    });
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="检查申请"
        subtitle="登记检查申请、安排排程、执行与取消"
        actions={<Button icon={RefreshCw} variant="secondary" onClick={() => refreshExams(status)} busy={busyKey === 'exams'}>刷新</Button>}
      />

      <section className="panel">
        <SectionHeader icon={ClipboardList} title="检查申请列表" actions={
          <label className="inline-field">
            <span>状态</span>
            <SelectField name="status" value={status} onChange={onStatusChange} options={STATUS_OPTIONS} />
          </label>
        } />
        <DataTable
          rows={exams}
          emptyText="暂无检查申请"
          rowKey={(e) => e.id}
          columns={[
            { key: 'id', label: '编号' },
            { key: 'patient', label: '患者', render: (e) => patientName(e.patientId) },
            { key: 'examItem', label: '检查项目' },
            { key: 'clinicalDiagnosis', label: '临床诊断' },
            { key: 'priority', label: '优先级', render: (e) => <StatusTag tone={e.priority === '加急' ? 'danger' : 'neutral'}>{e.priority || '普通'}</StatusTag> },
            {
              key: 'risk',
              label: '安全风险',
              render: (e) => {
                const view = riskView(e.riskLevel);
                return <div className="risk-cell"><StatusTag tone={view.tone}>{view.label}</StatusTag>{e.riskSummary ? <small>{e.riskSummary}</small> : null}</div>;
              },
            },
            { key: 'status', label: '状态', render: (e) => <StatusTag status={e.status} /> },
            {
              key: 'actions',
              label: '操作',
              render: (e) => (
                <div className="table-actions">
                  <Button icon={Play} variant="ghost" disabled={e.status !== 'REQUESTED'} onClick={() => start(e)}>开始</Button>
                  <Button icon={CheckCircle2} variant="ghost" disabled={e.status !== 'IN_PROGRESS'} onClick={() => act(e, api.completeExam, '完成检查', 'complete')}>完成</Button>
                  <Button icon={Ban} variant="ghost" disabled={e.status !== 'REQUESTED' && e.status !== 'IN_PROGRESS'} onClick={() => cancel(e)}>取消</Button>
                  <Button icon={Pencil} variant="ghost" disabled={e.status !== 'REQUESTED'} onClick={() => startEdit(e)}>编辑</Button>
                  <Button icon={Trash2} variant="ghost" onClick={() => remove(e)}>删除</Button>
                </div>
              ),
            },
          ]}
        />
      </section>

      {editingId ? (
        <section className="panel">
          <SectionHeader icon={Pencil} title="编辑检查申请" actions={<Button variant="ghost" onClick={resetForm}>取消</Button>} />
          <form className="form-grid" onSubmit={submit}>
            <SelectField label="患者" name="patientId" value={form.patientId} onChange={update} options={patientOptions} placeholder="选择患者" />
            <TextField label="检查项目" name="examItem" value={form.examItem} onChange={update} placeholder="如：头颅MRI平扫" />
            <TextField label="临床诊断" name="clinicalDiagnosis" value={form.clinicalDiagnosis} onChange={update} placeholder="如：头痛待查" />
            <SelectField label="优先级" name="priority" value={form.priority} onChange={update} options={['普通', '加急']} />
            <div className="form-submit"><Button icon={Send} busy={busyKey === 'saveExam'}>保存</Button></div>
          </form>
        </section>
      ) : null}

      <SchedulesSection />
      {dialog}
    </div>
  );
}

function SchedulesSection() {
  const { exams, runAction, patientName, busyKey } = useApp();
  const [examId, setExamId] = useState(exams[0] ? String(exams[0].id) : '');
  const [schedules, setSchedules] = useState([]);
  const [form, setForm] = useState({ scannerRoom: 'MRI-01', scheduledAt: '', technologist: '', durationMinutes: '30' });
  const { ask, dialog } = useConfirm();

  useEffect(() => {
    if (!examId) { setSchedules([]); return; }
    api.schedules(examId).then(setSchedules).catch(() => setSchedules([]));
  }, [examId]);

  function update(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function create(event) {
    event.preventDefault();
    await runAction('createSchedule', '新增排程', () => api.createSchedule({
      ...form,
      examOrderId: Number(examId),
      durationMinutes: Number(form.durationMinutes),
    }), (created) => {
      setSchedules((current) => [created, ...current]);
      setForm({ scannerRoom: 'MRI-01', scheduledAt: '', technologist: '', durationMinutes: '30' });
    });
  }

  async function remove(s) {
    const ok = await ask({ message: '确定删除该排程吗？' });
    if (!ok) return;
    await runAction(`del-schedule-${s.id}`, '删除排程', () => api.deleteSchedule(s.id), () => {
      setSchedules((current) => current.filter((x) => x.id !== s.id));
    });
  }

  const examOptions = exams.map((e) => ({ value: String(e.id), label: `${patientName(e.patientId)} · ${e.examItem}` }));

  return (
    <section className="panel">
      <SectionHeader icon={CalendarDays} title="检查排程" />
      <label className="field inline-field">
        <span>检查申请</span>
        <SelectField name="examId" value={examId} onChange={(e) => setExamId(e.target.value)} options={examOptions} placeholder="选择检查申请" />
      </label>
      <form className="form-grid compact" onSubmit={create}>
        <TextField label="检查室" name="scannerRoom" value={form.scannerRoom} onChange={update} />
        <TextField label="排程时间" name="scheduledAt" type="datetime-local" value={form.scheduledAt} onChange={update} />
        <TextField label="技师" name="technologist" value={form.technologist} onChange={update} />
        <SelectField label="检查时长" name="durationMinutes" value={form.durationMinutes} onChange={update} options={[
          { value: '30', label: '30 分钟' },
          { value: '45', label: '45 分钟' },
          { value: '60', label: '60 分钟' },
        ]} />
        <div className="form-submit"><Button icon={CalendarDays} variant="secondary" busy={busyKey === 'createSchedule'} disabled={!examId}>新增排程</Button></div>
      </form>
      <p className="muted">冲突检测覆盖全部未取消的检查申请；如提示其他检查申请占用，请切换上方检查申请查看或删除。</p>
      {schedules.length ? (
        <div className="schedule-strip">
          {schedules.map((s) => (
            <span key={s.id}>
              {s.scannerRoom} · {formatScheduleRange(s.scheduledAt, s.durationMinutes)} · {s.technologist ? `技师${s.technologist}` : '未指定技师'}
              <button className="link-danger" onClick={() => remove(s)}>删除</button>
            </span>
          ))}
        </div>
      ) : <p className="muted">暂无排程。</p>}
      {dialog}
    </section>
  );
}
