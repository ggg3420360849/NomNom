<script setup>
import { useRouter } from 'vue-router'
import MetricStrip from '../components/MetricStrip.vue'
import PortalHeader from '../components/PortalHeader.vue'
import { useNomNomApp } from '../utils/useNomNomApp'

// The merchant page is route-driven now, but still shares one app store with the rest of the UI.
const router = useRouter()
const app = useNomNomApp()

async function exitPortal() {
  app.logout()
  await router.push('/login')
}

await app.loadPortalData()
</script>

<template>
  <div class="portal-shell">
    <PortalHeader
      title="商家端工作台"
      :display-name="app.displayName.value"
      :role-label="app.currentUser.value.roleLabel"
      :loading="app.pageLoading.value"
      @logout="exitPortal"
    />

    <p v-if="app.portalMessage.value" class="form-message">{{ app.portalMessage.value }}</p>

    <MetricStrip :metrics="app.merchantStats.value" />

    <main class="portal-grid merchant-grid">
      <section class="panel module-panel">
        <div class="panel-head">
          <div>
            <p class="section-tag">商家模块</p>
            <h2>只保留商家能力</h2>
          </div>
        </div>
        <div class="module-grid">
          <article v-for="module in app.merchantModules" :key="module.title" class="module-card">
            <h3>{{ module.title }}</h3>
            <p>{{ module.detail }}</p>
          </article>
        </div>
      </section>

      <section class="panel inventory-panel">
        <div class="panel-head">
          <div>
            <p class="section-tag">商家菜品</p>
            <h2>菜品与套餐列表</h2>
          </div>
        </div>
        <div class="inventory-list">
          <article v-for="item in app.menuItems.value" :key="item.id" class="inventory-card">
            <div>
              <p class="queue-label">{{ item.categoryLabel || item.category }}</p>
              <h3>{{ item.name }}</h3>
              <p>{{ item.description || '暂无描述。' }}</p>
            </div>
            <div class="inventory-side">
              <span class="status-chip">{{ item.status === 'AVAILABLE' ? '在售' : '下架' }}</span>
              <strong>￥ {{ Number(item.price).toFixed(2) }}</strong>
            </div>
          </article>
          <p v-if="app.categories.value.length > 0 && app.menuItems.value.length === 0" class="empty-hint">
            当前没有读取到套餐数据，请检查菜品相关表。
          </p>
        </div>
      </section>

      <section class="panel queue-panel">
        <div class="panel-head">
          <div>
            <p class="section-tag">商家订单</p>
            <h2>全店订单看板</h2>
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
          <p v-if="app.orderQueue.value.length === 0" class="empty-hint">当前没有订单数据。</p>
        </div>
      </section>
    </main>
  </div>
</template>
