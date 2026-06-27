import { NavLink, Navigate, Outlet } from 'react-router-dom';
import { ClipboardList, FileText, History, Image, LayoutDashboard, LogOut, Settings2, ShieldCheck, Users } from 'lucide-react';
import { useApp } from '../lib/app-context.jsx';
import { Button, StatusTag } from './ui.jsx';

const navItems = [
  { to: '/', label: '工作台', icon: LayoutDashboard, end: true },
  { to: '/patients', label: '患者档案', icon: Users },
  { to: '/exams', label: '检查申请', icon: ClipboardList },
  { to: '/images', label: '影像归档', icon: Image },
  { to: '/reports', label: '诊断报告', icon: FileText },
  { to: '/settings', label: '系统设置', icon: Settings2 },
  { to: '/activity', label: '操作记录', icon: History },
];

export function RequireAuth({ children }) {
  const { loggedIn } = useApp();
  if (!loggedIn) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function AppLayout() {
  const { username, loggedIn, logout, busyKey } = useApp();
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">MRI</div>
          <div>
            <strong>MRI影像工作台</strong>
            <span>医院核磁共振图像信息管理</span>
          </div>
        </div>
        <nav className="nav-list" aria-label="主导航">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => (isActive ? 'active' : '')}>
              <item.icon size={17} aria-hidden="true" />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <StatusTag tone={loggedIn ? 'success' : 'warning'}>{loggedIn ? '已登录' : '未登录'}</StatusTag>
          <span>{username || '未登录'}</span>
        </div>
      </aside>
      <main className="workspace">
        <header className="topbar">
          <div>
            <h1>医院核磁共振影像管理系统</h1>
            <p className="topbar-sub">患者登记 · 检查申请 · 影像管理 · 诊断报告</p>
          </div>
          <div className="user-box">
            <ShieldCheck size={18} aria-hidden="true" />
            <span>{username || '未登录'}</span>
            {loggedIn ? (
              <Button icon={LogOut} variant="ghost" onClick={logout} busy={busyKey === 'logout'}>
                退出登录
              </Button>
            ) : null}
          </div>
        </header>
        <div className="page-content">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
