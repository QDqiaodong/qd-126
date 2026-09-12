<template>
  <div class="combo-record-detail" v-loading="loading">
    <el-page-header @back="goBack" title="返回" content="套用记录详情" class="page-header" />

    <template v-if="detail">
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>{{ detail.recordNo }}</span>
            <div class="header-stats">
              <el-tag type="success">调入 {{ detail.appliedCount }}</el-tag>
              <el-tag type="primary" effect="plain">已在房间 {{ detail.presentCount }}</el-tag>
              <el-tag type="danger" effect="plain">跳过 {{ detail.skippedCount }}</el-tag>
            </div>
          </div>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="组合">{{ detail.comboName }}</el-descriptions-item>
          <el-descriptions-item label="楼层">{{ detail.floorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接待室">{{ detail.roomName }}（{{ detail.roomCode }}）</el-descriptions-item>
          <el-descriptions-item label="值班员">{{ detail.operator }}</el-descriptions-item>
          <el-descriptions-item label="套用时间">{{ formatTime(detail.applyTime) }}</el-descriptions-item>
          <el-descriptions-item label="组合设备总数">{{ detail.requiredCount }} 台</el-descriptions-item>
          <el-descriptions-item v-if="detail.remark" label="备注" :span="3">{{ detail.remark }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 套用前后对照 -->
      <el-card class="info-card">
        <template #header>套用前后房间设备对照</template>
        <el-row :gutter="16">
          <el-col :span="12">
            <div class="snapshot-title">套用前（{{ detail.beforeDevices.length }} 台）</div>
            <div class="snapshot-box before">
              <el-tag v-for="d in detail.beforeDevices" :key="d.id" size="small" class="snapshot-tag"
                :type="getTypeTagType(d.deviceType)">{{ d.deviceCode }} {{ d.deviceName }}</el-tag>
              <span v-if="detail.beforeDevices.length === 0" class="empty-text">（空房间）</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="snapshot-title">
              套用后（{{ detail.afterDevices.length }} 台）
              <el-tag type="success" size="small" class="new-badge">新增 {{ detail.appliedCount }} 台</el-tag>
            </div>
            <div class="snapshot-box after">
              <el-tag v-for="d in detail.afterDevices" :key="d.id" size="small" class="snapshot-tag"
                :type="getTypeTagType(d.deviceType)">
                {{ d.deviceCode }} {{ d.deviceName }}
              </el-tag>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 逐台结果 -->
      <el-card class="info-card">
        <template #header>组合设备逐台处理结果</template>
        <el-table :data="detail.items" size="small">
          <el-table-column label="#" type="index" width="45" />
          <el-table-column prop="deviceCode" label="编号" width="110" />
          <el-table-column prop="deviceName" label="名称" min-width="120" />
          <el-table-column label="类型" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="getTypeTagType(row.deviceType)">{{ row.deviceType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="套用结果" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="resultTagType(row.result)">{{ row.resultText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="原因 / 来源" min-width="220">
            <template #default="{ row }">
              <span v-if="row.result === 1">
                {{ row.fromRoomName ? '自「' + row.fromRoomName + '」所在楼层空闲位' : '自楼层空闲备用' }}调入
              </span>
              <span v-else-if="row.result === 2" class="present-text">套用前已在该接待室</span>
              <span v-else class="skip-reason">{{ row.skipReason }}</span>
            </template>
          </el-table-column>
          <el-table-column label="实时台账（刷新）" min-width="200">
            <template #default="{ row }">
              <template v-if="row.exists">
                <div>
                  <el-tag size="small" :type="getStatusTagType(row.liveStatus)">{{ row.liveStatusText }}</el-tag>
                  <span class="live-location">{{ row.liveRoomName || '未分配接待室（空闲备用/送修）' }}</span>
                </div>
                <div v-if="row.result !== 3" class="live-check">
                  <el-icon v-if="row.liveInTargetRoom" color="#67c23a"><CircleCheckFilled /></el-icon>
                  <el-icon v-else color="#f56c6c"><WarningFilled /></el-icon>
                  <span :class="row.liveInTargetRoom ? 'ok-text' : 'warn-text'">
                    {{ row.liveInTargetRoom ? '当前仍在目标接待室，与记录一致' : '当前已不在目标接待室，事后被重新调配' }}
                  </span>
                </div>
              </template>
              <el-tag v-else type="info" size="small">设备已从台账删除</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheckFilled, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { comboApi } from '../api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref(null)

const formatTime = (time) => (time ? new Date(time).toLocaleString('zh-CN') : '-')
const getTypeTagType = (type) =>
  ({ 电视: 'primary', 音响: 'success', 麦克风: 'info', 投影仪: 'warning', 其他: 'danger' }[type] || 'info')
const getStatusTagType = (status) => ({ 1: 'success', 0: 'danger', 2: 'warning' }[status] || 'info')
const resultTagType = (result) => ({ 1: 'success', 2: 'primary', 3: 'danger' }[result] || 'info')

const loadDetail = async () => {
  loading.value = true
  try {
    detail.value = await comboApi.getRecordById(route.params.id)
  } catch (error) {
    ElMessage.error(error.message || '加载套用记录失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => router.push('/combo-record')

onMounted(loadDetail)
</script>

<style scoped>
.combo-record-detail {
  padding: 10px;
}

.page-header {
  margin-bottom: 16px;
}

.info-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-stats {
  display: flex;
  gap: 8px;
}

.snapshot-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

.new-badge {
  margin-left: 8px;
}

.snapshot-box {
  min-height: 100px;
  border-radius: 6px;
  padding: 10px;
  border: 1px solid #ebeef5;
}

.snapshot-box.before {
  background: #f4f4f5;
}

.snapshot-box.after {
  background: #f0f9eb;
  border-color: #c2e7b0;
}

.snapshot-tag {
  margin-right: 6px;
  margin-bottom: 6px;
}

.empty-text {
  color: #909399;
  font-size: 13px;
}

.skip-reason {
  color: #f56c6c;
}

.present-text {
  color: #409eff;
}

.live-location {
  margin-left: 6px;
  font-size: 12px;
  color: #606266;
}

.live-check {
  margin-top: 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.ok-text {
  color: #67c23a;
}

.warn-text {
  color: #f56c6c;
}
</style>
