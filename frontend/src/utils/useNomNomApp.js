import { computed, onMounted, reactive, ref } from 'vue'
import {
  createOrder,
  fetchCategories,
  fetchMenuItems,
  fetchOrderPreview,
  fetchOrders,
  login,
  register,
} from '../services/api'

const STORAGE_KEY = 'nomnom-auth-user'

// Shared copy used by the auth page.
const portalOptions = [
  { code: 'CUSTOMER', label: '顾客端', description: '点餐、下单、查看自己的订单' },
  { code: 'MERCHANT', label: '商家端', description: '管理菜单、查看全店订单与服务面板' },
]

const customerModules = [
  { title: '在线点餐', detail: '浏览菜单与套餐，直接加入购物车并下单。' },
  { title: '我的订单', detail: '只查看当前顾客自己的订单记录。' },
  { title: '订单预览', detail: '提交前先核对总额、数量与备注信息。' },
]

const merchantModules = [
  { title: '菜品管理', detail: '围绕菜品、分类和套餐继续扩展维护能力。' },
  { title: '订单看板', detail: '查看全店订单状态，跟进出餐和支付进度。' },
  { title: '评价与消息', detail: '后续可继续接入评价、图片与消息模块。' },
  { title: '会员与钱包', detail: '后续可继续接入用户、钱包等商家能力。' },
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
const displayName = computed(
  () => currentUser.value?.username || currentUser.value?.account || '未登录用户',
)

// Keep filtering in one place so the customer page stays presentational.
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

function clearPortalState() {
  categories.value = []
  menuItems.value = []
  orderQueue.value = []
  activeCategory.value = ''
  keyword.value = ''
  portalMessage.value = ''
  resetCustomerDraft()
}

function logout() {
  localStorage.removeItem(STORAGE_KEY)
  currentUser.value = null
  authMode.value = 'login'
  authMessage.value = ''
  clearPortalState()
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
    PREPARING: '备餐中',
    COMPLETED: '已完成',
    UNKNOWN: '未知状态',
  }

  return dictionary[status] || status
}

export function resolvePortalPathForUser(user) {
  if (user?.roleCode === 'MERCHANT') {
    return '/merchant'
  }
  if (user?.roleCode === 'CUSTOMER') {
    return '/customer'
  }
  return '/login'
}

export async function loadPortalData(force = false) {
  if (!currentUser.value) {
    return
  }

  if (!force && (categories.value.length > 0 || menuItems.value.length > 0 || orderQueue.value.length > 0)) {
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

async function refreshPortalData() {
  await loadPortalData(true)
}

async function handleLogin() {
  authLoading.value = true
  authMessage.value = ''

  try {
    const user = await login(loginForm)
    persistUser(user)
    resetCustomerDraft()
    await refreshPortalData()
    return user
  } catch (error) {
    authMessage.value = error.message || '登录失败。'
    return null
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
    await refreshPortalData()
    return user
  } catch (error) {
    authMessage.value = error.message || '注册失败。'
    return null
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
    await refreshPortalData()
  } catch (error) {
    submitMessage.value = error.message || '订单提交失败。'
  }
}

export function useNomNomApp() {
  // This composable is intentionally stateful at module scope so every route
  // reads and writes the same app state instead of creating isolated copies.
  onMounted(async () => {
    if (currentUser.value) {
      await loadPortalData()
    }
  })

  return {
    activeCategory,
    addToCart,
    authLoading,
    authMessage,
    authMode,
    cart,
    cartCount,
    cartTotal,
    categories,
    clearPortalState,
    currentUser,
    customerDraft,
    customerModules,
    customerStats,
    displayName,
    filteredMenuItems,
    formatStatus,
    handleLogin,
    handlePreview,
    handleRegister,
    handleSubmit,
    isCustomerPortal,
    isMerchantPortal,
    keyword,
    loadPortalData,
    loginForm,
    logout,
    menuItems,
    merchantModules,
    merchantStats,
    orderQueue,
    pageLoading,
    portalMessage,
    portalOptions,
    preview,
    registerForm,
    submitMessage,
    updateQuantity,
  }
}
