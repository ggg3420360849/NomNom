<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import MetricStrip from '../components/MetricStrip.vue'
import PortalHeader from '../components/PortalHeader.vue'
import { useNomNomApp } from '../utils/useNomNomApp'

// This page focuses on rendering and user interaction. Shared state lives in useNomNomApp.
const router = useRouter()
const app = useNomNomApp()

async function exitPortal() {
  app.logout()
  await router.push('/login')
}

onMounted(() => {
  app.loadPortalData()
})
</script>

<template>
  <div class="portal-shell">
    <PortalHeader
      title="顾客端点餐台"
      :display-name="app.displayName.value"
      :role-label="app.currentUser.value.roleLabel"
      :loading="app.pageLoading.value"
      @logout="exitPortal"
    />

    <p v-if="app.portalMessage.value" class="form-message">{{ app.portalMessage.value }}</p>

    <MetricStrip :metrics="app.customerStats.value" />

    <main class="portal-grid customer-grid">
      <section class="panel module-panel">
        <div class="panel-head">
          <div>
            <p class="section-tag">顾客模块</p>
            <h2>只保留顾客功能</h2>
          </div>
        </div>
        <div class="module-grid">
          <article v-for="module in app.customerModules" :key="module.title" class="module-card">
            <h3>{{ module.title }}</h3>
            <p>{{ module.detail }}</p>
          </article>
        </div>
      </section>

      <section class="panel menu-panel">
        <div class="panel-head">
          <div>
            <p class="section-tag">顾客点餐</p>
            <h2>菜单与套餐</h2>
          </div>
        </div>

        <div class="toolbar">
          <!-- Category and keyword changes are written back to the shared store. -->
          <div class="category-list">
            <button class="ghost-button" :class="{ active: app.activeCategory.value === '' }" @click="app.activeCategory.value = ''">
              全部
            </button>
            <button
              v-for="category in app.categories.value"
              :key="category.code"
              class="ghost-button"
              :class="{ active: app.activeCategory.value === category.code }"
              @click="app.activeCategory.value = category.code"
            >
              {{ category.name }}
            </button>
          </div>
          <input
            v-model.lazy="app.keyword.value"
            class="search-input"
            type="text"
            placeholder="搜索菜品或套餐"
          />
        </div>

        <div class="menu-list">
          <article v-for="item in app.filteredMenuItems.value" :key="item.id" class="menu-card">
            <div class="menu-card__meta">
              <span class="menu-card__badge">{{ item.categoryLabel || item.category }}</span>
              <span class="menu-card__sales">{{ item.status === 'AVAILABLE' ? '可下单' : '已下架' }}</span>
            </div>
            <div>
              <h3>{{ item.name }}</h3>
              <p>{{ item.description || '暂无菜品描述。' }}</p>
            </div>
            <div class="menu-card__footer">
              <strong>￥ {{ Number(item.price).toFixed(2) }}</strong>
              <button class="primary-button" :disabled="item.status !== 'AVAILABLE'" @click="app.addToCart(item)">
                {{ item.status === 'AVAILABLE' ? '加入购物车' : '不可下单' }}
              </button>
            </div>
          </article>
          <p v-if="app.menuItems.value.length > 0 && app.filteredMenuItems.value.length === 0" class="empty-hint">
            当前筛选条件下没有可显示的菜品。
          </p>
        </div>
      </section>

      <section class="panel order-panel">
        <div class="panel-head">
          <div>
            <p class="section-tag">顾客订单</p>
            <h2>购物车与下单</h2>
          </div>
          <span class="status-chip">共 {{ app.cartCount.value }} 件</span>
        </div>

        <div class="form-grid">
          <label>
            <span>顾客名称</span>
            <input :value="app.displayName.value" type="text" disabled />
          </label>
          <label class="full-width">
            <span>备注</span>
            <textarea v-model="app.customerDraft.remark" rows="3" placeholder="口味、忌口、加急说明"></textarea>
          </label>
        </div>

        <div class="cart-list">
          <article v-for="item in app.cart.value" :key="item.id" class="cart-item">
            <div>
              <h3>{{ item.name }}</h3>
              <p>单价 ￥ {{ item.price.toFixed(2) }}</p>
            </div>
            <div class="stepper">
              <button @click="app.updateQuantity(item.id, -1)">-</button>
              <span>{{ item.quantity }}</span>
              <button @click="app.updateQuantity(item.id, 1)">+</button>
            </div>
          </article>
          <p v-if="app.cart.value.length === 0" class="empty-hint">购物车还是空的，可以先从左侧菜单中选择菜品。</p>
        </div>

        <div class="preview-box">
          <!-- Preview and submit are separate so teammates can extend them independently. -->
          <div>
            <p class="section-tag">订单预览</p>
            <p v-if="app.preview.value.remark">备注：{{ app.preview.value.remark }}</p>
            <h3>合计 {{ app.preview.value.itemCount }} 件，￥ {{ Number(app.preview.value.totalAmount || 0).toFixed(2) }}</h3>
            <p>{{ app.preview.value.suggestion }}</p>
            <p class="preview-total">购物车当前金额：￥ {{ app.cartTotal.value.toFixed(2) }}</p>
          </div>
          <div class="action-row">
            <button class="ghost-cta" @click="app.handlePreview">生成预览</button>
            <button class="primary-button" @click="app.handleSubmit">提交订单</button>
          </div>
          <p v-if="app.submitMessage.value" class="submit-message">{{ app.submitMessage.value }}</p>
        </div>
      </section>

      <section class="panel queue-panel">
        <div class="panel-head">
          <div>
            <p class="section-tag">顾客订单中心</p>
            <h2>我的订单</h2>
          </div>
        </div>

        <div class="queue-list">
          <article v-for="order in app.orderQueue.value" :key="order.orderNo" class="queue-card">
            <div>
              <p class="queue-label">{{ order.orderNo }}</p>
              <h3>{{ order.customerName }}</h3>
              <p>{{ order.itemCount }} 份菜品</p>
            </div>
            <div class="queue-side">
              <span class="status-chip">{{ app.formatStatus(order.status) }}</span>
              <strong>￥ {{ Number(order.totalAmount).toFixed(2) }}</strong>
            </div>
          </article>
          <p v-if="app.orderQueue.value.length === 0" class="empty-hint">当前账号还没有订单记录。</p>
        </div>
      </section>
    </main>
  </div>
</template>
