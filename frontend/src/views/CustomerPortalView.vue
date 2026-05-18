<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import PortalHeader from '../components/PortalHeader.vue'
import { useNomNomApp } from '../utils/useNomNomApp'
import axios from 'axios'

const router = useRouter()
const app = useNomNomApp()

// axios 配置
const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 充值功能
const wallet = ref(0)
const rechargeVisible = ref(false)
const rechargeAmount = ref('')

function openRechargeDialog() {
  rechargeAmount.value = ''
  rechargeVisible.value = true
}
function doRecharge() {
  const num = Number(rechargeAmount.value)
  if (!num || num <= 0) return alert('请输入正确金额')
  wallet.value = Number((wallet.value + num).toFixed(2))
  alert('充值成功！余额：¥' + wallet.value.toFixed(2))
  rechargeVisible.value = false
}

// 菜品详情弹窗 + 推荐搭配
const detailVisible = ref(false)
const currentDish = ref({})
const recommendList = ref([])

async function openDetail(item) {
  currentDish.value = item
  detailVisible.value = true
  try {
    const res = await http.get(`/menu/recommend/${item.id}`)
    recommendList.value = res.data.data || []
  } catch (err) {
    recommendList.value = []
  }
}

function closeDetailModal() {
  detailVisible.value = false
  recommendList.value = []
}

// 订单小票弹窗
const orderReceiptVisible = ref(false)
const orderInfo = ref({})

// 分类（用 ID）
const categoryList = ref([])
const activeCategory = ref(null) // 这里存分类 ID

// 获取分类
async function loadCategory() {
  try {
    const res = await http.get('/menu/categories')
    categoryList.value = res.data.data || []
  } catch (err) {
    console.error('加载分类失败', err)
  }
}

// 加载菜品（按 typeId 查询）
const localMenuItems = ref([])

async function loadMenu() {
  try {
    const params = {}
    if (activeCategory.value != null) {
      params.category = activeCategory.value // 传分类 ID
    }
    const res = await http.get('/menu/items', { params })
    localMenuItems.value = res.data.data || []
  } catch (err) {
    console.error('加载菜单失败', err)
    alert('菜单加载失败')
  }
}

// 搜索筛选
const keyword = ref('')
const filteredMenuItems = computed(() => {
  return localMenuItems.value.filter(item => {
    if (item.status !== 'AVAILABLE' && item.status !== 1) return false
    const keyMatch = keyword.value
        ? item.name.includes(keyword.value) || (item.detail || '').includes(keyword.value)
        : true
    return keyMatch
  })
})

// 切换分类（传 ID）
function changeCategory(catId) {
  activeCategory.value = catId
  loadMenu()
}

// 购物车
const cart = ref([])
const cartCount = computed(() => cart.value.reduce((s, i) => s + i.quantity, 0))
const cartTotal = computed(() => {
  const total = cart.value.reduce((s, i) => s + i.price * i.quantity, 0)
  return Number(total.toFixed(2))
})

function addToCart(item) {
  const found = cart.value.find(i => i.id === item.id)
  if (found) found.quantity++
  else cart.value.push({ ...item, quantity: 1 })
}
function updateQuantity(id, num) {
  const it = cart.value.find(i => i.id === id)
  if (!it) return
  it.quantity += num
  if (it.quantity <= 0) cart.value = cart.value.filter(i => i.id !== id)
}

// 提交订单
function handleSubmit() {
  if (cartCount.value === 0) return alert('购物车是空的！')
  if (wallet.value < cartTotal.value) return alert('余额不足，请先充值')

  orderInfo.value = {
    time: new Date().toLocaleString(),
    items: [...cart.value],
    total: cartTotal.value,
    balance: Number((wallet.value - cartTotal.value).toFixed(2))
  }

  wallet.value = orderInfo.balance
  orderReceiptVisible.value = true
  cart.value = []
}

async function exitPortal() {
  app.logout()
  await router.push('/login')
}

