import axios from 'axios';

const STORAGE_KEY = 'nomnom-auth-user'

function getToken() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const user = JSON.parse(raw)
    return user?.token || null
  } catch {
    return null
  }
}

function clearAuthAndRedirect() {
  localStorage.removeItem(STORAGE_KEY)
  window.location.hash = '#/login'
}

// ==========================================
// 1. fetch 请求封装 (供顾客端使用)
// ==========================================
async function request(url, options = {}) {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  let response
  try {
    response = await fetch(url, {
      headers,
      ...options,
    })
  } catch {
    throw new Error('无法连接到服务器。请先确认前端开发服务 http://localhost:5173 和后端接口 http://localhost:8080 已启动。')
  }

  if (response.status === 401) {
    clearAuthAndRedirect()
    throw new Error('登录已过期，请重新登录')
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
// 2. Axios 商家端请求封装
// ==========================================
const axiosRequest = axios.create({ baseURL: '/api' })

axiosRequest.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

axiosRequest.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401) {
        clearAuthAndRedirect()
      }
      return Promise.reject(error)
    }
)

export const merchantApi = {
  uploadFile: (formData) => axiosRequest.post('/common/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),

  getDishes: () => axiosRequest.get('/admin/dishes'),
  addDish: (data) => axiosRequest.post('/admin/dishes/add', data),
  updateDish: (id, data) => axiosRequest.put(`/admin/dishes/${id}`, data),
  updateDishStatus: (id, status) => axiosRequest.put(`/admin/dishes/${id}/status`, null, {
    params: { status }
  }),
  deleteDish: (id) => axiosRequest.delete(`/admin/dishes/${id}`),

  getCategories: (keyword) => axiosRequest.get('/admin/categories', { params: keyword ? { keyword } : {} }),
  addCategory: (data) => axiosRequest.post('/admin/categories', data),
  updateCategory: (id, data) => axiosRequest.put(`/admin/categories/${id}`, data),
  deleteCategory: (id) => axiosRequest.delete(`/admin/categories/${id}`),

  getOrders: (status) => axiosRequest.get('/admin/orders', { params: status != null ? { status } : {} }),
  updateOrderStatus: (id, status) => axiosRequest.put(`/admin/orders/${id}/status`, null, {
    params: { status }
  }),

  getTables: () => axiosRequest.get('/admin/tables'),
  addTable: (data) => axiosRequest.post('/admin/tables', data),
  updateTable: (id, data) => axiosRequest.put(`/admin/tables/${id}`, data),
  deleteTable: (id) => axiosRequest.delete(`/admin/tables/${id}`),
  updateTableStatus: (tableId, status) => axiosRequest.put('/admin/tables/status', null, {
    params: { tableId, status }
  })
}
