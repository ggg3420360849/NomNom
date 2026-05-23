<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import PortalHeader from '../components/PortalHeader.vue'
import { useNomNomApp } from '../utils/useNomNomApp'
import { merchantApi } from '../services/api'

const router = useRouter()
const app = useNomNomApp()

const currentMenu = ref(localStorage.getItem('adminTab') || 'B01')
const subTab = ref('list')
const b03SubTab = ref('orders')

function switchTab(tab) {
  currentMenu.value = tab
  localStorage.setItem('adminTab', tab)
  if (tab === 'B01') subTab.value = 'list'
  if (tab === 'B02') subTab.value = 'layout'
  if (tab === 'B03') { b03SubTab.value = 'orders'; fetchOrders(); }
  if (tab === 'B04') { subTab.value = 'reviews'; fetchReviews(); }
}

const toast = ref('')
let toastTimer = null
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 3000)
}

function getErrorMessage(err, fallback = '操作失败') {
  return err?.response?.data?.message || err?.message || fallback
}

const TABLE_STATUS = {
  0: { label: '空闲', class: 'status-idle', color: '#689f38' },
  1: { label: '已入座', class: 'status-seated', color: '#f57c00' },
  2: { label: '就餐中', class: 'status-dining', color: '#fbc02d' },
  3: { label: '已下单', class: 'status-ordered', color: '#e64a19' },
  4: { label: '待结账', class: 'status-pending', color: '#d32f2f' },
  5: { label: '预订中', class: 'status-booked', color: '#7b1fa2' },
  6: { label: '维修中', class: 'status-repair', color: '#795548' }
}

const selectedArea = ref('全部')
const tableList = ref([])
const tableLoading = ref(false)
const tableAreas = ref([])

function formatTable(raw) {
  return {
    id: raw.id, number: raw.number, name: (raw.personNumber || 2) + '人桌',
    seats: raw.personNumber || 2, area: raw.area || '大厅', status: raw.status || 0,
    amount: raw.amount || 0, minSpend: raw.minSpend || 0,
  }
}

async function fetchTables() {
  tableLoading.value = true
  try {
    const res = await merchantApi.getTables()
    tableList.value = (res.data?.data || res.data || []).map(formatTable)
    tableAreas.value = [...new Set(tableList.value.map(t => t.area))]
  } catch (err) {} finally { tableLoading.value = false }
}

const filteredTables = computed(() => {
  if (selectedArea.value === '全部') return tableList.value
  return tableList.value.filter(t => t.area === selectedArea.value)
})

// === 菜品管理 ===
const showDishModal = ref(false)
const editingId = ref(null)
const dishList = ref([])
const dishForm = reactive({
  name: '', type_id: null, price: 0, memberPrice: 0, stock: 99, status: 1, cover_url: '', isRecommend: 0,
  recommend_ids: []
})

function resetDishForm() {
  Object.assign(dishForm, {
    name: '', type_id: null, price: 0, memberPrice: 0, stock: 99, status: 1, cover_url: '', isRecommend: 0,
    recommend_ids: []
  })
}

function openAddModal() {
  editingId.value = null;
  resetDishForm();
  showDishModal.value = true;
}

function openEditModal(dish) {
  editingId.value = dish.id

  // ★ 核心修复点 1：将后端返回的 JSON 字符串解析成 Vue 能看懂的真正的数组 ★
  let parsedRecs = []
  try {
    if (typeof dish.recommendIds === 'string') {
      parsedRecs = JSON.parse(dish.recommendIds)
    } else if (Array.isArray(dish.recommendIds)) {
      parsedRecs = dish.recommendIds
    }
  } catch (e) {
    parsedRecs = []
  }

  Object.assign(dishForm, {
    name: dish.name || '', type_id: dish.typeId || null, price: dish.price || 0, memberPrice: dish.memberPrice || 0,
    stock: dish.stock || 99, status: dish.status ?? 1, cover_url: dish.coverUrl || '', isRecommend: dish.isRecommend || 0,
    recommend_ids: parsedRecs // 绑定解析出来的真实数组，复选框就会自动亮起！
  })
  showDishModal.value = true
}

// === 分类配置与透视逻辑 ===
const categoryList = ref([])
const categoryForm = reactive({ name: '', iconUri: '' })
const editingCategoryId = ref(null)

const PRESET_CATEGORIES = ['主食', '招牌荤菜', '家常素菜', '荤素搭配', '海鲜水产', '特色汤羹', '风味小吃', '精美甜点', '果汁饮品', '酒水饮料', '新鲜水果', '凉拌凉菜']
const availablePresets = computed(() => {
  return PRESET_CATEGORIES.filter(preset => !categoryList.value.some(cat => cat.name === preset))
})

const expandedCategoryIds = ref(new Set())
function toggleCategoryExpand(id) {
  const newSet = new Set(expandedCategoryIds.value)
  if (newSet.has(id)) newSet.delete(id)
  else newSet.add(id)
  expandedCategoryIds.value = newSet
}