// 初始化
onMounted(() => {
  app.loadPortalData()
  loadCategory()
  loadMenu()
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

    <div class="wallet-bar">
      <span>余额：¥{{ wallet.toFixed(2) }}</span>
      <button class="recharge-btn" @click="openRechargeDialog">充值</button>
    </div>

    <div class="main-layout">
      <!-- 左侧分类栏 -->
      <aside class="category-sidebar">
        <button
            class="category-btn"
            :class="{ active: activeCategory === null }"
            @click="changeCategory(null)"
        >
          全部
        </button>
        <button
            v-for="cat in categoryList"
            :key="cat.code"
            class="category-btn"
            :class="{ active: activeCategory === cat.code }"
            @click="changeCategory(cat.code)"
        >
          {{ cat.name }}
        </button>
      </aside>

      <!-- 菜单 -->
      <section class="menu-area">
        <div class="search-box">
          <input
              v-model.lazy="keyword"
              class="search-input"
              type="text"
              placeholder="搜索菜品或套餐"
          />
        </div>

        <div class="menu-list">
          <article
              v-for="item in filteredMenuItems"
              :key="item.id"
              class="menu-card"
              @click="openDetail(item)"
          >
            <img :src="item.coverUrl" class="menu-image" />
            <h3 class="menu-title">{{ item.name }}</h3>
            <p class="menu-price">¥{{ item.price.toFixed(2) }}</p>
            <button class="add-btn" @click.stop="addToCart(item)">+</button>
          </article>
        </div>
      </section>
    </div>

    <!-- 底部购物车 -->
    <footer class="cart-footer">
      <div class="cart-summary">
        <div class="cart-info">
          <span class="cart-count">共 {{ cartCount }} 件</span>
          <span class="cart-total">合计：¥{{ cartTotal.toFixed(2) }}</span>
        </div>
        <button
            class="submit-btn"
            :disabled="cartCount === 0 || wallet < cartTotal"
            @click="handleSubmit"
        >
          提交订单
        </button>
      </div>

      <div class="cart-items">
        <article v-for="item in cart" :key="item.id" class="cart-item">
          <div class="item-info">
            <h3>{{ item.name }}</h3>
            <p>单价 ￥{{ item.price.toFixed(2) }}</p>
          </div>
          <div class="quantity-control">
            <button class="qty-btn" @click="updateQuantity(item.id, -1)">-</button>
            <span class="qty-text">{{ item.quantity }}</span>
            <button class="qty-btn" @click="updateQuantity(item.id, 1)">+</button>
          </div>
        </article>
        <p v-if="cart.length === 0" class="empty-hint">购物车还是空的~</p>
      </div>
    </footer>

    <!-- 充值弹窗 -->
    <div v-if="rechargeVisible" class="modal-overlay" @click.self="rechargeVisible = false">
      <div class="modal">
        <h3>模拟充值</h3>
        <input v-model.number="rechargeAmount" type="number" placeholder="输入金额" />
        <div class="modal-buttons">
          <button @click="rechargeVisible = false">取消</button>
          <button class="primary-button" @click="doRecharge">确认</button>
        </div>
      </div>
    </div>

    <!-- 菜品详情 -->
    <div v-if="detailVisible" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal modal-lg">
        <img :src="currentDish.coverUrl" class="detail-img" />
        <h3>{{ currentDish.name }}</h3>
        <p>{{ currentDish.detail }}</p>
        <p class="detail-price">¥{{ currentDish.price.toFixed(2) }}</p>
        <button class="primary-button" @click="addToCart(currentDish)">加入购物车</button>

        <div class="recommend-wrap">
          <h4 class="recommend-title">推荐搭配</h4>
          <div class="recommend-list">
            <div v-if="recommendList.length === 0" class="no-recommend">暂无</div>
            <div v-for="rec in recommendList" :key="rec.id" class="recommend-item">
              <img :src="rec.coverUrl" class="rec-img" />
              <div class="rec-info">
                <div class="rec-name">{{ rec.name }}</div>
                <div class="rec-price">¥{{ rec.price.toFixed(2) }}</div>
              </div>
              <button class="rec-add-btn" @click="addToCart(rec)">+</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 订单小票 -->
    <div v-if="orderReceiptVisible" class="modal-overlay" @click.self="orderReceiptVisible = false">
      <div class="modal receipt-modal">
        <div class="receipt-header">
          <h3>订单支付成功</h3>
          <p>{{ orderInfo.time }}</p>
        </div>
        <div class="receipt-body">
          <div class="receipt-item" v-for="(item, idx) in orderInfo.items" :key="idx">
            <div class="name">{{ item.name }}</div>
            <div class="count">x{{ item.quantity }}</div>
            <div class="sub">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
          </div>
        </div>
        <div class="line"></div>
        <div class="total-row">
          <span>实付金额</span>
          <span>¥{{ orderInfo.total }}</span>
        </div>
        <div class="balance-row">
          <span>剩余余额</span>
          <span>¥{{ orderInfo.balance }}</span>
        </div>
        <button class="close-btn" @click="orderReceiptVisible = false">完成</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 样式完全不变，保持你原来的样式 */
.portal-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}
.wallet-bar {
  display: flex;
  justify-content: space-between;
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid #eee;
  font-weight: bold;
}
.recharge-btn {
  background: #4CAF50;
  color: white;
  border: none;
  padding: 6px 14px;
  border-radius: 4px;
  cursor: pointer;
}
.main-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}
.category-sidebar {
  width: 80px;
  background: #fafafa;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  padding: 10px 0;
}
.category-btn {
  padding: 12px 8px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}
