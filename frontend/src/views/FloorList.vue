<template>
  <div class="floor-list">
    <el-card>
      <div class="toolbar">
        <div class="search-bar">
          <el-input v-model="searchBuilding" placeholder="楼栋名称" class="search-input" clearable />
          <el-button @click="searchFloors" type="primary">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
        <div class="action-bar">
          <el-button @click="handleAdd" type="primary">
            <el-icon><Plus /></el-icon>
            添加楼层
          </el-button>
        </div>
      </div>

      <el-table :data="floors" v-loading="loading">
        <el-table-column prop="floorName" label="楼层名称" width="150" />
        <el-table-column prop="floorNumber" label="楼层编号" width="100" />
        <el-table-column prop="buildingName" label="所属楼栋" width="150" />
        <el-table-column prop="deviceCount" label="设备数量" width="100">
          <template #default="{ row }">{{ getDeviceCount(row.id) }}</template>
        </el-table-column>
        <el-table-column prop="roomCount" label="接待室数量" width="120">
          <template #default="{ row }">{{ getRoomCount(row.id) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button @click="handleEdit(row)" size="small" type="primary">编辑</el-button>
            <el-button @click="handleDelete(row.id)" size="small" type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑楼层' : '添加楼层'" width="400px">
      <el-form :model="floorForm" label-width="100px">
        <el-form-item label="楼层名称" required>
          <el-input v-model="floorForm.floorName" placeholder="请输入楼层名称" />
        </el-form-item>
        <el-form-item label="楼层编号" required>
          <el-input-number v-model="floorForm.floorNumber" :min="1" :max="99" />
        </el-form-item>
        <el-form-item label="所属楼栋">
          <el-input v-model="floorForm.buildingName" placeholder="请输入楼栋名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="submitForm" type="primary">{{ isEdit ? '保存' : '提交' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { floorApi, roomApi, deviceApi } from '../api'

const loading = ref(false)
const floors = ref([])
const searchBuilding = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)

const floorForm = reactive({
  floorName: '',
  floorNumber: 1,
  buildingName: ''
})

const roomCountMap = ref({})
const deviceCountMap = ref({})

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const getRoomCount = (floorId) => {
  return roomCountMap.value[floorId] || 0
}

const getDeviceCount = (floorId) => {
  return deviceCountMap.value[floorId] || 0
}

const loadFloors = async () => {
  loading.value = true
  try {
    let result = await floorApi.getAll()
    if (searchBuilding.value) {
      result = result.filter(f => f.buildingName && f.buildingName.includes(searchBuilding.value))
    }
    floors.value = result
    await loadCounts()
  } catch (error) {
    console.error('加载楼层列表失败', error)
  } finally {
    loading.value = false
  }
}

const loadCounts = async () => {
  try {
    const rooms = await roomApi.getAll()
    const devices = await deviceApi.getAll()

    roomCountMap.value = rooms.reduce((map, room) => {
      map[room.floorId] = (map[room.floorId] || 0) + 1
      return map
    }, {})

    deviceCountMap.value = devices.reduce((map, device) => {
      if (device.currentFloorId) {
        map[device.currentFloorId] = (map[device.currentFloorId] || 0) + 1
      }
      return map
    }, {})
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const searchFloors = () => {
  loadFloors()
}

const handleAdd = () => {
  isEdit.value = false
  editId.value = null
  floorForm.floorName = ''
  floorForm.floorNumber = 1
  floorForm.buildingName = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  floorForm.floorName = row.floorName
  floorForm.floorNumber = row.floorNumber
  floorForm.buildingName = row.buildingName
  dialogVisible.value = true
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该楼层吗？', '提示', { type: 'warning' })
    await floorApi.delete(id)
    ElMessage.success('删除成功')
    loadFloors()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const submitForm = async () => {
  if (!floorForm.floorName || !floorForm.floorNumber) {
    ElMessage.warning('请填写必填字段')
    return
  }

  try {
    if (isEdit.value) {
      await floorApi.update(editId.value, floorForm)
      ElMessage.success('楼层更新成功')
    } else {
      await floorApi.create(floorForm)
      ElMessage.success('楼层添加成功')
    }
    dialogVisible.value = false
    loadFloors()
  } catch (error) {
    ElMessage.error(isEdit.value ? '楼层更新失败' : '楼层添加失败')
  }
}

onMounted(() => {
  loadFloors()
})
</script>

<style scoped>
.floor-list {
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

.action-bar {
  display: flex;
  gap: 12px;
}
</style>