function getDishesByCat(catId) {
  return dishList.value.filter(d => d.typeId === catId || d.categoryId === catId)
}

async function fetchCategories() { try { const res = await merchantApi.getCategories(); categoryList.value = res.data?.data || res.data || [] } catch (err) {} }

async function saveCategory() {
  if (!categoryForm.name.trim()) return showToast('分类名称必填')
  try {
    if (editingCategoryId.value) await merchantApi.updateCategory(editingCategoryId.value, categoryForm)
    else await merchantApi.addCategory(categoryForm)
    showToast('分类维护成功'); editingCategoryId.value = null; categoryForm.name = ''; await fetchCategories()
  } catch (err) { showToast('操作失败') }
}

async function quickAddCategory(presetName) {
  try {
    await merchantApi.addCategory({ name: presetName, iconUri: '' });
    showToast(`分类「${presetName}」已写入数据库！`);
    await fetchCategories();
  } catch (err) {
    showToast('快捷添加失败');
  }
}

function editCategory(cat) { editingCategoryId.value = cat.id; categoryForm.name = cat.name; categoryForm.iconUri = cat.iconUri; }
async function removeCategory(cat) { try { await merchantApi.deleteCategory(cat.id); await fetchCategories() } catch { showToast('删除失败') } }

async function fetchDishes() { try { const res = await merchantApi.getDishes(); dishList.value = res.data?.data || res.data || [] } catch {} }
async function toggleDishStatus(dish) { try { await merchantApi.updateDishStatus(dish.id, dish.status === 1 ? 0 : 1); await fetchDishes(); } catch { showToast('状态修改失败') } }
async function removeDish(dish) { try { await merchantApi.deleteDish(dish.id); await fetchDishes() } catch { showToast('删除失败') } }

const handleImageUpload = async (event) => {
  const file = event.target.files[0]; if (!file) return;
  const formData = new FormData(); formData.append('file', file);
  try {
    const res = await merchantApi.uploadFile(formData)
    dishForm.cover_url = res.data.data || res.data
  } catch (err) {
    showToast(getErrorMessage(err, '图片上传失败'))
  }
};

const saveDish = async () => {
  if (!dishForm.name.trim()) return showToast('菜品名称不能为空')

  try {
    // ★ 核心修复点 2：在保存给后端前，把数组转换成字符串 ★
    const payload = { ...dishForm }
    payload.price = Number(dishForm.price || 0)
    payload.memberPrice = Number(dishForm.memberPrice || 0)
    payload.stock = Number(dishForm.stock || 0)
    payload.recommendIds = JSON.stringify(dishForm.recommend_ids)

    if (editingId.value) await merchantApi.updateDish(editingId.value, payload)
    else await merchantApi.addDish(payload)

    showToast('菜品保存成功'); showDishModal.value = false; resetDishForm(); await fetchDishes()
  } catch (err) { showToast(getErrorMessage(err, '保存失败')) }
};

const handleTableClick = async (table) => {
  let nextStatus = table.status === 0 ? 1 : 0; if (table.status === 4) nextStatus = 0;
  try { await merchantApi.updateTableStatus(table.id, nextStatus); await fetchTables(); } catch { }
};

const orderList = ref([]); const auditList = ref([]); const reconciliationList = ref([]); const reviewList = ref([]); const statsList = ref([]);
const orderStatusFilter = ref(null); const ORDER_STATUS = { 1: '待支付', 2: '已支付', 3: '制作中', 4: '已完成', 5: '已取消' };

async function fetchOrders() { try { const res = await merchantApi.getOrders(orderStatusFilter.value); orderList.value = res.data?.data || res.data || [] } catch { } }
async function fetchAudits() { try { const res = await merchantApi.getAudits(); auditList.value = res.data?.data || [] } catch { } }
async function fetchReconciliation() { try { const res = await merchantApi.getFinanceReconciliation(); reconciliationList.value = res.data?.data || [] } catch { } }
async function fetchReviews() { try { const res = await merchantApi.getReviews(); reviewList.value = res.data?.data || [] } catch { } }
async function fetchStats() { try { const res = await merchantApi.getStats(); statsList.value = res.data?.data || [] } catch { } }

async function changeOrderStatus(order, nextStatus) { try { await merchantApi.updateOrderStatus(order.id, nextStatus); await fetchOrders(); } catch {} }
async function handleAuditAction(id, isPass) { try { await merchantApi.auditRegistrationOrRefund(id, isPass); showToast(isPass ? '审批已通过' : '申请已驳回'); await fetchAudits() } catch {} }

