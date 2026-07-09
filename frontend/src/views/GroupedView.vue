<template>
  <div class="grouped-view">
    <el-card>
      <div class="toolbar">
        <el-button @click="handleBatchTransfer" type="success" :disabled="selectedIds.length === 0">
          <el-icon><RefreshLeft /></el-icon>
          批量调配 ({{ selectedIds.length }})
        </el-button>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <div v-loading="loading" class="grouped-content">
        <div v-for="(devices, floorName) in groupedDevices" :key="floorName" class="floor-group">
          <div class="floor-header">
            <el-icon><OfficeBuilding /></el-icon>
            <span class="floor-name">{{ floorName }}</span>
            <span class="device-count">{{ devices.length }}台设备</span>
          </div>
          <div class="device-grid">
            <div
              v-for="device in devices"
              :key="device.id"
              class="device-card"
              :class="{ selected: selectedIds.includes(device.id) }"
              @click="toggleSelect(device.id)"
            >
              <div class="card-header">
                <el-checkbox :checked="isSelected(device.id)" @change.stop="toggleSelect(device.id)" />
                <span class="device-code">{{ device.deviceCode }}</span>
              </div>
              <div class="device-image">
                <img v-if="device.imageUrl" :src="device.imageUrl" alt="设备图片" />
                <div v-else class="no-image">
                  <el-icon><Monitor /></el-icon>
                </div>
              </div>
              <div class="device-info">
                <div class="device-name">{{ device.deviceName }}</div>
                <div class="device-type">
                  <el-tag :type="getTypeTagType(device.deviceType)" size="small">{{ device.deviceType }}</el-tag>
                </div>
                <div class="device-status">
                  <el-tag :type="getStatusTagType(device.status)" size="small">{{ device.statusText }}</el-tag>
                </div>
                <div class="device-room">{{ device.currentRoomName || '未分配' }}</div>
              </div>
              <div class="card-actions">
                <el-button size="small" @click.stop="goToDetail(device.id)">详情</el-button>
                <el-button size="small" type="primary" @click.stop="handleTransfer(device)">调配</el-button>
              </div>
            </div>
          </div>
        </div>
        <div v-if="Object.keys(groupedDevices).length === 0" class="empty-state">
          <el-icon size="64" color="#ccc"><Monitor /></el-icon>
          <p>暂无设备数据</p>
        </div>
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

    <el-dialog v-model="batchTransferDialogVisible" title="批量调配" width="500px">
      <el-form :model="batchTransferForm" label-width="100px">
        <el-form-item label="目标楼层">
          <el-select v-model="batchTransferForm.toFloorId" @change="onBatchFloorChange" placeholder="请选择楼层">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标接待室">
          <el-select v-model="batchTransferForm.toRoomId" placeholder="请选择接待室">
            <el-option v-for="room in batchRooms" :key="room.id" :label="room.roomName" :value="room.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="流转原因">
          <el-input v-model="batchTransferForm.transferReason" type="textarea" placeholder="请输入流转原因" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="batchTransferForm.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="batchTransferForm.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchTransferDialogVisible = false">取消</el-button>
        <el-button @click="confirmBatchTransfer" type="primary">确认批量调配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { OfficeBuilding, Monitor, Refresh, RefreshLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { deviceApi, floorApi, roomApi } from '../api'

const router = useRouter()

const loading = ref(false)
const groupedDevices = ref({})
const selectedIds = ref([])
const floors = ref([])
const rooms = ref([])
const batchRooms = ref([])

const transferDialogVisible = ref(false)
const transferForm = ref({
  deviceId: null,
  toFloorId: null,
  toRoomId: null,
  transferReason: '',
  operator: '',
  remark: ''
})

const batchTransferDialogVisible = ref(false)
const batchTransferForm = ref({
  toFloorId: null,
  toRoomId: null,
  transferReason: '',
  operator: '',
  remark: ''
})

const getTypeTagType = (type) => {
  const map = { '电视': 'primary', '音响': 'success', '麦克风': 'info', '投影仪': 'warning', '其他': 'danger' }
  return map[type] || 'info'
}

const getStatusTagType = (status) => {
  const map = { 1: 'success', 0: 'danger', 2: 'warning' }
  return map[status] || 'info'
}

const isSelected = (id) => {
  return selectedIds.value.includes(id)
}

const toggleSelect = (id) => {
  const index = selectedIds.value.indexOf(id)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
  } else {
    selectedIds.value.push(id)
  }
}

