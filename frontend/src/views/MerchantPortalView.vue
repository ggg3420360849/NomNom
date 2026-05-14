<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import PortalHeader from '../components/PortalHeader.vue'
import { useNomNomApp } from '../utils/useNomNomApp'
import { merchantApi } from '../services/api' // 引入真实 API

const router = useRouter()
const app = useNomNomApp()

// --- 顶级路由与子页签控制 ---
const currentMenu = ref(localStorage.getItem('adminTab') || 'B02')
const subTab = ref('layout')

function switchTab(tab) {
  currentMenu.value = tab
  localStorage.setItem('adminTab', tab)
  if (tab === 'B03') fetchOrders()
}

// --- Toast 提示（替代 alert） ---
const toast = ref('')
let toastTimer = null
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 2000)
}

// --- 模拟状态数据映射 (已调整为更柔和的颜色) ---
const TABLE_STATUS = {
  0: { label: '空闲', class: 'status-idle', color: '#689f38' },
  1: { label: '已入座', class: 'status-seated', color: '#f57c00' },
  2: { label: '就餐中', class: 'status-dining', color: '#fbc02d' },
  3: { label: '已下单', class: 'status-ordered', color: '#e64a19' },
  4: { label: '待结账', class: 'status-pending', color: '#d32f2f' },
  5: { label: '预订中', class: 'status-booked', color: '#7b1fa2' },
  6: { label: '维修中', class: 'status-repair', color: '#795548' }
}

// --- 桌台数据与交互 ---
const selectedArea = ref('全部')
const tableList = ref([])
const tableLoading = ref(false)
const tableError = ref('')
const tableAreas = ref([])

function formatTable(raw) {
  return {
    id: raw.id,
    number: raw.number,
    name: (raw.personNumber || 2) + '人桌',
    seats: raw.personNumber || 2,
    area: raw.area || '大厅',
    status: raw.status || 0,
    amount: raw.amount || 0,
    minSpend: raw.minSpend || 0,
  }
}

async function fetchTables() {
  tableLoading.value = true
  tableError.value = ''
  try {
    const res = await merchantApi.getTables()
    const rawList = res.data?.data || res.data || []
    const formatted = rawList.map(formatTable)
    tableList.value = formatted
    const areas = [...new Set(formatted.map(t => t.area))]
    tableAreas.value = areas
  } catch (err) {
    console.error(err)
    tableError.value = '桌台数据加载失败，请检查后端是否启动'
    tableList.value = []
  } finally {
    tableLoading.value = false
  }
}

const filteredTables = computed(() => {
  if (selectedArea.value === '全部') return tableList.value
  return tableList.value.filter(t => t.area === selectedArea.value)
})

// --- 菜品弹窗逻辑 ---
const showDishModal = ref(false)
const editingId = ref(null)
const dishList = ref([])
const dishLoading = ref(false)
const dishForm = reactive({
  name: '', type_id: null, price: 0, memberPrice: 0, stock: 99, status: 1, cover_url: '', specs: [], tastes: []
})

function resetDishForm() {
  Object.assign(dishForm, { name: '', type_id: null, price: 0, memberPrice: 0, stock: 99, status: 1, cover_url: '', specs: [], tastes: [] })
}

function openAddModal() {
  editingId.value = null
  resetDishForm()
  showDishModal.value = true
}

function openEditModal(dish) {
  editingId.value = dish.id
  dishForm.name = dish.name || ''
  dishForm.type_id = dish.typeId || null
  dishForm.price = dish.price || 0
  dishForm.memberPrice = dish.memberPrice || 0
  dishForm.stock = dish.stock || 99
  dishForm.status = dish.status ?? 1
  dishForm.cover_url = dish.coverUrl || ''
  dishForm.specs = []
  dishForm.tastes = []
  showDishModal.value = true
}

// --- 分类管理 ---
const categoryList = ref([])
const categoryForm = reactive({ name: '', iconUri: '' })
const editingCategoryId = ref(null)
const catKeyword = ref('')

async function fetchCategories() {
  try {
    const res = await merchantApi.getCategories(catKeyword.value || null)
    categoryList.value = res.data?.data || res.data || []
  } catch (err) {
    console.error(err)
  }
}

function onCatKeywordChange() {
  fetchCategories()
}

