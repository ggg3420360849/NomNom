<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createOrder,
  fetchCategories,
  fetchMenuItems,
  fetchOrderPreview,
  fetchOrders,
  login,
  register,
} from './services/api'

const STORAGE_KEY = 'nomnom-auth-user'

const portalOptions = [
  { code: 'CUSTOMER', label: '顾客端', description: '点餐、下单、查看自己的订单' },
  { code: 'MERCHANT', label: '商家端', description: '管理菜品、查看全店订单与服务模块' },
]

const customerModules = [
  { title: '在线点餐', detail: '浏览 dishes、dishes_package 并直接生成订单。' },
  { title: '我的订单', detail: '只查看当前顾客自己的 orders / orders_item。' },
  { title: '购物体验', detail: '下单前先预览金额和备注信息。' },
]

const merchantModules = [
  { title: '菜品管理', detail: '基于 dishes、dishes_type、dishes_package 做菜品与套餐维护。' },
  { title: '订单看板', detail: '查看全店 orders 与 orders_item，跟踪出餐和支付状态。' },
  { title: '评价与消息', detail: '后续可继续接 dishes_evaluations、images、messages。' },
  { title: '会员与钱包', detail: '后续可继续接 user、wallet、wallet_info 的商家能力。' },
]

function readStoredUser() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

const currentUser = ref(readStoredUser())
const authMode = ref('login')
const authLoading = ref(false)
const authMessage = ref('')
const pageLoading = ref(false)
const portalMessage = ref('')
const submitMessage = ref('')

const categories = ref([])
const menuItems = ref([])
const orderQueue = ref([])
const activeCategory = ref('')
const keyword = ref('')
const cart = ref([])
const preview = ref({
  itemCount: 0,
  totalAmount: 0,
  suggestion: '请先选择菜品，然后再生成订单预览。',
})

const customerDraft = reactive({
  remark: '',
})

const loginForm = reactive({
  roleCode: 'CUSTOMER',
  account: '',
  password: '',
})

const registerForm = reactive({
  roleCode: 'CUSTOMER',
  account: '',
  username: '',
  password: '',
  email: '',
  phone: '',
})

const isCustomerPortal = computed(() => currentUser.value?.roleCode === 'CUSTOMER')
const isMerchantPortal = computed(() => currentUser.value?.roleCode === 'MERCHANT')
const displayName = computed(() => currentUser.value?.username || currentUser.value?.account || '未登录用户')

const filteredMenuItems = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return menuItems.value.filter((item) => {
    const matchCategory = !activeCategory.value || item.category === activeCategory.value
    const matchKeyword =
      !normalizedKeyword ||
      item.name.toLowerCase().includes(normalizedKeyword) ||
      (item.description || '').toLowerCase().includes(normalizedKeyword)
    return matchCategory && matchKeyword
  })
})

const cartCount = computed(() => cart.value.reduce((sum, item) => sum + item.quantity, 0))
const cartTotal = computed(() => cart.value.reduce((sum, item) => sum + item.quantity * item.price, 0))

const merchantStats = computed(() => [
  { label: '菜品分类', value: categories.value.length },
  { label: '套餐数量', value: menuItems.value.length },
  { label: '全店订单', value: orderQueue.value.length },
])

const customerStats = computed(() => [
  { label: '可点套餐', value: menuItems.value.length },
  { label: '购物车数量', value: cartCount.value },
  { label: '我的订单', value: orderQueue.value.length },
])

function persistUser(user) {
  currentUser.value = user
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
}

function resetCustomerDraft() {
  cart.value = []
  customerDraft.remark = ''
  submitMessage.value = ''
  preview.value = {
    itemCount: 0,
    totalAmount: 0,
    suggestion: '请先选择菜品，然后再生成订单预览。',
  }
}

function logout() {
  localStorage.removeItem(STORAGE_KEY)
  currentUser.value = null
  categories.value = []
  menuItems.value = []
  orderQueue.value = []
  activeCategory.value = ''
  keyword.value = ''
  authMode.value = 'login'
  authMessage.value = ''
  portalMessage.value = ''
  resetCustomerDraft()
}

