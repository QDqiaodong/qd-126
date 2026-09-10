<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-blue">
            <el-icon><Monitor /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalDevices }}</div>
            <div class="stat-label">设备总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-green">
            <el-icon><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalFloors }}</div>
            <div class="stat-label">楼层数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-purple">
            <el-icon><HomeFilled /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalRooms }}</div>
            <div class="stat-label">接待室数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-orange">
            <el-icon><RefreshLeft /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalTransfers }}</div>
            <div class="stat-label">本月流转</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <el-card title="设备状态分布">
          <div class="status-chart">
            <div v-for="item in statusData" :key="item.label" class="status-item">
              <div class="status-bar">
                <div class="status-fill" :style="{ width: item.percent + '%', background: item.color }"></div>
              </div>
              <div class="status-info">
                <span class="status-label">{{ item.label }}</span>
                <span class="status-count">{{ item.count }}台</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card title="最近流转记录">
          <div class="recent-transfers">
            <div v-for="item in recentTransfers" :key="item.id" class="transfer-item">
              <div class="transfer-device">{{ item.deviceName }}</div>
              <div class="transfer-info">
                <span>{{ item.fromFloorName || '-' }}</span>
                <el-icon><ArrowRight /></el-icon>
                <span>{{ item.toFloorName || '-' }}</span>
              </div>
              <div class="transfer-time">{{ formatTime(item.transferTime) }}</div>
            </div>
            <div v-if="recentTransfers.length === 0" class="empty-tip">暂无流转记录</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card title="设备类型统计">
          <div class="type-stats">
            <div v-for="item in typeData" :key="item.type" class="type-item">
              <div class="type-circle" :style="{ background: item.color }">
                {{ item.count }}
              </div>
              <div class="type-label">{{ item.type }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card title="楼层设备分布">
          <div class="floor-stats">
            <div v-for="item in floorData" :key="item.floorName" class="floor-item">
              <div class="floor-bar-wrapper">
                <div class="floor-bar" :style="{ height: item.percent + '%' }"></div>
              </div>
              <div class="floor-label">{{ item.floorName }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Monitor, OfficeBuilding, HomeFilled, RefreshLeft, ArrowRight } from '@element-plus/icons-vue'
import { deviceApi, floorApi, roomApi } from '../api'

const stats = ref({
  totalDevices: 0,
  totalFloors: 0,
  totalRooms: 0,
  totalTransfers: 0
})

const statusData = ref([
  { label: '正常', count: 0, percent: 0, color: '#67c23a' },
  { label: '损坏', count: 0, percent: 0, color: '#f56c6c' },
  { label: '待维修', count: 0, percent: 0, color: '#e6a23c' }
])

const recentTransfers = ref([])

const typeData = ref([
  { type: '电视', count: 0, color: '#409eff' },
  { type: '音响', count: 0, color: '#67c23a' },
  { type: '麦克风', count: 0, color: '#909399' },
  { type: '投影仪', count: 0, color: '#e6a23c' },
  { type: '其他', count: 0, color: '#f56c6c' }
])

const floorData = ref([])

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const loadStats = async () => {
  try {
    const [devices, floors, rooms, transfers] = await Promise.all([
      deviceApi.getAll(),
      floorApi.getAll(),
      roomApi.getAll(),
      deviceApi.getAllTransferRecords({ pageNum: 1, pageSize: 5 })
    ])
    stats.value = {
      totalDevices: devices.length,
      totalFloors: floors.length,
      totalRooms: rooms.length,
      totalTransfers: transfers.total || 0
    }

    const statusCount = { 1: 0, 0: 0, 2: 0 }
    devices.forEach(d => {
      statusCount[d.status] = (statusCount[d.status] || 0) + 1
    })
    const total = devices.length || 1
    statusData.value = [
      { label: '正常', count: statusCount[1], percent: Math.round(statusCount[1] / total * 100), color: '#67c23a' },
      { label: '损坏', count: statusCount[0], percent: Math.round(statusCount[0] / total * 100), color: '#f56c6c' },
      { label: '待维修', count: statusCount[2], percent: Math.round(statusCount[2] / total * 100), color: '#e6a23c' }
    ]

    recentTransfers.value = transfers.records || []

    const typeCount = {}
    devices.forEach(d => {
      typeCount[d.deviceType] = (typeCount[d.deviceType] || 0) + 1
    })
    typeData.value = typeData.value.map(item => ({
      ...item,
      count: typeCount[item.type] || 0
    }))

    const grouped = await deviceApi.getGrouped()
    const maxCount = Math.max(...Object.values(grouped).map(arr => arr.length), 1)
    floorData.value = Object.entries(grouped).map(([floorName, devices]) => ({
      floorName,
      count: devices.length,
      percent: Math.round(devices.length / maxCount * 100)
    }))
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.dashboard {
  padding: 10px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  height: 120px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 28px;
  color: #fff;
}

.stat-icon.bg-blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.bg-green {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.stat-icon.bg-purple {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #666;
}

.stat-icon.bg-orange {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
  color: #666;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
}

.status-chart {
  padding: 10px 0;
}

.status-item {
  margin-bottom: 16px;
}

.status-bar {
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.status-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s;
}

.status-info {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 14px;
}

.status-label {
  color: #666;
}

.status-count {
  color: #333;
  font-weight: bold;
}

.recent-transfers {
  max-height: 250px;
  overflow-y: auto;
}

.transfer-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.transfer-item:last-child {
  border-bottom: none;
}

.transfer-device {
  font-weight: bold;
  color: #333;
  font-size: 14px;
}

.transfer-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 13px;
  color: #666;
}

.transfer-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 20px;
}

.type-stats {
  display: flex;
  justify-content: space-around;
  padding: 20px 0;
}

.type-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.type-circle {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  font-size: 16px;
}

.type-label {
  margin-top: 8px;
  font-size: 13px;
  color: #666;
}

.floor-stats {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  height: 150px;
  padding: 20px 0;
}

.floor-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.floor-bar-wrapper {
  height: 100px;
  width: 30px;
  background: #f0f0f0;
  border-radius: 15px;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.floor-bar {
  width: 100%;
  background: linear-gradient(180deg, #409eff 0%, #66b1ff 100%);
  border-radius: 15px;
  transition: height 0.3s;
}

.floor-label {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
  writing-mode: vertical-rl;
}
</style>
