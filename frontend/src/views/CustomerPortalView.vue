<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PortalHeader from '../components/PortalHeader.vue'
import { useNomNomApp } from '../utils/useNomNomApp'
import { customerApi, fetchAvailableTables } from '../services/api'
import axios from 'axios'

const router = useRouter()
const app = useNomNomApp()
const http = axios.create({ baseURL: '/api', timeout: 10000 })

const wallet = ref(0)
const rechargeVisible = ref(false)
const rechargeAmount = ref('')
const detailVisible = ref(false)
const currentDish = ref({})
const recommendList = ref([])
const detailEvaluations = ref([])
const orderReceiptVisible = ref(false)
const orderInfo = ref({})
const categoryList = ref([])
const activeView = ref('menu')
const activeCategory = ref(null)
const localMenuItems = ref([])
const keyword = ref('')
const tableList = ref([])
const selectedTableId = ref(null)
const cart = ref([])
const orderList = ref([])
const collectionList = ref([])
const favoriteIds = ref(new Set())
const myEvaluations = ref([])
const evalVisible = ref(false)
const evaluatingOrder = ref(null)
const evaluatingItem = ref(null)
const evaluationForm = ref({ ratingValue: 5, content: '', imageUrls: [] })
const orderRequestVisible = ref(false)
const orderRequestType = ref('CANCEL')
const orderRequestTarget = ref(null)
const orderRequestReason = ref('')
const orderMode = ref('all')
const messageList = ref([])
const unreadMessages = ref(0)
const messagesVisible = ref(false)
let messageTimer = null

const cartCount = computed(() => cart.value.reduce((s, i) => s + i.quantity, 0))
const cartTotal = computed(() => Number(cart.value.reduce((s, i) => s + i.price * i.quantity, 0).toFixed(2)))
const selectedTable = computed(() => tableList.value.find(t => t.id === selectedTableId.value))
const showCart = computed(() => activeView.value !== 'orders' && cart.value.length > 0)
const paidOrders = computed(() => orderList.value.filter(order => isPaidOrder(order.status)))
const completedOrders = computed(() => orderList.value.filter(order => canEvaluateOrder(order.status)))
const reviewableOrders = computed(() =>
  completedOrders.value.filter(order => (order.items || []).some(item => !hasEvaluation(order.id, item.dishesId)))
)
const visibleOrderList = computed(() => {
  if (orderMode.value === 'paid') return paidOrders.value
  if (orderMode.value === 'reviewable') return reviewableOrders.value
  return orderList.value
})
const orderEmptyText = computed(() => {
  if (orderMode.value === 'paid') return '暂无已支付订单'
  if (orderMode.value === 'reviewable') return '暂无可评价菜品'
  return '暂无订单'
})
const filteredMenuItems = computed(() => {
  return localMenuItems.value.filter(item => {
    if (item.status !== 'AVAILABLE' && item.status !== 1) return false
    const text = `${item.name || ''} ${item.detail || ''} ${item.description || ''}`
    return keyword.value ? text.includes(keyword.value) : true
  })
})
const favoriteItems = computed(() => {
  const menuByDishId = new Map(localMenuItems.value.map(item => [Number(item.dishesId || item.id), item]))

  return collectionList.value.map(collection => {
    const dishesId = Number(collection.dishesId)
    const menuItem = menuByDishId.get(dishesId)
    const packageId = Number(collection.packageId || menuItem?.id || 0)
    const status = menuItem?.status || collection.dishStatus
    const available = Boolean(packageId) && (status === 'AVAILABLE' || status === 1)

    return {
      ...(menuItem || {}),
      id: packageId || dishesId,
      dishesId,
      name: menuItem?.name || collection.dishName || '未知菜品',
      coverUrl: menuItem?.coverUrl || collection.dishCover,
      price: Number(collection.price ?? menuItem?.price ?? 0),
      status,
      monthlySales: menuItem?.monthlySales || 0,
      description: menuItem?.description || '',
      available
    }
  })
})

function openRechargeDialog() {
  rechargeAmount.value = ''
  rechargeVisible.value = true
}

async function doRecharge() {
  const num = Number(rechargeAmount.value)
  if (!num || num <= 0) return alert('请输入正确金额')
  try {
    const res = await customerApi.rechargeWallet(num)
    wallet.value = Number(res.data?.data?.surplus || 0)
    rechargeVisible.value = false
    await loadMessages()
    alert('充值成功！余额：￥' + wallet.value.toFixed(2))
  } catch (err) {
    alert(err?.response?.data?.message || '充值失败')
  }
}

