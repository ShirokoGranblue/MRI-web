import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { KeyRound, ShieldCheck, UserPlus } from 'lucide-react';
import { useApp } from '../lib/app-context.jsx';
import { nextRegistrationUsername } from '../lib/role-utils.js';
import { Button, TextField } from '../components/ui.jsx';

export default function LoginPage() {
  const { loggedIn, login, register, busyKey } = useApp();
  const navigate = useNavigate();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ username: 'admin', password: 'admin123' });
  const [registration, setRegistration] = useState({
    displayName: '',
    username: '',
    password: '',
    confirmPassword: '',
  });
  const [usernameEdited, setUsernameEdited] = useState(false);

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

  async function handleRegister(event) {
    event.preventDefault();
    if (registration.password !== registration.confirmPassword) {
      return;
    }
    const data = await register({
      displayName: registration.displayName,
      username: registration.username,
      password: registration.password,
    });
    if (data?.username) {
      setForm({ username: data.username, password: '' });
      setMode('login');
    }
  }

  function update(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function updateRegistration(event) {
    const { name, value } = event.target;
    if (name === 'username') {
      setUsernameEdited(true);
    }
    setRegistration((current) => {
      const next = { ...current, [name]: value };
      if (name === 'displayName') {
        next.username = nextRegistrationUsername(value, usernameEdited, current.username);
      }
      return next;
    });
  }

  return (
    <div className="login-shell">
      <form className="login-card" onSubmit={mode === 'login' ? handleSubmit : handleRegister}>
        <div className="login-brand">
          <div className="brand-mark">MRI</div>
          <div>
            <strong>医院核磁共振影像管理系统</strong>
            <span>请登录后使用</span>
          </div>
        </div>
        <div className="login-tabs">
          <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>登录</button>
          <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>患者注册</button>
        </div>
        {mode === 'login' ? (
          <>
            <TextField label="账号" name="username" value={form.username} onChange={update} />
            <TextField label="密码" name="password" type="password" value={form.password} onChange={update} />
            <Button icon={KeyRound} busy={busyKey === 'login'}>登录</Button>
            <p className="login-hint">
              <ShieldCheck size={14} aria-hidden="true" /> 医生账号 admin / admin123
            </p>
          </>
        ) : (
          <>
            <TextField label="患者姓名" name="displayName" value={registration.displayName} onChange={updateRegistration} />
            <TextField label="登录用户名" name="username" value={registration.username} onChange={updateRegistration} placeholder="默认使用姓名，也可改为 patient01" />
            <TextField label="密码" name="password" type="password" value={registration.password} onChange={updateRegistration} />
            <TextField label="确认密码" name="confirmPassword" type="password" value={registration.confirmPassword} onChange={updateRegistration} />
            {registration.confirmPassword && registration.password !== registration.confirmPassword ? (
              <p className="field-error">两次输入的密码不一致</p>
            ) : null}
            <Button icon={UserPlus} busy={busyKey === 'register'} disabled={registration.password !== registration.confirmPassword}>注册患者账号</Button>
          </>
        )}
      </form>
    </div>
  );
}
