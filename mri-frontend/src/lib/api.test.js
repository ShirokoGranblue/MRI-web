import assert from 'node:assert/strict';
import { beforeEach, test } from 'node:test';
import {
  apiRequest,
  clearSession,
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
