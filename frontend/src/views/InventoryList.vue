<template>
  <div class="inventory-list">
    <el-card>
      <div class="toolbar">
        <div class="search-bar">
          <el-input v-model="searchName" placeholder="批次名称/编号" class="search-input" clearable @keyup.enter="searchBatches" />
          <el-select v-model="searchStatus" placeholder="批次状态" clearable class="search-select">
            <el-option label="盘点中" :value="0" />
            <el-option label="已提交" :value="1" />
            <el-option label="已关闭" :value="2" />
          </el-select>
          <el-button @click="searchBatches" type="primary">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
        <el-button @click="openCreate" type="primary">
          <el-icon><Plus /></el-icon>
          新建盘点批次
        </el-button>
      </div>

      <el-table :data="batches" v-loading="loading">
        <el-table-column prop="batchNo" label="批次编号" width="180" />
        <el-table-column prop="batchName" label="批次名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="盘点范围" width="160">
          <template #default="{ row }">
            <el-tag size="small" :type="row.scopeType === 'ROOM' ? 'warning' : 'success'">
              {{ row.scopeTypeText }}
            </el-tag>
            <span class="scope-name">{{ row.scopeType === 'ROOM' ? row.roomName : row.floorName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="负责人" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="盘点进度" width="170">
          <template #default="{ row }">
            <el-progress :percentage="row.progressPercent" :stroke-width="14"
              :status="row.progressPercent === 100 ? 'success' : ''" />
            <div class="progress-text">{{ row.checkedCount }}/{{ row.totalCount }}</div>
          </template>
        </el-table-column>
        <el-table-column label="差异" width="130">
          <template #default="{ row }">
            <div class="diff-summary">
              <el-badge v-if="row.status !== 0 && row.pendingDiffCount > 0" :value="row.pendingDiffCount" class="pending-badge">
                <el-tag type="danger" size="small">待处理</el-tag>
              </el-badge>
              <span class="diff-line">缺{{ row.missingCount }} · 不符{{ row.mismatchCount }} · 修{{ row.repairCount }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="snapshotTime" label="快照时间" width="170">
          <template #default="{ row }">{{ formatTime(row.snapshotTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button @click="goDetail(row.id)" size="small" type="primary">工作台</el-button>
            <el-button v-if="row.status !== 2" @click="handleClose(row)" size="small" type="warning">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pageNum"
          :page-sizes="[10, 20, 50]"
          :page-size="pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>

    <el-dialog v-model="createVisible" title="新建盘点批次" width="520px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="盘点范围">
          <el-radio-group v-model="createForm.scopeType">
            <el-radio label="FLOOR">按楼层</el-radio>
            <el-radio label="ROOM">按接待室</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="楼层">
          <el-select v-model="createForm.floorId" placeholder="请选择楼层" class="full-width"
            @change="onCreateFloorChange">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createForm.scopeType === 'ROOM'" label="接待室">
          <el-select v-model="createForm.roomId" placeholder="请选择接待室" class="full-width">
            <el-option v-for="room in createRooms" :key="room.id"
              :label="room.roomName" :value="room.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="批次名称">
          <el-input v-model="createForm.batchName" placeholder="留空则按范围自动生成" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="createForm.operator" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button @click="confirmCreate" type="primary" :loading="creating">生成快照并创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { inventoryApi, floorApi, roomApi } from '../api'

const router = useRouter()

const loading = ref(false)
const batches = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchName = ref('')
const searchStatus = ref(null)

const createVisible = ref(false)
const creating = ref(false)
const createForm = ref(defaultForm())
const floors = ref([])
const createRooms = ref([])

function defaultForm() {
  return { scopeType: 'FLOOR', floorId: null, roomId: null, batchName: '', operator: '管理员', remark: '' }
}

const statusTagType = (status) => ({ 0: 'primary', 1: 'success', 2: 'info' }[status] || 'info')

const formatTime = (time) => (time ? new Date(time).toLocaleString('zh-CN') : '')

let requestSeq = 0

const loadBatches = async () => {
  const seq = ++requestSeq
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    const name = searchName.value.trim()
    if (name) params.batchName = name
    if (searchStatus.value !== null && searchStatus.value !== '') params.status = searchStatus.value
    const result = await inventoryApi.getBatches(params)
    if (seq !== requestSeq) return
    batches.value = result.records || []
    total.value = result.total || 0
  } catch (error) {
    if (seq === requestSeq) ElMessage.error(error.message || '加载盘点批次失败')
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

const searchBatches = () => {
  pageNum.value = 1
  loadBatches()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadBatches()
}

const handleCurrentChange = (page) => {
  pageNum.value = page
  loadBatches()
}

const goDetail = (id) => router.push(`/inventory/${id}`)

const openCreate = () => {
  createForm.value = defaultForm()
  createRooms.value = []
  createVisible.value = true
}

const onCreateFloorChange = async (floorId) => {
  createForm.value.roomId = null
  createRooms.value = floorId ? await roomApi.getByFloor(floorId) : []
}

const confirmCreate = async () => {
  const form = createForm.value
  if (!form.floorId) {
    ElMessage.warning('请选择楼层')
    return
  }
  if (form.scopeType === 'ROOM' && !form.roomId) {
    ElMessage.warning('请选择接待室')
    return
  }
  if (!form.operator.trim()) {
    ElMessage.warning('请输入负责人')
    return
  }
  creating.value = true
  try {
    const payload = {
      scopeType: form.scopeType,
      floorId: form.floorId,
      roomId: form.scopeType === 'ROOM' ? form.roomId : null,
      batchName: form.batchName,
      operator: form.operator,
      remark: form.remark
    }
    const batch = await inventoryApi.createBatch(payload)
    ElMessage.success(`已生成 ${batch.totalCount} 台设备快照`)
    createVisible.value = false
    router.push(`/inventory/${batch.id}`)
  } catch (error) {
    ElMessage.error(error.message || '创建盘点批次失败')
  } finally {
    creating.value = false
  }
}

const handleClose = async (row) => {
  const tip = row.status === 0
    ? `确定关闭批次「${row.batchName}」吗？关闭后批次结束，未盘设备将回到在库可调配，且不可再修改。`
    : `确定关闭批次「${row.batchName}」吗？关闭后将不可再修改。`
  try {
    await ElMessageBox.confirm(tip, '关闭批次', {
      type: 'warning',
      confirmButtonText: '确定关闭',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await inventoryApi.closeBatch(row.id)
    ElMessage.success('批次已关闭')
    loadBatches()
  } catch (error) {
    ElMessage.error(error.message || '关闭失败')
  }
}

onMounted(async () => {
  loadBatches()
  try {
    floors.value = await floorApi.getAll()
  } catch (error) {
    console.error('加载楼层失败', error)
  }
})
</script>

<style scoped>
.inventory-list {
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
  width: 140px;
}

.full-width {
  width: 100%;
}

.scope-name {
  margin-left: 6px;
}

.progress-text {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.diff-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.diff-line {
  font-size: 12px;
  color: #606266;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