async function openDetail(item) {
  currentDish.value = item
  detailVisible.value = true
  try {
    const res = await http.get(`/menu/recommend/${item.id}`)
    recommendList.value = res.data.data || []
  } catch {
    recommendList.value = []
  }
  try {
    const res = await http.get(`/evaluations/dish/${item.dishesId || item.id}`)
    detailEvaluations.value = res.data.data || []
  } catch {
    detailEvaluations.value = []
  }
}

function closeDetailModal() {
  detailVisible.value = false
  recommendList.value = []
  detailEvaluations.value = []
}

async function loadCategory() {
  try {
    const res = await http.get('/menu/categories')
    categoryList.value = res.data.data || []
  } catch (err) {
    console.error('加载分类失败', err)
  }
}

async function loadMenu() {
  try {
    const params = {}
    if (activeCategory.value != null) params.category = activeCategory.value
    const res = await http.get('/menu/items', { params })
    localMenuItems.value = res.data.data || []
  } catch (err) {
    console.error('加载菜单失败', err)
    alert('菜单加载失败')
  }
}

async function loadTables() {
  try {
    tableList.value = (await fetchAvailableTables()).filter(table => Number(table.status ?? 0) === 0)
    if (!tableList.value.some(table => Number(table.id) === Number(selectedTableId.value))) {
      selectedTableId.value = tableList.value.length > 0 ? tableList.value[0].id : null
    }
  } catch {
    tableList.value = []
    selectedTableId.value = null
  }
}

async function loadWallet() {
  try {
    const res = await customerApi.getWallet()
    wallet.value = Number(res.data?.data?.surplus || 0)
  } catch {
    wallet.value = 0
  }
}

async function loadOrders() {
  try {
    const res = await customerApi.getOrders()
    orderList.value = res.data?.data || []
  } catch {
    orderList.value = []
  }
}

async function loadCollections() {
  try {
    const res = await customerApi.getCollections()
    collectionList.value = res.data?.data || []
    favoriteIds.value = new Set(collectionList.value.map(item => Number(item.dishesId)))
  } catch {
    collectionList.value = []
    favoriteIds.value = new Set()
  }
}

async function loadMyEvaluations() {
  try {
    const res = await customerApi.getMyEvaluations()
    myEvaluations.value = res.data?.data || []
  } catch {
    myEvaluations.value = []
  }
}

async function loadMessages() {
  try {
    const [listRes, countRes] = await Promise.all([
      customerApi.getMessages(),
      customerApi.getUnreadCount()
    ])
    messageList.value = listRes.data?.data || []
    unreadMessages.value = Number(countRes.data?.data || 0)
  } catch {
    messageList.value = []
    unreadMessages.value = 0
  }
}

async function markAllMessagesRead() {
  try {
    await customerApi.markAllMessagesRead()
    await loadMessages()
  } catch {}
}

async function markMessageRead(message) {
  if (!message?.id || message.readStatus !== 0) return
  try {
    await customerApi.markMessageRead(message.id)
    message.readStatus = 1
    unreadMessages.value = Math.max(0, unreadMessages.value - 1)
  } catch {}
}

function changeCategory(catId) {
  activeView.value = 'menu'
  activeCategory.value = catId
  loadMenu()
}

function showFavorites() {
  activeView.value = 'favorites'
  loadCollections()
}

function showOrders() {
  activeView.value = 'orders'
  loadOrders()
  loadMyEvaluations()
}

function addToCart(item) {
  if (item.available === false) return
  const found = cart.value.find(i => i.id === item.id)
  if (found) found.quantity++
  else cart.value.push({ ...item, price: Number(item.price || 0), quantity: 1 })
}

function updateQuantity(id, num) {
  const it = cart.value.find(i => i.id === id)
  if (!it) return
  it.quantity += num
  if (it.quantity <= 0) cart.value = cart.value.filter(i => i.id !== id)
}

async function handleSubmit() {
  if (cartCount.value === 0) return alert('购物车是空的！')
  if (wallet.value < cartTotal.value) return alert('余额不足，请先充值')
  if (!selectedTableId.value) return alert('请选择桌台')

  try {
    const res = await customerApi.createOrder({
      tableId: selectedTableId.value,
      tableNumber: selectedTable.value?.number || '',
      customerName: app.displayName.value,
      items: cart.value.map(item => ({
        itemId: item.id,
        itemName: item.name,
        quantity: item.quantity,
        unitPrice: item.price
      }))
    })
    const result = res.data?.data || {}
    orderInfo.value = {
      time: new Date().toLocaleString(),
      orderNo: result.orderNo,
      tableNumber: selectedTable.value?.number || '',
      items: [...cart.value],
      total: cartTotal.value,
      balance: Number((wallet.value - cartTotal.value).toFixed(2))
    }
    await loadWallet()
    await loadOrders()
    await loadMenu()
    orderReceiptVisible.value = true
    cart.value = []
  } catch (err) {
    alert(err?.response?.data?.message || '订单提交失败')
  }
}

