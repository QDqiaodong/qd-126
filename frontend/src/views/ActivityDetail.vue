<template>
  <div class="activity-detail" v-loading="loading">
    <div class="page-head">
      <el-button @click="goBack" plain>
        <el-icon><ArrowLeft /></el-icon>
        返回活动列表
      </el-button>
    </div>

    <el-card v-if="activity" class="head-card">
      <div class="activity-head">
        <div class="activity-title">
          <h2>{{ activity.activityName }}</h2>
          <el-tag :type="statusTagType(activity.status)">{{ activity.statusText }}</el-tag>
          <el-tag size="small" type="success">{{ activity.floorName }}</el-tag>
          <el-tag size="small" type="warning">{{ activity.roomName }}</el-tag>
        </div>
        <div class="activity-actions">
          <el-popconfirm
            v-if="activity.status !== 2"
            title="确定结束活动并立即释放占用的影音设备吗？"
            confirm-button-text="结束并释放"
            cancel-button-text="取消"
            @confirm="handleFinish">
            <template #reference>
              <el-button type="warning">结束并释放</el-button>
            </template>
          </el-popconfirm>
          <el-tag v-else type="info">活动已结束，设备占用已释放</el-tag>
        </div>
      </div>

      <el-alert
        v-if="activity.status === 1"
        title="活动进行中：占用的影音设备已锁定，不可调配到其他接待室"
        type="error"
        :closable="false"
        show-icon
        class="lock-alert"
      />
      <el-alert
        v-else-if="activity.status === 0"
        title="活动待开始：登记设备已预占，进行中期间将锁定设备调配"
        type="info"
        :closable="false"
        show-icon
        class="lock-alert"
      />
      <el-alert
        v-else
        title="活动已结束，占用已释放，设备可正常调配"
        type="success"
        :closable="false"
        show-icon
        class="lock-alert"
      />

      <el-descriptions :column="3" border size="small" class="meta">
        <el-descriptions-item label="活动编号">{{ activity.activityNo }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ activity.manager }}</el-descriptions-item>
        <el-descriptions-item label="占用设备">{{ activity.deviceCount }} 台</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatTime(activity.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ formatTime(activity.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="释放时间">{{ formatTime(activity.releasedAt) || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="activity.remark" label="备注" :span="3">{{ activity.remark }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="activity.conflicts && activity.conflicts.length"
        :title="`检测到 ${activity.conflicts.length} 条冲突提示`"
        type="warning"
        :closable="false"
        show-icon
        class="conflict-alert"
      >
        <div v-for="(msg, idx) in activity.conflicts" :key="idx" class="conflict-line">· {{ msg }}</div>
      </el-alert>
      <el-alert
        v-else
        title="暂无冲突：时段与占用设备均正常"
        type="success"
        :closable="false"
        show-icon
        class="conflict-alert"
      />
    </el-card>

    <el-card v-if="activity" class="table-card">
      <template #header>
        <span>占用影音设备（{{ devices.length }}）</span>
      </template>
      <el-table :data="devices" row-key="id">
        <el-table-column prop="deviceCode" label="设备编号" width="140" />
        <el-table-column prop="deviceName" label="设备名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="deviceType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.deviceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="设备状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="deviceStatusTagType(row.deviceStatus)">
              {{ row.deviceStatusText || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前台账归属" min-width="200">
          <template #default="{ row }">
            {{ row.currentFloorName || '—' }} / {{ row.currentRoomName || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column label="冲突提示" min-width="220">
          <template #default="{ row }">
            <span v-if="row.conflict" class="conflict-text">
              <el-icon><WarningFilled /></el-icon>
              {{ row.conflict }}
            </span>
            <el-tag v-else size="small" type="success">正常</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { activityApi } from '../api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const activity = ref(null)

const devices = computed(() => activity.value?.devices || [])

const pad = (n) => String(n).padStart(2, '0')

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const statusTagType = (status) => ({ 0: 'info', 1: 'danger', 2: 'success' }[status] || 'info')

const deviceStatusTagType = (status) => ({ 1: 'success', 0: 'danger', 2: 'warning' }[status] || 'info')

const loadDetail = async () => {
  loading.value = true
  try {
    activity.value = await activityApi.getById(route.params.id)
  } catch (error) {
    ElMessage.error(error.message || '加载活动详情失败')
  } finally {
    loading.value = false
  }
}

const handleFinish = async () => {
  try {
    await activityApi.finish(route.params.id)
    ElMessage.success('活动已结束，占用设备已释放')
    loadDetail()
  } catch (error) {
    ElMessage.error(error.message || '结束活动失败')
  }
}

const goBack = () => router.push('/activity')

onMounted(loadDetail)
</script>

<style scoped>
.activity-detail {
  padding: 10px;
}

.page-head {
  margin-bottom: 16px;
}

.activity-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.activity-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.activity-title h2 {
  margin: 0;
}

.lock-alert {
  margin-bottom: 16px;
}

.meta {
  margin-bottom: 4px;
}

.conflict-alert {
  margin-top: 16px;
}

.conflict-line {
  line-height: 1.8;
}

.table-card {
  margin-top: 16px;
}

.conflict-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #e6a23c;
}
</style>