function openAddCategory() {
  editingCategoryId.value = null
  categoryForm.name = ''
  categoryForm.iconUri = ''
}

async function saveCategory() {
  try {
    if (editingCategoryId.value) {
      await merchantApi.updateCategory(editingCategoryId.value, categoryForm)
    } else {
      await merchantApi.addCategory(categoryForm)
    }
    showToast(editingCategoryId.value ? '更新成功' : '新增成功')
    editingCategoryId.value = null
    categoryForm.name = ''
    categoryForm.iconUri = ''
    await fetchCategories()
    app.loadPortalData(true)
  } catch (err) {
    console.error(err)
    showToast('保存失败')
  }
}

function editCategory(cat) {
  editingCategoryId.value = cat.id
  categoryForm.name = cat.name || ''
  categoryForm.iconUri = cat.iconUri || ''
}

async function removeCategory(cat) {
  try {
    await merchantApi.deleteCategory(cat.id)
    showToast('删除成功')
    await fetchCategories()
    app.loadPortalData(true)
  } catch (err) {
    console.error(err)
    showToast('删除失败')
  }
}

async function fetchDishes() {
  dishLoading.value = true
  try {
    const res = await merchantApi.getDishes()
    dishList.value = res.data?.data || res.data || []
  } catch (err) {
    console.error(err)
    dishList.value = []
  } finally {
    dishLoading.value = false
  }
}

async function toggleDishStatus(dish) {
  const next = dish.status === 1 ? 0 : 1
  try {
    await merchantApi.updateDishStatus(dish.id, next)
    showToast(next === 1 ? '已上架' : '已下架')
    await fetchDishes()
    app.loadPortalData()
  } catch (err) {
    console.error(err)
    showToast('操作失败')
  }
}

async function removeDish(dish) {
  try {
    await merchantApi.deleteDish(dish.id)
    showToast('删除成功')
    await fetchDishes()
    app.loadPortalData()
  } catch (err) {
    console.error(err)
    showToast('删除失败')
  }
}

// --- 真实接口逻辑 ---
const handleImageUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  const formData = new FormData();
  formData.append('file', file);

  try {
    const res = await merchantApi.uploadFile(formData);
    dishForm.cover_url = res.data.data || res.data;
    showToast('图片上传成功！');
  } catch (err) {
    console.error(err);
    showToast('图片上传失败，请检查后端');
  }
};

const saveDish = async () => {
  try {
    if (editingId.value) {
      await merchantApi.updateDish(editingId.value, dishForm);
      showToast('更新成功！');
    } else {
      await merchantApi.addDish(dishForm);
      showToast('新增成功！');
    }
    showDishModal.value = false;
    resetDishForm()
    await fetchDishes()
    app.loadPortalData(true);
  } catch (err) {
    console.error(err);
    showToast('保存失败，请检查后端');
  }
};

const handleTableClick = async (table) => {
  let nextStatus = table.status === 0 ? 1 : 0;
  if (table.status === 4) nextStatus = 0;

  try {
    await merchantApi.updateTableStatus(table.id, nextStatus);
    await fetchTables();
  } catch (err) {
    console.error(err);
    showToast('状态更新失败');
  }
};

// --- 订单管理 ---
const orderList = ref([])
const orderStatusFilter = ref(null)
const ORDER_STATUS = { 1:'待支付', 2:'已支付', 3:'出餐中', 4:'已完成' }

async function fetchOrders() {
  try {
    const res = await merchantApi.getOrders(orderStatusFilter.value)
    orderList.value = res.data?.data || res.data || []
  } catch (err) { console.error(err) }
}

async function changeOrderStatus(order, nextStatus) {
  try {
    await merchantApi.updateOrderStatus(order.id, nextStatus)
    showToast('状态已更新')
    await fetchOrders()
  } catch (err) { console.error(err); showToast('更新失败') }
}

// --- 桌台增删改 ---
const tableForm = reactive({ number: '', personNumber: 2, area: '大厅', minSpend: 0 })
const showTableForm = ref(false)
const editingTableId = ref(null)

function openAddTable() {
  editingTableId.value = null
  tableForm.number = ''; tableForm.personNumber = 2; tableForm.area = '大厅'; tableForm.minSpend = 0
  showTableForm.value = true
}

