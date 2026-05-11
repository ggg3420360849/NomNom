<script setup>
// 定义接收父组件传来的参数
defineProps({
  title: { type: String, default: '工作台' },
  displayName: { type: String, default: '商家' },
  roleLabel: { type: String, default: '管理员' }
})

// 定义向父组件发送的事件
defineEmits(['logout'])
</script>

<template>
  <header class="portal-header">
    <div class="header-left">
      <h2 class="page-title">{{ title }}</h2>
    </div>
    <div class="header-right">
      <div class="user-info">
        <div class="avatar">👨‍🍳</div>
        <div class="user-details">
          <span class="name">{{ displayName }}</span>
          <span class="role">{{ roleLabel }}</span>
        </div>
      </div>
      <button class="logout-btn" @click="$emit('logout')">退出登录</button>
    </div>
  </header>
</template>

<style scoped>
/* 暖色调头部样式 */
.portal-header {
  background-color: #fffcf8; /* 暖白色背景 */
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(121, 85, 72, 0.05);
  border-bottom: 1px solid #f0e6d2; /* 奶油色边框 */
  margin-bottom: 24px;
  border-radius: 0 0 12px 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  color: #4e342e; /* 深咖色文字 */
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  font-size: 24px;
  background: #fff8eb;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 1px solid #f0e6d2;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.name {
  font-size: 14px;
  font-weight: 600;
  color: #5d4037;
}

.role {
  font-size: 12px;
  color: #8d6e63;
}

/* 退出登录按钮 */
.logout-btn {
  background: transparent;
  border: 1px solid #e0d4c3;
  color: #d32f2f;
  padding: 6px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: #fff5f5;
  border-color: #ef5350;
  color: #c62828;
}
</style>