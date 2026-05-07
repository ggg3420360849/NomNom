<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  displayName: {
    type: String,
    required: true,
  },
  roleLabel: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['logout'])
</script>

<template>
  <header class="portal-header">
    <div>
      <p class="eyebrow">NomNom Portal</p>
      <h1>{{ title }}</h1>
      <p class="portal-copy">
        当前账号：<strong>{{ displayName }}</strong>
        <span class="role-chip">{{ roleLabel }}</span>
      </p>
    </div>
    <div class="portal-actions">
      <span class="status-chip" :class="{ muted: loading }">
        {{ loading ? '同步中...' : '数据库已连接' }}
      </span>
      <button class="ghost-button" @click="$emit('logout')">退出登录</button>
    </div>
  </header>
</template>
