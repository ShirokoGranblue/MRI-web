import assert from 'node:assert/strict';
import { beforeEach, test } from 'node:test';
import {
  api,
  apiRequest,
  clearSession,
  downloadAttachment,
  getStoredSession,
  getStoredUser,
  getToken,
  saveSession,
} from './api.js';

function installLocalStorage() {
  const values = new Map();
  globalThis.localStorage = {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: (key) => values.delete(key),
  };
}

beforeEach(() => {
  installLocalStorage();
  clearSession();
});

test('401 响应使用登录失效提示并清理本地会话', async () => {
  saveSession({ token: 'expired-token', username: 'admin' });
  globalThis.fetch = async () => new Response('', { status: 401 });

  await assert.rejects(
    apiRequest('/api/patients', { method: 'POST', body: '{}' }),
    (error) => {
      assert.equal(error.status, 401);
      assert.equal(error.message, '登录状态已失效，请重新登录');
      return true;
    },
  );

  assert.equal(getToken(), '');
  assert.equal(getStoredUser(), '');
});

test('没有业务提示的 HTTP 错误转换为用户可理解的中文', async () => {
  globalThis.fetch = async () => new Response('', { status: 503 });

  await assert.rejects(
    apiRequest('/api/patients'),
    (error) => {
      assert.equal(error.status, 503);
      assert.equal(error.message, '系统服务暂时不可用，请稍后重试');
      assert.doesNotMatch(error.message, /HTTP\s*\d+/i);
      return true;
    },
  );
});

test('后端返回的业务提示优先于通用状态提示', async () => {
  globalThis.fetch = async () => new Response(
    JSON.stringify({ success: false, message: '患者编号已存在' }),
    { status: 409, headers: { 'Content-Type': 'application/json' } },
  );

  await assert.rejects(
    apiRequest('/api/patients', { method: 'POST', body: '{}' }),
    (error) => {
      assert.equal(error.status, 409);
      assert.equal(error.message, '患者编号已存在');
      return true;
    },
  );
});

test('网络不可达时不暴露浏览器技术错误', async () => {
  globalThis.fetch = async () => {
    throw new TypeError('Failed to fetch');
  };

  await assert.rejects(
    apiRequest('/api/patients'),
    (error) => {
      assert.equal(error.status, 0);
      assert.equal(error.message, '无法连接服务器，请检查网络或稍后重试');
      return true;
    },
  );
});

test('会话保存显示名称和角色', () => {
  saveSession({
    token: 'patient-token',
    username: 'patient01',
    displayName: '张三',
    roles: ['PATIENT'],
  });

  assert.deepEqual(getStoredSession(), {
    token: 'patient-token',
    username: 'patient01',
    displayName: '张三',
    roles: ['PATIENT'],
  });
});

test('403 响应提示无权限但保留登录会话', async () => {
  saveSession({ token: 'patient-token', username: 'patient01', roles: ['PATIENT'] });
  globalThis.fetch = async () => new Response('', { status: 403 });

  await assert.rejects(apiRequest('/api/exams/1/start', { method: 'POST' }), (error) => {
    assert.equal(error.status, 403);
    assert.equal(error.message, '当前账号没有执行此操作的权限');
    return true;
  });

  assert.equal(getToken(), 'patient-token');
  assert.equal(getStoredUser(), 'patient01');
});

test('下载助手附带令牌并解析 UTF-8 文件名且延迟释放临时地址', async () => {
  saveSession({ token: 'doctor-token', username: 'admin' });
  const clicks = [];
  const removals = [];
  const revoked = [];
  const scheduled = [];
  const originalSetTimeout = globalThis.setTimeout;
  const anchor = {
    click: () => clicks.push(true),
    remove: () => removals.push(true),
    style: {},
  };
  globalThis.document = {
    createElement: () => anchor,
    body: { appendChild: () => {} },
  };
  globalThis.URL.createObjectURL = () => 'blob:mri-download';
  globalThis.URL.revokeObjectURL = (value) => revoked.push(value);
  globalThis.setTimeout = (callback, delay) => {
    scheduled.push({ callback, delay });
  };
  globalThis.fetch = async (path, options) => {
    assert.equal(path, '/api/images/studies/31/download?reason=%E4%BC%9A%E8%AF%8A&transport=browser');
    assert.equal(options.headers.Authorization, 'Bearer doctor-token');
    return new Response(new Blob(['zip-bytes']), {
      status: 200,
      headers: {
        'Content-Type': 'application/vnd.mri.study-archive',
      },
    });
  };

  try {
    const fileName = await api.downloadStudy(31, '会诊');

    assert.equal(fileName, 'Study-31-影像.zip');
    assert.equal(anchor.download, 'Study-31-影像.zip');
    assert.equal(anchor.href, 'blob:mri-download');
    assert.equal(clicks.length, 1);
    assert.deepEqual(removals, []);
    assert.deepEqual(revoked, []);
    assert.equal(scheduled.length, 1);
    assert.equal(scheduled[0].delay, 1000);

    scheduled[0].callback();
    assert.equal(removals.length, 1);
    assert.deepEqual(revoked, ['blob:mri-download']);
  } finally {
    globalThis.setTimeout = originalSetTimeout;
  }
});

test('下载 403 保留会话并优先显示后端业务提示', async () => {
  saveSession({ token: 'patient-token', username: 'patient01' });
  globalThis.fetch = async () => new Response(
    JSON.stringify({ message: '诊断报告发布后方可下载影像' }),
    { status: 403, headers: { 'Content-Type': 'application/json' } },
  );

  await assert.rejects(downloadAttachment('/api/images/mine/files/41/download'), (error) => {
    assert.equal(error.status, 403);
    assert.equal(error.message, '诊断报告发布后方可下载影像');
    return true;
  });
  assert.equal(getToken(), 'patient-token');
});