const tableForm = reactive({ number: '', personNumber: 2, area: '大厅', minSpend: 0 })
const showTableForm = ref(false); const editingTableId = ref(null);
function openAddTable() { editingTableId.value = null; Object.assign(tableForm, { number: '', personNumber: 2, area: '大厅', minSpend: 0 }); showTableForm.value = true; }
function openEditTable(table) { editingTableId.value = table.id; Object.assign(tableForm, { number: table.number, personNumber: table.seats, area: table.area, minSpend: table.minSpend || 0 }); showTableForm.value = true }
async function saveTable() { try { if (editingTableId.value) await merchantApi.updateTable(editingTableId.value, tableForm); else await merchantApi.addTable(tableForm); showTableForm.value = false; await fetchTables(); } catch { } }
async function removeTable(table) { try { await merchantApi.deleteTable(table.id); await fetchTables(); } catch { } }

const activeReview = ref(null); const inputReplyText = ref(''); const showReplyModal = ref(false);
function openReplyDialog(review) { activeReview.value = review; inputReplyText.value = review.reply || ''; showReplyModal.value = true }
async function submitReviewReply() {
  if (!inputReplyText.value.trim()) return showToast('回复不可为空')
  try { await merchantApi.replyToReview(activeReview.value.id, inputReplyText.value.trim()); showToast('回复成功'); showReplyModal.value = false; await fetchReviews(); } catch {}
}
async function deleteReviewItem(id) { try { await merchantApi.deleteReview(id); showToast('评价已删除'); await fetchReviews(); } catch {} }

async function exitPortal() { app.logout(); await router.push('/login'); }

onMounted(() => {
  app.loadPortalData(); fetchTables(); fetchDishes(); fetchCategories();
  fetchAudits(); fetchReconciliation(); fetchReviews(); fetchStats();
})
</script>

