<template>
  <div class="replacement-detail" v-loading="loading">
    <el-page-header @back="goBack" title="返回" content="替换记录详情" class="page-header" />

    <template v-if="detail">
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>{{ detail.replacementNo }}</span>
            <el-tag :type="resultTagType(detail.processResult)">{{ detail.processResultText }}</el-tag>
          </div>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="楼层">{{ detail.floorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接待室">{{ detail.roomName || '-' }}（{{ detail.roomCode || '-' }}）</el-descriptions-item>
          <el-descriptions-item label="替换时间">{{ formatTime(detail.replacementTime) }}</el-descriptions-item>
          <el-descriptions-item label="故障现象" :span="2">{{ detail.faultPhenomenon }}</el-descriptions-item>
          <el-descriptions-item label="替换人">{{ detail.operator }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.remark" label="备注" :span="3">{{ detail.remark }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 换上 / 卸下 对照 -->
      <el-card class="info-card">
        <template #header>设备对照（卸下 → 换上）</template>
        <el-row :gutter="20">
          <el-col :span="11">
            <div class="device-panel faulty">
              <div class="panel-title">
                <el-tag type="danger">卸下 · 故障设备</el-tag>
              </div>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="设备编号">{{ detail.faultyDeviceCode }}</el-descriptions-item>
                <el-descriptions-item label="设备名称">{{ detail.faultyDeviceName }}</el-descriptions-item>
                <el-descriptions-item label="设备类型">{{ detail.faultyDeviceType }}</el-descriptions-item>
                <el-descriptions-item label="当前状态">
                  <el-tag :type="deviceStatusTagType(detail.faultyDeviceStatus)">
                    {{ detail.faultyDeviceStatusText }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="当前所在">
                  {{ detail.faultyDeviceRoomName || '楼层备用/送修中（未分配接待室）' }}
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-col>
          <el-col :span="2" class="arrow-col">
            <el-icon size="28" color="#409eff"><Right /></el-icon>
          </el-col>
          <el-col :span="11">
            <div class="device-panel spare">
              <div class="panel-title">
                <el-tag type="success">换上 · 备用设备</el-tag>
              </div>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="设备编号">{{ detail.spareDeviceCode }}</el-descriptions-item>
                <el-descriptions-item label="设备名称">{{ detail.spareDeviceName }}</el-descriptions-item>
                <el-descriptions-item label="设备类型">{{ detail.spareDeviceType }}</el-descriptions-item>
                <el-descriptions-item label="当前状态">
                  <el-tag :type="deviceStatusTagType(detail.spareDeviceStatus)">
                    {{ detail.spareDeviceStatusText }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="当前所在">
                  {{ detail.spareDeviceRoomName || '楼层备用（未分配接待室）' }}
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 维修处理结果登记 -->
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>故障设备处理结果</span>
            <el-button v-if="detail.processResult === 1" type="primary" size="small" @click="resolveVisible = true">
              登记处理结果
            </el-button>
          </div>
        </template>
        <el-descriptions v-if="detail.processResult !== 1" :column="3" border>
          <el-descriptions-item label="处理结果">
            <el-tag :type="resultTagType(detail.processResult)">{{ detail.processResultText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="登记人">{{ detail.processedBy }}</el-descriptions-item>
          <el-descriptions-item label="登记时间">{{ formatTime(detail.processedAt) }}</el-descriptions-item>
          <el-descriptions-item label="处理备注" :span="3">{{ detail.processRemark }}</el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="原设备仍处于待维修状态，修复或报废后请登记处理结果" :image-size="60" />
      </el-card>
    </template>

    <el-dialog v-model="resolveVisible" title="登记故障设备处理结果" width="500px">
      <el-alert type="info" :closable="false" class="tip"
        title="已修复：设备恢复正常并回到楼层备用机；已报废：设备标记为损坏，不可再使用。" />
      <el-form :model="resolveForm" label-width="90px">
        <el-form-item label="处理结果" required>
          <el-radio-group v-model="resolveForm.processResult">
            <el-radio :value="2">已修复</el-radio>
            <el-radio :value="3">已报废</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注" required>
          <el-input v-model="resolveForm.processRemark" type="textarea" :rows="3" maxlength="500" show-word-limit
            placeholder="如：更换主板后测试正常 / 无维修价值作报废" />
        </el-form-item>
        <el-form-item label="登记人" required>
          <el-input v-model="resolveForm.processedBy" placeholder="处理登记人" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" :loading="resolving" @click="confirmResolve">确认登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Right } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { replacementApi } from '../api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref(null)
const resolveVisible = ref(false)
const resolving = ref(false)
const resolveForm = ref({ processResult: 2, processRemark: '', processedBy: '' })

const resultTagType = (result) => ({ 1: 'warning', 2: 'success', 3: 'info' }[result] || 'info')
const deviceStatusTagType = (status) => ({ 1: 'success', 0: 'info', 2: 'warning' }[status] || 'danger')
const formatTime = (time) => (time ? new Date(time).toLocaleString('zh-CN') : '-')

const loadDetail = async () => {
  loading.value = true
  try {
    detail.value = await replacementApi.getById(route.params.id)
  } catch (error) {
    ElMessage.error(error.message || '加载详情失败')
  } finally {
    loading.value = false
  }
}

const confirmResolve = async () => {
  if (!resolveForm.value.processResult) {
    ElMessage.warning('请选择处理结果')
    return
  }
  if (!resolveForm.value.processRemark.trim()) {
    ElMessage.warning('请填写处理备注')
    return
  }
  if (!resolveForm.value.processedBy.trim()) {
    ElMessage.warning('请填写登记人')
    return
  }
  resolving.value = true
  try {
    detail.value = await replacementApi.resolve(route.params.id, {
      ...resolveForm.value,
      processRemark: resolveForm.value.processRemark.trim(),
      processedBy: resolveForm.value.processedBy.trim()
    })
    ElMessage.success('处理结果已登记，设备状态已同步')
    resolveVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '登记失败')
  } finally {
    resolving.value = false
  }
}

const goBack = () => {
  router.push('/replacement')
}

onMounted(loadDetail)
</script>

<style scoped>
.replacement-detail {
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

.device-panel {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  height: 100%;
}

.device-panel.faulty {
  background: #fef0f0;
}

.device-panel.spare {
  background: #f0f9eb;
}

.panel-title {
  margin-bottom: 12px;
}

.arrow-col {
  display: flex;
  align-items: center;
  justify-content: center;
}

.tip {
  margin-bottom: 16px;
}
</style>
