import { Activity } from 'lucide-react';
import { useApp } from '../lib/app-context.jsx';
import { PageHeader, SectionHeader } from '../components/ui.jsx';

export default function ActivityPage() {
  const { logs } = useApp();
  return (
    <div className="page-stack">
      <PageHeader title="操作记录" subtitle="所有操作的成功与失败记录" />
      <section className="panel">
        <SectionHeader icon={Activity} title={`操作记录（${logs.length}）`} />
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
    </div>
  );
}
