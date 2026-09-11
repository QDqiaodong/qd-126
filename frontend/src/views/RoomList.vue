<template>
  <div class="room-list">
    <el-card>
      <div class="toolbar">
        <div class="search-bar">
          <el-select v-model="searchFloorId" placeholder="选择楼层" clearable class="search-select">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
          <el-input v-model="searchRoomName" placeholder="接待室名称" class="search-input" clearable />
          <el-button @click="searchRooms" type="primary">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
        <div class="action-bar">
          <el-button @click="handleAdd" type="primary">
            <el-icon><Plus /></el-icon>
            添加接待室
          </el-button>
        </div>
      </div>

      <el-table :data="rooms" v-loading="loading">
        <el-table-column prop="roomName" label="接待室名称" width="150" />
        <el-table-column prop="roomCode" label="接待室编号" width="120" />
        <el-table-column prop="floorName" label="所属楼层" width="120" />
        <el-table-column prop="capacity" label="容纳人数" width="100" />
        <el-table-column prop="equipmentCount" label="设备数量" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前占用" width="200">
          <template #default="{ row }">
            <el-tag v-if="row.occupied" type="danger" size="small" class="occupy-tag">
              活动占用中
            </el-tag>
            <el-tooltip v-if="row.occupied" :content="`${row.activityName}（负责人：${row.manager || '-'}）`" placement="top">
              <span class="occupy-name">{{ row.activityName }}</span>
            </el-tooltip>
            <el-tag v-else-if="row.status === 1" type="success" size="small" effect="plain">空闲</el-tag>
            <span v-else class="occupy-idle">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button @click="viewDevices(row.id)" size="small">查看设备</el-button>
            <el-button @click="handleEdit(row)" size="small" type="primary">编辑</el-button>
            <el-button @click="handleDelete(row.id)" size="small" type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑接待室' : '添加接待室'" width="450px">
      <el-form :model="roomForm" label-width="100px">
        <el-form-item label="接待室名称" required>
          <el-input v-model="roomForm.roomName" placeholder="请输入接待室名称" />
        </el-form-item>
        <el-form-item label="接待室编号" required>
          <el-input v-model="roomForm.roomCode" placeholder="请输入接待室编号" />
        </el-form-item>
        <el-form-item label="所属楼层" required>
          <el-select v-model="roomForm.floorId" placeholder="请选择楼层">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="容纳人数">
          <el-input-number v-model="roomForm.capacity" :min="1" :max="500" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="roomForm.status">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="submitForm" type="primary">{{ isEdit ? '保存' : '提交' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="devicesDialogVisible" title="设备列表" width="700px">
      <el-table :data="roomDevices" v-loading="devicesLoading">
        <el-table-column prop="deviceCode" label="设备编号" width="120" />
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="deviceType" label="设备类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.deviceType)">{{ row.deviceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column prop="statusText" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="devicesDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { roomApi, floorApi, deviceApi, activityApi } from '../api'

const loading = ref(false)
const devicesLoading = ref(false)
const rooms = ref([])
const floors = ref([])
const searchFloorId = ref(null)
const searchRoomName = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)

const devicesDialogVisible = ref(false)
const roomDevices = ref([])

const roomForm = reactive({
  roomName: '',
  roomCode: '',
  floorId: null,
  capacity: 0,
  equipmentCount: 0,
  status: 1
})

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const getTypeTagType = (type) => {
  const map = { '电视': 'primary', '音响': 'success', '麦克风': 'info', '投影仪': 'warning', '其他': 'danger' }
  return map[type] || 'info'
}

const getStatusTagType = (status) => {
  const map = { 1: 'success', 0: 'danger', 2: 'warning' }
  return map[status] || 'info'
}

const loadRooms = async () => {
  loading.value = true
  try {
    let result = await roomApi.getAll()
    if (searchFloorId.value) {
      result = result.filter(r => r.floorId === searchFloorId.value)
    }
    if (searchRoomName.value) {
      result = result.filter(r => r.roomName && r.roomName.includes(searchRoomName.value))
    }
    // 当前活动占用态：与活动列表同一数据源，刷新后保持一致
    let occupancyMap = new Map()
    try {
      const occupancies = await activityApi.getRoomOccupancies(
        searchFloorId.value ? { floorId: searchFloorId.value } : {})
      occupancyMap = new Map((occupancies || []).map(o => [o.roomId, o]))
    } catch (error) {
      console.error('加载接待室占用状态失败', error)
    }
    const floorMap = new Map(floors.value.map(f => [f.id, f.floorName]))
    rooms.value = result.map(r => {
      const occupancy = occupancyMap.get(r.id)
      return {
        ...r,
        floorName: floorMap.get(r.floorId) || '-',
        occupied: occupancy ? occupancy.occupied : false,
        activityName: occupancy ? occupancy.activityName : null,
        manager: occupancy ? occupancy.manager : null
      }
    })
  } catch (error) {
    console.error('加载接待室列表失败', error)
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

const searchRooms = () => {
  loadRooms()
}

const handleAdd = () => {
  isEdit.value = false
  editId.value = null
  roomForm.roomName = ''
  roomForm.roomCode = ''
  roomForm.floorId = null
  roomForm.capacity = 0
  roomForm.equipmentCount = 0
  roomForm.status = 1
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  roomForm.roomName = row.roomName
  roomForm.roomCode = row.roomCode
  roomForm.floorId = row.floorId
  roomForm.capacity = row.capacity || 0
  roomForm.equipmentCount = row.equipmentCount || 0
  roomForm.status = row.status
  dialogVisible.value = true
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该接待室吗？', '提示', { type: 'warning' })
    await roomApi.delete(id)
    ElMessage.success('删除成功')
    loadRooms()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const viewDevices = async (roomId) => {
  devicesLoading.value = true
  try {
    roomDevices.value = await deviceApi.getByRoom(roomId)
    devicesDialogVisible.value = true
  } catch (error) {
    console.error('加载设备列表失败', error)
  } finally {
    devicesLoading.value = false
  }
}

const submitForm = async () => {
  if (!roomForm.roomName || !roomForm.roomCode || !roomForm.floorId) {
    ElMessage.warning('请填写必填字段')
    return
  }

  try {
    if (isEdit.value) {
      await roomApi.update(editId.value, roomForm)
      ElMessage.success('接待室更新成功')
    } else {
      await roomApi.create(roomForm)
      ElMessage.success('接待室添加成功')
    }
    dialogVisible.value = false
    loadRooms()
  } catch (error) {
    ElMessage.error(isEdit.value ? '接待室更新失败' : '接待室添加失败')
  }
}

onMounted(() => {
  loadFloors()
  loadRooms()
})
</script>

<style scoped>
.room-list {
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

.occupy-tag {
  margin-right: 6px;
}

.occupy-name {
  font-size: 12px;
  color: #f56c6c;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  vertical-align: middle;
}

.occupy-idle {
  color: #c0c4cc;
}
</style>
