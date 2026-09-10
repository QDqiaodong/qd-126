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
  updateStatus: (id, status) => request.put(`/spec-template/${id}/status`, { status })
}