function isFavorited(item) {
  return favoriteIds.value.has(Number(item.dishesId || item.id))
}

async function toggleFavorite(item) {
  const dishesId = Number(item.dishesId || item.id)
  if (!dishesId) return
  try {
    const res = await customerApi.toggleCollection(dishesId)
    const next = new Set(favoriteIds.value)
    if (res.data?.data) {
      next.add(dishesId)
    } else {
      next.delete(dishesId)
    }
    favoriteIds.value = next
    await loadCollections()
  } catch (err) {
    alert(err?.response?.data?.message || '收藏操作失败')
  }
}

function orderStatusText(status) {
  return {
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    PREPARING: '制作中',
    COMPLETED: '已完成',
    CANCELED: '已取消'
  }[status] || status || '未知'
}

function hasEvaluation(orderId, dishesId) {
  return myEvaluations.value.some(ev => Number(ev.ordersId) === Number(orderId) && Number(ev.dishesId) === Number(dishesId))
}

function canEvaluateOrder(status) {
  return status === 'COMPLETED'
}

function isPaidOrder(status) {
  return ['PAID', 'PREPARING', 'COMPLETED'].includes(status)
}

function canCancelOrder(order) {
  return ['PAID', 'PREPARING'].includes(order.status) && !order.refundStatus
}

function canRequestRefund(order) {
  return order.status === 'COMPLETED' && !order.refundStatus
}

function refundStatusText(status) {
  return {
    2: '审核中',
    3: '已退款',
    4: '已驳回'
  }[Number(status)] || ''
}

function refundCauseText(cause) {
  return (cause || '').replace(/^取消订单：/, '').replace(/^退款：/, '')
}

function openOrderRequest(order, type) {
  orderRequestTarget.value = order
  orderRequestType.value = type
  orderRequestReason.value = ''
  orderRequestVisible.value = true
}

async function submitOrderRequest() {
  const reason = orderRequestReason.value.trim()
  if (!reason) return alert('请填写原因')
  try {
    await customerApi.submitRefund({
      ordersId: orderRequestTarget.value.id,
      requestType: orderRequestType.value,
      refundCause: reason
    })
    orderRequestVisible.value = false
    await loadOrders()
    await loadWallet()
    await loadMessages()
    alert('申请已提交，等待商家审核')
  } catch (err) {
    alert(err?.response?.data?.message || '申请提交失败')
  }
}

function openEvaluation(order, item) {
  evaluatingOrder.value = order
  evaluatingItem.value = item
  evaluationForm.value = { ratingValue: 5, content: '', imageUrls: [] }
  evalVisible.value = true
}

async function handleEvaluationImage(event) {
  const file = event.target.files[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await customerApi.uploadFile(formData)
    evaluationForm.value.imageUrls.push(res.data?.data || res.data)
  } catch (err) {
    alert(err?.response?.data?.message || '图片上传失败')
  }
}

async function submitEvaluation() {
  if (!evaluationForm.value.content.trim()) return alert('请输入评价内容')
  try {
    await customerApi.createEvaluation({
      ordersId: evaluatingOrder.value.id,
      dishesId: evaluatingItem.value.dishesId,
      ratingValue: Number(evaluationForm.value.ratingValue),
      content: evaluationForm.value.content.trim(),
      imageUrls: evaluationForm.value.imageUrls
    })
    evalVisible.value = false
    await loadMyEvaluations()
    await loadOrders()
    await loadMessages()
  } catch (err) {
    alert(err?.response?.data?.message || '评价失败')
  }
}

function messageTypeText(type) {
  return {
    2: '订单',
    3: '钱包',
    4: '评价'
  }[type] || '通知'
}

async function exitPortal() {
  app.logout()
  await router.push('/login')
}

onMounted(() => {
  app.loadPortalData()
  loadCategory()
  loadMenu()
  loadTables()
  loadWallet()
  loadOrders()
  loadCollections()
  loadMyEvaluations()
  loadMessages()
  messageTimer = window.setInterval(loadMessages, 30000)
})

onUnmounted(() => {
  if (messageTimer) {
    window.clearInterval(messageTimer)
  }
})
</script>