const loadDevices = async () => {
  loading.value = true
  try {
    groupedDevices.value = await deviceApi.getGrouped()
  } catch (error) {
    console.error('加载设备数据失败', error)
  } finally {
    loading.value = false
  }
}

const loadFloors = async () => {
  try {
    floors.value = await floorApi.getAll()
  } catch (error) {
    console.error('加载楼层失败', error)
  }
}

const refreshData = () => {
  loadDevices()
}

const goToDetail = (id) => {
  router.push(`/device/detail/${id}`)
}

const handleTransfer = (device) => {
  transferForm.value = {
    deviceId: device.id,
    toFloorId: null,
    toRoomId: null,
    transferReason: '',
    operator: '',
    remark: ''
  }
  rooms.value = []
  transferDialogVisible.value = true
}

const handleBatchTransfer = () => {
  batchTransferForm.value = {
    toFloorId: null,
    toRoomId: null,
    transferReason: '',
    operator: '',
    remark: ''
  }
  batchRooms.value = []
  batchTransferDialogVisible.value = true
}

const onFloorChange = async (floorId) => {
  if (floorId) {
    rooms.value = await roomApi.getByFloor(floorId)
  } else {
    rooms.value = []
  }
  transferForm.value.toRoomId = null
}

const onBatchFloorChange = async (floorId) => {
  if (floorId) {
    batchRooms.value = await roomApi.getByFloor(floorId)
  } else {
    batchRooms.value = []
  }
  batchTransferForm.value.toRoomId = null
}

const confirmTransfer = async () => {
  if (!transferForm.value.toFloorId || !transferForm.value.toRoomId) {
    ElMessage.warning('请选择目标楼层和接待室')
    return
  }
  try {
    await deviceApi.transfer(transferForm.value)
    ElMessage.success('调配成功')
    transferDialogVisible.value = false
    loadDevices()
  } catch (error) {
    ElMessage.error('调配失败')
  }
}

const confirmBatchTransfer = async () => {
  if (!batchTransferForm.value.toFloorId || !batchTransferForm.value.toRoomId) {
    ElMessage.warning('请选择目标楼层和接待室')
    return
  }
  try {
    await deviceApi.batchTransfer({
      deviceIds: selectedIds.value,
      ...batchTransferForm.value
    })
    ElMessage.success('批量调配成功')
    batchTransferDialogVisible.value = false
    selectedIds.value = []
    loadDevices()
  } catch (error) {
    ElMessage.error('批量调配失败')
  }
}

onMounted(() => {
  loadFloors()
  loadDevices()
})
</script>

<style scoped>
.grouped-view {
  padding: 10px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.grouped-content {
  padding: 10px;
}

.floor-group {
  margin-bottom: 30px;
}

.floor-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
}

.floor-name {
  font-size: 18px;
  font-weight: bold;
}

.device-count {
  margin-left: auto;
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 14px;
}

.device-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.device-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: all 0.3s;
  cursor: pointer;
}

.device-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
}

.device-card.selected {
  border: 2px solid #409eff;
  background: #f0f5ff;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.device-code {
  font-size: 12px;
  color: #666;
  font-family: monospace;
}

.device-image {
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
}

.device-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.no-image {
  font-size: 48px;
  color: #ccc;
}

.device-info {
  padding: 12px;
}

.device-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.device-type, .device-status {
  margin-bottom: 6px;
}

.device-room {
  font-size: 13px;
  color: #999;
  margin-top: 8px;
}

.card-actions {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #f0f0f0;
}

.empty-state {
  text-align: center;
  padding: 60px;
  color: #999;
}

.empty-state p {
  margin-top: 16px;
  font-size: 16px;
}
</style>
