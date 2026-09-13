<template>
  <div class="booking-list">
    <el-card>
      <div class="toolbar">
        <div class="search-bar">
          <el-select v-model="searchFloorId" placeholder="全部楼层" clearable class="search-select"
            @change="searchBookings">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
          <el-button @click="searchBookings" type="primary">
            <el-icon><Search /></el-icon>
            筛选
          </el-button>
        </div>
        <el-button @click="openCreate" type="primary">
          <el-icon><Plus /></el-icon>
          预约随行翻译
        </el-button>
      </div>

      <el-table :data="bookings" v-loading="loading">
        <el-table-column prop="bookingNo" label="预约单号" width="170" />
        <el-table-column label="接待室" width="180">
          <template #default="{ row }">
            <el-tag size="small" type="success">{{ row.floorName }}</el-tag>
            <span class="room-name">{{ row.roomName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="语种" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="warning" effect="plain">{{ row.language }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="interpreterName" label="随行译员" width="100" />
        <el-table-column label="预约时段" width="220">
          <template #default="{ row }">
            <div class="time-cell">{{ formatRange(row.startTime, row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button @click="openDetail(row)" size="small" type="primary">查看</el-button>
            <el-button v-if="row.status === 0" @click="openEdit(row)" size="small">编辑</el-button>
            <el-button v-if="row.status === 0" @click="handleDelete(row)" size="small" type="danger">
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无翻译预约，未预约翻译的接待室仍可正常登记活动占用" />
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

    <el-dialog v-model="formVisible" :title="isEdit ? '修改翻译预约' : '预约随行翻译'" width="560px">
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
        <el-form-item label="翻译语种" required>
          <el-select v-model="form.language" placeholder="请选择或输入语种" class="full-width"
            filterable allow-create default-first-option>
            <el-option v-for="lang in languageOptions" :key="lang" :label="lang" :value="lang" />
          </el-select>
        </el-form-item>
        <el-form-item label="随行译员" required>
          <el-input v-model="form.interpreterName" placeholder="请输入随行译员姓名" />
        </el-form-item>
        <el-form-item label="预约时段" required>
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="预约开始时间"
            end-placeholder="预约结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="full-width"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="可选" />
        </el-form-item>
      </el-form>
      <el-alert
        title="同一译员在重叠时段只能服务一间接待室，撞车将提示其已约接待室并拦住提交"
        type="info"
        :closable="false"
        show-icon
      />
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button @click="confirmSubmit" type="primary" :loading="submitting">
          {{ isEdit ? '保存' : '预约' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="翻译预约详情" width="520px">
      <el-descriptions :column="1" border v-if="detail">
        <el-descriptions-item label="预约单号">{{ detail.bookingNo }}</el-descriptions-item>
        <el-descriptions-item label="接待室">
          <el-tag size="small" type="success">{{ detail.floorName }}</el-tag>
          <span class="room-name">{{ detail.roomName }}（{{ detail.roomCode }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="翻译语种">{{ detail.language }}</el-descriptions-item>
        <el-descriptions-item label="随行译员">{{ detail.interpreterName }}</el-descriptions-item>
        <el-descriptions-item label="预约时段">
          {{ formatRange(detail.startTime, detail.endTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)">{{ detail.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-alert
        v-if="detail && detail.status !== 0"
        title="活动已开始，该预约只允许查看，不能修改译员或时段"
        type="warning"
        :closable="false"
        show-icon
        class="detail-alert"
      />
      <template #footer>
        <el-button @click="detailVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { interpreterBookingApi, floorApi, roomApi } from '../api'

const languageOptions = ['英语', '日语', '韩语', '法语', '德语', '俄语', '西班牙语', '葡萄牙语', '阿拉伯语']

const loading = ref(false)
const bookings = ref([])
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

const detailVisible = ref(false)
const detail = ref(null)

function defaultForm() {
  return {
    floorId: null,
    roomId: null,
    language: '',
    interpreterName: '',
    timeRange: null,
    remark: ''
  }
}

const statusTagType = (status) => ({ 0: 'info', 1: 'danger', 2: 'success' }[status] || 'info')

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

const loadBookings = async () => {
  const seq = ++requestSeq
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchFloorId.value) params.floorId = searchFloorId.value
    const result = await interpreterBookingApi.getPage(params)
    if (seq !== requestSeq) return
    bookings.value = result.records || []
    total.value = result.total || 0
  } catch (error) {
    if (seq === requestSeq) ElMessage.error(error.message || '加载翻译预约失败')
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

const searchBookings = () => {
  pageNum.value = 1
  loadBookings()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadBookings()
}

const handleCurrentChange = (page) => {
  pageNum.value = page
  loadBookings()
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
    language: row.language,
    interpreterName: row.interpreterName,
    timeRange: [row.startTime, row.endTime],
    remark: row.remark || ''
  }
  formRooms.value = row.floorId ? await roomApi.getByFloor(row.floorId) : []
  formVisible.value = true
}

const onFormFloorChange = async (floorId) => {
  form.value.roomId = null
  formRooms.value = floorId ? await roomApi.getByFloor(floorId) : []
}

// 译员时段撞车业务码：与后端 GlobalExceptionHandler 约定
const INTERPRETER_CONFLICT_CODE = 461

const showSubmitError = (error, fallback) => {
  if (error && error.code === INTERPRETER_CONFLICT_CODE) {
    // 译员撞车：弹出已约接待室并拦住提交（弹窗保留，不关闭表单）
    ElMessageBox.alert(error.message, '译员时段撞车', {
      type: 'warning',
      confirmButtonText: '知道了'
    }).catch(() => {})
    return
  }
  ElMessage.error((error && error.message) || fallback)
}

const confirmSubmit = async () => {
  const f = form.value
  if (!f.floorId) return ElMessage.warning('请选择楼层')
  if (!f.roomId) return ElMessage.warning('请选择接待室')
  if (!f.language || !String(f.language).trim()) return ElMessage.warning('请选择翻译语种')
  if (!f.interpreterName.trim()) return ElMessage.warning('请填写随行译员')
  if (!f.timeRange || f.timeRange.length !== 2) return ElMessage.warning('请选择预约起止时间')

  submitting.value = true
  try {
    const payload = {
      roomId: f.roomId,
      language: String(f.language).trim(),
      interpreterName: f.interpreterName.trim(),
      startTime: f.timeRange[0],
      endTime: f.timeRange[1],
      remark: f.remark
    }
    if (isEdit.value) {
      await interpreterBookingApi.update(editId.value, payload)
      ElMessage.success('翻译预约已更新')
    } else {
      await interpreterBookingApi.create(payload)
      ElMessage.success('随行翻译预约成功')
    }
    formVisible.value = false
    loadBookings()
  } catch (error) {
    showSubmitError(error, isEdit.value ? '翻译预约更新失败' : '随行翻译预约失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除接待室「${row.roomName}」${row.language}翻译预约（译员：${row.interpreterName}，${formatRange(row.startTime, row.endTime)}）吗？`,
      '删除翻译预约',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await interpreterBookingApi.delete(row.id)
    ElMessage.success('翻译预约已删除')
    loadBookings()
  } catch (error) {
    ElMessage.error(error.message || '删除翻译预约失败')
  }
}

const openDetail = async (row) => {
  detail.value = null
  detailVisible.value = true
  try {
    // 详情走单条查询，拿到服务端按当前时间推进后的最新状态
    detail.value = await interpreterBookingApi.getById(row.id)
  } catch (error) {
    detailVisible.value = false
    ElMessage.error(error.message || '加载预约详情失败')
  }
}

onMounted(async () => {
  loadBookings()
  try {
    floors.value = await floorApi.getAll()
  } catch (error) {
    console.error('加载楼层失败', error)
  }
})
</script>

<style scoped>
.booking-list {
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

.detail-alert {
  margin-top: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
