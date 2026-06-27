import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Ban, CheckCircle2, FileText, History, Pencil, Plus, RefreshCw, RotateCcw, Send, Trash2, UploadCloud, X } from 'lucide-react';
import { api } from '../lib/api.js';
import { statusLabel, useApp } from '../lib/app-context.jsx';
import { Button, DataTable, PageHeader, SectionHeader, SelectField, StatusTag, TextAreaField, useConfirm } from '../components/ui.jsx';

const STATUS_OPTIONS = [
  { value: '', label: '全部' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'SUBMITTED', label: '待审核' },
  { value: 'APPROVED', label: '已审核' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'PUBLISHED', label: '已发布' },
];

export default function ReportsPage() {
  const { reports, setReports, exams, studies, runAction, refreshReports, examLabel, studyLabel, busyKey } = useApp();
  const [params, setParams] = useSearchParams();
  const [status, setStatus] = useState(params.get('status') || '');
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({ examOrderId: '', studyId: '', findings: '', impression: '' });
  const [detail, setDetail] = useState(null);
  const { ask, dialog } = useConfirm();

  const examOptions = exams.map((e) => ({ value: String(e.id), label: examLabel(e.id) }));
  const studyOptions = studies
    .filter((s) => !form.examOrderId || s.examOrderId === Number(form.examOrderId))
    .map((s) => ({ value: String(s.id), label: studyLabel(s.id) }));

  function update(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }
  function onStatusChange(event) {
    const value = event.target.value;
    setStatus(value);
    setParams(value ? { status: value } : {});
    refreshReports(value);
  }
  function startEdit(r) {
    setEditingId(r.id);
    setForm({ examOrderId: String(r.examOrderId), studyId: String(r.studyId), findings: r.findings || '', impression: r.impression || '' });
  }
  function resetForm() {
    setEditingId(null);
    setForm({ examOrderId: '', studyId: '', findings: '', impression: '' });
  }

  async function submit(event) {
    event.preventDefault();
    const body = { ...form, examOrderId: Number(form.examOrderId), studyId: Number(form.studyId) };
    if (editingId) {
      await runAction('saveReport', '保存报告', () => api.updateReport(editingId, body), (updated) => {
        setReports((current) => current.map((r) => (r.id === updated.id ? { ...r, ...updated } : r)));
        resetForm();
      });
    } else {
      await runAction('createReport', '新增报告', () => api.createReport(body), (created) => {
        setReports((current) => [created, ...current.filter((r) => r.id !== created.id)]);
        resetForm();
      });
    }
  }

  async function act(r, action, label, key) {
    await runAction(`${key}-${r.id}`, label, () => action(r.id), (updated) => {
      setReports((current) => current.map((x) => (x.id === updated.id ? { ...x, ...updated } : x)));
    });
  }

  async function reject(r) {
    const reason = window.prompt('驳回原因：', '退回修改');
    if (reason === null) return;
    await act(r, (id) => api.rejectReport(id, reason), '驳回报告', 'reject');
  }

  async function remove(r) {
    const ok = await ask({ message: `确定删除该报告吗？此操作不可撤销。` });
    if (!ok) return;
    await runAction(`del-report-${r.id}`, '删除报告', () => api.deleteReport(r.id), () => {
      setReports((current) => current.filter((x) => x.id !== r.id));
    });
  }

  async function openAudit(r) {
    const logs = await api.auditLogs(r.id).catch(() => []);
    setDetail({ report: r, logs });
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="诊断报告"
        subtitle="编写、提交、审核、驳回、回到草稿、发布"
        actions={<Button icon={RefreshCw} variant="secondary" onClick={() => refreshReports(status)} busy={busyKey === 'reports'}>刷新</Button>}
      />

      <section className="panel">
        <SectionHeader icon={FileText} title="报告列表" actions={
          <label className="inline-field"><span>状态</span>
            <SelectField name="status" value={status} onChange={onStatusChange} options={STATUS_OPTIONS} />
          </label>
        } />
        <DataTable
          rows={reports}
          emptyText="暂无报告"
          rowKey={(r) => r.id}
          columns={[
            { key: 'id', label: '编号' },
            { key: 'exam', label: '检查', render: (r) => examLabel(r.examOrderId) },
            { key: 'findings', label: '影像所见', render: (r) => <span className="cell-ellipsis">{r.findings || '-'}</span> },
            { key: 'impression', label: '诊断意见', render: (r) => <span className="cell-ellipsis">{r.impression || '-'}</span> },
            { key: 'status', label: '状态', render: (r) => <StatusTag status={r.status} /> },
            {
              key: 'actions',
              label: '操作',
              render: (r) => (
                <div className="table-actions">
                  <Button icon={Send} variant="ghost" disabled={r.status !== 'DRAFT'} onClick={() => act(r, api.submitReport, '提交审核', 'submit')}>提交</Button>
                  <Button icon={CheckCircle2} variant="ghost" disabled={r.status !== 'SUBMITTED'} onClick={() => act(r, api.approveReport, '审核通过', 'approve')}>审核</Button>
                  <Button icon={Ban} variant="ghost" disabled={r.status !== 'SUBMITTED'} onClick={() => reject(r)}>驳回</Button>
                  <Button icon={RotateCcw} variant="ghost" disabled={r.status !== 'REJECTED'} onClick={() => act(r, api.reopenReport, '回到草稿', 'reopen')}>回到草稿</Button>
                  <Button icon={UploadCloud} variant="ghost" disabled={r.status !== 'APPROVED'} onClick={() => act(r, api.publishReport, '发布报告', 'publish')}>发布</Button>
                  <Button icon={Pencil} variant="ghost" disabled={r.status !== 'DRAFT' && r.status !== 'REJECTED'} onClick={() => startEdit(r)}>编辑</Button>
                  <Button icon={History} variant="ghost" onClick={() => openAudit(r)}>日志</Button>
                  <Button icon={Trash2} variant="ghost" onClick={() => remove(r)}>删除</Button>
                </div>
              ),
            },
          ]}
        />
      </section>

      <section className="panel">
        <SectionHeader icon={editingId ? Pencil : Plus} title={editingId ? '编辑报告' : '新增报告'} actions={editingId ? <Button variant="ghost" onClick={resetForm}>取消</Button> : null} />
        <form className="form-grid" onSubmit={submit}>
          <SelectField label="检查申请" name="examOrderId" value={form.examOrderId} onChange={update} options={examOptions} placeholder="选择检查" />
          <SelectField label="影像检查" name="studyId" value={form.studyId} onChange={update} options={studyOptions} placeholder="选择影像" />
          <TextAreaField label="影像所见" name="findings" value={form.findings} onChange={update} />
          <TextAreaField label="诊断意见" name="impression" value={form.impression} onChange={update} />
          <div className="form-submit"><Button icon={Send} busy={busyKey === 'createReport' || busyKey === 'saveReport'}>{editingId ? '保存' : '新增报告'}</Button></div>
        </form>
      </section>

      {detail ? <ReportAudit detail={detail} onClose={() => setDetail(null)} /> : null}
      {dialog}
    </div>
  );
}

function ReportAudit({ detail, onClose }) {
  const { report, logs } = detail;
  return (
    <div className="dialog-overlay" role="dialog" aria-modal="true">
      <div className="dialog-card dialog-wide">
        <div className="dialog-head dialog-head-row">
          <div>
            <strong>报告审核日志</strong>
            <span className="muted"> 编号 {report.id}</span>
          </div>
          <Button icon={X} variant="ghost" onClick={onClose}>关闭</Button>
        </div>
        {logs.length ? (
          <div className="audit-timeline">
            {logs.map((l, i) => (
              <div className="audit-item" key={i}>
                <span className="audit-dot" />
                <div>
                  <strong>{l.action}</strong>
                  <span className="muted"> {l.operator} · {l.operatedAt ? String(l.operatedAt).replace('T', ' ').slice(0, 16) : '—'}</span>
                  <p>{l.comment || '-'}</p>
                </div>
              </div>
            ))}
          </div>
        ) : <p className="muted">暂无审核记录。</p>}
      </div>
    </div>
  );
}
