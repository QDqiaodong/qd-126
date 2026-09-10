<template>
  <div class="device-detail">
    <el-card v-loading="loading">
      <div class="card-header">
        <h3>设备详情</h3>
        <el-button @click="goBack">返回列表</el-button>
      </div>

      <div class="detail-content">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card title="基本信息">
              <div class="info-item">
                <span class="label">设备编号</span>
                <span class="value">{{ device.deviceCode }}</span>
              </div>
              <div class="info-item">
                <span class="label">设备名称</span>
                <span class="value">{{ device.deviceName }}</span>
              </div>
              <div class="info-item">
                <span class="label">设备类型</span>
                <el-tag :type="getTypeTagType(device.deviceType)">{{ device.deviceType }}</el-tag>
              </div>
              <div class="info-item">
                <span class="label">品牌</span>
                <span class="value">{{ device.brand || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">型号</span>
                <span class="value">{{ device.model || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">设备状态</span>
                <el-tag :type="getStatusTagType(device.status)">{{ device.statusText }}</el-tag>
              </div>
              <div class="info-item">
                <span class="label">采购日期</span>
                <span class="value">{{ formatDate(device.purchaseDate) }}</span>
              </div>
              <div class="info-item">
                <span class="label">保修截止</span>
                <span class="value">{{ formatDate(device.warrantyEndDate) }}</span>
              </div>
              <div class="info-item">
                <span class="label">创建人</span>
                <span class="value">{{ device.createdBy || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">建档时间</span>
                <span class="value">{{ formatTime(device.createdAt) }}</span>
              </div>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card title="归属信息">
              <div class="info-item">
                <span class="label">当前楼层</span>
                <span class="value">{{ device.currentFloorName || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="label">当前接待室</span>
                <span class="value">{{ device.currentRoomName || '-' }}</span>
              </div>
              <div class="info-item" style="margin-top: 20px;">
                <el-button @click="handleTransfer" type="primary">
                  <el-icon><RefreshLeft /></el-icon>
                  执行调配
                </el-button>
              </div>
            </el-card>

            <el-card title="设备图片" style="margin-top: 20px;">
              <div v-if="device.imageUrl" class="device-image">
                <img :src="device.imageUrl" alt="设备图片" />
              </div>
              <div v-else class="no-image">暂无图片</div>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card title="规格参数">
              <template v-if="templateSpecRows.length > 0">
                <div v-for="row in templateSpecRows" :key="row.key" class="spec-item">
                  <span class="spec-key">{{ row.label }}</span>
                  <span class="spec-value">{{ formatSpecValue(row.value) }}</span>
                </div>
              </template>
              <template v-else-if="rawSpecKeys.length > 0">
                <div v-for="key in rawSpecKeys" :key="key" class="spec-item">
                  <span class="spec-key">{{ key }}</span>
                  <span class="spec-value">{{ formatSpecValue(device.specJson[key]) }}</span>
                </div>
              </template>
              <div v-else class="no-spec">暂无规格参数</div>
            </el-card>
          </el-col>
        </el-row>

        <el-card title="流转历史记录" style="margin-top: 20px;">
          <el-table :data="transferHistory" v-loading="historyLoading">
            <el-table-column prop="transferTime" label="流转时间" width="170">
              <template #default="{ row }">{{ formatTime(row.transferTime) }}</template>
            </el-table-column>
            <el-table-column label="流转路径" width="300">
              <template #default="{ row }">
                <span>{{ row.fromFloorName || '无' }} / {{ row.fromRoomName || '无' }}</span>
                <el-icon style="margin: 0 8px;"><ArrowRight /></el-icon>
                <span>{{ row.toFloorName || '无' }} / {{ row.toRoomName || '无' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="transferReason" label="流转原因" width="200" />
            <el-table-column prop="operator" label="操作人" width="100" />
            <el-table-column prop="remark" label="备注" />
          </el-table>
          <div v-if="transferHistory.length === 0" class="empty-history">暂无流转记录</div>
        </el-card>
      </div>
    </el-card>

    <el-dialog v-model="transferDialogVisible" title="设备调配" width="500px">
      <el-form :model="transferForm" label-width="100px">
        <el-form-item label="目标楼层">
          <el-select v-model="transferForm.toFloorId" @change="onFloorChange" placeholder="请选择楼层">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标接待室">
          <el-select v-model="transferForm.toRoomId" placeholder="请选择接待室">
            <el-option v-for="room in rooms" :key="room.id" :label="room.roomName" :value="room.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="流转原因">
          <el-input v-model="transferForm.transferReason" type="textarea" placeholder="请输入流转原因" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="transferForm.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="transferForm.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button @click="confirmTransfer" type="primary">确认调配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { RefreshLeft, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { deviceApi, floorApi, roomApi } from '../api'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const historyLoading = ref(false)
const deviceId = ref(route.params.id)

const device = reactive({
  deviceCode: '',
  deviceName: '',
  deviceType: '',
  brand: '',
  model: '',
  specJson: {},
  specFields: [],
  imageUrl: '',
  currentFloorName: '',
  currentRoomName: '',
  status: 1,
  statusText: '',
  purchaseDate: null,
  warrantyEndDate: null,
  createdBy: '',
  createdAt: null
})

// 按模板字段顺序渲染；模板已停用时 specFields 仍由后端返回，历史规格正常展示
const templateSpecRows = computed(() => {
  const fields = device.specFields || []
  if (fields.length === 0) return []
  const rows = []
  fields.forEach(field => {
    const value = device.specJson ? device.specJson[field.fieldKey] : undefined
    if (value !== undefined && value !== null && value !== '') {
      rows.push({ key: field.fieldKey, label: field.fieldLabel, value })
    }
  })
  return rows
})

// 模板未覆盖（或已被删除的字段）的历史键值，兜底展示
const rawSpecKeys = computed(() => {
  const spec = device.specJson || {}
  const definedKeys = new Set((device.specFields || []).map(f => f.fieldKey))
  return Object.keys(spec).filter(k => !definedKeys.has(k) && spec[k] !== null && spec[k] !== '')
})

const formatSpecValue = (value) => {
  if (value === true || value === 'true') return '是'
  if (value === false || value === 'false') return '否'
  return value === null || value === undefined ? '-' : String(value)
}

const transferHistory = ref([])
const floors = ref([])
const rooms = ref([])

const transferDialogVisible = ref(false)
const transferForm = reactive({
  deviceId: null,
  toFloorId: null,
  toRoomId: null,
  transferReason: '',
  operator: '',
  remark: ''
})

const goBack = () => {
  router.push('/device')
}

const getTypeTagType = (type) => {
  const map = { '电视': 'primary', '音响': 'success', '麦克风': 'info', '投影仪': 'warning', '其他': 'danger' }
  return map[type] || 'info'
}

const getStatusTagType = (status) => {
  const map = { 1: 'success', 0: 'danger', 2: 'warning' }
  return map[status] || 'info'
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const loadDevice = async () => {
  loading.value = true
  try {
    const data = await deviceApi.getById(deviceId.value)
    Object.assign(device, data)
  } catch (error) {
    console.error('加载设备信息失败', error)
  } finally {
    loading.value = false
  }
}

const loadTransferHistory = async () => {
  historyLoading.value = true
  try {
    transferHistory.value = await deviceApi.getTransferHistory(deviceId.value)
  } catch (error) {
    console.error('加载流转历史失败', error)
  } finally {
    historyLoading.value = false
  }
}

const handleTransfer = () => {
  transferForm.deviceId = deviceId.value
  transferForm.toFloorId = null
  transferForm.toRoomId = null
  transferForm.transferReason = ''
  transferForm.operator = ''
  transferForm.remark = ''
  rooms.value = []
  transferDialogVisible.value = true
}

const onFloorChange = async (floorId) => {
  if (floorId) {
    rooms.value = await roomApi.getByFloor(floorId)
  } else {
    rooms.value = []
  }
  transferForm.toRoomId = null
}

const confirmTransfer = async () => {
  if (!transferForm.toFloorId || !transferForm.toRoomId) {
    ElMessage.warning('请选择目标楼层和接待室')
    return
  }
  try {
    await deviceApi.transfer(transferForm)
    ElMessage.success('调配成功')
    transferDialogVisible.value = false
    loadDevice()
    loadTransferHistory()
  } catch (error) {
    ElMessage.error('调配失败')
  }
}

const loadFloors = async () => {
  try {
    floors.value = await floorApi.getAll()
  } catch (error) {
    console.error('加载楼层失败', error)
  }
}

onMounted(() => {
  loadFloors()
  loadDevice()
  loadTransferHistory()
})
</script>

<style scoped>
.device-detail {
  padding: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
}

.detail-content {
  padding: 10px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.label {
  color: #999;
  font-size: 14px;
}

.value {
  color: #333;
  font-weight: 500;
}

.device-image {
  max-width: 250px;
}

.device-image img {
  max-width: 100%;
  border-radius: 4px;
}

.no-image {
  text-align: center;
  color: #999;
  padding: 20px;
}

.spec-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
}

.spec-key {
  color: #666;
}

.spec-value {
  color: #333;
  font-weight: 500;
}

.no-spec {
  text-align: center;
  color: #999;
  padding: 20px;
}

.empty-history {
  text-align: center;
  color: #999;
  padding: 30px;
}
</style>
