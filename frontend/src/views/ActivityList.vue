<template>
  <div class="activity-list">
    <el-card>
      <div class="toolbar">
        <div class="search-bar">
          <el-date-picker
            v-model="searchDate"
            type="date"
            placeholder="按日期筛选"
            value-format="YYYY-MM-DD"
            class="search-date"
            clearable
          />
          <el-select v-model="searchFloorId" placeholder="全部楼层" clearable class="search-select"
            @change="searchActivities">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
          <el-select v-model="searchStatus" placeholder="全部状态" clearable class="search-select">
            <el-option label="待开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已结束" :value="2" />
          </el-select>
          <el-button @click="searchActivities" type="primary">
            <el-icon><Search /></el-icon>
            筛选
          </el-button>
        </div>
        <el-button @click="openCreate" type="primary">
          <el-icon><Plus /></el-icon>
          登记活动占用
        </el-button>
      </div>

      <el-table :data="activities" v-loading="loading">
        <el-table-column prop="activityNo" label="活动编号" width="170" />
        <el-table-column prop="activityName" label="活动名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="接待室" width="170">
          <template #default="{ row }">
            <el-tag size="small" type="success">{{ row.floorName }}</el-tag>
            <span class="room-name">{{ row.roomName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="活动时间" width="210">
          <template #default="{ row }">
            <div class="time-cell">{{ formatRange(row.startTime, row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="占用设备" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.deviceCount }} 台</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="manager" label="负责人" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button @click="goDetail(row.id)" size="small" type="primary">详情</el-button>
            <el-button v-if="row.status !== 2" @click="handleFinish(row)" size="small" type="warning">
              结束释放
            </el-button>
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

    <el-dialog v-model="createVisible" title="登记接待室活动占用" width="640px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="所属楼层" required>
          <el-select v-model="createForm.floorId" placeholder="请选择楼层" class="full-width"
            @change="onCreateFloorChange">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="接待室" required>
          <el-select v-model="createForm.roomId" placeholder="请选择接待室" class="full-width"
            @change="onCreateRoomChange">
            <el-option v-for="room in createRooms" :key="room.id" :label="room.roomName" :value="room.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动名称" required>
          <el-input v-model="createForm.activityName" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="活动时间" required>
          <el-date-picker
            v-model="createForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="full-width"
          />
        </el-form-item>
        <el-form-item label="影音设备" required>
          <el-select
            v-model="createForm.deviceIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择预计使用的影音设备"
            class="full-width"
            :disabled="!createForm.roomId"
          >
            <el-option v-for="device in roomDevices" :key="device.id"
              :label="`${device.deviceName}（${device.deviceCode}）`" :value="device.id">
              <span>{{ device.deviceName }}</span>
              <span class="opt-type">{{ device.deviceType }}</span>
              <span class="opt-code">{{ device.deviceCode }}</span>
            </el-option>
          </el-select>
          <div v-if="createForm.roomId && roomDevices.length === 0" class="form-hint">
            该接待室暂无可调配的正常设备
          </div>
        </el-form-item>
        <el-form-item label="负责人" required>
          <el-input v-model="createForm.manager" placeholder="请输入负责人" />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { activityApi, floorApi, roomApi, deviceApi } from '../api'

const router = useRouter()

const loading = ref(false)
const activities = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchDate = ref(null)
const searchFloorId = ref(null)
const searchStatus = ref(null)
const floors = ref([])

const createVisible = ref(false)
const creating = ref(false)
const createRooms = ref([])
const roomDevices = ref([])
const createForm = ref(defaultForm())

function defaultForm() {
  return {
    floorId: null,
    roomId: null,
    activityName: '',
    timeRange: null,
    deviceIds: [],
    manager: '管理员',
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

const loadActivities = async () => {
  const seq = ++requestSeq
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchDate.value) params.date = searchDate.value
    if (searchFloorId.value) params.floorId = searchFloorId.value
    if (searchStatus.value !== null && searchStatus.value !== '') params.status = searchStatus.value
    const result = await activityApi.getPage(params)
    if (seq !== requestSeq) return
    activities.value = result.records || []
    total.value = result.total || 0
  } catch (error) {
    if (seq === requestSeq) ElMessage.error(error.message || '加载活动列表失败')
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

const searchActivities = () => {
  pageNum.value = 1
  loadActivities()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadActivities()
}

const handleCurrentChange = (page) => {
  pageNum.value = page
  loadActivities()
}

const goDetail = (id) => router.push(`/activity/${id}`)

const openCreate = () => {
  createForm.value = defaultForm()
  createRooms.value = []
  roomDevices.value = []
  createVisible.value = true
}

const onCreateFloorChange = async (floorId) => {
  createForm.value.roomId = null
  createForm.value.deviceIds = []
  roomDevices.value = []
  createRooms.value = floorId ? await roomApi.getByFloor(floorId) : []
}

const onCreateRoomChange = async (roomId) => {
  createForm.value.deviceIds = []
  if (!roomId) {
    roomDevices.value = []
    return
  }
  try {
    const list = await deviceApi.getByRoom(roomId)
    // 仅正常设备可登记投入活动
    roomDevices.value = list.filter(d => d.status === 1)
  } catch (error) {
    roomDevices.value = []
  }
}

const confirmCreate = async () => {
  const form = createForm.value
  if (!form.floorId) return ElMessage.warning('请选择楼层')
  if (!form.roomId) return ElMessage.warning('请选择接待室')
  if (!form.activityName.trim()) return ElMessage.warning('请输入活动名称')
  if (!form.timeRange || form.timeRange.length !== 2) return ElMessage.warning('请选择活动开始与结束时间')
  if (form.deviceIds.length === 0) return ElMessage.warning('请至少选择一台影音设备')
  if (!form.manager.trim()) return ElMessage.warning('请输入负责人')

  creating.value = true
  try {
    const created = await activityApi.create({
      activityName: form.activityName.trim(),
      roomId: form.roomId,
      startTime: form.timeRange[0],
      endTime: form.timeRange[1],
      deviceIds: form.deviceIds,
      manager: form.manager.trim(),
      remark: form.remark
    })
    ElMessage.success('活动登记成功')
    createVisible.value = false
    router.push(`/activity/${created.id}`)
  } catch (error) {
    ElMessage.error(error.message || '活动登记失败')
  } finally {
    creating.value = false
  }
}

const handleFinish = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定提前结束活动「${row.activityName}」吗？结束后将立即释放所占用的影音设备。`,
      '结束活动并释放占用',
      { type: 'warning', confirmButtonText: '结束并释放', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await activityApi.finish(row.id)
    ElMessage.success('活动已结束，占用设备已释放')
    loadActivities()
  } catch (error) {
    ElMessage.error(error.message || '结束活动失败')
  }
}

onMounted(async () => {
  loadActivities()
  try {
    floors.value = await floorApi.getAll()
  } catch (error) {
    console.error('加载楼层失败', error)
  }
})
</script>

<style scoped>
.activity-list {
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

.search-date {
  width: 180px;
}

.search-select {
  width: 140px;
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

.opt-type {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.opt-code {
  float: right;
  color: #c0c4cc;
  font-size: 12px;
}

.form-hint {
  font-size: 12px;
  color: #e6a23c;
  margin-top: 4px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
