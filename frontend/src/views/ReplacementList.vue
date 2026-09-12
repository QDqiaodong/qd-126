<template>
  <div class="replacement-list">
    <el-card>
      <div class="toolbar">
        <div class="search-bar">
          <el-select v-model="filters.floorId" placeholder="按楼层过滤" clearable filterable class="search-select"
            @change="onFilterFloorChange">
            <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
          </el-select>
          <el-select v-model="filters.roomId" placeholder="按接待室过滤" clearable filterable class="search-select">
            <el-option v-for="room in filteredRooms" :key="room.id" :label="room.roomName" :value="room.id" />
          </el-select>
          <el-select v-model="filters.processResult" placeholder="按处理结果过滤" clearable class="search-select">
            <el-option label="待维修" :value="1" />
            <el-option label="已修复" :value="2" />
            <el-option label="已报废" :value="3" />
          </el-select>
          <el-button @click="searchReplacements" type="primary">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
        <el-button @click="openCreate" type="danger" :loading="creating">
          <el-icon><Switch /></el-icon>
          接待中故障替换
        </el-button>
      </div>

      <el-table :data="replacements" v-loading="loading">
        <el-table-column prop="replacementNo" label="替换单号" width="180" />
        <el-table-column label="楼层/接待室" width="170">
          <template #default="{ row }">
            <div>{{ row.floorName || '-' }}</div>
            <div class="sub-text">{{ row.roomName || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="卸下（故障）" min-width="180">
          <template #default="{ row }">
            <el-tag type="danger" size="small">{{ row.faultyDeviceType }}</el-tag>
            <span class="device-name">{{ row.faultyDeviceName }}</span>
            <div class="sub-text">{{ row.faultyDeviceCode }}</div>
          </template>
        </el-table-column>
        <el-table-column label="换上（备用）" min-width="180">
          <template #default="{ row }">
            <el-tag type="success" size="small">{{ row.spareDeviceType }}</el-tag>
            <span class="device-name">{{ row.spareDeviceName }}</span>
            <div class="sub-text">{{ row.spareDeviceCode }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="faultPhenomenon" label="故障现象" min-width="180" show-overflow-tooltip />
        <el-table-column prop="operator" label="替换人" width="90" />
        <el-table-column label="处理结果" width="100">
          <template #default="{ row }">
            <el-tag :type="resultTagType(row.processResult)">{{ row.processResultText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="replacementTime" label="替换时间" width="170">
          <template #default="{ row }">{{ formatTime(row.replacementTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button @click="goDetail(row.id)" size="small" type="primary">详情</el-button>
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

    <!-- 接待中故障应急替换 -->
    <el-dialog v-model="createVisible" title="接待中故障应急替换" width="640px" @closed="resetCreateForm">
      <el-alert type="warning" :closable="false" class="tip"
        title="从故障设备同楼层的备用机中选择一台立刻顶上；原设备将转为待维修并卸下，不能再调往其他接待室。" />
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="故障设备" required>
          <el-select v-model="createForm.faultyDeviceId" placeholder="选择接待中发生故障的设备" filterable class="full-width"
            :loading="devicesLoading" @change="onFaultyDeviceChange">
            <el-option-group v-for="group in faultyDeviceGroups" :key="group.floorId" :label="group.floorName">
              <el-option v-for="d in group.devices" :key="d.id"
                :label="`${d.deviceCode} ${d.deviceName}（${d.currentRoomName || '未分配接待室'}）`" :value="d.id" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedFaulty" label="故障位置">
          <el-tag type="info">{{ selectedFaulty.currentFloorName }} / {{ selectedFaulty.currentRoomName || '未分配接待室' }}</el-tag>
          <el-tag class="inline-tag" :type="statusTagType(selectedFaulty.status)">{{ selectedFaulty.statusText }}</el-tag>
        </el-form-item>
        <el-form-item label="备用机" required>
          <el-select v-model="createForm.spareDeviceId" placeholder="仅可选择同楼层、未上墙的正常备用机" class="full-width"
            :disabled="!createForm.faultyDeviceId" :loading="sparesLoading">
            <el-option v-for="s in spares" :key="s.id"
              :label="`${s.deviceCode} ${s.deviceName}${s.sameDeviceType ? '（同类型' + (s.sameType ? '·同型号' : '') + '）' : ''}`"
              :value="s.id">
              <span>{{ s.deviceCode }} {{ s.deviceName }}</span>
              <el-tag v-if="s.sameType" size="small" type="success" class="spare-tag">同型号推荐</el-tag>
              <el-tag v-else-if="s.sameDeviceType" size="small" type="primary" class="spare-tag">同类型</el-tag>
            </el-option>
          </el-select>
          <div v-if="createForm.faultyDeviceId && !sparesLoading && spares.length === 0" class="empty-tip">
            同楼层暂无可调用的备用机（需要状态正常且未分配接待室）
          </div>
        </el-form-item>
        <el-form-item label="故障现象" required>
          <el-input v-model="createForm.faultPhenomenon" type="textarea" :rows="3" maxlength="500" show-word-limit
            placeholder="如：投影画面闪烁、麦克风无声音响杂音等" />
        </el-form-item>
        <el-form-item label="替换人" required>
          <el-input v-model="createForm.operator" placeholder="执行替换的值班员" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button @click="confirmCreate" type="danger" :loading="creating">确认替换</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Switch } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { deviceApi, floorApi, roomApi, replacementApi } from '../api'

const router = useRouter()

const loading = ref(false)
const replacements = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filters = ref({ floorId: null, roomId: null, processResult: null })

const floors = ref([])
const allRooms = ref([])
const filteredRooms = computed(() => {
  if (!filters.value.floorId) return allRooms.value
  return allRooms.value.filter(r => r.floorId === filters.value.floorId)
})

// 请求序号：并发/重复搜索时仅采用最后一次请求结果
let requestSeq = 0

const statusTagType = (status) => ({ 1: 'success', 0: 'danger', 2: 'warning' }[status] || 'info')
const resultTagType = (result) => ({ 1: 'warning', 2: 'success', 3: 'info' }[result] || 'info')

const formatTime = (time) => (time ? new Date(time).toLocaleString('zh-CN') : '')

const loadReplacements = async () => {
  const seq = ++requestSeq
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filters.value.floorId) params.floorId = filters.value.floorId
    if (filters.value.roomId) params.roomId = filters.value.roomId
    if (filters.value.processResult !== null && filters.value.processResult !== '') {
      params.processResult = filters.value.processResult
    }
    const result = await replacementApi.getPage(params)
    if (seq !== requestSeq) return
    replacements.value = result.records || []
    total.value = result.total || 0
  } catch (error) {
    if (seq === requestSeq) {
      ElMessage.error(error.message || '加载替换记录失败')
    }
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

const searchReplacements = () => {
  pageNum.value = 1
  loadReplacements()
}

const onFilterFloorChange = () => {
  filters.value.roomId = null
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadReplacements()
}

const handleCurrentChange = (page) => {
  pageNum.value = page
  loadReplacements()
}

const goDetail = (id) => {
  router.push(`/replacement/${id}`)
}

// ---------------- 新建替换 ----------------

const createVisible = ref(false)
const creating = ref(false)
const devicesLoading = ref(false)
const sparesLoading = ref(false)
const allDevices = ref([])
const spares = ref([])
const createForm = ref({
  faultyDeviceId: null,
  spareDeviceId: null,
  faultPhenomenon: '',
  operator: '',
  remark: ''
})

// 仅在接待室在用的设备可作为故障设备，按楼层分组
const faultyDeviceGroups = computed(() => {
  const groups = new Map()
  for (const floor of floors.value) {
    groups.set(floor.id, { floorId: floor.id, floorName: floor.floorName, devices: [] })
  }
  for (const d of allDevices.value) {
    if (d.currentRoomId && groups.has(d.currentFloorId)) {
      groups.get(d.currentFloorId).devices.push(d)
    }
  }
  return Array.from(groups.values()).filter(g => g.devices.length > 0)
})

const selectedFaulty = computed(() =>
  allDevices.value.find(d => d.id === createForm.value.faultyDeviceId) || null
)

const openCreate = async () => {
  createVisible.value = true
  devicesLoading.value = true
  try {
    allDevices.value = await deviceApi.getAll()
  } catch (error) {
    ElMessage.error(error.message || '加载设备失败')
  } finally {
    devicesLoading.value = false
  }
}

const onFaultyDeviceChange = async (deviceId) => {
  createForm.value.spareDeviceId = null
  spares.value = []
  if (!deviceId) return
  sparesLoading.value = true
  try {
    spares.value = await replacementApi.getSpares(deviceId)
  } catch (error) {
    ElMessage.error(error.message || '加载同楼层备用机失败')
  } finally {
    sparesLoading.value = false
  }
}

const resetCreateForm = () => {
  createForm.value = { faultyDeviceId: null, spareDeviceId: null, faultPhenomenon: '', operator: '', remark: '' }
  spares.value = []
  allDevices.value = []
}

const confirmCreate = async () => {
  if (!createForm.value.faultyDeviceId) {
    ElMessage.warning('请选择发生故障的设备')
    return
  }
  if (!createForm.value.spareDeviceId) {
    ElMessage.warning('请选择一台同楼层备用机')
    return
  }
  if (!createForm.value.faultPhenomenon.trim()) {
    ElMessage.warning('请填写故障现象')
    return
  }
  if (!createForm.value.operator.trim()) {
    ElMessage.warning('请填写替换人')
    return
  }
  creating.value = true
  try {
    const created = await replacementApi.create({
      ...createForm.value,
      faultPhenomenon: createForm.value.faultPhenomenon.trim(),
      operator: createForm.value.operator.trim()
    })
    ElMessage.success('备用机已顶上，原设备已转待维修')
    createVisible.value = false
    pageNum.value = 1
    filters.value = { floorId: null, roomId: null, processResult: null }
    loadReplacements()
    router.push(`/replacement/${created.id}`)
  } catch (error) {
    ElMessage.error(error.message || '替换失败')
  } finally {
    creating.value = false
  }
}

onMounted(async () => {
  loadReplacements()
  try {
    floors.value = await floorApi.getAll()
    allRooms.value = await roomApi.getAll()
  } catch (error) {
    console.error('加载楼层/接待室失败', error)
  }
})
</script>

<style scoped>
.replacement-list {
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
  width: 180px;
}

.device-name {
  margin-left: 6px;
  font-weight: 500;
}

.sub-text {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.full-width {
  width: 100%;
}

.tip {
  margin-bottom: 16px;
}

.inline-tag {
  margin-left: 8px;
}

.spare-tag {
  margin-left: 8px;
}

.empty-tip {
  color: #e6a23c;
  font-size: 12px;
  margin-top: 4px;
}
</style>
