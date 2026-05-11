import axios from 'axios';

// ==========================================
// 1. 原本系统自带的 fetch 请求封装 (供顾客端等使用)
// ==========================================
async function request(url, options = {}) {
  let response
  try {
    response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {}),
      },
      ...options,
    })
  } catch {
    throw new Error('无法连接到服务器。请先确认前端开发服务 http://localhost:5173 和后端接口 http://localhost:8080 已启动。')
  }

  const result = await response
      .json()
      .catch(() => ({ success: false, message: `HTTP ${response.status}` }))

  if (!response.ok || result.success === false) {
    throw new Error(result.message || `HTTP ${response.status}`)
  }

  return result.data
}

export function login(payload) {
  return request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function register(payload) {
  return request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function fetchCategories() {
  return request('/api/menu/categories')
}

export function fetchMenuItems() {
  return request('/api/menu/items')
}

export function fetchOrders(userId = null) {
  const query = userId == null ? '' : `?userId=${encodeURIComponent(userId)}`
  return request(`/api/orders/current${query}`)
}

export function fetchOrderPreview(payload) {
  return request('/api/orders/preview', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function createOrder(payload) {
  return request('/api/orders', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}


// ==========================================
// 2. 新增的 Axios 商家端请求封装 (供咱们新写的商家页面使用)
// ==========================================
// 为了防止名字冲突，这里命名为 axiosRequest
const axiosRequest = axios.create({ baseURL: '/api' });

export const merchantApi = {
  // 上传图片
  uploadFile: (formData) => axiosRequest.post('/common/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),

  // 菜品操作
  addDish: (data) => axiosRequest.post('/admin/dishes/add', data),

  // 桌台操作
  getTables: () => axiosRequest.get('/admin/tables'),
  updateTableStatus: (tableId, status) => axiosRequest.put('/admin/tables/status', null, {
    params: { tableId, status }
  })
};