.category-btn.active {
  background: #fff;
  color: #1890ff;
  border-left: 3px solid #1890ff;
}
.menu-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 10px;
}
.search-box {
  margin-bottom: 10px;
}
.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-sizing: border-box;
}
.menu-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}
.menu-card {
  aspect-ratio: 1 / 1;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
}
.menu-image {
  width: 100%;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  margin-bottom: 6px;
}
.menu-title {
  font-size: 12px;
  margin: 0 0 4px;
  text-align: center;
  line-height: 1.2;
}
.menu-price {
  font-size: 11px;
  color: #ff4d4f;
  margin: 0;
}
.add-btn {
  position: absolute;
  bottom: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.cart-footer {
  background: #fff;
  border-top: 1px solid #eee;
  padding: 10px 20px;
}
.cart-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.cart-info {
  display: flex;
  gap: 20px;
}
.cart-count {
  font-size: 14px;
}
.cart-total {
  font-size: 16px;
  font-weight: bold;
  color: #ff4d4f;
}
.submit-btn {
  background: #1890ff;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 6px;
  cursor: pointer;
}
.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.cart-items {
  max-height: 150px;
  overflow-y: auto;
}
.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}
.item-info h3 {
  margin: 0 0 4px;
  font-size: 13px;
}
.item-info p {
  margin: 0;
  font-size: 11px;
  color: #666;
}
.quantity-control {
  display: flex;
  align-items: center;
  gap: 8px;
}
.qty-btn {
  width: 24px;
  height: 24px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}
.qty-text {
  width: 20px;
  text-align: center;
  font-size: 13px;
}
.empty-hint {
  text-align: center;
  color: #999;
  padding: 10px 0;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}
.modal {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  width: 320px;
}
.modal-lg {
  width: 460px;
  max-height: 80vh;
  overflow-y: auto;
}
.detail-img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: 8px;
}
.detail-price {
  color: #ff4d4f;
  font-size: 16px;
  font-weight: bold;
}
.primary-button {
  background: #1890ff;
  color: #fff;
  border: none;
  padding: 6px 10px;
  border-radius: 6px;
  cursor: pointer;
}
.recommend-wrap {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}
.recommend-title {
  font-size: 15px;
  margin: 0 0 12px;
  color: #333;
}
.recommend-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.no-recommend {
  color: #999;
  font-size: 13px;
  padding: 5px 0;
}
.recommend-item {
  display: flex;
  align-items: center;
  width: 180px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 6px;
}
.rec-img {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  object-fit: cover;
}
.rec-info {
  flex: 1;
  padding: 0 8px;
}
.rec-name {
  font-size: 12px;
  margin-bottom: 3px;
}
.rec-price {
  font-size: 12px;
  color: #f53f3f;
}
.rec-add-btn {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  background: #1890ff;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.receipt-modal {
  width: 380px;
  padding: 24px;
  border-radius: 12px;
  text-align: center;
}
.receipt-header h3 {
  margin: 0 0 4px;
  color: #1890ff;
}
.receipt-header p {
  margin: 0 0 16px;
  color: #666;
  font-size: 12px;
}
.receipt-body {
  margin: 16px 0;
  text-align: left;
}
.receipt-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 14px;
}
.receipt-item .name {
  flex: 1;
}
.receipt-item .count {
  width: 40px;
  text-align: center;
}
.receipt-item .sub {
  width: 60px;
  text-align: right;
  font-weight: 500;
}
.line {
  height: 1px;
  background: #eee;
  margin: 12px 0;
}
.total-row {
  display: flex;
  justify-content: space-between;
  font-size: 16px;
  font-weight: bold;
  color: #ff4d4f;
  margin-bottom: 4px;
}
.balance-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #666;
  margin-bottom: 16px;
}
.close-btn {
  width: 100%;
  background: #1890ff;
  color: white;
  border: none;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
}
</style>