<template>
  <div class="quiet-list">
    <el-card>
      <div class="toolbar">
        <div class="search-bar">
          <el-select v-model="searchFloorId" placeholder="全部楼层" clearable class="search-select"
            @change="searchQuietPeriods">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
          <el-button @click="searchQuietPeriods" type="primary">
            <el-icon><Search /></el-icon>
            筛选
          </el-button>
        </div>
        <el-button @click="openCreate" type="primary">
          <el-icon><Plus /></el-icon>
          登记静音时段
        </el-button>
      </div>

      <el-table :data="quietPeriods" v-loading="loading">
        <el-table-column prop="quietNo" label="静音单号" width="170" />
        <el-table-column label="接待室" width="180">
          <template #default="{ row }">
            <el-tag size="small" type="success">{{ row.floorName }}</el-tag>
            <span class="room-name">{{ row.roomName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="静音时段" width="220">
          <template #default="{ row }">
            <div class="time-cell">{{ formatRange(row.startTime, row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="静音原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column label="登记时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button @click="openEdit(row)" size="small" type="primary">编辑</el-button>
            <el-button @click="handleDelete(row)" size="small" type="danger">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无静音时段，各接待室均可正常登记活动占用" />
        </template>
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

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑静音时段' : '登记静音时段'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="所属楼层" required>
          <el-select v-model="form.floorId" placeholder="请选择楼层" class="full-width"
            @change="onFormFloorChange">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="接待室" required>
          <el-select v-model="form.roomId" placeholder="请选择接待室" class="full-width">
            <el-option v-for="room in formRooms" :key="room.id" :label="room.roomName" :value="room.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="静音时段" required>
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="静音开始时间"
            end-placeholder="静音结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="full-width"
          />
        </el-form-item>
        <el-form-item label="静音原因" required>
          <el-input v-model="form.reason" placeholder="如：接待室设备检修 / 重要会议保障" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="可选" />
        </el-form-item>
      </el-form>
      <el-alert
        title="静音时段内，该接待室新建或修改活动占用将被拦截并提示此原因；已开始的活动不受影响"
        type="info"
        :closable="false"
        show-icon
      />
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button @click="confirmSubmit" type="primary" :loading="submitting">
          {{ isEdit ? '保存' : '登记' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { quietPeriodApi, floorApi, roomApi } from '../api'

const loading = ref(false)
const quietPeriods = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchFloorId = ref(null)
const floors = ref([])

const formVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)
const formRooms = ref([])
const form = ref(defaultForm())

function defaultForm() {
  return {
    floorId: null,
    roomId: null,
    timeRange: null,
    reason: '',
    remark: ''
  }
}

const pad = (n) => String(n).padStart(2, '0')

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const formatRange = (start, end) => {
  if (!start || !end) return ''
  return `${formatTime(start)} ~ ${formatTime(end)}`
}

let requestSeq = 0

const loadQuietPeriods = async () => {
  const seq = ++requestSeq
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchFloorId.value) params.floorId = searchFloorId.value
    const result = await quietPeriodApi.getPage(params)
    if (seq !== requestSeq) return
    quietPeriods.value = result.records || []
    total.value = result.total || 0
  } catch (error) {
    if (seq === requestSeq) ElMessage.error(error.message || '加载静音时段失败')
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

const searchQuietPeriods = () => {
  pageNum.value = 1
  loadQuietPeriods()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadQuietPeriods()
}

const handleCurrentChange = (page) => {
  pageNum.value = page
  loadQuietPeriods()
}

const openCreate = () => {
  isEdit.value = false
  editId.value = null
  form.value = defaultForm()
  formRooms.value = []
  formVisible.value = true
}

const openEdit = async (row) => {
  isEdit.value = true
  editId.value = row.id
  form.value = {
    floorId: row.floorId,
    roomId: row.roomId,
    timeRange: [row.startTime, row.endTime],
    reason: row.reason,
    remark: row.remark || ''
  }
  formRooms.value = row.floorId ? await roomApi.getByFloor(row.floorId) : []
  formVisible.value = true
}

const onFormFloorChange = async (floorId) => {
  form.value.roomId = null
  formRooms.value = floorId ? await roomApi.getByFloor(floorId) : []
}

const confirmSubmit = async () => {
  const f = form.value
  if (!f.floorId) return ElMessage.warning('请选择楼层')
  if (!f.roomId) return ElMessage.warning('请选择接待室')
  if (!f.timeRange || f.timeRange.length !== 2) return ElMessage.warning('请选择静音起止时间')
  if (!f.reason.trim()) return ElMessage.warning('请输入静音原因')

  submitting.value = true
  try {
    const payload = {
      roomId: f.roomId,
      startTime: f.timeRange[0],
      endTime: f.timeRange[1],
      reason: f.reason.trim(),
      remark: f.remark
    }
    if (isEdit.value) {
      await quietPeriodApi.update(editId.value, payload)
      ElMessage.success('静音时段已更新')
    } else {
      await quietPeriodApi.create(payload)
      ElMessage.success('静音时段登记成功')
    }
    formVisible.value = false
    loadQuietPeriods()
  } catch (error) {
    ElMessage.error(error.message || (isEdit.value ? '静音时段更新失败' : '静音时段登记失败'))
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除接待室「${row.roomName}」的静音时段（${formatRange(row.startTime, row.endTime)}）吗？删除后该时段可正常登记活动占用。`,
      '删除静音时段',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await quietPeriodApi.delete(row.id)
    ElMessage.success('静音时段已删除')
    loadQuietPeriods()
  } catch (error) {
    ElMessage.error(error.message || '删除静音时段失败')
  }
}

onMounted(async () => {
  loadQuietPeriods()
  try {
    floors.value = await floorApi.getAll()
  } catch (error) {
    console.error('加载楼层失败', error)
  }
})
</script>

<style scoped>
.quiet-list {
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
  width: 150px;
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

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
