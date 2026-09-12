<template>
  <div class="welcome-board-list">
    <el-card class="availability-card">
      <template #header>
        <div class="card-header">
          <span>接待室可接待情况</span>
          <el-select v-model="availabilityFloorId" placeholder="全部楼层" clearable
            size="small" class="floor-filter" @change="loadAvailability">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
        </div>
      </template>
      <div class="availability-grid" v-loading="availabilityLoading">
        <div v-for="room in availability" :key="room.roomId"
          class="room-chip" :class="room.available ? 'is-available' : 'is-busy'">
          <div class="room-chip-header">
            <span class="room-name">{{ room.roomName }}</span>
            <el-tag size="small" :type="room.available ? 'success' : 'danger'">
              {{ room.available ? '可接待' : '不可接待' }}
            </el-tag>
          </div>
          <div v-if="!room.available" class="room-chip-detail">
            <template v-if="!room.roomEnabled">接待室已停用</template>
            <template v-else>
              <span class="board-text" :title="room.boardText">{{ room.boardText }}</span>
              <el-tag v-if="room.overdueRemove" size="small" type="warning" effect="dark" class="overdue-tag">
                待撤
              </el-tag>
            </template>
          </div>
        </div>
      </div>
    </el-card>

    <el-card>
      <div class="toolbar">
        <div class="search-bar">
          <el-select v-model="searchFloorId" placeholder="全部楼层" clearable class="search-select"
            @change="onSearchFloorChange">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
          <el-select v-model="searchRoomId" placeholder="全部接待室" clearable class="search-select"
            :disabled="!searchFloorId">
            <el-option v-for="room in searchRooms" :key="room.id" :label="room.roomName" :value="room.id" />
          </el-select>
          <el-select v-model="searchStatus" placeholder="全部状态" clearable class="search-select">
            <el-option label="待上墙" :value="0" />
            <el-option label="已上墙" :value="1" />
            <el-option label="已撤下" :value="2" />
          </el-select>
          <el-checkbox v-model="overdueOnly" class="overdue-checkbox" @change="searchBoards">仅看待撤</el-checkbox>
          <el-button @click="searchBoards" type="primary">
            <el-icon><Search /></el-icon>
            筛选
          </el-button>
        </div>
        <el-button @click="openCreate" type="primary">
          <el-icon><Plus /></el-icon>
          登记欢迎牌
        </el-button>
      </div>

      <el-table :data="boards" v-loading="loading" :row-class-name="rowClassName">
        <el-table-column prop="boardNo" label="单号" width="180" />
        <el-table-column label="欢迎牌文案" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.boardText }}</span>
            <el-tag v-if="row.overdueRemove" size="small" type="warning" effect="dark" class="overdue-inline">
              待撤
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="接待室" width="160">
          <template #default="{ row }">
            <el-tag size="small" type="success">{{ row.floorName }}</el-tag>
            <span class="room-name">{{ row.roomName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上墙 / 撤下时间" width="250">
          <template #default="{ row }">
            <div class="time-cell">
              <div>{{ formatTime(row.mountTime) }} 上墙</div>
              <div :class="{ 'overdue-time': row.overdueRemove }">
                {{ formatTime(row.plannedRemoveTime) }} 撤下
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="registrar" label="登记人" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" @click="openRemove(row)" size="small" type="warning">
              登记撤下回执
            </el-button>
            <span v-else-if="row.status === 2" class="receipt-hint" :title="row.removeReceipt">
              回执：{{ row.removeReceipt }}
            </span>
            <span v-else class="receipt-hint">未到上墙时间</span>
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

    <el-dialog v-model="createVisible" title="登记接待室欢迎牌" width="600px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="所属楼层" required>
          <el-select v-model="createForm.floorId" placeholder="请选择楼层" class="full-width"
            @change="onCreateFloorChange">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="接待室" required>
          <el-select v-model="createForm.roomId" placeholder="请选择接待室" class="full-width">
            <el-option v-for="room in createRooms" :key="room.id" :label="room.roomName" :value="room.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="欢迎牌文案" required>
          <el-input v-model="createForm.boardText" type="textarea" :rows="2"
            maxlength="300" show-word-limit placeholder="如：热烈欢迎XX考察团莅临指导" />
        </el-form-item>
        <el-form-item label="上墙时间" required>
          <el-date-picker
            v-model="createForm.mountTime"
            type="datetime"
            placeholder="选择上墙时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="full-width"
          />
        </el-form-item>
        <el-form-item label="撤下时间" required>
          <el-date-picker
            v-model="createForm.plannedRemoveTime"
            type="datetime"
            placeholder="选择计划撤下时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="full-width"
          />
        </el-form-item>
        <el-form-item label="登记人" required>
          <el-input v-model="createForm.registrar" placeholder="行政登记人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button @click="confirmCreate" type="primary" :loading="creating">登记</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="removeVisible" title="登记撤下回执" width="520px">
      <div class="remove-info">
        <div>接待室：<b>{{ removeRow?.roomName }}</b></div>
        <div>欢迎牌：<b>{{ removeRow?.boardText }}</b></div>
        <div>计划撤下时间：{{ formatTime(removeRow?.plannedRemoveTime) }}
          <el-tag v-if="removeRow?.overdueRemove" size="small" type="warning" effect="dark" class="overdue-inline">
            已到期待撤
          </el-tag>
        </div>
      </div>
      <el-input v-model="removeReceipt" type="textarea" :rows="3"
        maxlength="500" show-word-limit placeholder="撤下回执必填：说明实物已取下、物料去向等" />
      <div class="remove-operator">
        登记人：
        <el-input v-model="removeOperator" placeholder="撤下登记人" />
      </div>
      <template #footer>
        <el-button @click="removeVisible = false">取消</el-button>
        <el-button @click="confirmRemove" type="warning" :loading="removing">提交回执并撤下</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { welcomeBoardApi, floorApi, roomApi } from '../api'

const loading = ref(false)
const boards = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchFloorId = ref(null)
const searchRoomId = ref(null)
const searchStatus = ref(null)
const overdueOnly = ref(false)
const floors = ref([])
const searchRooms = ref([])

const availabilityLoading = ref(false)
const availability = ref([])
const availabilityFloorId = ref(null)

const createVisible = ref(false)
const creating = ref(false)
const createRooms = ref([])
const createForm = ref(defaultForm())

const removeVisible = ref(false)
const removing = ref(false)
const removeRow = ref(null)
const removeReceipt = ref('')
const removeOperator = ref('管理员')

function defaultForm() {
  return {
    floorId: null,
    roomId: null,
    boardText: '',
    mountTime: '',
    plannedRemoveTime: '',
    registrar: '管理员',
    remark: ''
  }
}

const statusTagType = (status) => ({ 0: 'info', 1: 'danger', 2: 'success' }[status] || 'info')

const rowClassName = ({ row }) => (row.overdueRemove ? 'row-overdue' : '')

const pad = (n) => String(n).padStart(2, '0')

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

let requestSeq = 0

const loadBoards = async () => {
  const seq = ++requestSeq
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchFloorId.value) params.floorId = searchFloorId.value
    if (searchRoomId.value) params.roomId = searchRoomId.value
    if (searchStatus.value !== null && searchStatus.value !== '') params.status = searchStatus.value
    if (overdueOnly.value) params.overdueOnly = true
    const result = await welcomeBoardApi.getPage(params)
    if (seq !== requestSeq) return
    boards.value = result.records || []
    total.value = result.total || 0
  } catch (error) {
    if (seq === requestSeq) ElMessage.error(error.message || '加载欢迎牌列表失败')
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

const loadAvailability = async () => {
  availabilityLoading.value = true
  try {
    const params = {}
    if (availabilityFloorId.value) params.floorId = availabilityFloorId.value
    availability.value = await welcomeBoardApi.getRoomAvailability(params)
  } catch (error) {
    ElMessage.error(error.message || '加载可接待情况失败')
  } finally {
    availabilityLoading.value = false
  }
}

const searchBoards = () => {
  pageNum.value = 1
  loadBoards()
}

const onSearchFloorChange = async (floorId) => {
  searchRoomId.value = null
  searchRooms.value = floorId ? await roomApi.getByFloor(floorId) : []
  searchBoards()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadBoards()
}

const handleCurrentChange = (page) => {
  pageNum.value = page
  loadBoards()
}

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
  if (!form.floorId) return ElMessage.warning('请选择楼层')
  if (!form.roomId) return ElMessage.warning('请选择接待室')
  if (!form.boardText.trim()) return ElMessage.warning('请输入欢迎牌文案')
  if (!form.mountTime) return ElMessage.warning('请选择上墙时间')
  if (!form.plannedRemoveTime) return ElMessage.warning('请选择撤下时间')
  if (!form.registrar.trim()) return ElMessage.warning('请输入登记人')

  creating.value = true
  try {
    await welcomeBoardApi.create({
      boardText: form.boardText.trim(),
      roomId: form.roomId,
      mountTime: form.mountTime,
      plannedRemoveTime: form.plannedRemoveTime,
      registrar: form.registrar.trim(),
      remark: form.remark
    })
    ElMessage.success('欢迎牌排期登记成功')
    createVisible.value = false
    loadBoards()
    loadAvailability()
  } catch (error) {
    ElMessage.error(error.message || '欢迎牌登记失败')
  } finally {
    creating.value = false
  }
}

const openRemove = (row) => {
  removeRow.value = row
  removeReceipt.value = ''
  removeOperator.value = '管理员'
  removeVisible.value = true
}

const confirmRemove = async () => {
  if (!removeReceipt.value.trim()) {
    return ElMessage.warning('撤下必须填写回执，否则不能拿掉欢迎牌')
  }
  removing.value = true
  try {
    await welcomeBoardApi.remove(removeRow.value.id, {
      receipt: removeReceipt.value.trim(),
      operator: removeOperator.value.trim()
    })
    ElMessage.success('撤下回执已登记，欢迎牌已撤下')
    removeVisible.value = false
    loadBoards()
    loadAvailability()
  } catch (error) {
    ElMessage.error(error.message || '撤下登记失败')
  } finally {
    removing.value = false
  }
}

onMounted(async () => {
  loadBoards()
  loadAvailability()
  try {
    floors.value = await floorApi.getAll()
  } catch (error) {
    console.error('加载楼层失败', error)
  }
})
</script>

<style scoped>
.welcome-board-list {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.availability-card :deep(.el-card__body) {
  padding: 12px 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.floor-filter {
  width: 140px;
}

.availability-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.room-chip {
  width: 240px;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  padding: 10px 12px;
  background: #fafafa;
}

.room-chip.is-busy {
  border-color: #f56c6c;
  background: #fef0f0;
}

.room-chip.is-available {
  border-color: #67c23a;
  background: #f0f9eb;
}

.room-chip-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.room-chip-detail {
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 6px;
}

.board-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 170px;
}

.overdue-tag {
  flex-shrink: 0;
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
  align-items: center;
}

.search-select {
  width: 140px;
}

.overdue-checkbox {
  margin-right: 0;
}

.full-width {
  width: 100%;
}

.room-name {
  margin-left: 6px;
}

.time-cell {
  font-size: 13px;
  line-height: 1.5;
}

.overdue-time {
  color: #e6a23c;
  font-weight: bold;
}

.overdue-inline {
  margin-left: 6px;
}

.receipt-hint {
  font-size: 12px;
  color: #909399;
  display: inline-block;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.remove-info {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 12px;
  font-size: 13px;
  line-height: 1.8;
}

.remove-operator {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.remove-operator .el-input {
  flex: 1;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

:deep(.row-overdue) {
  background-color: #fdf6ec;
}
</style>
