<template>
  <div class="inventory-detail" v-loading="loading">
    <div class="page-head">
      <el-button @click="goBack" plain>
        <el-icon><ArrowLeft /></el-icon>
        返回批次列表
      </el-button>
    </div>

    <el-card v-if="batch" class="head-card">
      <div class="batch-head">
        <div class="batch-title">
          <h2>{{ batch.batchName }}</h2>
          <el-tag :type="statusTagType(batch.status)">{{ batch.statusText }}</el-tag>
          <el-tag size="small" :type="batch.scopeType === 'ROOM' ? 'warning' : 'success'">
            {{ batch.scopeTypeText }}：{{ batch.scopeType === 'ROOM' ? batch.roomName : batch.floorName }}
          </el-tag>
        </div>
        <div class="batch-actions">
          <template v-if="batch.status === 0">
            <el-button type="primary" @click="handleSubmit">提交盘点</el-button>
          </template>
          <template v-else-if="batch.status === 1">
            <el-popconfirm
              title="关闭后将不可再修改，确定关闭该批次吗？"
              confirm-button-text="确定关闭"
              cancel-button-text="取消"
              @confirm="handleClose">
              <template #reference>
                <el-button type="warning">关闭批次</el-button>
              </template>
            </el-popconfirm>
          </template>
          <el-tag v-else type="info">批次已关闭，只读</el-tag>
        </div>
      </div>

      <el-descriptions :column="4" border size="small" class="meta">
        <el-descriptions-item label="批次编号">{{ batch.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ batch.operator }}</el-descriptions-item>
        <el-descriptions-item label="快照时间">{{ formatTime(batch.snapshotTime) }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatTime(batch.submittedAt) || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-row :gutter="16" class="stat-row">
        <el-col :span="5">
          <div class="stat-box stat-total">
            <div class="stat-num">{{ batch.totalCount }}</div>
            <div class="stat-label">快照设备</div>
          </div>
        </el-col>
        <el-col :span="5">
          <div class="stat-box stat-present">
            <div class="stat-num">{{ batch.presentCount }}</div>
            <div class="stat-label">在场</div>
          </div>
        </el-col>
        <el-col :span="5">
          <div class="stat-box stat-missing">
            <div class="stat-num">{{ batch.missingCount }}</div>
            <div class="stat-label">缺失</div>
          </div>
        </el-col>
        <el-col :span="5">
          <div class="stat-box stat-mismatch">
            <div class="stat-num">{{ batch.mismatchCount }}</div>
            <div class="stat-label">位置不符</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-box stat-repair">
            <div class="stat-num">{{ batch.repairCount }}</div>
            <div class="stat-label">待维修</div>
          </div>
        </el-col>
      </el-row>

      <el-progress :percentage="batch.progressPercent" :stroke-width="16"
        :status="batch.progressPercent === 100 ? 'success' : ''" class="head-progress" />
      <div class="progress-hint">
        已盘点 {{ batch.checkedCount }} / {{ batch.totalCount }}
        <template v-if="batch.status !== 0">
          ，待处理差异 <b :class="batch.pendingDiffCount > 0 ? 'danger-text' : ''">{{ batch.pendingDiffCount }}</b> 项
        </template>
      </div>
    </el-card>

    <el-card v-if="batch" class="table-card">
      <el-tabs v-model="activeTab" @tab-change="loadItems">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane :label="`未盘点 (${counts.unchecked})`" name="unchecked" />
        <el-tab-pane :label="`在场 (${batch.presentCount})`" name="1" />
        <el-tab-pane :label="`缺失 (${batch.missingCount})`" name="2" />
        <el-tab-pane :label="`位置不符 (${batch.mismatchCount})`" name="3" />
        <el-tab-pane :label="`待维修 (${batch.repairCount})`" name="4" />
      </el-tabs>

      <el-table :data="items" v-loading="itemsLoading" row-key="id">
        <el-table-column prop="deviceCode" label="设备编号" width="130" />
        <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="deviceType" label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.deviceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="快照归属" width="190">
          <template #default="{ row }">
            <div class="loc">{{ row.snapshotFloorName || '-' }} / {{ row.snapshotRoomName || '-' }}</div>
          </template>
        </el-table-column>

        <!-- 盘点中：现场逐台标记 -->
        <el-table-column v-if="batch.status === 0" label="盘点结果" width="330">
          <template #default="{ row }">
            <el-radio-group v-model="row.checkResult" :disabled="savingId === row.id"
              @change="(val) => onResultChange(row, val)">
              <el-radio-button :value="1">在场</el-radio-button>
              <el-radio-button :value="2">缺失</el-radio-button>
              <el-radio-button :value="3">位置不符</el-radio-button>
              <el-radio-button :value="4">待维修</el-radio-button>
            </el-radio-group>
            <div class="remark-row">
              <el-input v-model="row.remark" size="small" placeholder="备注（可选）"
                :disabled="savingId === row.id || row.checkResult == null"
                @keyup.enter="saveRemark(row)" />
              <el-button size="small" plain :loading="savingId === row.id"
                :disabled="row.checkResult == null" @click="saveRemark(row)">保存备注</el-button>
            </div>
          </template>
        </el-table-column>

        <!-- 提交/关闭：展示盘点结果与处理状态 -->
        <el-table-column v-if="batch.status !== 0" label="盘点结果" width="110">
          <template #default="{ row }">
            <el-tag :type="resultTagType(row.checkResult)">{{ row.checkResultText }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column v-if="batch.status !== 0" label="提交时台账归属" width="200">
          <template #default="{ row }">
            <div class="loc" :class="{ 'mismatch-loc': row.locationMismatch }">
              {{ row.ledgerFloorName || '-' }} / {{ row.ledgerRoomName || '-' }}
              <el-tooltip v-if="row.locationMismatch" content="与快照位置不一致" placement="top">
                <el-icon class="warn-icon"><WarningFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <el-table-column v-if="batch.status !== 0" label="差异/处理" width="240">
          <template #default="{ row }">
            <div v-if="row.checkResult === 1 && !row.locationMismatch" class="ok-text">无差异</div>
            <div v-else class="diff-cell">
              <el-tag v-if="row.processStatus" size="small"
                :type="row.processStatus === 2 ? 'success' : 'danger'">
                {{ row.processStatusText }}
              </el-tag>
              <span v-if="row.processRemark" class="process-remark">{{ row.processRemark }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column v-if="batch.status === 1" label="处理操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.checkResult === 3 && row.processStatus === 1"
              size="small" type="success" @click="openTransfer(row)">去调配</el-button>
            <el-button v-if="(row.checkResult === 2 || row.checkResult === 4) && row.processStatus === 1"
              size="small" type="primary" @click="openResolve(row)">登记处理</el-button>
            <span v-if="row.processStatus === 2" class="ok-text">已处理</span>
          </template>
        </el-table-column>

        <el-table-column v-if="batch.status === 0" label="台账实时提示" width="180">
          <template #default="{ row }">
            <el-tag v-if="row.locationMismatch" type="danger" size="small">台账已移位</el-tag>
            <el-tag v-else type="info" size="small">与快照一致</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 位置不符 -> 调配 -->
    <el-dialog v-model="transferVisible" title="位置不符设备调配" width="520px">
      <el-alert type="warning" :closable="false" class="dialog-alert"
        :title="`设备 ${transferRow?.deviceName}（${transferRow?.deviceCode}）标记为位置不符，调配将更新设备台账并写入流转记录`" />
      <el-form :model="transferForm" label-width="100px">
        <el-form-item label="快照位置">
          <span class="readonly-loc">{{ transferRow?.snapshotFloorName }} / {{ transferRow?.snapshotRoomName }}</span>
        </el-form-item>
        <el-form-item label="目标楼层">
          <el-select v-model="transferForm.toFloorId" placeholder="请选择楼层" @change="onTransferFloorChange">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标接待室">
          <el-select v-model="transferForm.toRoomId" placeholder="请选择接待室">
            <el-option v-for="room in transferRooms" :key="room.id" :label="room.roomName" :value="room.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="流转原因">
          <el-input v-model="transferForm.transferReason" type="textarea" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="transferForm.operator" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="transferForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="confirmTransfer">确认调配</el-button>
      </template>
    </el-dialog>

    <!-- 缺失/待维修 -> 登记处理 -->
    <el-dialog v-model="resolveVisible" title="登记处理结果" width="480px">
      <el-alert type="info" :closable="false" class="dialog-alert"
        :title="`为设备 ${resolveRow?.deviceName}（${resolveRow?.deviceCode}）登记处理结果，缺失设备将保留处理状态`" />
      <el-form label-width="100px">
        <el-form-item label="盘点结果">
          <el-tag :type="resultTagType(resolveRow?.checkResult)">{{ resolveRow?.checkResultText }}</el-tag>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="resolveRemark" type="textarea" :rows="3"
            :placeholder="resolveRow?.checkResult === 2 ? '如：已找回并放回；或报废处理' : '如：已送修，维修中'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="confirmResolve">确认登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { inventoryApi, floorApi, roomApi } from '../api'

const route = useRoute()
const router = useRouter()
const batchId = route.params.id

const loading = ref(false)
const itemsLoading = ref(false)
const batch = ref(null)
const items = ref([])
const activeTab = ref('all')
const savingId = ref(null)
const actionLoading = ref(false)

const floors = ref([])
const transferRooms = ref([])
const transferVisible = ref(false)
const transferRow = ref(null)
const transferForm = ref({ toFloorId: null, toRoomId: null, transferReason: '', operator: '', remark: '' })

const resolveVisible = ref(false)
const resolveRow = ref(null)
const resolveRemark = ref('')

const statusTagType = (status) => ({ 0: 'primary', 1: 'success', 2: 'info' }[status] || 'info')
const resultTagType = (r) => ({ 1: 'success', 2: 'danger', 3: 'warning', 4: 'info' }[r] || 'info')
const formatTime = (time) => (time ? new Date(time).toLocaleString('zh-CN') : '')

const counts = computed(() => {
  const unchecked = (batch.value?.totalCount || 0) - (batch.value?.checkedCount || 0)
  return { unchecked }
})

const goBack = () => router.push('/inventory')

const loadBatch = async () => {
  loading.value = true
  try {
    batch.value = await inventoryApi.getBatch(batchId)
  } catch (error) {
    ElMessage.error(error.message || '加载批次失败')
  } finally {
    loading.value = false
  }
}

const loadItems = async () => {
  itemsLoading.value = true
  try {
    let params = {}
    if (activeTab.value === 'unchecked') {
      // 未盘点无后端过滤值，取全部后前端过滤
      params = {}
    } else if (activeTab.value !== 'all') {
      params.checkResult = Number(activeTab.value)
    }
    let list = await inventoryApi.getItems(batchId, params)
    if (activeTab.value === 'unchecked') {
      list = list.filter(i => i.checkResult == null)
    }
    items.value = list
  } catch (error) {
    ElMessage.error(error.message || '加载盘点明细失败')
  } finally {
    itemsLoading.value = false
  }
}

// 现场逐台标记：选择结果即保存，保存成功后刷新汇总
const onResultChange = async (row, val) => {
  row.checkResult = val
  await saveCheck(row)
}

const saveCheck = async (row) => {
  if (row.checkResult == null) return
  savingId.value = row.id
  try {
    const updated = await inventoryApi.checkItem(batchId, row.id, {
      checkResult: row.checkResult,
      remark: row.remark
    })
    row.updatedAt = updated.updatedAt
    await loadBatch()
  } catch (error) {
    ElMessage.error(error.message || '保存盘点结果失败')
  } finally {
    savingId.value = null
  }
}

const saveRemark = async (row) => {
  if (row.checkResult == null) return
  await saveCheck(row)
}

const handleSubmit = async () => {
  if (batch.value.checkedCount < batch.value.totalCount) {
    ElMessage.warning(`还有 ${batch.value.totalCount - batch.value.checkedCount} 台未盘点，无法提交`)
    return
  }
  try {
    await ElMessageBox.confirm(
      '提交后将冻结当前台账作为差异基准，盘点结果不可再修改。确认提交？',
      '提交盘点',
      { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  loading.value = true
  try {
    const result = await inventoryApi.submitBatch(batchId)
    ElMessage.success(
      `提交成功：在场 ${result.presentCount}，缺失 ${result.missingCount}，位置不符 ${result.mismatchCount}，待维修 ${result.repairCount}`
    )
    activeTab.value = 'all'
    await Promise.all([loadBatch(), loadItems()])
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  } finally {
    loading.value = false
  }
}

const handleClose = async () => {
  try {
    await inventoryApi.closeBatch(batchId)
    ElMessage.success('批次已关闭')
    await loadBatch()
  } catch (error) {
    ElMessage.error(error.message || '关闭失败')
  }
}

const openTransfer = async (row) => {
  transferRow.value = row
  transferForm.value = {
    toFloorId: row.snapshotFloorId || null,
    toRoomId: row.snapshotRoomId || null,
    transferReason: '盘点位置不符调配，批次' + batch.value.batchNo,
    operator: batch.value.operator,
    remark: ''
  }
  transferRooms.value = row.snapshotFloorId ? await ensureRoomsByFloor(row.snapshotFloorId) : []
  transferVisible.value = true
}

const roomCache = ref({})
const ensureRoomsByFloor = async (floorId) => {
  if (!floorId) return []
  if (!roomCache.value[floorId]) {
    roomCache.value[floorId] = await roomApi.getByFloor(floorId)
  }
  return roomCache.value[floorId]
}

const onTransferFloorChange = async (floorId) => {
  transferForm.value.toRoomId = null
  transferRooms.value = floorId ? await ensureRoomsByFloor(floorId) : []
}

const confirmTransfer = async () => {
  if (!transferForm.value.toFloorId || !transferForm.value.toRoomId) {
    ElMessage.warning('请选择目标楼层和接待室')
    return
  }
  actionLoading.value = true
  try {
    await inventoryApi.transferItem(batchId, transferRow.value.id, transferForm.value)
    ElMessage.success('调配成功，差异已处理')
    transferVisible.value = false
    await Promise.all([loadBatch(), loadItems()])
  } catch (error) {
    ElMessage.error(error.message || '调配失败')
  } finally {
    actionLoading.value = false
  }
}

const openResolve = (row) => {
  resolveRow.value = row
  resolveRemark.value = row.processRemark || ''
  resolveVisible.value = true
}

const confirmResolve = async () => {
  actionLoading.value = true
  try {
    await inventoryApi.resolveItem(batchId, resolveRow.value.id, { processRemark: resolveRemark.value })
    ElMessage.success('处理结果已登记')
    resolveVisible.value = false
    await Promise.all([loadBatch(), loadItems()])
  } catch (error) {
    ElMessage.error(error.message || '登记失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadBatch(), loadItems()])
  try {
    floors.value = await floorApi.getAll()
  } catch (error) {
    console.error('加载楼层失败', error)
  }
})
</script>

<style scoped>
.inventory-detail {
  padding: 10px;
}

.page-head {
  margin-bottom: 12px;
}

.head-card {
  margin-bottom: 16px;
}

.batch-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.batch-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.batch-title h2 {
  margin: 0;
  font-size: 20px;
}

.meta {
  margin-bottom: 16px;
}

.stat-row {
  margin-bottom: 14px;
}

.stat-box {
  border-radius: 8px;
  padding: 14px;
  text-align: center;
  color: #fff;
}

.stat-num {
  font-size: 26px;
  font-weight: bold;
}

.stat-label {
  font-size: 13px;
  opacity: 0.9;
  margin-top: 4px;
}

.stat-total { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-present { background: linear-gradient(135deg, #11998e, #38ef7d); }
.stat-missing { background: linear-gradient(135deg, #eb3349, #f45c43); }
.stat-mismatch { background: linear-gradient(135deg, #f7971e, #ffd200); color: #5a3d00; }
.stat-repair { background: linear-gradient(135deg, #4facfe, #00f2fe); }

.head-progress {
  margin-top: 4px;
}

.progress-hint {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}

.danger-text {
  color: #f56c6c;
}

.table-card {
  margin-bottom: 16px;
}

.remark-row {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.loc {
  font-size: 13px;
  color: #606266;
}

.mismatch-loc {
  color: #f56c6c;
  font-weight: bold;
}

.warn-icon {
  color: #e6a23c;
  vertical-align: middle;
}

.ok-text {
  color: #67c23a;
  font-size: 13px;
}

.diff-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.process-remark {
  font-size: 12px;
  color: #909399;
}

.readonly-loc {
  color: #606266;
}

.dialog-alert {
  margin-bottom: 14px;
}
</style>