<template>
  <div class="portal-shell">
    <PortalHeader
      title="顾客端点餐台"
      :display-name="app.displayName.value"
      :role-label="app.currentUser.value?.roleLabel || '顾客'"
      :loading="app.pageLoading.value"
      @logout="exitPortal"
    />

    <div class="wallet-bar">
      <span>余额：￥{{ wallet.toFixed(2) }}</span>
      <select v-model.number="selectedTableId" class="table-select">
        <option :value="null">选择桌台</option>
        <option v-for="table in tableList" :key="table.id" :value="table.id">
          {{ table.area || '大厅' }} · {{ table.number }}号桌 · {{ table.personNumber || 2 }}人
        </option>
      </select>
      <button class="message-toggle" @click="messagesVisible = !messagesVisible">
        消息<span v-if="unreadMessages > 0">{{ unreadMessages }}</span>
      </button>
      <button class="recharge-btn" @click="openRechargeDialog">充值</button>
    </div>

    <section v-if="messagesVisible" class="message-panel">
      <div class="message-head">
        <strong>消息提醒</strong>
        <button :disabled="unreadMessages === 0" @click="markAllMessagesRead">全部已读</button>
      </div>
      <div v-if="messageList.length === 0" class="empty-hint">暂无消息</div>
      <article
        v-for="msg in messageList"
        :key="msg.id"
        class="message-line"
        :class="{ unread: msg.readStatus === 0 }"
      >
        <span>{{ messageTypeText(msg.type) }}</span>
        <p>{{ msg.content }}</p>
        <small>{{ msg.createTime || '-' }}</small>
        <button
          v-if="msg.readStatus === 0"
          class="message-read-btn"
          type="button"
          @click="markMessageRead(msg)"
        >
          设为已读
        </button>
      </article>
    </section>

    <div class="main-layout">
      <aside class="category-sidebar">
        <button
          class="category-btn favorite-category-btn"
          :class="{ active: activeView === 'favorites' }"
          type="button"
          @click="showFavorites"
        >
          <span>♥</span>
          我的收藏
        </button>
        <button class="category-btn" :class="{ active: activeView === 'menu' && activeCategory === null }" @click="changeCategory(null)">
          全部菜品
        </button>
        <button
          v-for="cat in categoryList"
          :key="cat.code"
          class="category-btn"
          :class="{ active: activeView === 'menu' && activeCategory === cat.code }"
          @click="changeCategory(cat.code)"
        >
          {{ cat.name }}
        </button>
        <button
          class="category-btn order-category-btn"
          :class="{ active: activeView === 'orders' }"
          type="button"
          @click="showOrders"
        >
          我的订单
        </button>
      </aside>

      <section class="menu-area">
        <template v-if="activeView === 'menu'">
          <div class="search-box">
            <input v-model.lazy="keyword" class="search-input" type="text" placeholder="搜索菜品或套餐" />
          </div>

          <div class="menu-list">
            <article v-for="item in filteredMenuItems" :key="item.id" class="menu-card" @click="openDetail(item)">
              <button
                class="fav-btn"
                :class="{ active: isFavorited(item) }"
                type="button"
                :title="isFavorited(item) ? '取消收藏' : '收藏菜品'"
                :aria-label="isFavorited(item) ? '取消收藏' : '收藏菜品'"
                @click.stop="toggleFavorite(item)"
              >
                {{ isFavorited(item) ? '♥' : '♡' }}
              </button>
              <img :src="item.coverUrl || '/placeholder.png'" class="menu-image" />
              <h3 class="menu-title">{{ item.name }}</h3>
              <p class="menu-sales">月售 {{ item.monthlySales || 0 }}</p>
              <p class="menu-price">￥{{ Number(item.price || 0).toFixed(2) }}</p>
              <button class="add-btn" @click.stop="addToCart(item)">+</button>
            </article>
          </div>
        </template>

        <section v-else-if="activeView === 'favorites'" class="favorites-panel single-view-panel">
          <div class="favorites-head">
            <h3>我的收藏</h3>
          </div>
          <div v-if="favoriteItems.length === 0" class="empty-hint">暂无收藏菜品</div>
          <div v-else class="favorite-list">
            <article
              v-for="fav in favoriteItems"
              :key="fav.dishesId"
              class="favorite-card"
              :class="{ unavailable: !fav.available }"
              @click="fav.available && openDetail(fav)"
            >
              <img :src="fav.coverUrl || '/placeholder.png'" class="favorite-image" />
              <div class="favorite-info">
                <h4>{{ fav.name }}</h4>
                <p v-if="fav.available">￥{{ Number(fav.price || 0).toFixed(2) }}</p>
                <p v-else>已下架</p>
              </div>
              <button
                class="favorite-icon-button active"
                type="button"
                title="取消收藏"
                aria-label="取消收藏"
                @click.stop="toggleFavorite(fav)"
              >
                ♥
              </button>
              <button
                class="favorite-add-button"
                type="button"
                title="加入购物车"
                aria-label="加入购物车"
                :disabled="!fav.available"
                @click.stop="addToCart(fav)"
              >
                +
              </button>
            </article>
          </div>
        </section>

        <section v-else-if="activeView === 'orders'" class="order-history single-view-panel">
          <div class="history-title">
            <h3>我的订单</h3>
            <div class="history-actions">
              <button :class="{ active: orderMode === 'all' }" @click="orderMode = 'all'">全部</button>
              <button :class="{ active: orderMode === 'paid' }" @click="orderMode = 'paid'">已支付订单</button>
              <button :class="{ active: orderMode === 'reviewable' }" @click="orderMode = 'reviewable'">可评价</button>
              <button @click="loadOrders">刷新</button>
            </div>
          </div>
          <div v-if="visibleOrderList.length === 0" class="empty-hint">{{ orderEmptyText }}</div>
          <article v-for="order in visibleOrderList" :key="order.id" class="history-card">
            <div class="history-head">
              <span>{{ order.orderNo }}</span>
              <strong>{{ orderStatusText(order.status) }}</strong>
            </div>
            <p class="history-meta">
              {{ order.createTime || '-' }} · {{ order.tableNumber ? order.tableNumber + '号桌' : '未记录桌台' }} ·
              ￥{{ Number(order.totalAmount || 0).toFixed(2) }}
            </p>
            <div v-if="order.refundStatus" class="refund-status-line" :class="`status-${order.refundStatus}`">
              <strong>{{ refundStatusText(order.refundStatus) }}</strong>
              <span v-if="order.refundCause">原因：{{ refundCauseText(order.refundCause) }}</span>
              <span v-if="Number(order.refundStatus) === 4 && order.rejectRefundCause">
                商家驳回理由：{{ order.rejectRefundCause }}
              </span>
            </div>
            <div class="history-items">
              <div v-for="item in order.items || []" :key="item.id" class="history-item">
                <span>{{ item.name }} x{{ item.quantity }}</span>
                <button
                  v-if="canEvaluateOrder(order.status) && !hasEvaluation(order.id, item.dishesId)"
                  @click="openEvaluation(order, item)"
                >
                  评价
                </button>
                <em v-else-if="hasEvaluation(order.id, item.dishesId)">已评价</em>
              </div>
            </div>
            <div class="order-request-actions">
              <button v-if="canCancelOrder(order)" @click="openOrderRequest(order, 'CANCEL')">取消订单</button>
              <button v-if="canRequestRefund(order)" @click="openOrderRequest(order, 'REFUND')">申请退款</button>
            </div>
          </article>
        </section>
      </section>
    </div>

    <footer v-if="showCart" class="cart-footer">
      <div class="cart-summary">
        <div class="cart-info">
          <span class="cart-count">共 {{ cartCount }} 份</span>
          <span class="cart-total">合计：￥{{ cartTotal.toFixed(2) }}</span>
        </div>
        <button class="submit-btn" :disabled="cartCount === 0 || wallet < cartTotal" @click="handleSubmit">
          提交并支付
        </button>
      </div>

      <div class="cart-items">
        <article v-for="item in cart" :key="item.id" class="cart-item">
          <div class="item-info">
            <h3>{{ item.name }}</h3>
            <p>单价 ￥{{ Number(item.price || 0).toFixed(2) }}</p>
          </div>
          <div class="quantity-control">
            <button class="qty-btn" @click="updateQuantity(item.id, -1)">-</button>
            <span class="qty-text">{{ item.quantity }}</span>
            <button class="qty-btn" @click="updateQuantity(item.id, 1)">+</button>
          </div>
        </article>
        <p v-if="cart.length === 0" class="empty-hint">购物车为空</p>
      </div>
    </footer>

    <div v-if="rechargeVisible" class="modal-overlay" @click.self="rechargeVisible = false">
      <div class="modal">
        <h3>模拟充值</h3>
        <input v-model.number="rechargeAmount" type="number" placeholder="金额" />
        <div class="modal-buttons">
          <button @click="rechargeVisible = false">取消</button>
          <button class="primary-button" @click="doRecharge">确认</button>
        </div>
      </div>
    </div>

    <div v-if="detailVisible" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal modal-lg">
        <img :src="currentDish.coverUrl || '/placeholder.png'" class="detail-img" />
        <h3>{{ currentDish.name }}</h3>
        <p>{{ currentDish.description || currentDish.detail }}</p>
        <p class="detail-price">￥{{ Number(currentDish.price || 0).toFixed(2) }}</p>
        <div class="detail-actions">
          <button class="primary-button" :disabled="currentDish.available === false" @click="addToCart(currentDish)">
            加入购物车
          </button>
          <button
            class="favorite-action"
            :class="{ active: isFavorited(currentDish) }"
            type="button"
            :title="isFavorited(currentDish) ? '取消收藏' : '收藏菜品'"
            :aria-label="isFavorited(currentDish) ? '取消收藏' : '收藏菜品'"
            @click="toggleFavorite(currentDish)"
          >
            {{ isFavorited(currentDish) ? '♥' : '♡' }}
          </button>
        </div>

        <div class="recommend-wrap">
          <h4 class="recommend-title">推荐搭配</h4>
          <div class="recommend-list">
            <div v-if="recommendList.length === 0" class="no-recommend">暂无</div>
            <div v-for="rec in recommendList" :key="rec.id" class="recommend-item">
              <img :src="rec.coverUrl || '/placeholder.png'" class="rec-img" />
              <div class="rec-info">
                <div class="rec-name">{{ rec.name }}</div>
                <div class="rec-price">￥{{ Number(rec.price || 0).toFixed(2) }}</div>
              </div>
              <button class="rec-add-btn" @click="addToCart(rec)">+</button>
            </div>
          </div>
        </div>

        <div class="recommend-wrap">
          <h4 class="recommend-title">顾客评价</h4>
          <div v-if="detailEvaluations.length === 0" class="no-recommend">暂无</div>
          <div v-for="ev in detailEvaluations" :key="ev.id" class="evaluation-line">
            <strong>{{ '★'.repeat(ev.ratingValue || 0) }}</strong>
            <span>{{ ev.content }}</span>
            <small v-if="ev.replyContent">商家回复：{{ ev.replyContent }}</small>
          </div>
        </div>
      </div>
    </div>

    <div v-if="evalVisible" class="modal-overlay" @click.self="evalVisible = false">
      <div class="modal">
        <h3>评价菜品</h3>
        <p class="eval-dish-name">{{ evaluatingItem?.name }}</p>
        <div class="rating-picker" aria-label="评价星级">
          <button
            v-for="n in 5"
            :key="n"
            type="button"
            class="star-button"
            :class="{ active: n <= evaluationForm.ratingValue }"
            :aria-label="`${n} 星`"
            @click="evaluationForm.ratingValue = n"
          >
            ★
          </button>
          <span>{{ evaluationForm.ratingValue }} 星</span>
        </div>
        <textarea v-model="evaluationForm.content" class="eval-textarea" placeholder="评价内容"></textarea>
        <input type="file" accept="image/*" @change="handleEvaluationImage" />
        <div class="eval-images">
          <img v-for="url in evaluationForm.imageUrls" :key="url" :src="url" />
        </div>
        <div class="modal-buttons">
          <button @click="evalVisible = false">取消</button>
          <button class="primary-button" @click="submitEvaluation">提交</button>
        </div>
      </div>
    </div>

    <div v-if="orderRequestVisible" class="modal-overlay" @click.self="orderRequestVisible = false">
      <div class="modal">
        <h3>{{ orderRequestType === 'CANCEL' ? '取消订单申请' : '退款申请' }}</h3>
        <p class="request-order-no">订单号：{{ orderRequestTarget?.orderNo }}</p>
        <textarea
          v-model="orderRequestReason"
          class="eval-textarea"
          :placeholder="orderRequestType === 'CANCEL' ? '请说明取消订单原因' : '请说明申请退款原因'"
        ></textarea>
        <div class="modal-buttons">
          <button @click="orderRequestVisible = false">取消</button>
          <button class="primary-button" @click="submitOrderRequest">提交申请</button>
        </div>
      </div>
    </div>

    <div v-if="orderReceiptVisible" class="modal-overlay" @click.self="orderReceiptVisible = false">
      <div class="modal receipt-modal">
        <div class="receipt-header">
          <h3>订单支付成功</h3>
          <p>{{ orderInfo.time }}</p>
          <p v-if="orderInfo.orderNo">订单号：{{ orderInfo.orderNo }}</p>
          <p v-if="orderInfo.tableNumber">桌台：{{ orderInfo.tableNumber }}号桌</p>
        </div>
        <div class="receipt-body">
          <div class="receipt-item" v-for="(item, idx) in orderInfo.items" :key="idx">
            <div class="name">{{ item.name }}</div>
            <div class="count">x{{ item.quantity }}</div>
            <div class="sub">￥{{ (item.price * item.quantity).toFixed(2) }}</div>
          </div>
        </div>
        <div class="line"></div>
        <div class="total-row">
          <span>实付金额</span>
          <span>￥{{ orderInfo.total }}</span>
        </div>
        <div class="balance-row">
          <span>剩余余额</span>
          <span>￥{{ wallet.toFixed(2) }}</span>
        </div>
        <button class="close-btn" @click="orderReceiptVisible = false">完成</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.portal-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}