<template>
  <div class="admin-layout">
    <aside class="main-sidebar">
      <div class="brand">
        <div class="logo-box">NN</div>
        <div class="brand-text">
          <h1>NomNom 智慧餐厅</h1>
        </div>
      </div>

      <nav class="nav-links">
        <div class="nav-item" :class="{ active: currentMenu === 'B01' }" @click="switchTab('B01')"><i class="icon">🍱</i> 菜品管理 (B01)</div>
        <div class="nav-item" :class="{ active: currentMenu === 'B02' }" @click="switchTab('B02')"><i class="icon">🪑</i> 桌台管理 (B02)</div>
        <div class="nav-item" :class="{ active: currentMenu === 'B03' }" @click="switchTab('B03')"><i class="icon">📊</i> 订单与审批 (B03)</div>
        <div class="nav-item" :class="{ active: currentMenu === 'B04' }" @click="switchTab('B04')"><i class="icon">⭐</i> 评价与统计 (B04)</div>
      </nav>
    </aside>

    <main class="content-wrapper">
      <PortalHeader title="商家工作台" :display-name="app.displayName.value" role-label="店长" @logout="exitPortal" />

      <div v-if="toast" class="toast-bar">{{ toast }}</div>
      <div class="page-container">

        <div v-if="currentMenu === 'B01'" class="module-box">
          <div class="tab-header">
            <div class="tab-triggers">
              <span :class="{ active: subTab === 'list' }" @click="subTab = 'list'">菜品库管理</span>
              <span :class="{ active: subTab === 'category' }" @click="subTab = 'category'">分类与品类透视</span>
            </div>
            <button v-if="subTab === 'list'" class="btn-primary" @click="openAddModal">+ 新增菜品/酒水</button>
          </div>

          <table v-if="subTab === 'list'" class="biz-table">
            <thead>
            <tr><th width="40"><input type="checkbox" /></th><th>菜品信息</th><th>分类</th><th>售价/会员价</th><th>属性标</th><th>状态</th><th>操作</th></tr>
            </thead>
            <tbody>
            <tr v-for="item in dishList" :key="item.id">
              <td><input type="checkbox" /></td>
              <td>
                <div class="dish-info">
                  <img :src="item.coverUrl || item.cover_url || '/placeholder.png'" class="dish-img" />
                  <div><p class="dish-name">{{ item.name }}</p></div>
                </div>
              </td>
              <td>{{ item.typeName || '-' }}</td>
              <td>
                <p class="sell-price">￥{{ Number(item.price || 0).toFixed(2) }}</p>
                <p class="member-price">会员: ￥{{ Number(item.memberPrice || 0).toFixed(2) }}</p>
              </td>
              <td>
                <span v-if="item.isRecommend === 1" class="chip chip-orange">👍店长推荐</span>
                <span v-else class="chip chip-gray">普通</span>
              </td>
              <td><span :class="['chip', item.status === 1 ? 'chip-green' : 'chip-gray']">{{ item.status === 1 ? '在售' : '下架' }}</span></td>
              <td>
                <button class="btn-link" @click="openEditModal(item)">编辑</button>
                <button class="btn-link" @click="toggleDishStatus(item)">{{ item.status === 1 ? '下架' : '上架' }}</button>
                <button class="btn-link danger" @click="removeDish(item)">删除</button>
              </td>
            </tr>
            </tbody>
          </table>

          <div v-if="subTab === 'category'" style="margin-top:20px">

            <div class="preset-category-panel" v-if="availablePresets.length > 0">
              <p class="preset-title">快捷分类</p>
              <div class="preset-tags">
                <span
                    v-for="preset in availablePresets"
                    :key="preset"
                    class="chip chip-gray preset-chip"
                    @click="quickAddCategory(preset)"
                >
                  + {{ preset }}
                </span>
              </div>
            </div>

            <div style="display:flex;gap:12px;margin-bottom:24px;align-items:center">
              <input v-model="categoryForm.name" placeholder="分类名称" style="padding:8px 12px;border:1px solid #e0d4c3;border-radius:6px;width:200px" />
              <button class="btn-primary" @click="saveCategory" style="padding:8px 20px">{{ editingCategoryId ? '保存修改' : '确认新增分类' }}</button>
              <button v-if="editingCategoryId" class="btn-cancel" @click="editingCategoryId=null; categoryForm.name=''">放弃编辑</button>
            </div>

            <div class="category-accordion">
              <div v-for="(cat, index) in categoryList" :key="cat.id" class="cat-panel">
                <div class="cat-header" @click="toggleCategoryExpand(cat.id)">
                  <div class="cat-title">
                    <span class="cat-index">{{ index + 1 }}</span>
                    <span class="cat-name">{{ cat.name }}</span>
                    <span class="chip chip-gray" style="margin-left: 10px;">包含 {{ getDishesByCat(cat.id).length }} 款菜品</span>
                  </div>
                  <div class="cat-actions">
                    <button class="btn-link" @click.stop="editCategory(cat)">重命名</button>
                    <button class="btn-link danger" @click.stop="removeCategory(cat)">整类移除</button>
                    <span class="expand-wrapper" :class="{ 'is-expanded': expandedCategoryIds.has(cat.id) }">
                      {{ expandedCategoryIds.has(cat.id) ? '收起列表' : '展开菜品' }}
                      <i class="expand-icon">▼</i>
                    </span>
                  </div>
                </div>

                <div class="cat-body" v-show="expandedCategoryIds.has(cat.id)">
                  <table v-if="getDishesByCat(cat.id).length > 0" class="mini-dish-table">
                    <thead><tr><th>缩略图</th><th>菜品名称</th><th>售价</th><th>库存剩余</th><th>当前状态</th></tr></thead>
                    <tbody>
                    <tr v-for="dish in getDishesByCat(cat.id)" :key="dish.id">
                      <td><img :src="dish.coverUrl || dish.cover_url || '/placeholder.png'" class="mini-img" /></td>
                      <td style="font-weight: bold;">{{ dish.name }}</td>
                      <td style="color: #d84315;">￥{{ Number(dish.price || 0).toFixed(2) }}</td>
                      <td :style="{ color: dish.stock > 0 ? '#558b2f' : '#d32f2f' }">{{ dish.stock > 0 ? dish.stock + ' 份' : '❌ 已售罄' }}</td>
                      <td>
                          <span :class="['chip', dish.status === 1 ? 'chip-green' : 'chip-gray']">
                            {{ dish.status === 1 ? '✅ 已上架' : '🚫 已下架' }}
                          </span>
                      </td>
                    </tr>
                    </tbody>
                  </table>
                  <div v-else class="empty-hint">无菜品</div>
                </div>
              </div>

              <div v-if="categoryList.length === 0" class="empty-hint" style="padding: 40px;">
                无分类
              </div>
            </div>
          </div>
        </div>

        <div v-if="currentMenu === 'B02'" class="module-box">
          <div class="tab-header">
            <div class="tab-triggers"><span class="active">实时桌态布局</span></div>
            <button class="btn-primary" @click="openAddTable" style="padding:6px 16px">+ 新增桌台</button>
          </div>
          <div class="table-tool-bar">
            <div class="area-filters"><button v-for="a in ['全部', ...tableAreas]" :key="a" :class="{ active: selectedArea === a }" @click="selectedArea = a">{{ a }}</button></div>
            <div class="status-legends"><span v-for="(v, k) in TABLE_STATUS" :key="k"><i :style="{ backgroundColor: v.color }"></i> {{ v.label }}</span></div>
          </div>
          <div class="visual-grid">
            <div v-for="table in filteredTables" :key="table.id" class="table-card" :class="TABLE_STATUS[table.status].class" @click="handleTableClick(table)">
              <div class="card-top"><span class="t-no">{{ table.number }}</span><span class="t-area">{{ table.area }}</span></div>
              <div class="card-mid">
                <p class="t-seats">{{ table.seats }} 人桌</p>
                <p v-if="table.amount > 0" class="t-amount">￥{{ table.amount.toFixed(2) }}</p>
                <p v-else class="t-status-text">{{ TABLE_STATUS[table.status].label }}</p>
              </div>
              <div style="display:flex;gap:4px;margin-top:8px;justify-content:center">
                <button class="btn-link" @click.stop="openEditTable(table)" style="font-size:12px">编辑</button>
                <button class="btn-link danger" @click.stop="removeTable(table)" style="font-size:12px">删除</button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="currentMenu === 'B03'" class="module-box">
          <div class="tab-header">
            <div class="tab-triggers">
              <span :class="{ active: b03SubTab === 'orders' }" @click="b03SubTab = 'orders'">流水及业务流转</span>
              <span :class="{ active: b03SubTab === 'audits' }" @click="b03SubTab = 'audits'">充值/退款审核</span>
              <span :class="{ active: b03SubTab === 'reconciliation' }" @click="b03SubTab = 'reconciliation'">系统自动核账</span>
            </div>
          </div>
          <div v-if="b03SubTab === 'orders'">
            <table class="biz-table">
              <thead><tr><th>订单编号</th><th>桌台</th><th>金额</th><th>状态</th><th>订单时间</th><th>操作</th></tr></thead>
              <tbody>
              <tr v-if="orderList.length===0"><td colspan="6" style="text-align:center; color:#999;">暂无订单</td></tr>
              <tr v-for="o in orderList" :key="o.id"><td>{{ o.code }}</td><td>{{ o.tableNumber || '-' }}</td><td>￥{{ Number(o.totalPrice||0).toFixed(2) }}</td><td>{{ ORDER_STATUS[o.status]||o.status }}</td><td>{{ o.createTime }}</td>
                <td>
                  <select @change="changeOrderStatus(o,Number($event.target.value))" style="padding:4px 8px;border-radius:4px">
                    <option value="">更新推进</option><option v-for="(label,val) in ORDER_STATUS" :key="val" :value="val" :disabled="val===o.status">{{ label }}</option>
                  </select>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
          <div v-if="b03SubTab === 'audits'">
            <table class="biz-table">
              <thead><tr><th>事项编号</th><th>类别</th><th>申请详情</th><th>当前进度</th><th>裁决</th></tr></thead>
              <tbody>
              <tr v-if="auditList.length===0"><td colspan="5" style="text-align:center; color:#999;">暂无审核</td></tr>
              <tr v-for="audit in auditList" :key="audit.id"><td>#{{ audit.id }}</td><td>{{ audit.type==='CHARGE'?'钱包充值':'订单退款' }}</td><td>{{ audit.desc }}</td><td>{{ audit.status }}</td>
                <td>
                  <div v-if="audit.status==='PENDING'" style="display:flex; gap:6px;">
                    <button class="btn-primary" @click="handleAuditAction(audit.id, true)" style="padding:4px 10px; font-size:12px;">通过</button>
                    <button class="btn-cancel" @click="handleAuditAction(audit.id, false)" style="padding:4px 10px; font-size:12px;">驳回</button>
                  </div>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
          <div v-if="b03SubTab === 'reconciliation'">
            <table class="biz-table">
              <thead><tr><th>审计凭证号</th><th>应用端消费</th><th>底层流水</th><th>结果</th></tr></thead>
              <tbody>
              <tr v-if="reconciliationList.length===0"><td colspan="4" style="text-align:center; color:#999;">暂无对账</td></tr>
              <tr v-for="rec in reconciliationList" :key="rec.id"><td>{{ rec.id }}</td><td>￥{{ rec.orderAmt.toFixed(2) }}</td><td>￥{{ rec.flowAmt.toFixed(2) }}</td><td>平账</td></tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-if="currentMenu === 'B04'" class="module-box">
          <div class="tab-header">
            <div class="tab-triggers"><span :class="{ active: subTab === 'reviews' }" @click="subTab = 'reviews'">客评交互管理</span><span :class="{ active: subTab === 'stats' }" @click="subTab = 'stats'">决策分析统计</span></div>
          </div>
          <div v-if="subTab === 'reviews'">
            <table class="biz-table">
              <thead><tr><th>消费者</th><th>受评菜品</th><th>评分</th><th>评语内容</th><th>操作</th></tr></thead>
              <tbody>
              <tr v-if="reviewList.length===0"><td colspan="5" style="text-align:center; color:#999;">暂无评价</td></tr>
              <tr v-for="r in reviewList" :key="r.id"><td>{{ r.user }}</td><td>{{ r.dishName }}</td><td style="color: #f57c00;">{{ '★'.repeat(r.rating) }}</td>
                <td>{{ r.content }} <p v-if="r.reply" style="color:#f57c00; font-size:12px;">↩ 回函：{{ r.reply }}</p></td>
                <td><button class="btn-link" @click="openReplyDialog(r)">回复</button><button class="btn-link danger" @click="deleteReviewItem(r.id)">删除</button></td>
              </tr>
              </tbody>
            </table>
          </div>
          <div v-if="subTab === 'stats'">
            <table class="biz-table">
              <thead><tr><th>名次</th><th>菜品名称</th><th>累积销量</th><th>账面营收</th></tr></thead>
              <tbody>
              <tr v-if="statsList.length===0"><td colspan="4" style="text-align:center; color:#999;">暂无统计数据</td></tr>
              <tr v-for="s in statsList" :key="s.rank"><td>{{ s.rank }}</td><td>{{ s.name }}</td><td>{{ s.sales }}</td><td class="sell-price">￥{{ s.revenue.toFixed(2) }}</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </main>

    <div v-if="showReplyModal" class="biz-modal">
      <div class="modal-content">
        <div class="modal-header"><h3>回复消费者评价</h3><button @click="showReplyModal = false">×</button></div>
        <div class="modal-body"><textarea v-model="inputReplyText" placeholder="回复内容" style="width:100%; height:100px; padding:10px; border:1px solid #e0d4c3; border-radius:6px; resize:none;"></textarea></div>
        <div class="modal-footer"><button class="btn-cancel" @click="showReplyModal = false">放弃</button><button class="btn-primary" @click="submitReviewReply">发布</button></div>
      </div>
    </div>

    <div v-if="showDishModal" class="biz-modal">
      <div class="modal-content">
        <div class="modal-header"><h3>{{ editingId ? '编辑菜品' : '新增菜品' }}</h3><button @click="showDishModal = false">×</button></div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group span-2"><label>名称</label><input type="text" v-model="dishForm.name" /></div>
            <div class="form-group"><label>所属分类</label><select v-model.number="dishForm.type_id" style="padding:8px 12px;border:1px solid #e0d4c3;border-radius:6px"><option :value="null">选择分类</option><option v-for="cat in categoryList" :key="cat.id" :value="cat.id">{{ cat.name }}</option></select></div>
            <div class="form-group"><label>基础售价(元)</label><input type="number" v-model="dishForm.price" /></div>

            <div class="form-group span-2">
              <label style="display:flex; align-items:center; gap:8px; cursor: pointer; color: #4e342e; background: #fff8eb; padding: 12px; border-radius: 6px; border: 1px dashed #f57c00;">
                <input type="checkbox" v-model="dishForm.isRecommend" :true-value="1" :false-value="0" style="width:18px; height:18px;" /> 店长推荐
              </label>
            </div>

            <div class="form-group span-2">
              <label style="color: #4e342e; font-weight: bold; margin-bottom: 4px;">推荐搭配</label>

              <div class="recommend-select-box">
                <label
                    v-for="dish in dishList.filter(d => d.id !== editingId)"
                    :key="dish.id"
                    class="recommend-checkbox"
                >
                  <input
                      type="checkbox"
                      :value="dish.id"
                      v-model="dishForm.recommend_ids"
                  />
                  <img :src="dish.coverUrl || dish.cover_url || '/placeholder.png'" class="rc-img" />
                  <div class="rc-info">
                    <span class="rc-name">{{ dish.name }}</span>
                    <span class="rc-price">￥{{ Number(dish.price || 0).toFixed(2) }}</span>
                  </div>
                </label>
                <div v-if="dishList.filter(d => d.id !== editingId).length === 0" style="padding: 20px; color: #999; text-align: center; width: 100%;">
                  暂无可选菜品
                </div>
              </div>
            </div>

            <div class="form-group span-2"><label>上传主图</label><input type="file" @change="handleImageUpload" accept="image/*" />
              <img v-if="dishForm.cover_url" :src="dishForm.cover_url" class="upload-preview" /></div>
          </div>
        </div>
        <div class="modal-footer"><button class="btn-cancel" @click="showDishModal = false">取消</button><button class="btn-primary" @click="saveDish">保存发布</button></div>
      </div>
    </div>

    <div v-if="showTableForm" class="biz-modal">
      <div class="modal-content">
        <div class="modal-header"><h3>{{ editingTableId ? '编辑桌台' : '新增桌台' }}</h3><button @click="showTableForm = false">×</button></div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group"><label>桌号</label><input v-model="tableForm.number" /></div>
            <div class="form-group"><label>人数</label><input type="number" v-model.number="tableForm.personNumber" /></div>
            <div class="form-group"><label>区域</label><input v-model="tableForm.area" /></div>
          </div>
        </div>
        <div class="modal-footer"><button class="btn-cancel" @click="showTableForm = false">取消</button><button class="btn-primary" @click="saveTable">保存</button></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-layout { display: flex; height: 100vh; width: 100vw; overflow: hidden; background: #fcfaf5; font-family: 'PingFang SC', sans-serif; }
.main-sidebar { width: 240px; background: #fff8eb; border-right: 1px solid #f0e6d2; color: #5d4037; display: flex; flex-direction: column; }
.brand { padding: 32px 24px; display: flex; align-items: center; gap: 12px; }
.logo-box { width: 40px; height: 40px; background: #f57c00; border-radius: 8px; color: white; display: grid; place-items: center; font-weight: bold; font-size: 20px; }
.brand-text h1 { color: #4e342e; font-size: 16px; margin: 0; }
.brand-text span { font-size: 12px; opacity: 0.6; color: #795548;}
.nav-links { flex: 1; padding: 20px 0; }
.nav-item { padding: 14px 24px; cursor: pointer; transition: 0.3s; display: flex; align-items: center; gap: 12px; font-weight: 500;}
.nav-item:hover { color: #f57c00; background: rgba(245, 124, 0, 0.08); }
.nav-item.active { background: #f57c00; color: white; box-shadow: 0 4px 10px rgba(245, 124, 0, 0.2);}
.toast-bar { background: #323232; color: #fff; padding: 10px 24px; text-align: center; font-size: 14px; position: fixed; top: 10px; left: 50%; transform: translateX(-50%); border-radius: 8px; z-index: 1000; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.content-wrapper { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.page-container { padding: 24px; overflow-y: auto; flex: 1; }
.module-box { background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 16px rgba(121, 85, 72, 0.04); border: 1px solid #f8f4ec;}
.tab-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f0e6d2; margin-bottom: 20px; }
.tab-triggers { display: flex; gap: 32px; }
.tab-triggers span { padding: 12px 0; cursor: pointer; position: relative; font-weight: 500; color: #8d6e63; }
.tab-triggers span.active { color: #f57c00; }
.tab-triggers span.active::after { content: ''; position: absolute; bottom: -1px; left: 0; width: 100%; height: 2px; background: #f57c00; }
.table-tool-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.area-filters { display: flex; gap: 8px; }
.area-filters button { border: 1px solid #e0d4c3; padding: 5px 16px; border-radius: 6px; background: white; color:#5d4037; cursor: pointer; transition: 0.2s;}
.area-filters button.active { background: #f57c00; color: white; border-color: #f57c00; box-shadow: 0 2px 6px rgba(245, 124, 0, 0.2);}
.status-legends { display: flex; gap: 12px; font-size: 12px; color: #5d4037;}
.status-legends i { display: inline-block; width: 10px; height: 10px; border-radius: 2px; }
.biz-table { width: 100%; border-collapse: collapse; }
.biz-table th { background: #fbf6ec; padding: 12px; text-align: left; font-size: 14px; border-bottom: 1px solid #f0e6d2; color: #5d4037; }
.biz-table td { padding: 16px 12px; border-bottom: 1px solid #f9f5f0; font-size: 14px; color: #4e342e;}
.dish-info { display: flex; gap: 12px; align-items: center; }
.dish-img { width: 48px; height: 48px; border-radius: 6px; object-fit: cover; }
.dish-name { font-weight: bold; margin: 0; }
.sell-price { color: #d84315; font-weight: bold; margin: 0; font-size: 16px;}
.member-price { font-size: 12px; color: #8d6e63; margin: 0; }
.chip { padding: 4px 10px; border-radius: 12px; font-size: 12px; display: inline-block;}
.chip-green { background: #f1f8e9; color: #558b2f; border: 1px solid #c5e1a5; }
.chip-gray { background: #f5f5f5; color: #9e9e9e; border: 1px solid #eeeeee; }
.chip-orange { background: #fff3e0; color: #e65100; border: 1px solid #ffcc80; }
.visual-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 20px; }
.table-card { border-radius: 10px; padding: 16px; border-top: 5px solid #e0d4c3; background: #fffcf8; cursor: pointer; transition: 0.3s; position: relative; display: flex; flex-direction: column; min-height: 120px; box-shadow: 0 2px 8px rgba(121, 85, 72, 0.05);}
.table-card:hover { transform: translateY(-4px); box-shadow: 0 10px 20px rgba(121, 85, 72, 0.1); }
.status-idle { background: #f1f8e9; border-top-color: #7cb342; }
.status-seated { background: #fff3e0; border-top-color: #ff9800; }
.status-dining { background: #fffde7; border-top-color: #fbc02d; }
.status-ordered { background: #fbe9e7; border-top-color: #ff5722; }
.status-pending { background: #ffebee; border-top-color: #e53935; }
.status-booked { background: #f3e5f5; border-top-color: #8e24aa; }
.card-top { display: flex; justify-content: space-between; margin-bottom: 12px; color: #4e342e;}
.t-no { font-size: 20px; font-weight: bold; }
.t-area { font-size: 12px; opacity: 0.7; }
.card-mid { flex: 1; text-align: center; }
.t-seats { color: #8d6e63; font-size: 14px; }
.t-amount { color: #d32f2f; font-size: 18px; font-weight: bold; }

/* === 分类透视手风琴样式 === */
.preset-category-panel { background: #fffdf9; border: 1px dashed #f57c00; border-radius: 8px; padding: 16px; margin-bottom: 20px; }
.preset-title { font-size: 14px; font-weight: bold; color: #d84315; margin-bottom: 12px; }
.preset-tags { display: flex; gap: 10px; flex-wrap: wrap; }
.preset-chip { cursor: pointer; transition: all 0.2s; padding: 6px 12px; font-size: 13px; border: 1px solid #e0d4c3; background: #fff; }
.preset-chip:hover { background: #f57c00; color: white; border-color: #f57c00; transform: translateY(-2px); box-shadow: 0 4px 8px rgba(245, 124, 0, 0.2); }

.category-accordion { display: flex; flex-direction: column; gap: 12px; }
.cat-panel { border: 1px solid #f0e6d2; border-radius: 8px; background: #fff; overflow: hidden; box-shadow: 0 2px 6px rgba(121, 85, 72, 0.04); }
.cat-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; background: #fbf6ec; cursor: pointer; transition: 0.2s; }
.cat-header:hover { background: #fff3e0; }
.cat-title { display: flex; align-items: center; gap: 12px; }
.cat-index { display: inline-block; width: 24px; height: 24px; background: #e0d4c3; color: white; text-align: center; line-height: 24px; border-radius: 4px; font-weight: bold; }
.cat-name { font-size: 16px; font-weight: bold; color: #4e342e; }
.cat-actions { display: flex; align-items: center; gap: 12px; }

.expand-wrapper { font-size: 13px; color: #8d6e63; display: flex; align-items: center; gap: 6px; transition: 0.2s; margin-left: 8px; }
.expand-wrapper.is-expanded { color: #f57c00; font-weight: bold; }
.expand-icon { transition: transform 0.3s; display: inline-block; font-size: 11px; }
.expand-wrapper.is-expanded .expand-icon { transform: rotate(180deg); }

.cat-body { padding: 16px; border-top: 1px solid #f0e6d2; background: white; }
.mini-dish-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.mini-dish-table th { text-align: left; padding: 8px; color: #a1887f; border-bottom: 1px solid #f9f5f0; }
.mini-dish-table td { padding: 8px; border-bottom: 1px solid #f9f5f0; }
.mini-img { width: 36px; height: 36px; border-radius: 4px; object-fit: cover; }
.empty-hint { text-align: center; padding: 20px; color: #a1887f; font-size: 13px; }

/* 模态框与推荐卡片 */
.biz-modal { position: fixed; inset: 0; background: rgba(62, 39, 35, 0.6); display: grid; place-items: center; z-index: 999; }
.modal-content { background: #fffcf8; width: 560px; max-height: 90vh; border-radius: 12px; overflow-y: auto; box-shadow: 0 10px 30px rgba(0,0,0,0.1);}
.modal-header { padding: 16px 24px; border-bottom: 1px solid #f0e6d2; display: flex; justify-content: space-between; align-items: center; color: #4e342e; position: sticky; top: 0; background: #fffcf8; z-index: 10;}
.modal-body { padding: 24px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.form-group { display: flex; flex-direction: column; gap: 8px; }
.form-group.span-2 { grid-column: span 2; }
.form-group label { font-size: 14px; font-weight: 500; color: #5d4037;}
.form-group input[type="text"], .form-group input[type="number"], .form-group select { padding: 8px 12px; border: 1px solid #e0d4c3; border-radius: 6px; background: white; color:#4e342e;}
.form-group input:focus, .form-group select:focus { outline: none; border-color: #f57c00;}

.recommend-select-box { display: flex; flex-wrap: wrap; gap: 10px; max-height: 200px; overflow-y: auto; border: 1px solid #f0e6d2; border-radius: 6px; padding: 10px; background: white; }
.recommend-checkbox { display: flex; align-items: center; gap: 8px; padding: 8px; background: #f9f5f0; border: 1px solid #e0d4c3; border-radius: 6px; cursor: pointer; transition: 0.2s; width: calc(50% - 5px); box-sizing: border-box; }
.recommend-checkbox:hover { border-color: #f57c00; background: #fff8eb; }
.recommend-checkbox input { margin: 0; cursor: pointer; }
.rc-img { width: 40px; height: 40px; border-radius: 4px; object-fit: cover; }
.rc-info { display: flex; flex-direction: column; flex: 1; overflow: hidden; }
.rc-name { font-size: 13px; color: #4e342e; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-weight: bold; margin-bottom: 2px; }
.rc-price { font-size: 12px; color: #d84315; font-weight: bold; }

.modal-footer { padding: 16px 24px; border-top: 1px solid #f0e6d2; text-align: right; display: flex; justify-content: flex-end; gap: 12px; background: #fbf6ec; position: sticky; bottom: 0;}
.btn-primary { background: #f57c00; color: white; border: none; padding: 8px 24px; border-radius: 6px; cursor: pointer; font-weight: 500; transition: 0.2s;}
.btn-primary:hover { background: #e65100; }
.btn-cancel { background: white; border: 1px solid #e0d4c3; color: #5d4037; padding: 8px 24px; border-radius: 6px; cursor: pointer; transition: 0.2s;}
.btn-cancel:hover { background: #f5f5f5; }
.btn-link { background: none; border: none; color: #f57c00; cursor: pointer; margin-right: 8px; font-weight: 500;}
.btn-link.danger { color: #d32f2f; }
.upload-preview { width: 120px; height: 120px; object-fit: cover; border-radius: 6px; margin-top: 8px; border: 1px solid #e0d4c3; }
</style>
