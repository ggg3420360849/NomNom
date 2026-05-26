<script setup>
import { useRouter } from 'vue-router'
import { resolvePortalPathForUser, useNomNomApp } from '../utils/useNomNomApp'

const router = useRouter()
const app = useNomNomApp()

const portalCopy = {
  CUSTOMER: {
    label: '顾客端',
    title: '点餐、支付、查看订单',
    badge: '点餐入口',
  },
  MERCHANT: {
    label: '商家端',
    title: '管理菜品、桌台与订单',
    badge: '工作台入口',
  },
}

const loginTitle = {
  CUSTOMER: '欢迎来 nomnom 用餐',
  MERCHANT: '欢迎回到 nomnom 工作台',
}

function portalMeta(code) {
  return portalCopy[code] || portalCopy.CUSTOMER
}

function switchMode(mode) {
  app.authMode.value = mode
  app.authMessage.value = ''
}

async function submitLogin() {
  const user = await app.handleLogin()
  if (user) {
    await router.push(resolvePortalPathForUser(user))
  }
}

async function submitRegister() {
  const user = await app.handleRegister()
  if (user) {
    await router.push(resolvePortalPathForUser(user))
  }
}
</script>

<template>
  <main class="nomnom-auth">
    <section class="brand-stage" aria-label="nomnom 餐厅入口">
      <div class="brand-copy">
        <p class="brand-kicker">NOMNOM RESTAURANT</p>
        <h1>nomnom</h1>
        <p class="brand-line">把点餐、支付、订单流转和餐厅运营放进一个清爽的入口。</p>
      </div>

      <div class="photo-strip">
        <img src="/restaurant.png" alt="nomnom 餐厅环境" />
        <div class="service-note">
          <span>今日服务</span>
          <strong>堂食点餐 · 商家管理 · 订单追踪</strong>
        </div>
      </div>
    </section>

    <section class="auth-panel" aria-label="登录注册表单">
      <div class="panel-headline">
        <span class="panel-mark">NN</span>
        <div>
          <p>nomnom 智慧餐厅</p>
          <h2>{{ app.authMode.value === 'login' ? loginTitle[app.loginForm.roleCode] : '创建 nomnom 账号' }}</h2>
        </div>
      </div>

      <div class="auth-tabs" role="tablist" aria-label="账号操作">
        <button
          type="button"
          :class="{ active: app.authMode.value === 'login' }"
          @click="switchMode('login')"
        >
          登录
        </button>
        <button
          type="button"
          :class="{ active: app.authMode.value === 'register' }"
          @click="switchMode('register')"
        >
          注册
        </button>
      </div>

      <form v-if="app.authMode.value === 'login'" class="auth-form" @submit.prevent="submitLogin">
        <div class="portal-switch">
          <button
            v-for="portal in app.portalOptions"
            :key="portal.code"
            type="button"
            :class="{ active: app.loginForm.roleCode === portal.code }"
            @click="app.loginForm.roleCode = portal.code"
          >
            <span>{{ portalMeta(portal.code).badge }}</span>
            <strong>{{ portalMeta(portal.code).label }}</strong>
          </button>
        </div>

        <label>
          <span>账号</span>
          <input v-model="app.loginForm.account" type="text" placeholder="请输入账号" autocomplete="username" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="app.loginForm.password" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </label>

        <button class="submit-button" type="submit" :disabled="app.authLoading.value">
          {{ app.authLoading.value ? '正在进入...' : '进入 nomnom' }}
        </button>
      </form>

      <form v-else class="auth-form" @submit.prevent="submitRegister">
        <div class="portal-switch">
          <button
            v-for="portal in app.portalOptions"
            :key="portal.code"
            type="button"
            :class="{ active: app.registerForm.roleCode === portal.code }"
            @click="app.registerForm.roleCode = portal.code"
          >
            <span>{{ portalMeta(portal.code).badge }}</span>
            <strong>{{ portalMeta(portal.code).label }}</strong>
          </button>
        </div>

        <label>
          <span>账号</span>
          <input v-model="app.registerForm.account" type="text" placeholder="设置登录账号" autocomplete="username" />
        </label>
        <label>
          <span>显示名称</span>
          <input v-model="app.registerForm.username" type="text" placeholder="例如：前台小林" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="app.registerForm.password" type="password" placeholder="至少 6 位" autocomplete="new-password" />
        </label>
        <div class="field-row">
          <label>
            <span>邮箱</span>
            <input v-model="app.registerForm.email" type="email" placeholder="可选" autocomplete="email" />
          </label>
          <label>
            <span>手机号</span>
            <input v-model="app.registerForm.phone" type="text" placeholder="可选" autocomplete="tel" />
          </label>
        </div>

        <button class="submit-button" type="submit" :disabled="app.authLoading.value">
          {{ app.authLoading.value ? '正在创建...' : '创建并进入' }}
        </button>
      </form>

      <p v-if="app.authMessage.value" class="form-message">{{ app.authMessage.value }}</p>
    </section>
  </main>
</template>

<style scoped>
.nomnom-auth {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(360px, 0.72fr);
  gap: 32px;
  align-items: stretch;
  padding: 40px;
  background:
    linear-gradient(135deg, rgba(20, 32, 45, 0.96), rgba(31, 53, 59, 0.9)),
    #14202d;
  color: #17212b;
}