function openEditTable(table) {
  editingTableId.value = table.id
  tableForm.number = table.number; tableForm.personNumber = table.seats
  tableForm.area = table.area; tableForm.minSpend = table.minSpend || 0
  showTableForm.value = true
}

async function saveTable() {
  try {
    if (editingTableId.value) {
      await merchantApi.updateTable(editingTableId.value, tableForm)
    } else {
      await merchantApi.addTable(tableForm)
    }
    showToast(editingTableId.value ? '更新成功' : '新增成功')
    showTableForm.value = false
    await fetchTables()
  } catch (err) { console.error(err); showToast('保存失败') }
}

async function removeTable(table) {
  try {
    await merchantApi.deleteTable(table.id)
    showToast('删除成功')
    await fetchTables()
  } catch (err) { console.error(err); showToast('删除失败') }
}

async function exitPortal() {
  app.logout(); await router.push('/login')
}

onMounted(() => { app.loadPortalData(); fetchTables(); fetchDishes(); fetchCategories() })
</script>

<template>
  <div class="admin-layout">
    <aside class="main-sidebar">
      <div class="brand">
        <div class="logo-box">NN</div>
        <div class="brand-text">
          <h1>NomNom 智慧餐厅</h1>
          <span>商家管理后台 V2.0</span>
        </div>
      </div>

      <nav class="nav-links">
        <div class="nav-item" :class="{ active: currentMenu === 'B01' }" @click="switchTab('B01')">
          <i class="icon">🍱</i> 菜品管理 (B01)
        </div>
        <div class="nav-item" :class="{ active: currentMenu === 'B02' }" @click="switchTab('B02')">
          <i class="icon">🪑</i> 桌台管理 (B02)
        </div>
        <div class="nav-item" @click="switchTab('B03')">
          <i class="icon">📊</i> 营业数据
        </div>
      </nav>
    </aside>

    <main class="content-wrapper">
      <PortalHeader
          title="工作台"
          :display-name="app.displayName.value"
          :role-label="app.currentUser.value.roleLabel"
          @logout="exitPortal"
      />

      <div v-if="toast" class="toast-bar">{{ toast }}</div>
      <div class="page-container">

        <div v-if="currentMenu === 'B01'" class="module-box">
          <div class="tab-header">
            <div class="tab-triggers">
              <span :class="{ active: subTab === 'list' }" @click="subTab = 'list'">菜品库管理</span>
              <span :class="{ active: subTab === 'category' }" @click="subTab = 'category'">分类配置</span>
            </div>
            <button v-if="subTab === 'list'" class="btn-primary" @click="openAddModal">+ 新增菜品</button>
          </div>

          <table class="biz-table">
            <thead>
            <tr>
              <th width="40"><input type="checkbox" /></th>
              <th>菜品信息</th>
              <th>分类</th>
              <th>售价/会员价</th>
              <th>库存</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="item in dishList" :key="item.id">
              <td><input type="checkbox" /></td>
              <td>
                <div class="dish-info">
                  <img :src="item.coverUrl || '/placeholder.png'" class="dish-img" />
                  <div>
                    <p class="dish-name">{{ item.name }}</p>
                  </div>
                </div>
              </td>
              <td>{{ item.typeName || '-' }}</td>
              <td>
                <p class="sell-price">￥{{ Number(item.price || 0).toFixed(2) }}</p>
                <p class="member-price">会员: ￥{{ Number(item.memberPrice || 0).toFixed(2) }}</p>
              </td>
              <td>{{ item.stock || 0 }}</td>
              <td>
                  <span :class="['chip', item.status === 1 ? 'chip-green' : 'chip-gray']">
                    {{ item.status === 1 ? '在售' : '下架' }}
                  </span>
              </td>
              <td>
                <button class="btn-link" @click="openEditModal(item)">编辑</button>
                <button class="btn-link" @click="toggleDishStatus(item)">
                  {{ item.status === 1 ? '下架' : '上架' }}
                </button>
                <button class="btn-link danger" @click="removeDish(item)">删除</button>
              </td>
            </tr>
            </tbody>
          </table>

          <div v-if="subTab === 'category'" style="margin-top:24px">
            <div style="display:flex;gap:12px;margin-bottom:8px">
              <input v-model="catKeyword" @input="onCatKeywordChange" placeholder="搜索分类名称..." style="padding:6px 12px;border:1px solid #e0d4c3;border-radius:6px;width:200px" />
            </div>
            <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center">
              <input v-model="categoryForm.name" placeholder="分类名称" style="padding:6px 12px;border:1px solid #e0d4c3;border-radius:6px;width:160px" />
              <input v-model="categoryForm.iconUri" placeholder="图标URL(可选)" style="padding:6px 12px;border:1px solid #e0d4c3;border-radius:6px;width:200px" />
              <button class="btn-primary" @click="saveCategory" style="padding:6px 16px">{{ editingCategoryId ? '更新' : '新增' }}</button>
              <button v-if="editingCategoryId" class="btn-cancel" @click="openAddCategory" style="padding:6px 16px">取消</button>
            </div>
            <table class="biz-table">
              <thead><tr><th>ID</th><th>名称</th><th>图标</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="cat in categoryList" :key="cat.id">
                  <td>{{ cat.id }}</td>
                  <td>{{ cat.name }}</td>
                  <td>{{ cat.iconUri || '-' }}</td>
                  <td>
                    <button class="btn-link" @click="editCategory(cat)">编辑</button>
                    <button class="btn-link danger" @click="removeCategory(cat)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-if="currentMenu === 'B02'" class="module-box">
          <div class="tab-header">
            <div class="tab-triggers">
              <span :class="{ active: subTab === 'layout' }" @click="subTab = 'layout'">实时桌态布局</span>
            </div>
            <button class="btn-primary" @click="openAddTable" style="padding:6px 16px">+ 新增桌台</button>
          </div>

          <div class="table-tool-bar">
            <div class="area-filters">
              <button
                  v-for="a in ['全部', ...tableAreas]"
                  :key="a"
                  :class="{ active: selectedArea === a }"
                  @click="selectedArea = a"
              >{{ a }}</button>
            </div>
            <div class="status-legends">
              <span v-for="(v, k) in TABLE_STATUS" :key="k">
                <i :style="{ backgroundColor: v.color }"></i> {{ v.label }}
              </span>
            </div>
          </div>

          <p v-if="tableError" class="form-message" style="color:#d32f2f">{{ tableError }}</p>
          <p v-if="tableLoading" class="empty-hint">桌台数据加载中...</p>
          <div v-if="!tableLoading" class="visual-grid">
            <div
                v-for="table in filteredTables"
                :key="table.id"
                class="table-card"
                :class="TABLE_STATUS[table.status].class"
                @click="handleTableClick(table)"
            >
              <div class="card-top">
                <span class="t-no">{{ table.number }}</span>
                <span class="t-area">{{ table.area }}</span>
              </div>
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
          <div class="tab-header"><h3>全店订单</h3></div>
          <div style="display:flex;gap:8px;margin-bottom:12px">
            <button v-for="(label,val) in ORDER_STATUS" :key="val" class="ghost-button" :class="{active:orderStatusFilter===Number(val)}" @click="orderStatusFilter=orderStatusFilter===Number(val)?null:Number(val);fetchOrders()">{{ label }}</button>
          </div>
          <table class="biz-table">
            <thead><tr><th>订单号</th><th>金额</th><th>状态</th><th>时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="o in orderList" :key="o.id">
                <td>{{ o.code }}</td>
                <td>￥{{ Number(o.totalPrice||0).toFixed(2) }}</td>
                <td><span :class="['chip', o.status===4?'chip-green':o.status===1?'chip-gray':'']">{{ ORDER_STATUS[o.status]||o.status }}</span></td>
                <td>{{ o.createTime }}</td>
                <td>
                  <select @change="changeOrderStatus(o,Number($event.target.value))" style="padding:4px 8px;border-radius:4px">
                    <option value="">变更状态</option>
                    <option v-for="(label,val) in ORDER_STATUS" :key="val" :value="val" :disabled="val===o.status">{{ label }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

      </div>
    </main>

    <div v-if="showDishModal" class="biz-modal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingId ? '编辑菜品' : '新增菜品信息' }}</h3>
          <button @click="showDishModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group span-2">
              <label>菜品名称</label>
              <input type="text" v-model="dishForm.name" placeholder="输入菜品名称" />
            </div>

            <div class="form-group">
              <label>菜品分类</label>
              <select v-model.number="dishForm.type_id" style="padding:8px 12px;border:1px solid #e0d4c3;border-radius:6px">
                <option :value="null">未分类</option>
                <option v-for="cat in categoryList" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
              </select>
            </div>

            <div class="form-group">
              <label>基础售价 (元)</label>
              <input type="number" v-model="dishForm.price" />
            </div>

            <div class="form-group">
              <label>上传菜品图片</label>
              <div class="upload-container">
                <input type="file" @change="handleImageUpload" accept="image/*" id="dish-file" hidden />
                <label for="dish-file" class="upload-trigger">
                  <img v-if="dishForm.cover_url" :src="dishForm.cover_url" class="preview-img" />
                  <span v-else class="upload-btn-text">+ 点击选择图片</span>
                </label>
              </div>
            </div>

          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showDishModal = false">取消</button>
          <button class="btn-primary" @click="saveDish">保存配置</button>
        </div>
      </div>
    </div>
  </div>

    <div v-if="showTableForm" class="biz-modal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingTableId ? '编辑桌台' : '新增桌台' }}</h3>
          <button @click="showTableForm = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group">
              <label>桌号</label>
              <input v-model="tableForm.number" placeholder="如 A01" />
            </div>
            <div class="form-group">
              <label>人数</label>
              <input type="number" v-model.number="tableForm.personNumber" />
            </div>
            <div class="form-group">
              <label>区域</label>
              <input v-model="tableForm.area" placeholder="大厅/包间/二楼" />
            </div>
            <div class="form-group">
              <label>最低消费</label>
              <input type="number" v-model.number="tableForm.minSpend" />
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showTableForm = false">取消</button>
          <button class="btn-primary" @click="saveTable">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 核心后台架构 - 暖白底色 */
.admin-layout { display: flex; height: 100vh; width: 100vw; overflow: hidden; background: #fcfaf5; font-family: 'PingFang SC', sans-serif; }

/* 侧边栏样式 - 奶油暖色调 */
.main-sidebar { width: 240px; background: #fff8eb; border-right: 1px solid #f0e6d2; color: #5d4037; display: flex; flex-direction: column; }
.brand { padding: 32px 24px; display: flex; align-items: center; gap: 12px; }
.logo-box { width: 40px; height: 40px; background: #f57c00; border-radius: 8px; color: white; display: grid; place-items: center; font-weight: bold; font-size: 20px; }
.brand-text h1 { color: #4e342e; font-size: 16px; margin: 0; }
.brand-text span { font-size: 12px; opacity: 0.6; color: #795548;}
.nav-links { flex: 1; padding: 20px 0; }
.nav-item { padding: 14px 24px; cursor: pointer; transition: 0.3s; display: flex; align-items: center; gap: 12px; font-weight: 500;}
.nav-item:hover { color: #f57c00; background: rgba(245, 124, 0, 0.08); }
.nav-item.active { background: #f57c00; color: white; box-shadow: 0 4px 10px rgba(245, 124, 0, 0.2);}

/* Toast 提示条 */
.toast-bar { background: #323232; color: #fff; padding: 10px 24px; text-align: center; font-size: 14px; animation: fadeInOut 2s ease; }
@keyframes fadeInOut { 0% { opacity: 0; transform: translateY(-8px); } 10% { opacity: 1; transform: translateY(0); } 80% { opacity: 1; } 100% { opacity: 0; } }

/* 主体内容区 */
.content-wrapper { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.page-container { padding: 24px; overflow-y: auto; flex: 1; }
.module-box { background: white; border-radius: 12px; padding: 24px; box-shadow: 0 4px 16px rgba(121, 85, 72, 0.04); border: 1px solid #f8f4ec;}

/* 子页签样式 - 暖橙色下划线 */
.tab-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f0e6d2; margin-bottom: 20px; }
.tab-triggers { display: flex; gap: 32px; }
.tab-triggers span { padding: 12px 0; cursor: pointer; position: relative; font-weight: 500; color: #8d6e63; }
.tab-triggers span.active { color: #f57c00; }
.tab-triggers span.active::after { content: ''; position: absolute; bottom: -1px; left: 0; width: 100%; height: 2px; background: #f57c00; }

/* 业务工具条 */
.table-tool-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.area-filters { display: flex; gap: 8px; }
.area-filters button { border: 1px solid #e0d4c3; padding: 5px 16px; border-radius: 6px; background: white; color:#5d4037; cursor: pointer; transition: 0.2s;}
.area-filters button.active { background: #f57c00; color: white; border-color: #f57c00; box-shadow: 0 2px 6px rgba(245, 124, 0, 0.2);}
.status-legends { display: flex; gap: 12px; font-size: 12px; color: #5d4037;}
.status-legends i { display: inline-block; width: 10px; height: 10px; border-radius: 2px; }

/* 菜品展示表格 */
.biz-table { width: 100%; border-collapse: collapse; }
.biz-table th { background: #fbf6ec; padding: 12px; text-align: left; font-size: 14px; border-bottom: 1px solid #f0e6d2; color: #5d4037; }
.biz-table td { padding: 16px 12px; border-bottom: 1px solid #f9f5f0; font-size: 14px; color: #4e342e;}
.dish-info { display: flex; gap: 12px; align-items: center; }
.dish-img { width: 48px; height: 48px; border-radius: 6px; object-fit: cover; }
.dish-name { font-weight: bold; margin: 0; }
.sell-price { color: #d84315; font-weight: bold; margin: 0; font-size: 16px;}
.member-price { font-size: 12px; color: #8d6e63; margin: 0; }
.chip { padding: 4px 10px; border-radius: 12px; font-size: 12px; }
.chip-green { background: #f1f8e9; color: #558b2f; border: 1px solid #c5e1a5; }
.chip-gray { background: #f5f5f5; color: #9e9e9e; border: 1px solid #eeeeee; }

/* 桌台网格可视化 */
.visual-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 20px; }
.table-card { border-radius: 10px; padding: 16px; border-top: 5px solid #e0d4c3; background: #fffcf8; cursor: pointer; transition: 0.3s; position: relative; display: flex; flex-direction: column; min-height: 120px; box-shadow: 0 2px 8px rgba(121, 85, 72, 0.05);}
.table-card:hover { transform: translateY(-4px); box-shadow: 0 10px 20px rgba(121, 85, 72, 0.1); }

/* 状态颜色方案 (柔和暖色) */
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

/* 弹窗设计与上传组件 */
.biz-modal { position: fixed; inset: 0; background: rgba(62, 39, 35, 0.6); display: grid; place-items: center; z-index: 999; }
.modal-content { background: #fffcf8; width: 500px; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.1);}
.modal-header { padding: 16px 24px; border-bottom: 1px solid #f0e6d2; display: flex; justify-content: space-between; align-items: center; color: #4e342e;}
.modal-body { padding: 24px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.form-group { display: flex; flex-direction: column; gap: 8px; }
.form-group.span-2 { grid-column: span 2; }
.form-group label { font-size: 14px; font-weight: 500; color: #5d4037;}
.form-group input { padding: 8px 12px; border: 1px solid #e0d4c3; border-radius: 6px; background: white; color:#4e342e;}
.form-group input:focus { outline: none; border-color: #f57c00;}

/* 上传按钮样式 */
.upload-trigger { display: flex; align-items: center; justify-content: center; width: 100px; height: 100px; border: 1px dashed #cfb9a3; border-radius: 8px; cursor: pointer; background: #fff; overflow: hidden; transition: 0.2s;}
.upload-trigger:hover { border-color: #f57c00; background: #fff8eb;}
.upload-btn-text { font-size: 12px; color: #a1887f; text-align: center; padding: 10px; }
.preview-img { width: 100%; height: 100%; object-fit: cover; }

.modal-footer { padding: 16px 24px; border-top: 1px solid #f0e6d2; text-align: right; display: flex; justify-content: flex-end; gap: 12px; background: #fbf6ec;}
.btn-primary { background: #f57c00; color: white; border: none; padding: 8px 24px; border-radius: 6px; cursor: pointer; font-weight: 500; transition: 0.2s;}
.btn-primary:hover { background: #e65100; }
.btn-cancel { background: white; border: 1px solid #e0d4c3; color: #5d4037; padding: 8px 24px; border-radius: 6px; cursor: pointer; transition: 0.2s;}
.btn-cancel:hover { background: #f5f5f5; }
.btn-link { background: none; border: none; color: #f57c00; cursor: pointer; margin-right: 8px; font-weight: 500;}
.btn-link.danger { color: #d32f2f; }
</style>