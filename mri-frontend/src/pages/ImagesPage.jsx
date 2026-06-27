import { useEffect, useState } from 'react';
import { Archive, Eye, Image, Plus, RefreshCw, Trash2, UploadCloud, Zap } from 'lucide-react';
import { api } from '../lib/api.js';
import { statusLabel, useApp } from '../lib/app-context.jsx';
import { Button, DataTable, EmptyState, PageHeader, SectionHeader, SelectField, StatusTag, TextField, useConfirm } from '../components/ui.jsx';

function ImageThumb({ file }) {
  const [url, setUrl] = useState(null);
  useEffect(() => {
    let current = null;
    setUrl(null);
    api.fileContent(file.id)
      .then((blob) => {
        const objectUrl = URL.createObjectURL(blob);
        current = objectUrl;
        setUrl(objectUrl);
      })
      .catch(() => setUrl(null));
    return () => {
      if (current) URL.revokeObjectURL(current);
    };
  }, [file.id]);
  return url ? (
    <img src={url} alt={file.fileName} className="scan-thumb-img" />
  ) : (
    <div className="scan-thumb thumb-0" aria-label="影像占位" />
  );
}

export default function ImagesPage() {
  const { studies, setStudies, exams, runAction, refreshStudies, patientName, examLabel, busyKey } = useApp();
  const [archiveForm, setArchiveForm] = useState({ examOrderId: '', description: '' });
  const [selectedId, setSelectedId] = useState(null);
  const [manifest, setManifest] = useState(null);
  const [seriesForm, setSeriesForm] = useState({ seriesName: '', bodyPosition: '头部' });
  const [uploadSeriesId, setUploadSeriesId] = useState('');
  const [files, setFiles] = useState([]);
  const { ask, dialog } = useConfirm();

  const examOptions = exams
    .filter((e) => e.status === 'COMPLETED')
    .map((e) => ({ value: String(e.id), label: `${patientName(e.patientId)} · ${e.examItem}` }));

  function updateArchive(event) {
    const { name, value } = event.target;
    setArchiveForm((current) => ({ ...current, [name]: value }));
  }
  function updateSeries(event) {
    const { name, value } = event.target;
    setSeriesForm((current) => ({ ...current, [name]: value }));
  }

  async function archive(event) {
    event.preventDefault();
    const body = { ...archiveForm, examOrderId: Number(archiveForm.examOrderId) };
    await runAction('archiveStudy', '保存影像', () => api.archiveStudy(body), (created) => {
      setStudies((current) => [created, ...current.filter((x) => x.id !== created.id)]);
      setArchiveForm({ examOrderId: '', description: '' });
    });
  }

  async function removeStudy(s) {
    const ok = await ask({ message: `确定删除影像「${s.description || s.id}」吗？其下所有序列与影像将一并删除。` });
    if (!ok) return;
    await runAction(`del-study-${s.id}`, '删除影像', () => api.deleteStudy(s.id), () => {
      setStudies((current) => current.filter((x) => x.id !== s.id));
      if (selectedId === s.id) {
        setSelectedId(null);
        setManifest(null);
      }
    });
  }

  async function loadManifest(studyId) {
    const data = await api.viewerManifest(studyId).catch(() => null);
    setManifest(data);
    if (data?.series?.length && !uploadSeriesId) {
      setUploadSeriesId(String(data.series[0].seriesId));
    }
  }

  function selectStudy(s) {
    setSelectedId(s.id);
    loadManifest(s.id);
  }

  async function addSeries(event) {
    event.preventDefault();
    await runAction('createSeries', '添加序列', () => api.createSeries(selectedId, seriesForm), (created) => {
      loadManifest(selectedId);
      setSeriesForm({ seriesName: '', bodyPosition: '头部' });
    });
  }

  async function removeSeries(series) {
    const ok = await ask({ message: `确定删除序列「${series.seriesName}」及其影像吗？` });
    if (!ok) return;
    await runAction(`del-series-${series.seriesId}`, '删除序列', () => api.deleteSeries(series.seriesId), () => {
      loadManifest(selectedId);
    });
  }

  async function upload() {
    if (!uploadSeriesId || !files.length) return;
    const results = [];
    for (const file of files) {
      const created = await runAction(`upload-${file.name}`, '上传影像', () => api.uploadFile(selectedId, uploadSeriesId, file));
      if (created) results.push(created);
    }
    setFiles([]);
    if (results.length) loadManifest(selectedId);
  }

  async function removeFile(f) {
    const ok = await ask({ message: `确定删除影像「${f.fileName}」吗？` });
    if (!ok) return;
    await runAction(`del-file-${f.id}`, '删除影像', () => api.deleteFile(f.id), () => {
      loadManifest(selectedId);
    });
  }

  async function quickPreview() {
    await runAction('cacheDemo', '快速预览', () => api.cacheDemo(selectedId));
  }

  const allFiles = manifest?.series?.flatMap((s) => (s.files || []).map((f) => ({ ...f, seriesName: s.seriesName }))) || [];
  const seriesOptions = manifest?.series?.map((s) => ({ value: String(s.seriesId), label: s.seriesName })) || [];

  return (
    <div className="page-stack">
      <PageHeader
        title="影像归档"
        subtitle="完成检查后归档影像、上传图像、按患者浏览与管理"
        actions={<Button icon={RefreshCw} variant="secondary" onClick={() => refreshStudies()} busy={busyKey === 'studies'}>刷新</Button>}
      />

      <section className="panel">
        <SectionHeader icon={Archive} title="保存影像检查" />
        <form className="form-grid" onSubmit={archive}>
          <SelectField label="检查申请" name="examOrderId" value={archiveForm.examOrderId} onChange={updateArchive} options={examOptions} placeholder="选择已完成的检查" />
          <TextField label="影像描述" name="description" value={archiveForm.description} onChange={updateArchive} placeholder="如：头颅MRI平扫" />
          <div className="form-submit"><Button icon={Archive} busy={busyKey === 'archiveStudy'}>保存影像</Button></div>
        </form>
      </section>

      <section className="panel">
        <SectionHeader icon={Image} title={`影像检查列表（${studies.length}）`} />
        <DataTable
          rows={studies}
          emptyText="暂无影像检查"
          rowKey={(s) => s.id}
          columns={[
            { key: 'id', label: '影像编号' },
            { key: 'patient', label: '患者', render: (s) => examLabel(s.examOrderId) },
            { key: 'description', label: '描述' },
            { key: 'status', label: '状态', render: (s) => <StatusTag status={s.status} /> },
            {
              key: 'actions',
              label: '操作',
              render: (s) => (
                <div className="table-actions">
                  <Button icon={Eye} variant="ghost" onClick={() => selectStudy(s)}>影像管理</Button>
                  <Button icon={Trash2} variant="ghost" onClick={() => removeStudy(s)}>删除</Button>
                </div>
              ),
            },
          ]}
        />
      </section>

      {selectedId ? (
        <section className="panel">
          <SectionHeader
            icon={Image}
            title={`影像管理 · 检查 ${selectedId}`}
            actions={<Button icon={Zap} variant="secondary" onClick={quickPreview} busy={busyKey === 'cacheDemo'}>快速预览</Button>}
          />

          <form className="form-grid compact" onSubmit={addSeries}>
            <TextField label="序列名称" name="seriesName" value={seriesForm.seriesName} onChange={updateSeries} placeholder="如：T1_AX" />
            <TextField label="体位" name="bodyPosition" value={seriesForm.bodyPosition} onChange={updateSeries} />
            <div className="form-submit"><Button icon={Plus} variant="secondary" busy={busyKey === 'createSeries'}>添加序列</Button></div>
          </form>

          <div className="series-list">
            {manifest?.series?.length ? (
              manifest.series.map((s) => (
                <div className="series-row" key={s.seriesId}>
                  <span>{s.seriesName}</span>
                  <strong>{s.files?.length || 0} 张</strong>
                  <Button icon={Trash2} variant="ghost" onClick={() => removeSeries(s)}>删除序列</Button>
                </div>
              ))
            ) : (
              <p className="muted">暂无序列，请先添加序列再上传影像。</p>
            )}
          </div>

          <div className="upload-zone">
            <SelectField label="上传到序列" name="uploadSeriesId" value={uploadSeriesId} onChange={(e) => setUploadSeriesId(e.target.value)} options={seriesOptions} placeholder="选择序列" />
            <label className="file-picker">
              <input type="file" accept="image/*" multiple onChange={(e) => setFiles(Array.from(e.target.files || []))} />
              <span>{files.length ? `已选择 ${files.length} 个文件` : '选择影像文件（可多选）'}</span>
            </label>
            <Button icon={UploadCloud} onClick={upload} disabled={!files.length || !uploadSeriesId} busy={busyKey?.startsWith('upload-')}>上传影像</Button>
          </div>

          {allFiles.length ? (
            <div className="scan-grid">
              {allFiles.map((f) => (
                <div className="scan-item" key={f.id}>
                  <ImageThumb file={f} />
                  <strong>{f.seriesName}</strong>
                  <span>{f.fileName}</span>
                  <Button icon={Trash2} variant="ghost" onClick={() => removeFile(f)}>删除</Button>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState text="暂无影像文件，请上传。" icon={Image} />
          )}
        </section>
      ) : (
        <section className="panel">
          <p className="muted">暂未选择影像检查。点击上方记录的「影像管理」可进入后添加序列、上传图像与预览。</p>
        </section>
      )}
      {dialog}
    </div>
  );
}
