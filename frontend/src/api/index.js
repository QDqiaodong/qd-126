import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      const err = new Error(res.message || '请求失败')
      err.code = res.code
      return Promise.reject(err)
    }
    return res.data
  },
  error => {
    const serverMessage = error.response?.data?.message
    if (serverMessage) {
      const err = new Error(serverMessage)
      // 透传后端业务码（如 460 静音时段冲突），页面据此弹出原因
      err.code = error.response?.data?.code
      return Promise.reject(err)
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
  getWarrantyOverview: params => request.get('/device/warranty-overview', { params }),
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
  update: (id, data) => request.put(`/activity/${id}`, data),
  getPage: params => request.get('/activity', { params }),
  getById: id => request.get(`/activity/${id}`),
  finish: id => request.post(`/activity/${id}/finish`),
  getRoomOccupancies: params => request.get('/activity/room-occupancy', { params })
}

export const quietPeriodApi = {
  create: data => request.post('/quiet-period', data),
  update: (id, data) => request.put(`/quiet-period/${id}`, data),
  delete: id => request.delete(`/quiet-period/${id}`),
  getPage: params => request.get('/quiet-period', { params }),
  getByRoom: roomId => request.get(`/quiet-period/room/${roomId}`)
}

export const interpreterBookingApi = {
  create: data => request.post('/interpreter-booking', data),
  update: (id, data) => request.put(`/interpreter-booking/${id}`, data),
  delete: id => request.delete(`/interpreter-booking/${id}`),
  getPage: params => request.get('/interpreter-booking', { params }),
  getById: id => request.get(`/interpreter-booking/${id}`),
  getByRoom: roomId => request.get(`/interpreter-booking/room/${roomId}`)
}

export const replacementApi = {
  create: data => request.post('/replacement', data),
  getSpares: faultyDeviceId => request.get('/replacement/spares', { params: { faultyDeviceId } }),
  getPage: params => request.get('/replacement', { params }),
  getById: id => request.get(`/replacement/${id}`),
  resolve: (id, data) => request.post(`/replacement/${id}/resolve`, data)
}

export const welcomeBoardApi = {
  create: data => request.post('/welcome-board', data),
  getPage: params => request.get('/welcome-board', { params }),
  getById: id => request.get(`/welcome-board/${id}`),
  getOverdue: () => request.get('/welcome-board/overdue'),
  remove: (id, data) => request.post(`/welcome-board/${id}/remove`, data),
  getRoomAvailability: params => request.get('/welcome-board/room-availability', { params })
}

export const comboApi = {
  getAll: status => request.get('/combo', { params: status !== undefined ? { status } : {} }),
  getById: id => request.get(`/combo/${id}`),
  create: data => request.post('/combo', data),
  update: (id, data) => request.put(`/combo/${id}`, data),
  delete: id => request.delete(`/combo/${id}`),
  apply: (id, data) => request.post(`/combo/${id}/apply`, data),
  getRecords: params => request.get('/combo/records', { params }),
  getRecordById: id => request.get(`/combo/records/${id}`)
}
