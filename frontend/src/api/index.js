import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    const serverMessage = error.response?.data?.message
    if (serverMessage) {
      return Promise.reject(new Error(serverMessage))
    }
    return Promise.reject(error)
  }
)

export const floorApi = {
  getAll: () => request.get('/floor'),
  getById: id => request.get(`/floor/${id}`),
  create: data => request.post('/floor', data),
  update: (id, data) => request.put(`/floor/${id}`, data),
  delete: id => request.delete(`/floor/${id}`)
}

export const roomApi = {
  getAll: () => request.get('/room'),
  getById: id => request.get(`/room/${id}`),
  getByFloor: floorId => request.get(`/room/floor/${floorId}`),
  create: data => request.post('/room', data),
  update: (id, data) => request.put(`/room/${id}`, data),
  delete: id => request.delete(`/room/${id}`)
}

export const deviceApi = {
  getAll: () => request.get('/device'),
  getById: id => request.get(`/device/${id}`),
  getByCode: code => request.get(`/device/code/${code}`),
  getByFloor: floorId => request.get(`/device/floor/${floorId}`),
  getByRoom: roomId => request.get(`/device/room/${roomId}`),
  getPage: (params) => request.get('/device/page', { params }),
  getGrouped: () => request.get('/device/grouped'),
  create: data => request.post('/device', data),
  update: (id, data) => request.put(`/device/${id}`, data),
  delete: id => request.delete(`/device/${id}`),
  transfer: data => request.post('/device/transfer', data),
  batchTransfer: data => request.post('/device/batch-transfer', data),
  getTransferHistory: deviceId => request.get(`/device/transfer-history/${deviceId}`),
  getAllTransferRecords: params => request.get('/device/transfer-records', { params })
}

export const specTemplateApi = {
  getAll: () => request.get('/spec-template'),
  getEnabled: () => request.get('/spec-template/enabled'),
  getById: id => request.get(`/spec-template/${id}`),
  getByType: deviceType => request.get(`/spec-template/type/${encodeURIComponent(deviceType)}`),
  create: data => request.post('/spec-template', data),
  update: (id, data) => request.put(`/spec-template/${id}`, data),
  // 编辑保存前的变更预览（新增/删除/类型变化 + 受影响设备数），不落库
  preview: (id, data) => request.post(`/spec-template/${id}/preview`, data),
  updateStatus: (id, status) => request.put(`/spec-template/${id}/status`, { status })
}

export const inventoryApi = {
  createBatch: data => request.post('/inventory/batch', data),
  getBatches: params => request.get('/inventory/batch', { params }),
  getBatch: id => request.get(`/inventory/batch/${id}`),
  getItems: (id, params) => request.get(`/inventory/batch/${id}/items`, { params }),
  checkItem: (batchId, itemId, data) => request.put(`/inventory/batch/${batchId}/item/${itemId}`, data),
  submitBatch: id => request.post(`/inventory/batch/${id}/submit`),
  transferItem: (batchId, itemId, data) =>
    request.post(`/inventory/batch/${batchId}/item/${itemId}/transfer`, data),
  resolveItem: (batchId, itemId, data) =>
    request.post(`/inventory/batch/${batchId}/item/${itemId}/resolve`, data),
  closeBatch: id => request.post(`/inventory/batch/${id}/close`)
}

export const activityApi = {
  create: data => request.post('/activity', data),
  getPage: params => request.get('/activity', { params }),
  getById: id => request.get(`/activity/${id}`),
  finish: id => request.post(`/activity/${id}/finish`),
  getRoomOccupancies: params => request.get('/activity/room-occupancy', { params })
}