.wallet-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: space-between;
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid #eee;
  font-weight: bold;
}
.table-select {
  flex: 1;
  max-width: 320px;
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
}
.recharge-btn {
  background: #4CAF50;
  color: white;
  border: none;
  padding: 6px 14px;
  border-radius: 4px;
  cursor: pointer;
}
.message-toggle {
  border: 1px solid #d9e6ff;
  background: #f5f9ff;
  color: #1767c4;
  padding: 6px 14px;
  border-radius: 4px;
  cursor: pointer;
}
.message-toggle span {
  display: inline-grid;
  place-items: center;
  min-width: 18px;
  height: 18px;
  margin-left: 6px;
  border-radius: 9px;
  background: #ff4d4f;
  color: #fff;
  font-size: 11px;
}
.message-panel {
  padding: 10px 20px;
  background: #f8fbff;
  border-bottom: 1px solid #e4eefb;
  display: grid;
  gap: 8px;
  max-height: 220px;
  overflow-y: auto;
}
.message-head,
.message-line {
  display: flex;
  align-items: center;
  gap: 10px;
}
.message-head {
  justify-content: space-between;
}
.message-head button,
.history-actions button {
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
}
.message-head button:disabled {
  color: #aaa;
  cursor: not-allowed;
}
.message-line {
  background: #fff;
  border: 1px solid #edf2f8;
  border-radius: 6px;
  padding: 8px 10px;
}
.message-line.unread {
  border-color: #8ab8ff;
}
.message-line span {
  color: #1767c4;
  font-size: 12px;
  font-weight: 700;
  min-width: 36px;
}
.message-line p {
  flex: 1;
  margin: 0;
  font-size: 13px;
}
.message-line small {
  color: #888;
  font-size: 11px;
}
.message-read-btn {
  border: 1px solid #8ab8ff;
  background: #f5f9ff;
  color: #1767c4;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
}
.message-read-btn:hover {
  background: #eaf3ff;
}
.main-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}
.category-sidebar {
  width: 112px;
  background: #fafafa;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 8px;
}
.category-btn {
  min-height: 44px;
  padding: 9px 8px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 14px;
  border-radius: 8px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px;
}
.category-btn.active {
  background: #fff;
  color: #1890ff;
  box-shadow: inset 3px 0 0 #1890ff;
}
.favorite-category-btn {
  border: 1px solid #ffd6d6;
  background: #fff6f6;
  color: #d43838;
}
.favorite-category-btn span {
  font-size: 16px;
  line-height: 1;
}
.favorite-category-btn.active {
  background: #ff4d4f;
  border-color: #ff4d4f;
  color: #fff;
  box-shadow: none;
}
.order-category-btn {
  border: 1px solid #d9e6ff;
  background: #f5f9ff;
  color: #1767c4;
}
.order-category-btn.active {
  background: #1890ff;
  border-color: #1890ff;
  color: #fff;
  box-shadow: none;
}
.menu-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 10px;
}
.search-box {
  margin-bottom: 14px;
}
.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-sizing: border-box;
}
.favorites-panel {
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid #ffe0e0;
  border-radius: 8px;
  background: #fffafa;
}
.single-view-panel {
  margin-top: 0;
  margin-bottom: 0;
}
.favorites-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}
.favorites-head h3 {
  margin: 0;
  font-size: 15px;
}
.favorites-head span {
  color: #999;
  font-size: 12px;
}
.favorite-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 8px;
}
.favorite-card {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) 28px 28px;
  align-items: center;
  gap: 8px;
  min-height: 68px;
  padding: 8px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}
