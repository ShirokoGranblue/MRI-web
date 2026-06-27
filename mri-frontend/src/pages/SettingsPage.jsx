import { useEffect, useState } from 'react';
import { RefreshCw, Settings2 } from 'lucide-react';
import { api } from '../lib/api.js';
import { useApp } from '../lib/app-context.jsx';
import { Button, PageHeader, SectionHeader, StatusTag } from '../components/ui.jsx';

export default function SettingsPage() {
  const { runAction, busyKey } = useApp();
  const [config, setConfig] = useState({ watermark: '医院MRI影像系统', downloadEnabled: true });

  useEffect(() => {
    load();
  }, []);

  async function load() {
    await runAction('config', '读取设置', api.demoConfig, setConfig);
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="系统设置"
        subtitle="报告水印与影像下载开关（由配置中心统一管理）"
        actions={<Button icon={RefreshCw} variant="secondary" onClick={load} busy={busyKey === 'config'}>重新读取设置</Button>}
      />
      <section className="panel">
        <SectionHeader icon={Settings2} title="当前设置" />
        <div className="config-box">
          <div>
            <span>报告水印</span>
            <strong>{config.watermark || '-'}</strong>
          </div>
          <div>
            <span>允许下载影像</span>
            <StatusTag tone={config.downloadEnabled ? 'success' : 'danger'}>
              {config.downloadEnabled ? '开启' : '关闭'}
            </StatusTag>
          </div>
        </div>
        <p className="muted">如需修改，请在配置中心调整后点击「重新读取设置」。</p>
      </section>
    </div>
  );
}