.brand-stage {
  position: relative;
  min-height: 640px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 42px;
  border-radius: 8px;
  background: #0f1b25;
}

.brand-stage::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(15, 27, 37, 0.9), rgba(15, 27, 37, 0.35)),
    url('/restaurant.png') center / cover;
  opacity: 0.9;
}

.brand-stage::after {
  content: '';
  position: absolute;
  inset: auto 0 0;
  height: 46%;
  background: linear-gradient(0deg, rgba(15, 27, 37, 0.96), transparent);
}

.brand-copy,
.photo-strip {
  position: relative;
  z-index: 1;
}

.brand-kicker {
  margin: 0 0 14px;
  color: #f2b56b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.brand-copy h1 {
  margin: 0;
  color: #fffaf3;
  font-size: 84px;
  line-height: 0.95;
  font-weight: 800;
  letter-spacing: 0;
}

.brand-line {
  max-width: 560px;
  margin: 22px 0 0;
  color: rgba(255, 250, 243, 0.82);
  font-size: 20px;
  line-height: 1.7;
}

.photo-strip {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  gap: 18px;
  align-items: end;
}

.photo-strip img {
  width: 190px;
  height: 132px;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 8px;
}

.service-note {
  border-left: 3px solid #f2b56b;
  padding: 8px 0 8px 16px;
  color: #fffaf3;
}

.service-note span {
  display: block;
  color: rgba(255, 250, 243, 0.62);
  font-size: 13px;
}

.service-note strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
}

.auth-panel {
  align-self: center;
  width: 100%;
  max-width: 500px;
  justify-self: end;
  display: grid;
  gap: 22px;
  padding: 28px;
  border-radius: 8px;
  background: #fffaf3;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 26px 70px rgba(0, 0, 0, 0.28);
}

.panel-headline {
  display: flex;
  align-items: center;
  gap: 14px;
}

.panel-mark {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  flex: none;
  border-radius: 8px;
  background: #e65f2e;
  color: #fffaf3;
  font-weight: 800;
  letter-spacing: 0;
}

.panel-headline p {
  margin: 0 0 2px;
  color: #66717b;
  font-size: 13px;
}

.panel-headline h2 {
  margin: 0;
  color: #17212b;
  font-size: 24px;
  line-height: 1.25;
  letter-spacing: 0;
}

.auth-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 5px;
  border-radius: 8px;
  background: #ece4d9;
}

.auth-tabs button,
.portal-switch button {
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #5d6670;
  cursor: pointer;
}

.auth-tabs button {
  min-height: 42px;
  font-weight: 700;
}

.auth-tabs button.active {
  background: #17212b;
  color: #fffaf3;
}

.auth-form {
  display: grid;
  gap: 16px;
}

.portal-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.portal-switch button {
  min-height: 78px;
  padding: 12px;
  text-align: left;
  border: 1px solid #e3d8c9;
  background: #ffffff;
}

.portal-switch button span,
.portal-switch button strong {
  display: block;
}

.portal-switch button span {
  color: #7c8792;
  font-size: 12px;
}

.portal-switch button strong {
  margin-top: 6px;
  color: #17212b;
  font-size: 18px;
}

.portal-switch button.active {
  border-color: #e65f2e;
  background: #fff1e8;
  box-shadow: inset 0 0 0 1px #e65f2e;
}

.auth-form label {
  display: grid;
  gap: 8px;
  color: #44515c;
  font-size: 14px;
  font-weight: 700;
}

.auth-form input {
  width: 100%;
  height: 48px;
  border: 1px solid #ddd2c3;
  border-radius: 6px;
  padding: 0 14px;
  background: #ffffff;
  color: #17212b;
  outline: none;
}

.auth-form input:focus {
  border-color: #2c7a73;
  box-shadow: 0 0 0 3px rgba(44, 122, 115, 0.14);
}

.field-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.submit-button {
  min-height: 50px;
  border: 0;
  border-radius: 6px;
  background: #e65f2e;
  color: #fffaf3;
  cursor: pointer;
  font-weight: 800;
  box-shadow: 0 14px 30px rgba(230, 95, 46, 0.28);
}

.submit-button:hover {
  background: #d94f1f;
}

.submit-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
}

.form-message {
  margin: 0;
  padding: 12px 14px;
  border-radius: 6px;
  background: #eaf4f2;
  color: #245b55;
  font-size: 14px;
}

@media (max-width: 980px) {
  .nomnom-auth {
    grid-template-columns: 1fr;
    padding: 24px;
  }

  .brand-stage {
    min-height: 420px;
  }

  .auth-panel {
    max-width: none;
    justify-self: stretch;
  }
}

@media (max-width: 640px) {
  .nomnom-auth {
    padding: 14px;
    gap: 14px;
  }

  .brand-stage,
  .auth-panel {
    padding: 20px;
  }

  .brand-stage {
    min-height: 340px;
  }

  .brand-copy h1 {
    font-size: 52px;
  }

  .brand-line {
    font-size: 16px;
  }

  .photo-strip,
  .field-row,
  .portal-switch {
    grid-template-columns: 1fr;
  }

  .photo-strip img {
    display: none;
  }
}
</style>