.favorite-card.unavailable {
  opacity: 0.58;
  cursor: default;
}
.favorite-image {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  object-fit: cover;
}
.favorite-info {
  min-width: 0;
}
.favorite-info h4 {
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.favorite-info p {
  margin: 0;
  color: #ff4d4f;
  font-size: 12px;
  font-weight: 700;
}
.favorite-icon-button,
.favorite-add-button,
.favorite-action {
  display: inline-grid;
  place-items: center;
  border: none;
  cursor: pointer;
}
.favorite-icon-button,
.favorite-add-button {
  width: 28px;
  height: 28px;
  border-radius: 50%;
}
.favorite-icon-button {
  background: #fff1f0;
  color: #ff4d4f;
}
.favorite-add-button {
  background: #1890ff;
  color: #fff;
  font-size: 16px;
}
.favorite-add-button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.menu-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 14px;
}
.menu-card {
  aspect-ratio: 1 / 1.22;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  cursor: pointer;
  position: relative;
}
.fav-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  display: inline-grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border: none;
  background: rgba(255, 255, 255, 0.92);
  color: #666;
  border-radius: 50%;
  font-size: 16px;
  padding: 0;
  cursor: pointer;
}
.fav-btn.active {
  color: #ff4d4f;
}
.menu-image {
  width: 100%;
  height: 112px;
  object-fit: cover;
  border-radius: 6px;
  margin-bottom: 10px;
}
.menu-title {
  font-size: 15px;
  margin: 0 0 6px;
  text-align: center;
  line-height: 1.3;
}
.menu-sales {
  font-size: 12px;
  color: #999;
  margin: 0 0 4px;
}
.menu-price {
  font-size: 15px;
  color: #ff4d4f;
  margin: 0;
  font-weight: 700;
}
.add-btn {
  position: absolute;
  bottom: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.order-history {
  margin-top: 16px;
  border-top: 1px solid #eee;
  padding-top: 12px;
}
.order-history.single-view-panel {
  margin-top: 0;
  border-top: none;
  padding-top: 0;
}
.history-title,
.history-head,
.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.history-title h3 {
  margin: 0;
  font-size: 15px;
}
.history-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}
.history-actions button.active {
  background: #1890ff;
  border-color: #1890ff;
  color: #fff;
}
.history-title > button,
.history-item button {
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
}
.history-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 10px;
  margin-top: 8px;
  background: #fff;
}
.history-head span {
  font-weight: bold;
}
.history-head strong {
  color: #1890ff;
}
.history-meta,
.history-item em {
  color: #888;
  font-size: 12px;
}
.history-items {
  display: grid;
  gap: 6px;
}
.refund-status-line {
  display: grid;
  gap: 4px;
  margin: 8px 0;
  padding: 8px 10px;
  border-radius: 6px;
  background: #f8fbff;
  color: #1767c4;
  font-size: 12px;
}
.refund-status-line.status-3 {
  background: #f1f8e9;
  color: #558b2f;
}
.refund-status-line.status-4 {
  background: #fff1f0;
  color: #cf1322;
}
.order-request-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
.order-request-actions button {
  border: 1px solid #d9e6ff;
  background: #f5f9ff;
  color: #1767c4;
  border-radius: 4px;
  padding: 4px 10px;
  cursor: pointer;
  font-size: 12px;
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
.modal input,
.eval-textarea {
  width: 100%;
  box-sizing: border-box;
  margin-bottom: 10px;
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
}
.eval-textarea {
  height: 90px;
  resize: none;
}
.eval-dish-name {
  color: #666;
}
.request-order-no {
  margin: 0 0 10px;
  color: #666;
  font-size: 13px;
}
.rating-picker {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 6px 0 12px;
}
.star-button {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #d0d0d0;
  cursor: pointer;
  font-size: 26px;
  line-height: 1;
  padding: 0;
}
.star-button.active,
.star-button:hover {
  color: #f5b301;
}
.rating-picker span {
  margin-left: 4px;
  color: #666;
  font-size: 13px;
}
.eval-images {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}
.eval-images img {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 4px;
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
.detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.favorite-action {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff1f0;
  color: #999;
  font-size: 20px;
}
.favorite-action.active {
  color: #ff4d4f;
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
.rec-name,
.rec-price {
  font-size: 12px;
}
.rec-price {
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
}
.evaluation-line {
  display: grid;
  gap: 4px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}
.evaluation-line strong {
  color: #f57c00;
}
.evaluation-line small {
  color: #666;
}
.modal-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.modal-buttons button {
  border: 1px solid #ddd;
  background: #fff;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
}
.modal-buttons .primary-button {
  border: none;
  background: #1890ff;
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
  margin: 0 0 6px;
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
@media (max-width: 720px) {
  .favorite-list {
    grid-template-columns: 1fr;
  }
}
</style>
