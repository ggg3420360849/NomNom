<script setup>
import { useRouter } from 'vue-router'
import { resolvePortalPathForUser, useNomNomApp } from '../utils/useNomNomApp'

// The auth page owns form interaction, then hands API work to the shared app store.
const router = useRouter()
const app = useNomNomApp()

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
  <div class="auth-shell">
    <section class="auth-brand">
      <h1>餐厅系统登录入口</h1>
    </section>

    <section class="auth-panel">
      <div class="auth-tabs">
        <button :class="{ active: app.authMode.value === 'login' }" @click="app.authMode.value = 'login'">登录</button>
        <button :class="{ active: app.authMode.value === 'register' }" @click="app.authMode.value = 'register'">注册</button>
      </div>

      <div v-if="app.authMode.value === 'login'" class="auth-form">
        <h2>账号登录</h2>
        <div class="portal-switch">
          <button
            v-for="portal in app.portalOptions"
            :key="portal.code"
            :class="{ active: app.loginForm.roleCode === portal.code }"
            @click="app.loginForm.roleCode = portal.code"
          >
            {{ portal.label }}
          </button>
        </div>

        <label>
          <span>账号</span>
          <input v-model="app.loginForm.account" type="text" placeholder="账号" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="app.loginForm.password" type="password" placeholder="密码" />
        </label>

        <button class="primary-button wide-button" :disabled="app.authLoading.value" @click="submitLogin">
          {{ app.authLoading.value ? '登录中...' : '进入系统' }}
        </button>
      </div>

      <div v-else class="auth-form">
        <h2>创建账号</h2>
        <div class="portal-switch">
          <button
            v-for="portal in app.portalOptions"
            :key="portal.code"
            :class="{ active: app.registerForm.roleCode === portal.code }"
            @click="app.registerForm.roleCode = portal.code"
          >
            {{ portal.label }}
          </button>
        </div>

        <label>
          <span>账号</span>
          <input v-model="app.registerForm.account" type="text" placeholder="注册账号" />
        </label>
        <label>
          <span>显示名称</span>
          <input v-model="app.registerForm.username" type="text" placeholder="显示名称，可选" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="app.registerForm.password" type="password" placeholder="至少 6 位" />
        </label>
        <label>
          <span>邮箱</span>
          <input v-model="app.registerForm.email" type="email" placeholder="邮箱，可选" />
        </label>
        <label>
          <span>手机号</span>
          <input v-model="app.registerForm.phone" type="text" placeholder="手机号，可选" />
        </label>

        <button class="primary-button wide-button" :disabled="app.authLoading.value" @click="submitRegister">
          {{ app.authLoading.value ? '注册中...' : '注册并进入系统' }}
        </button>
      </div>

      <p v-if="app.authMessage.value" class="form-message">{{ app.authMessage.value }}</p>
    </section>
  </div>
</template>
