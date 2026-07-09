<template>
  <div class="device-list">
    <el-card>
      <div class="toolbar">
        <div class="search-bar">
          <el-input v-model="searchDeviceName" placeholder="设备名称" class="search-input" clearable />
          <el-select v-model="searchDeviceType" placeholder="设备类型" clearable class="search-select">
            <el-option label="电视" value="电视" />
            <el-option label="音响" value="音响" />
            <el-option label="麦克风" value="麦克风" />
            <el-option label="投影仪" value="投影仪" />
            <el-option label="其他" value="其他" />
          </el-select>
          <el-button @click="searchDevices" type="primary">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
        <div class="action-bar">
          <el-button @click="handleBatchTransfer" type="success" :disabled="selectedIds.length === 0">
            <el-icon><RefreshLeft /></el-icon>
            批量调配
          </el-button>
          <el-button @click="goToAdd" type="primary">
            <el-icon><Plus /></el-icon>
            添加设备
          </el-button>
        </div>
      </div>

      <el-table :data="devices" @selection-change="handleSelectionChange" v-loading="loading">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="deviceCode" label="设备编号" width="120" />
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="deviceType" label="设备类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.deviceType)">{{ row.deviceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="currentFloorName" label="所属楼层" width="100" />
        <el-table-column prop="currentRoomName" label="所属接待室" width="120" />
        <el-table-column prop="statusText" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="建档时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button @click="goToDetail(row.id)" size="small">详情</el-button>
            <el-button @click="goToEdit(row.id)" size="small" type="primary">编辑</el-button>
            <el-button @click="handleTransfer(row)" size="small" type="success">调配</el-button>
            <el-button @click="handleDelete(row.id)" size="small" type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pageNum"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
        />
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
import { Search, Plus, RefreshLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deviceApi, floorApi, roomApi } from '../api'

const router = useRouter()

const loading = ref(false)
const devices = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchDeviceName = ref('')
const searchDeviceType = ref('')

const selectedIds = ref([])

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

const floors = ref([])
const rooms = ref([])
const batchRooms = ref([])

const getTypeTagType = (type) => {
  const map = { '电视': 'primary', '音响': 'success', '麦克风': 'info', '投影仪': 'warning', '其他': 'danger' }
  return map[type] || 'info'
}

const getStatusTagType = (status) => {
  const map = { 1: 'success', 0: 'danger', 2: 'warning' }
  return map[status] || 'info'
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const loadDevices = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (searchDeviceName.value) params.deviceName = searchDeviceName.value
    if (searchDeviceType.value) params.deviceType = searchDeviceType.value
    const result = await deviceApi.getPage(params)
    devices.value = result.records
    total.value = result.total
  } catch (error) {
    console.error('加载设备列表失败', error)
  } finally {
    loading.value = false
  }
}

const searchDevices = () => {
  pageNum.value = 1
  loadDevices()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadDevices()
}

const handleCurrentChange = (page) => {
  pageNum.value = page
  loadDevices()
}

const handleSelectionChange = (val) => {
  selectedIds.value = val.map(item => item.id)
}

const goToAdd = () => {
  router.push('/device/add')
}

const goToEdit = (id) => {
  router.push(`/device/edit/${id}`)
}

const goToDetail = (id) => {
  router.push(`/device/detail/${id}`)
}

const handleTransfer = (row) => {
  transferForm.value = {
    deviceId: row.id,
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

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该设备吗？', '提示', { type: 'warning' })
    await deviceApi.delete(id)
    ElMessage.success('删除成功')
    loadDevices()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
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

const loadFloors = async () => {
  try {
    floors.value = await floorApi.getAll()
  } catch (error) {
    console.error('加载楼层失败', error)
  }
}

onMounted(() => {
  loadDevices()
  loadFloors()
})
</script>

<style scoped>
.device-list {
  padding: 10px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-bar {
  display: flex;
  gap: 12px;
}

.search-input {
  width: 200px;
}

.search-select {
  width: 150px;
}

.action-bar {
  display: flex;
  gap: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
