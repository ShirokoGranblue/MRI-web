import { useState } from 'react';
import { Eye, Pencil, Plus, RefreshCw, Send, Trash2, X } from 'lucide-react';
import { api } from '../lib/api.js';
import { statusLabel, useApp } from '../lib/app-context.jsx';
import { Button, DataTable, PageHeader, SearchBar, SectionHeader, SelectField, StatusTag, TextField, useConfirm } from '../components/ui.jsx';

function nextPatientNo() {
  return `P${new Date().toISOString().slice(0, 10).replaceAll('-', '')}${String(Math.floor(Math.random() * 9000) + 1000)}`;
}

const emptyForm = () => ({ patientNo: nextPatientNo(), name: '', gender: '男', birthDate: '1990-01-01', phone: '' });

export default function PatientsPage() {
  const { patients, setPatients, runAction, refreshPatients, busyKey } = useApp();
  const [keyword, setKeyword] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [detail, setDetail] = useState(null);
  const { ask, dialog } = useConfirm();

  function update(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }
  function resetForm() {
    setForm(emptyForm());
    setEditingId(null);
  }
  function startEdit(p) {
    setEditingId(p.id);
    setForm({ patientNo: p.patientNo, name: p.name, gender: p.gender, birthDate: p.birthDate, phone: p.phone });
  }

  async function submit(event) {
    event.preventDefault();
    if (editingId) {
      await runAction('savePatient', '保存患者', () => api.updatePatient(editingId, form), (updated) => {
        setPatients((current) => current.map((p) => (p.id === updated.id ? { ...p, ...updated } : p)));
        resetForm();
      });
    } else {
      await runAction('createPatient', '新增患者', () => api.createPatient(form), (created) => {
        setPatients((current) => [created, ...current.filter((p) => p.id !== created.id)]);
        resetForm();
      });
    }
  }

  async function remove(p) {
    const ok = await ask({ message: `确定删除患者「${p.name}」的档案吗？此操作不可撤销。` });
    if (!ok) return;
    await runAction(`del-patient-${p.id}`, '删除患者', () => api.deletePatient(p.id), () => {
      setPatients((current) => current.filter((x) => x.id !== p.id));
    });
  }

  async function openDetail(p) {
    const [contraindications, history] = await Promise.all([
      api.contraindications(p.id).catch(() => []),
      api.examHistory(p.id).catch(() => []),
    ]);
    setDetail({ patient: p, contraindications, history });
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="患者档案"
        subtitle="登记患者信息、MRI 禁忌症与检查历史"
        actions={<Button icon={RefreshCw} variant="secondary" onClick={() => refreshPatients(keyword)} busy={busyKey === 'patients'}>刷新</Button>}
      />
      <div className="toolbar">
        <SearchBar value={keyword} onChange={setKeyword} placeholder="按姓名或编号搜索" onSearch={() => refreshPatients(keyword)} />
      </div>

      <section className="panel">
        <SectionHeader
          icon={editingId ? Pencil : Plus}
          title={editingId ? '编辑患者' : '新增患者'}
          actions={editingId ? <Button variant="ghost" onClick={resetForm}>取消</Button> : null}
        />
        <form className="form-grid" onSubmit={submit}>
          <TextField label="患者编号" name="patientNo" value={form.patientNo} onChange={update} />
          <TextField label="姓名" name="name" value={form.name} onChange={update} />
          <SelectField label="性别" name="gender" value={form.gender} onChange={update} options={['男', '女']} />
          <TextField label="出生日期" name="birthDate" type="date" value={form.birthDate} onChange={update} />
          <TextField label="联系电话" name="phone" value={form.phone} onChange={update} />
          <div className="form-submit">
            <Button icon={Send} busy={busyKey === 'savePatient' || busyKey === 'createPatient'}>{editingId ? '保存' : '新增患者'}</Button>
          </div>
        </form>
      </section>

      <section className="panel">
        <SectionHeader icon={Plus} title={`患者列表（${patients.length}）`} />
        <DataTable
          rows={patients}
          emptyText="暂无患者档案"
          columns={[
            { key: 'patientNo', label: '患者编号' },
            { key: 'name', label: '姓名' },
            { key: 'gender', label: '性别' },
            { key: 'birthDate', label: '出生日期' },
            { key: 'phone', label: '联系电话' },
            {
              key: 'actions',
              label: '操作',
              render: (p) => (
                <div className="table-actions">
                  <Button icon={Eye} variant="ghost" onClick={() => openDetail(p)}>详情</Button>
                  <Button icon={Pencil} variant="ghost" onClick={() => startEdit(p)}>编辑</Button>
                  <Button icon={Trash2} variant="ghost" onClick={() => remove(p)}>删除</Button>
                </div>
              ),
            },
          ]}
        />
      </section>

      {detail ? <PatientDetail detail={detail} onClose={() => setDetail(null)} /> : null}
      {dialog}
    </div>
  );
}

