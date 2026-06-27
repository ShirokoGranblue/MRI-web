import { Link } from 'react-router-dom';
import { Activity, ArrowRight, ClipboardList, FileText, Image, Users } from 'lucide-react';
import { useApp, statusLabel } from '../lib/app-context.jsx';
import { Metric, PageHeader, SectionHeader, StatusTag } from '../components/ui.jsx';

const flow = ['患者登记', '检查申请', '执行检查', '影像归档', '诊断报告'];

export default function DashboardPage() {
  const { patients, exams, studies, reports, logs, username } = useApp();
  const pendingExams = exams.filter((e) => e.status === 'REQUESTED').length;
  const inProgressExams = exams.filter((e) => e.status === 'IN_PROGRESS').length;
  const pendingReview = reports.filter((r) => r.status === 'SUBMITTED').length;
  const draftReports = reports.filter((r) => r.status === 'DRAFT').length;

  const todos = [
    { count: pendingExams, label: '待检查申请', to: '/exams?status=REQUESTED', tone: 'warning' },
    { count: inProgressExams, label: '检查中', to: '/exams?status=IN_PROGRESS', tone: 'info' },
    { count: pendingReview, label: '待审核报告', to: '/reports?status=SUBMITTED', tone: 'info' },
    { count: draftReports, label: '草稿报告', to: '/reports?status=DRAFT', tone: 'neutral' },
  ].filter((t) => t.count > 0);

  return (
    <div className="page-stack">
      <PageHeader title={`欢迎，${username || '医生'}`} subtitle="医院核磁共振影像管理工作台" />

      <section className="metric-grid">
        <Metric label="患者档案" value={patients.length} detail="在管" tone="info" />
        <Metric label="检查申请" value={exams.length} detail="申请单" tone="warning" />
        <Metric label="影像检查" value={studies.length} detail="已归档" tone="success" />
        <Metric label="诊断报告" value={reports.length} detail="报告" tone="neutral" />
      </section>

      <section className="panel">
        <SectionHeader icon={Activity} title="检查流程" />
        <div className="workflow">
          {flow.map((item, index) => (
            <div className="workflow-step" key={item}>
              <span>{index + 1}</span>
              <strong>{item}</strong>
              {index < flow.length - 1 ? <ArrowRight size={14} className="workflow-arrow" aria-hidden="true" /> : null}
            </div>
          ))}
        </div>
      </section>

      <section className="panel">
        <SectionHeader icon={ClipboardList} title="待办事项" />
        {todos.length ? (
          <div className="todo-grid">
            {todos.map((t) => (
              <Link key={t.label} to={t.to} className="todo-card">
                <strong>{t.count}</strong>
                <span>{t.label}</span>
                <StatusTag tone={t.tone}>前往处理</StatusTag>
              </Link>
            ))}
          </div>
        ) : (
          <p className="muted">暂无待办事项。</p>
        )}
      </section>

      <section className="panel">
        <SectionHeader icon={Activity} title="最近操作" actions={<Link to="/activity" className="link">查看全部</Link>} />
        <div className="log-list">
          {logs.slice(0, 5).map((item) => (
            <div className={`log-row ${item.type}`} key={item.id}>
              <span>{item.time}</span>
              <strong>{item.title}</strong>
              <p>{item.detail}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
