<template>
  <div class="warranty-view">
    <el-card>
      <div class="toolbar">
        <div class="search-bar">
          <el-select
            v-model="searchFloorId"
            placeholder="全部楼层"
            clearable
            class="search-select"
            @change="onFloorChange"
          >
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
          <el-select
            v-model="searchRoomId"
            placeholder="全部接待室"
            clearable
            class="search-select"
            @change="loadOverview"
          >
            <el-option v-for="room in rooms" :key="room.id" :label="room.roomName" :value="room.id" />
          </el-select>
          <el-button @click="loadOverview" type="primary">
            <el-icon><Search /></el-icon>
            筛选
          </el-button>
          <el-button @click="loadOverview">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
        <div class="legend">
          <span class="legend-item"><span class="legend-dot expired"></span>已过期</span>
          <span class="legend-item"><span class="legend-dot expiring"></span>不足{{ overview.expiringSoonDays || 15 }}天</span>
          <span class="legend-item"><span class="legend-dot normal"></span>正常</span>
        </div>
      </div>

      <div v-loading="loading" class="overview-content">
        <div v-for="group in overview.groups" :key="group.floorId || 'unassigned'" class="floor-group">
          <div class="floor-header">
            <el-icon><OfficeBuilding /></el-icon>
            <span class="floor-name">{{ group.floorName }}</span>
            <span class="device-count">{{ group.devices.length }}台设备</span>
          </div>
          <el-table
            :data="group.devices"
            :row-class-name="rowClassName"
            @row-click="goToDetail"
            class="warranty-table"
          >
            <el-table-column prop="deviceCode" label="设备编号" width="130" />
            <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="deviceType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="getTypeTagType(row.deviceType)">{{ row.deviceType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="接待室" width="140">
              <template #default="{ row }">{{ row.currentRoomName || '未分配' }}</template>
            </el-table-column>
            <el-table-column label="保修截止日期" width="150">
              <template #default="{ row }">
                <el-tag :type="warrantyTagType(row.warrantyStatus)" effect="dark" size="small">
                  {{ formatDate(row.warrantyEndDate) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="剩余天数" width="120">
              <template #default="{ row }">
                <span :class="['days-remaining', row.warrantyStatus === 'EXPIRED' ? 'text-expired' : '']">
                  {{ formatDaysRemaining(row) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="保修状态" width="100">
              <template #default="{ row }">
                <el-tag :type="warrantyTagType(row.warrantyStatus)" size="small">
                  {{ row.warrantyStatusText }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="overview.noWarrantyDevices && overview.noWarrantyDevices.length > 0" class="floor-group">
          <div class="floor-header undated-header">
            <el-icon><Warning /></el-icon>
            <span class="floor-name">未设置保修截止日期</span>
            <span class="device-count">{{ overview.noWarrantyDevices.length }}台设备</span>
          </div>
          <el-table
            :data="overview.noWarrantyDevices"
            @row-click="goToDetail"
            class="warranty-table undated-table"
          >
            <el-table-column prop="deviceCode" label="设备编号" width="130" />
            <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="deviceType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="getTypeTagType(row.deviceType)">{{ row.deviceType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="所在位置" width="180">
              <template #default="{ row }">
                {{ row.currentFloorName || '未分配' }} / {{ row.currentRoomName || '未分配' }}
              </template>
            </el-table-column>
            <el-table-column label="保修截止日期" width="150">
              <template #default>
                <el-tag type="info" size="small">未设置</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="保修状态" width="100">
              <template #default="{ row }">
                <el-tag type="info" size="small">{{ row.warrantyStatusText }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="isEmpty" class="empty-state">
          <el-icon size="64" color="#ccc"><Monitor /></el-icon>
          <p>暂无设备数据</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { OfficeBuilding, Monitor, Refresh, Search, Warning } from '@element-plus/icons-vue'
import { deviceApi, floorApi, roomApi } from '../api'

const router = useRouter()

const loading = ref(false)
const overview = ref({ groups: [], noWarrantyDevices: [], expiringSoonDays: 15 })
const floors = ref([])
const rooms = ref([])
const searchFloorId = ref(null)
const searchRoomId = ref(null)

const isEmpty = computed(() => {
  const groups = overview.value.groups || []
  const undated = overview.value.noWarrantyDevices || []
  return groups.length === 0 && undated.length === 0
})

const getTypeTagType = (type) => {
  const map = { '电视': 'primary', '音响': 'success', '麦克风': 'info', '投影仪': 'warning', '其他': 'danger' }
  return map[type] || 'info'
}

const warrantyTagType = (status) => {
  const map = { EXPIRED: 'danger', EXPIRING: 'warning', NORMAL: 'success', NONE: 'info' }
  return map[status] || 'info'
}

const rowClassName = ({ row }) => {
  if (row.warrantyStatus === 'EXPIRED') return 'warranty-row-expired'
  if (row.warrantyStatus === 'EXPIRING') return 'warranty-row-expiring'
  return ''
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

const formatDaysRemaining = (row) => {
  if (row.daysRemaining === null || row.daysRemaining === undefined) return '-'
  if (row.daysRemaining < 0) return `已过期${-row.daysRemaining}天`
  return `剩${row.daysRemaining}天`
}

const loadOverview = async () => {
  loading.value = true
  try {
    const params = {}
    if (searchFloorId.value) params.floorId = searchFloorId.value
    if (searchRoomId.value) params.roomId = searchRoomId.value
    overview.value = await deviceApi.getWarrantyOverview(params)
  } catch (error) {
    console.error('加载保修概览失败', error)
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

const loadRooms = async () => {
  try {
    rooms.value = await roomApi.getAll()
  } catch (error) {
    console.error('加载接待室失败', error)
  }
}

const onFloorChange = async (floorId) => {
  searchRoomId.value = null
  if (floorId) {
    try {
      rooms.value = await roomApi.getByFloor(floorId)
    } catch (error) {
      console.error('加载接待室失败', error)
    }
  } else {
    loadRooms()
  }
  loadOverview()
}

const goToDetail = (row) => {
  router.push(`/device/detail/${row.id}`)
}

onMounted(() => {
  loadFloors()
  loadRooms()
  loadOverview()
})
</script>

<style scoped>
.warranty-view {
  padding: 10px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-select {
  width: 160px;
}

.legend {
  display: flex;
  gap: 16px;
  align-items: center;
  font-size: 13px;
  color: #666;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  display: inline-block;
}

.legend-dot.expired {
  background: #f56c6c;
}

.legend-dot.expiring {
  background: #e6a23c;
}

.legend-dot.normal {
  background: #67c23a;
}

.floor-group {
  margin-bottom: 24px;
}

.floor-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
}

.floor-header.undated-header {
  background: linear-gradient(135deg, #909399 0%, #606266 100%);
}

.floor-name {
  font-size: 16px;
  font-weight: bold;
}

.device-count {
  margin-left: auto;
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
}

.warranty-table {
  width: 100%;
  cursor: pointer;
}

.days-remaining.text-expired {
  color: #f56c6c;
  font-weight: bold;
}

.warranty-table :deep(.warranty-row-expired) {
  background: #fef0f0;
}

.warranty-table :deep(.warranty-row-expired:hover > td) {
  background: #fde2e2 !important;
}

.warranty-table :deep(.warranty-row-expiring) {
  background: #fdf6ec;
}

.warranty-table :deep(.warranty-row-expiring:hover > td) {
  background: #faecd8 !important;
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