function PatientDetail({ detail, onClose }) {
  const { runAction, busyKey } = useApp();
  const [contras, setContras] = useState(detail.contraindications);
  const [history] = useState(detail.history);
  const [cform, setCform] = useState({ type: '心脏起搏器', description: '', severity: 'HIGH' });
  const { ask, dialog } = useConfirm();
  const { patient } = detail;

  function updateC(event) {
    const { name, value } = event.target;
    setCform((current) => ({ ...current, [name]: value }));
  }

  async function addContra(event) {
    event.preventDefault();
    await runAction('addContra', '登记禁忌症', () => api.createContraindication(patient.id, cform), (created) => {
      setContras((current) => [created, ...current]);
      setCform({ type: '心脏起搏器', description: '', severity: 'HIGH' });
    });
  }

  async function removeContra(c) {
    const ok = await ask({ message: `确定删除该禁忌症记录吗？` });
    if (!ok) return;
    await runAction(`del-contra-${c.id}`, '删除禁忌症', () => api.deleteContraindication(c.id), () => {
      setContras((current) => current.filter((x) => x.id !== c.id));
    });
  }

  return (
    <div className="dialog-overlay" role="dialog" aria-modal="true">
      <div className="dialog-card dialog-wide">
        <div className="dialog-head dialog-head-row">
          <div>
            <strong>{patient.name}</strong>
            <span className="muted"> {patient.patientNo} · {patient.gender} · {patient.birthDate}</span>
          </div>
          <Button icon={X} variant="ghost" onClick={onClose}>关闭</Button>
        </div>

        <section className="detail-section">
          <SectionHeader icon={Plus} title="MRI 禁忌症" />
          {contras.length ? (
            <div className="contra-list">
              {contras.map((c) => (
                <div className="contra-row" key={c.id}>
                  <StatusTag tone={c.severity === 'HIGH' ? 'danger' : 'warning'}>{c.type}</StatusTag>
                  <span className="contra-desc">{c.description || '-'}</span>
                  <Button icon={Trash2} variant="ghost" onClick={() => removeContra(c)}>删除</Button>
                </div>
              ))}
            </div>
          ) : (
            <p className="muted">暂无禁忌症记录。</p>
          )}
          <form className="form-grid compact" onSubmit={addContra}>
            <SelectField label="类型" name="type" value={cform.type} onChange={updateC} options={['心脏起搏器', '金属植入物', '幽闭恐惧', '肾功能不全', '妊娠']} />
            <TextField label="说明" name="description" value={cform.description} onChange={updateC} placeholder="如：左膝钛合金内固定" />
            <SelectField label="严重程度" name="severity" value={cform.severity} onChange={updateC} options={[{ value: 'HIGH', label: '高' }, { value: 'LOW', label: '低' }]} />
            <div className="form-submit"><Button icon={Send} variant="secondary" busy={busyKey === 'addContra'}>登记</Button></div>
          </form>
        </section>

        <section className="detail-section">
          <SectionHeader icon={Plus} title="检查历史" />
          {history.length ? (
            <div className="log-list">
              {history.map((h, i) => (
                <div className="log-row info" key={i}>
                  <span>{h.examTime ? String(h.examTime).replace('T', ' ').slice(0, 16) : '—'}</span>
                  <strong>{h.examItem}</strong>
                  <p>状态：<StatusTag tone={statusToneFor(h.status)}>{statusLabel[h.status] || h.status}</StatusTag></p>
                </div>
              ))}
            </div>
          ) : (
            <p className="muted">暂无检查记录。</p>
          )}
        </section>
        {dialog}
      </div>
    </div>
  );
}

function statusToneFor(status) {
  return ({ COMPLETED: 'success', REPORT_PUBLISHED: 'success', CANCELLED: 'danger', REQUESTED: 'warning', IN_PROGRESS: 'info' })[status] || 'neutral';
}