function buildOrderPayload() {
  return {
    userId: currentUser.value?.id,
    customerName: displayName.value,
    remark: customerDraft.remark,
    items: cart.value.map((item) => ({
      itemId: item.id,
      itemName: item.name,
      quantity: item.quantity,
      unitPrice: item.price,
    })),
  }
}

function addToCart(item) {
  const existing = cart.value.find((cartItem) => cartItem.id === item.id)
  if (existing) {
    existing.quantity += 1
    return
  }

  cart.value.push({
    id: item.id,
    name: item.name,
    price: Number(item.price),
    quantity: 1,
  })
}

function updateQuantity(id, delta) {
  const target = cart.value.find((item) => item.id === id)
  if (!target) {
    return
  }

  target.quantity += delta
  if (target.quantity <= 0) {
    cart.value = cart.value.filter((item) => item.id !== id)
  }
}

function formatStatus(status) {
  const dictionary = {
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    PREPARING: '出餐中',
    COMPLETED: '已完成',
    UNKNOWN: '未知状态',
  }
  return dictionary[status] || status
}

async function loadPortalData() {
  if (!currentUser.value) {
    return
  }

  pageLoading.value = true
  portalMessage.value = ''
  try {
    const [categoryList, itemList, orderList] = await Promise.all([
      fetchCategories(),
      fetchMenuItems(),
      fetchOrders(isCustomerPortal.value ? currentUser.value.id : null),
    ])

    categories.value = categoryList
    menuItems.value = itemList
    orderQueue.value = orderList
  } catch (error) {
    categories.value = []
    menuItems.value = []
    orderQueue.value = []
    portalMessage.value = error.message || '门户数据加载失败。'
  } finally {
    pageLoading.value = false
  }
}

async function handleLogin() {
  authLoading.value = true
  authMessage.value = ''
  try {
    const user = await login(loginForm)
    persistUser(user)
    resetCustomerDraft()
    await loadPortalData()
  } catch (error) {
    authMessage.value = error.message || '登录失败。'
  } finally {
    authLoading.value = false
  }
}

async function handleRegister() {
  authLoading.value = true
  authMessage.value = ''
  try {
    const user = await register(registerForm)
    persistUser(user)
    resetCustomerDraft()
    await loadPortalData()
  } catch (error) {
    authMessage.value = error.message || '注册失败。'
  } finally {
    authLoading.value = false
  }
}

async function handlePreview() {
  if (cart.value.length === 0) {
    submitMessage.value = '请先把菜品加入购物车。'
    return
  }

  submitMessage.value = ''
  try {
    preview.value = await fetchOrderPreview(buildOrderPayload())
  } catch (error) {
    submitMessage.value = error.message || '订单预览失败。'
  }
}

async function handleSubmit() {
  if (cart.value.length === 0) {
    submitMessage.value = '购物车为空，暂时不能提交。'
    return
  }

  submitMessage.value = ''
  try {
    const result = await createOrder(buildOrderPayload())
    submitMessage.value = `下单成功，订单号：${result.orderNo}`
    resetCustomerDraft()
    await loadPortalData()
  } catch (error) {
    submitMessage.value = error.message || '订单提交失败。'
  }
}

onMounted(async () => {
  if (currentUser.value) {
    await loadPortalData()
  }
})
</script>

