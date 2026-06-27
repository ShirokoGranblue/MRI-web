import { useState } from 'react';
import { Search, X, AlertTriangle } from 'lucide-react';
import { statusLabel, statusTone } from '../lib/app-context.jsx';

export function StatusTag({ children, tone, status }) {
  let resolvedTone = tone;
  let label = children;
  if (status) {
    resolvedTone = statusTone[status] || tone || 'neutral';
    label = statusLabel[status] || status;
  }
  return <span className={`status-tag ${resolvedTone || 'neutral'}`}>{label}</span>;
}

export function Button({ children, icon: Icon, variant = 'primary', busy, ...props }) {
  return (
    <button className={`button ${variant}`} disabled={busy || props.disabled} {...props}>
      {Icon ? <Icon size={16} aria-hidden="true" /> : null}
      <span>{busy ? '处理中' : children}</span>
    </button>
  );
}

export function TextField({ label, name, value, onChange, type = 'text', placeholder }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input name={name} value={value} type={type} placeholder={placeholder} onChange={onChange} />
    </label>
  );
}

export function SelectField({ label, name, value, onChange, options, placeholder }) {
  const opts = Array.isArray(options) ? options : [];
  return (
    <label className="field">
      <span>{label}</span>
      <select name={name} value={value} onChange={onChange}>
        {placeholder ? <option value="">{placeholder}</option> : null}
        {opts.map((item) => {
          const v = typeof item === 'string' ? item : item.value;
          const l = typeof item === 'string' ? item : item.label;
          return (
            <option key={v} value={v}>
              {l}
            </option>
          );
        })}
      </select>
    </label>
  );
}

export function TextAreaField({ label, name, value, onChange, rows = 3 }) {
  return (
    <label className="field wide">
      <span>{label}</span>
      <textarea name={name} value={value} rows={rows} onChange={onChange} />
    </label>
  );
}

export function SectionHeader({ icon: Icon, title, actions }) {
  return (
    <div className="section-header">
      <div className="section-title">
        {Icon ? <Icon size={18} aria-hidden="true" /> : null}
        <h2>{title}</h2>
      </div>
      {actions ? <div className="section-actions">{actions}</div> : null}
    </div>
  );
}

export function Metric({ label, value, detail, tone }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
      <StatusTag tone={tone}>{detail}</StatusTag>
    </div>
  );
}

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle ? <p>{subtitle}</p> : null}
      </div>
      {actions ? <div className="section-actions">{actions}</div> : null}
    </div>
  );
}

export function DataTable({ columns, rows, emptyText = '暂无数据', rowKey }) {
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
              <tr key={rowKey ? rowKey(row) : row.id || `${columns[0].key}-${index}`}>
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

export function EmptyState({ text, icon: Icon }) {
  return (
    <div className="empty-state">
      {Icon ? <Icon size={28} aria-hidden="true" /> : null}
      <span>{text}</span>
    </div>
  );
}

export function SearchBar({ value, onChange, placeholder = '搜索', onSearch }) {
  return (
    <div className="search-bar">
      <Search size={16} aria-hidden="true" />
      <input
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter') onSearch?.(value);
        }}
      />
      {value ? (
        <button type="button" className="search-clear" onClick={() => onChange('')} aria-label="清除">
          <X size={14} />
        </button>
      ) : null}
    </div>
  );
}

export function ConfirmDialog({ open, title = '确认操作', message, confirmText = '确定', cancelText = '取消', onConfirm, onCancel, tone = 'danger' }) {
  if (!open) return null;
  return (
    <div className="dialog-overlay" role="dialog" aria-modal="true">
      <div className="dialog-card">
        <div className="dialog-head">
          <AlertTriangle size={20} aria-hidden="true" />
          <strong>{title}</strong>
        </div>
        <p className="dialog-message">{message}</p>
        <div className="dialog-actions">
          <Button variant="ghost" onClick={onCancel}>{cancelText}</Button>
          <Button variant={tone === 'danger' ? 'danger' : 'primary'} onClick={onConfirm}>{confirmText}</Button>
        </div>
      </div>
    </div>
  );
}

export function useConfirm() {
  const [state, setState] = useState({ open: false });
  const ask = (opts) => new Promise((resolve) => {
    setState({ open: true, ...opts, onConfirm: () => { setState({ open: false }); resolve(true); }, onCancel: () => { setState({ open: false }); resolve(false); } });
  });
  const dialog = <ConfirmDialog {...state} />;
  return { ask, dialog };
}
