import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { KeyRound, ShieldCheck } from 'lucide-react';
import { useApp } from '../lib/app-context.jsx';
import { Button, TextField } from '../components/ui.jsx';

export default function LoginPage() {
  const { loggedIn, login, busyKey } = useApp();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: 'admin', password: 'admin123' });

  if (loggedIn) {
    return <Navigate to="/" replace />;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const data = await login(form);
    if (data?.token) {
      navigate('/');
    }
  }

  function update(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  return (
    <div className="login-shell">
      <form className="login-card" onSubmit={handleSubmit}>
        <div className="login-brand">
          <div className="brand-mark">MRI</div>
          <div>
            <strong>医院核磁共振影像管理系统</strong>
            <span>请登录后使用</span>
          </div>
        </div>
        <TextField label="账号" name="username" value={form.username} onChange={update} />
        <TextField label="密码" name="password" type="password" value={form.password} onChange={update} />
        <Button icon={KeyRound} busy={busyKey === 'login'}>登录</Button>
        <p className="login-hint">
          <ShieldCheck size={14} aria-hidden="true" /> 演示账号 admin / admin123
        </p>
      </form>
    </div>
  );
}