<template>
  <div v-if="!currentUser" class="auth-shell">
    <section class="auth-brand">
      <p class="eyebrow">NomNom Access</p>
      <h1>餐厅系统登录入口</h1>
      <p class="auth-copy">
        登录和注册都基于现有的 <code>user</code> 表。
        登录后会根据账号角色自动进入对应门户，商家端和顾客端完全分开，不混用功能入口。
      </p>
      <div class="auth-role-list">
        <article v-for="portal in portalOptions" :key="portal.code" class="auth-role-card">
          <h3>{{ portal.label }}</h3>
          <p>{{ portal.description }}</p>
        </article>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-tabs">
        <button :class="{ active: authMode === 'login' }" @click="authMode = 'login'">登录</button>
        <button :class="{ active: authMode === 'register' }" @click="authMode = 'register'">注册</button>
      </div>

      <div v-if="authMode === 'login'" class="auth-form">
        <h2>账号登录</h2>
        <div class="portal-switch">
          <button
            v-for="portal in portalOptions"
            :key="portal.code"
            :class="{ active: loginForm.roleCode === portal.code }"
            @click="loginForm.roleCode = portal.code"
          >
            {{ portal.label }}
          </button>
        </div>

        <label>
          <span>账号</span>
          <input v-model="loginForm.account" type="text" placeholder="请输入账号" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="loginForm.password" type="password" placeholder="请输入密码" />
        </label>

        <button class="primary-button wide-button" :disabled="authLoading" @click="handleLogin">
          {{ authLoading ? '登录中...' : '进入系统' }}
        </button>
      </div>

      <div v-else class="auth-form">
        <h2>创建账号</h2>
        <div class="portal-switch">
          <button
            v-for="portal in portalOptions"
            :key="portal.code"
            :class="{ active: registerForm.roleCode === portal.code }"
            @click="registerForm.roleCode = portal.code"
          >
            {{ portal.label }}
          </button>
        </div>

        <label>
          <span>账号</span>
          <input v-model="registerForm.account" type="text" placeholder="注册账号" />
        </label>
        <label>
          <span>昵称</span>
          <input v-model="registerForm.username" type="text" placeholder="显示名称，可选" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="registerForm.password" type="password" placeholder="至少 6 位" />
        </label>
        <label>
          <span>邮箱</span>
          <input v-model="registerForm.email" type="email" placeholder="邮箱，可选" />
        </label>
        <label>
          <span>手机号</span>
          <input v-model="registerForm.phone" type="text" placeholder="手机号，可选" />
        </label>

        <button class="primary-button wide-button" :disabled="authLoading" @click="handleRegister">
          {{ authLoading ? '注册中...' : '注册并进入系统' }}
        </button>
      </div>

      <p v-if="authMessage" class="form-message">{{ authMessage }}</p>
    </section>
  </div>

  <div v-else class="portal-shell">
    <header class="portal-header">
      <div>
        <p class="eyebrow">NomNom Portal</p>
        <h1>{{ isMerchantPortal ? '商家端工作台' : '顾客端点餐台' }}</h1>
        <p class="portal-copy">
          当前账号：<strong>{{ displayName }}</strong>
          <span class="role-chip">{{ currentUser.roleLabel }}</span>
        </p>
      </div>
      <div class="portal-actions">
        <span class="status-chip" :class="{ muted: pageLoading }">
          {{ pageLoading ? '同步中...' : '数据库已连接' }}
        </span>
        <button class="ghost-button" @click="logout">退出登录</button>
      </div>
    </header>

    <p v-if="portalMessage" class="form-message">{{ portalMessage }}</p>

    <section class="metric-strip">
      <article
        v-for="metric in isMerchantPortal ? merchantStats : customerStats"
        :key="metric.label"
        class="metric-card"
      >
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
      </article>
    </section>

    <main v-if="isCustomerPortal" class="portal-grid customer-grid">
      <section class="panel module-panel">
        <div class="panel-head">
          <div>
            <p class="section-tag">顾客模块</p>
            <h2>只保留顾客功能</h2>
          </div>
        </div>
        <div class="module-grid">
          <article v-for="module in customerModules" :key="module.title" class="module-card">
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
          <div class="category-list">
            <button class="ghost-button" :class="{ active: activeCategory === '' }" @click="activeCategory = ''">
              全部
            </button>
            <button
              v-for="category in categories"
              :key="category.code"
              class="ghost-button"
              :class="{ active: activeCategory === category.code }"
              @click="activeCategory = category.code"
            >
              {{ category.name }}
            </button>
          </div>
          <input v-model="keyword" class="search-input" type="text" placeholder="搜索菜品或套餐" />
        </div>

        <div class="menu-list">
          <article v-for="item in filteredMenuItems" :key="item.id" class="menu-card">
            <div class="menu-card__meta">
              <span class="menu-card__badge">{{ item.categoryLabel || item.category }}</span>
              <span class="menu-card__sales">{{ item.status === 'AVAILABLE' ? '可下单' : '已下架' }}</span>
            </div>
            <div>
              <h3>{{ item.name }}</h3>
              <p>{{ item.description || '暂无菜品描述。' }}</p>
            </div>
            <div class="menu-card__footer">
              <strong>¥ {{ Number(item.price).toFixed(2) }}</strong>
              <button class="primary-button" :disabled="item.status !== 'AVAILABLE'" @click="addToCart(item)">
                {{ item.status === 'AVAILABLE' ? '加入购物车' : '不可下单' }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <section class="panel order-panel">
        <div class="panel-head">
          <div>
            <p class="section-tag">顾客订单</p>
            <h2>购物车与下单</h2>
          </div>
          <span class="status-chip">共 {{ cartCount }} 份</span>
        </div>

        <div class="form-grid">
          <label>
            <span>顾客名称</span>
            <input :value="displayName" type="text" disabled />
          </label>
          <label class="full-width">
            <span>备注</span>
            <textarea v-model="customerDraft.remark" rows="3" placeholder="口味、忌口、加急说明"></textarea>
          </label>
        </div>

        <div class="cart-list">
          <article v-for="item in cart" :key="item.id" class="cart-item">
            <div>
              <h3>{{ item.name }}</h3>
              <p>单价 ¥ {{ item.price.toFixed(2) }}</p>
            </div>
            <div class="stepper">
              <button @click="updateQuantity(item.id, -1)">-</button>
              <span>{{ item.quantity }}</span>
              <button @click="updateQuantity(item.id, 1)">+</button>
            </div>
          </article>
          <p v-if="cart.length === 0" class="empty-hint">购物车还是空的，可以从左侧菜单直接加入套餐。</p>
        </div>

        <div class="preview-box">
          <div>
            <p class="section-tag">订单预览</p>
            <h3>合计 {{ preview.itemCount }} 份，¥ {{ Number(preview.totalAmount || 0).toFixed(2) }}</h3>
            <p>{{ preview.suggestion }}</p>
            <p class="preview-total">购物车当前金额：¥ {{ cartTotal.toFixed(2) }}</p>
          </div>
          <div class="action-row">
            <button class="ghost-cta" @click="handlePreview">生成预览</button>
            <button class="primary-button" @click="handleSubmit">提交订单</button>
          </div>
          <p v-if="submitMessage" class="submit-message">{{ submitMessage }}</p>
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
          <article v-for="order in orderQueue" :key="order.orderNo" class="queue-card">
            <div>
              <p class="queue-label">{{ order.orderNo }}</p>
              <h3>{{ order.customerName }}</h3>
              <p>{{ order.itemCount }} 份菜品</p>
            </div>
            <div class="queue-side">
              <span class="status-chip">{{ formatStatus(order.status) }}</span>
              <strong>¥ {{ Number(order.totalAmount).toFixed(2) }}</strong>
            </div>
          </article>
          <p v-if="orderQueue.length === 0" class="empty-hint">当前账号还没有订单记录。</p>
        </div>
      </section>
    </main>

    <main v-else class="portal-grid merchant-grid">
      <section class="panel module-panel">
        <div class="panel-head">
          <div>
            <p class="section-tag">商家模块</p>
            <h2>只保留商家能力</h2>
          </div>
        </div>
        <div class="module-grid">
          <article v-for="module in merchantModules" :key="module.title" class="module-card">
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
          <article v-for="item in menuItems" :key="item.id" class="inventory-card">
            <div>
              <p class="queue-label">{{ item.categoryLabel || item.category }}</p>
              <h3>{{ item.name }}</h3>
              <p>{{ item.description || '暂无描述。' }}</p>
            </div>
            <div class="inventory-side">
              <span class="status-chip">{{ item.status === 'AVAILABLE' ? '在售' : '下架' }}</span>
              <strong>¥ {{ Number(item.price).toFixed(2) }}</strong>
            </div>
          </article>
          <p v-if="menuItems.length === 0" class="empty-hint">当前没有读取到套餐数据，请检查 dishes_package 表。</p>
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
          <article v-for="order in orderQueue" :key="order.orderNo" class="queue-card">
            <div>
              <p class="queue-label">{{ order.orderNo }}</p>
              <h3>{{ order.customerName }}</h3>
              <p>{{ order.itemCount }} 份菜品</p>
            </div>
            <div class="queue-side">
              <span class="status-chip">{{ formatStatus(order.status) }}</span>
              <strong>¥ {{ Number(order.totalAmount).toFixed(2) }}</strong>
            </div>
          </article>
          <p v-if="orderQueue.length === 0" class="empty-hint">当前没有订单数据。</p>
        </div>
      </section>
    </main>
  </div>
</template>
