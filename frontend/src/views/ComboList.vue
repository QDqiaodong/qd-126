<template>
  <div class="combo-list">
    <el-card>
      <div class="toolbar">
        <div class="search-bar">
          <el-select v-model="filters.status" placeholder="状态" clearable class="search-select" @change="loadCombos">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-input v-model="filters.keyword" placeholder="组合名称" clearable class="search-input"
            @keyup.enter="loadCombos" />
          <el-button type="primary" @click="loadCombos">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
        <el-button type="primary" @click="openEdit(null)">
          <el-icon><Plus /></el-icon>
          新建组合
        </el-button>
      </div>

      <el-table :data="filteredCombos" v-loading="loading">
        <el-table-column prop="comboName" label="组合名称" min-width="160" />
        <el-table-column prop="deviceCount" label="设备数" width="90" align="center" />
        <el-table-column label="设备概况" min-width="260">
          <template #default="{ row }">
            <el-tag v-for="t in typeSummary(row.devices)" :key="t.type" size="small" class="type-tag"
              :type="getTypeTagType(t.type)">
              {{ t.type }}×{{ t.count }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" label="创建人" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" :disabled="row.status !== 1" @click="openApply(row)">一键套用</el-button>
            <el-button type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建 / 编辑组合 -->
    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑影音组合' : '新建影音组合'" width="860px"
      @closed="resetEditForm">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="组合名称" required>
          <el-input v-model="editForm.comboName" maxlength="100" placeholder="如：贵宾接待标准影音组合" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="editForm.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="创建人">
              <el-input v-model="editForm.createdBy" maxlength="50" placeholder="管理员" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="组合说明">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit
            placeholder="可选" />
        </el-form-item>
        <el-form-item label="组合设备" required>
          <div class="device-picker">
            <div class="picker-toolbar">
              <el-select v-model="pickerFloorId" placeholder="按楼层过滤" clearable size="small" class="picker-select">
                <el-option v-for="f in floors" :key="f.id" :label="f.floorName" :value="f.id" />
              </el-select>
              <el-select v-model="pickerType" placeholder="按类型过滤" clearable size="small" class="picker-select">
                <el-option v-for="t in deviceTypes" :key="t" :label="t" :value="t" />
              </el-select>
              <el-input v-model="pickerKeyword" placeholder="编号/名称" clearable size="small" class="picker-keyword" />
              <span class="selected-count">已选 {{ editForm.deviceIds.length }} 台</span>
            </div>
            <el-table :data="pickerDevices" height="300" size="small" ref="pickerTable"
              :row-key="row => row.id" @selection-change="onSelectionChange">
              <el-table-column type="selection" width="45" reserve-selection />
              <el-table-column prop="deviceCode" label="编号" width="110" />
              <el-table-column prop="deviceName" label="名称" min-width="120" />
              <el-table-column label="类型" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="getTypeTagType(row.deviceType)">{{ row.deviceType }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="当前位置" min-width="150">
                <template #default="{ row }">
                  <span v-if="row.currentRoomId">{{ row.currentFloorName }} / {{ row.currentRoomName }}</span>
                  <span v-else class="idle-text">{{ row.currentFloorName }} · 空闲备用</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="getStatusTagType(row.status)">{{ row.statusText }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <div class="order-bar">
              <span class="order-label">已选设备（套用按此顺序处理）：</span>
              <el-tag v-for="(id, index) in editForm.deviceIds" :key="id" closable size="small" class="order-tag"
                @close="removeSelected(id)">
                {{ index + 1 }}. {{ deviceNameOf(id) }}
              </el-tag>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCombo">保存组合</el-button>
      </template>
    </el-dialog>

    <!-- 一键套用 -->
    <el-dialog v-model="applyVisible" title="一键套用影音组合" width="640px" @closed="resetApply">
      <el-alert type="info" :closable="false" class="tip"
        title="仅将空闲（未上墙、状态正常、未被进行中活动占用）的组合设备调入所选接待室；已在别的房间、待修、损坏或活动占用的设备会跳过并注明原因。" />
      <el-descriptions :column="1" border size="small" class="apply-combo-info">
        <el-descriptions-item label="组合">{{ applyForm.comboName }}（{{ applyForm.deviceCount }} 台）</el-descriptions-item>
      </el-descriptions>
      <el-form :model="applyForm" label-width="90px" class="apply-form">
        <el-form-item label="接待室" required>
          <el-select v-model="applyForm.roomId" placeholder="选择要调入的接待室" filterable class="full-width">
            <el-option-group v-for="f in floors" :key="f.id" :label="f.floorName">
              <el-option v-for="r in roomsOfFloor(f.id)" :key="r.id"
                :label="`${r.roomName}（${r.roomCode}，现有 ${r.equipmentCount || 0} 台）`" :value="r.id" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="值班员" required>
          <el-input v-model="applyForm.operator" maxlength="50" placeholder="执行套用的值班员" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="applyForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit
            placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="success" :loading="applying" @click="confirmApply">确认套用</el-button>
      </template>
    </el-dialog>

    <!-- 套用结果对照 -->
    <el-dialog v-model="resultVisible" title="套用结果对照" width="900px">
      <template v-if="applyResult">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="套用单号">{{ applyResult.recordNo }}</el-descriptions-item>
          <el-descriptions-item label="接待室">
            {{ applyResult.roomName }}（{{ applyResult.roomCode }}）
          </el-descriptions-item>
          <el-descriptions-item label="套用时间">{{ formatTime(applyResult.applyTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-row :gutter="12" class="summary-row">
          <el-col :span="6">
            <el-statistic title="组合设备" :value="applyResult.requiredCount" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="本次调入" :value="applyResult.appliedCount">
              <template #suffix><span class="stat-text stat-applied">台</span></template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="已在房间" :value="applyResult.presentCount">
              <template #suffix><span class="stat-text">台</span></template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="跳过" :value="applyResult.skippedCount">
              <template #suffix><span class="stat-text stat-skipped">台</span></template>
            </el-statistic>
          </el-col>
        </el-row>

        <el-table :data="applyResult.items" size="small" max-height="280" class="result-table">
          <el-table-column label="#" width="45" type="index" />
          <el-table-column prop="deviceCode" label="编号" width="110" />
          <el-table-column prop="deviceName" label="名称" min-width="120" />
          <el-table-column label="类型" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="getTypeTagType(row.deviceType)">{{ row.deviceType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="结果" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="resultTagType(row.result)">{{ row.resultText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="原因 / 来源" min-width="200">
            <template #default="{ row }">
              <span v-if="row.result === 1">{{ row.fromRoomName ? '自「' + row.fromRoomName + '」所在楼层空闲位调入' : '自楼层空闲备用调入' }}</span>
              <span v-else-if="row.result === 2" class="present-text">套用前已在该接待室，无需重复调入</span>
              <span v-else class="skip-reason">{{ row.skipReason }}</span>
            </template>
          </el-table-column>
        </el-table>

        <el-row :gutter="16" class="snapshot-row">
          <el-col :span="12">
            <div class="snapshot-title">套用前房间设备（{{ applyResult.beforeDevices.length }} 台）</div>
            <div class="snapshot-box before">
              <el-tag v-for="d in applyResult.beforeDevices" :key="d.id" size="small" class="snapshot-tag"
                :type="getTypeTagType(d.deviceType)">{{ d.deviceCode }} {{ d.deviceName }}</el-tag>
              <span v-if="applyResult.beforeDevices.length === 0" class="empty-text">（空房间）</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="snapshot-title">套用后房间设备（{{ applyResult.afterDevices.length }} 台，新增 {{ applyResult.appliedCount }} 台）</div>
            <div class="snapshot-box after">
              <el-tag v-for="d in applyResult.afterDevices" :key="d.id" size="small" class="snapshot-tag"
                :type="getTypeTagType(d.deviceType)">{{ d.deviceCode }} {{ d.deviceName }}</el-tag>
            </div>
          </el-col>
        </el-row>
      </template>
      <template #footer>
        <el-button @click="resultVisible = false">关闭</el-button>
        <el-button type="primary" @click="goRecord">查看套用记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { comboApi, deviceApi, floorApi, roomApi } from '../api'

const router = useRouter()

const loading = ref(false)
const combos = ref([])
const floors = ref([])
const rooms = ref([])
const allDevices = ref([])
const filters = ref({ status: null, keyword: '' })

const deviceTypes = ['电视', '音响', '麦克风', '投影仪', '其他']

const formatTime = (time) => (time ? new Date(time).toLocaleString('zh-CN') : '')
const getTypeTagType = (type) =>
  ({ 电视: 'primary', 音响: 'success', 麦克风: 'info', 投影仪: 'warning', 其他: 'danger' }[type] || 'info')
const getStatusTagType = (status) => ({ 1: 'success', 0: 'danger', 2: 'warning' }[status] || 'info')
const resultTagType = (result) => ({ 1: 'success', 2: 'primary', 3: 'danger' }[result] || 'info')

const filteredCombos = computed(() => {
  const kw = filters.value.keyword?.trim()
  if (!kw) return combos.value
  return combos.value.filter(c => c.comboName && c.comboName.includes(kw))
})

const typeSummary = (devices) => {
  const map = new Map()
  for (const d of devices || []) {
    map.set(d.deviceType, (map.get(d.deviceType) || 0) + 1)
  }
  return Array.from(map, ([type, count]) => ({ type, count }))
}

const roomsOfFloor = (floorId) => rooms.value.filter(r => r.floorId === floorId && r.status === 1)

const loadCombos = async () => {
  loading.value = true
  try {
    combos.value = await comboApi.getAll(filters.value.status ?? undefined)
  } catch (error) {
    ElMessage.error(error.message || '加载组合失败')
  } finally {
    loading.value = false
  }
}

const loadBaseData = async () => {
  try {
    const [floorList, roomList, deviceList] = await Promise.all([
      floorApi.getAll(),
      roomApi.getAll(),
      deviceApi.getAll()
    ])
    floors.value = floorList
    rooms.value = roomList
    allDevices.value = deviceList
  } catch (error) {
    ElMessage.error(error.message || '加载基础数据失败')
  }
}

// ---------------- 新建 / 编辑 ----------------

const editVisible = ref(false)
const saving = ref(false)
const pickerTable = ref(null)
const editForm = reactive({
  id: null,
  comboName: '',
  remark: '',
  status: 1,
  createdBy: '',
  deviceIds: []
})
const pickerFloorId = ref(null)
const pickerType = ref(null)
const pickerKeyword = ref('')

const pickerDevices = computed(() => {
  return allDevices.value.filter(d => {
    if (pickerFloorId.value && d.currentFloorId !== pickerFloorId.value) return false
    if (pickerType.value && d.deviceType !== pickerType.value) return false
    const kw = pickerKeyword.value.trim()
    if (kw && !(d.deviceCode?.includes(kw) || d.deviceName?.includes(kw))) return false
    return true
  })
})

const deviceNameOf = (id) => {
  const d = allDevices.value.find(x => x.id === id)
  return d ? `${d.deviceCode} ${d.deviceName}` : `设备#${id}`
}

const onSelectionChange = (selection) => {
  // 以勾选集为准重排：保留原有顺序，追加新勾选
  const selectedIds = selection.map(s => s.id)
  const kept = editForm.deviceIds.filter(id => selectedIds.includes(id))
  for (const id of selectedIds) {
    if (!kept.includes(id)) kept.push(id)
  }
  editForm.deviceIds = kept
}

const removeSelected = (id) => {
  editForm.deviceIds = editForm.deviceIds.filter(x => x !== id)
  const row = allDevices.value.find(d => d.id === id)
  if (row && pickerDevices.value.some(d => d.id === id)) {
    pickerTable.value?.toggleRowSelection(row, false)
  }
}

const openEdit = async (row) => {
  if (allDevices.value.length === 0) {
    await loadBaseData()
  }
  editVisible.value = true
  if (row) {
    let detail = row
    if (!row.devices) {
      try {
        detail = await comboApi.getById(row.id)
      } catch (error) {
        ElMessage.error(error.message || '加载组合详情失败')
        return
      }
    }
    editForm.id = detail.id
    editForm.comboName = detail.comboName
    editForm.remark = detail.remark || ''
    editForm.status = detail.status
    editForm.createdBy = detail.createdBy || ''
    editForm.deviceIds = (detail.devices || []).map(d => d.deviceId)
  } else {
    editForm.id = null
    editForm.comboName = ''
    editForm.remark = ''
    editForm.status = 1
    editForm.createdBy = '管理员'
    editForm.deviceIds = []
  }
  // 等待表格渲染后恢复勾选态
  setTimeout(() => {
    pickerTable.value?.clearSelection()
    for (const d of allDevices.value) {
      if (editForm.deviceIds.includes(d.id)) {
        pickerTable.value?.toggleRowSelection(d, true)
      }
    }
  }, 0)
}

const resetEditForm = () => {
  editForm.id = null
  editForm.comboName = ''
  editForm.remark = ''
  editForm.status = 1
  editForm.createdBy = ''
  editForm.deviceIds = []
  pickerFloorId.value = null
  pickerType.value = null
  pickerKeyword.value = ''
  pickerTable.value?.clearSelection()
}

const submitCombo = async () => {
  if (!editForm.comboName.trim()) {
    ElMessage.warning('请填写组合名称')
    return
  }
  if (editForm.deviceIds.length === 0) {
    ElMessage.warning('请至少选择一台组合设备')
    return
  }
  const payload = {
    comboName: editForm.comboName.trim(),
    remark: editForm.remark?.trim() || null,
    status: editForm.status,
    createdBy: editForm.createdBy?.trim() || null,
    deviceIds: editForm.deviceIds
  }
  saving.value = true
  try {
    if (editForm.id) {
      await comboApi.update(editForm.id, payload)
      ElMessage.success('组合已更新')
    } else {
      await comboApi.create(payload)
      ElMessage.success('组合已创建')
    }
    editVisible.value = false
    loadCombos()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除组合「${row.comboName}」吗？历史套用记录会保留。`, '提示', { type: 'warning' })
    await comboApi.delete(row.id)
    ElMessage.success('组合已删除')
    loadCombos()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// ---------------- 一键套用 ----------------

const applyVisible = ref(false)
const applying = ref(false)
const resultVisible = ref(false)
const applyResult = ref(null)
const applyForm = reactive({
  comboId: null,
  comboName: '',
  deviceCount: 0,
  roomId: null,
  operator: '',
  remark: ''
})

const openApply = (row) => {
  applyForm.comboId = row.id
  applyForm.comboName = row.comboName
  applyForm.deviceCount = row.deviceCount
  applyForm.roomId = null
  applyForm.operator = '值班员'
  applyForm.remark = ''
  applyVisible.value = true
}

const resetApply = () => {
  applyForm.comboId = null
  applyForm.roomId = null
  applyForm.operator = ''
  applyForm.remark = ''
}

const confirmApply = async () => {
  if (!applyForm.roomId) {
    ElMessage.warning('请选择要调入的接待室')
    return
  }
  if (!applyForm.operator.trim()) {
    ElMessage.warning('请填写值班员')
    return
  }
  applying.value = true
  try {
    applyResult.value = await comboApi.apply(applyForm.comboId, {
      roomId: applyForm.roomId,
      operator: applyForm.operator.trim(),
      remark: applyForm.remark?.trim() || null
    })
    applyVisible.value = false
    resultVisible.value = true
    loadCombos()
  } catch (error) {
    ElMessage.error(error.message || '套用失败')
  } finally {
    applying.value = false
  }
}

const goRecord = () => {
  resultVisible.value = false
  if (applyResult.value?.id) {
    router.push(`/combo-record/${applyResult.value.id}`)
  } else {
    router.push('/combo-record')
  }
}

onMounted(() => {
  loadCombos()
  loadBaseData()
})
</script>

<style scoped>
.combo-list {
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

.search-select {
  width: 130px;
}

.search-input {
  width: 180px;
}

.type-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}

.device-picker {
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px;
}

.picker-toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.picker-select {
  width: 130px;
}

.picker-keyword {
  width: 160px;
}

.selected-count {
  margin-left: auto;
  color: #409eff;
  font-size: 13px;
}

.idle-text {
  color: #67c23a;
}

.order-bar {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
  line-height: 28px;
}

.order-label {
  font-size: 13px;
  color: #666;
  margin-right: 8px;
}

.order-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}

.tip {
  margin-bottom: 16px;
}

.apply-combo-info {
  margin-bottom: 16px;
}

.apply-form {
  margin-top: 8px;
}

.full-width {
  width: 100%;
}

.summary-row {
  margin: 16px 0;
}

.stat-text {
  font-size: 13px;
  color: #909399;
  margin-left: 4px;
}

.stat-applied {
  color: #67c23a;
}

.stat-skipped {
  color: #f56c6c;
}

.result-table {
  margin-bottom: 12px;
}

.skip-reason {
  color: #f56c6c;
}

.present-text {
  color: #409eff;
}

.snapshot-row {
  margin-top: 8px;
}

.snapshot-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

.snapshot-box {
  min-height: 90px;
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
</style>
