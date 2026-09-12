<template>
  <div class="combo-record-list">
    <el-card>
      <div class="toolbar">
        <div class="search-bar">
          <el-select v-model="filters.floorId" placeholder="按楼层过滤" clearable filterable class="search-select"
            @change="onFloorChange">
            <el-option v-for="f in floors" :key="f.id" :label="f.floorName" :value="f.id" />
          </el-select>
          <el-select v-model="filters.roomId" placeholder="按接待室过滤" clearable filterable class="search-select">
            <el-option v-for="r in filteredRooms" :key="r.id" :label="r.roomName" :value="r.id" />
          </el-select>
          <el-select v-model="filters.comboId" placeholder="按组合过滤" clearable filterable class="search-select">
            <el-option v-for="c in combos" :key="c.id" :label="c.comboName" :value="c.id" />
          </el-select>
          <el-button type="primary" @click="searchRecords">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
      </div>

      <el-table :data="records" v-loading="loading">
        <el-table-column prop="recordNo" label="套用单号" width="180" />
        <el-table-column prop="comboName" label="组合" min-width="150" />
        <el-table-column label="楼层/接待室" min-width="160">
          <template #default="{ row }">
            <div>{{ row.floorName || '-' }}</div>
            <div class="sub-text">{{ row.roomName }}（{{ row.roomCode }}）</div>
          </template>
        </el-table-column>
        <el-table-column label="调入/已在/跳过" width="130" align="center">
          <template #default="{ row }">
            <el-tag type="success" size="small">{{ row.appliedCount }}</el-tag>
            <span class="count-sep">/</span>
            <el-tag type="primary" size="small" effect="plain">{{ row.presentCount }}</el-tag>
            <span class="count-sep">/</span>
            <el-tag type="danger" size="small" effect="plain">{{ row.skippedCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="值班员" width="100" />
        <el-table-column prop="applyTime" label="套用时间" width="170">
          <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="goDetail(row.id)">详情</el-button>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { comboApi, floorApi, roomApi } from '../api'

const router = useRouter()

const loading = ref(false)
const records = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const floors = ref([])
const allRooms = ref([])
const combos = ref([])
const filters = ref({ floorId: null, roomId: null, comboId: null })

const filteredRooms = computed(() => {
  if (!filters.value.floorId) return allRooms.value
  return allRooms.value.filter(r => r.floorId === filters.value.floorId)
})

const formatTime = (time) => (time ? new Date(time).toLocaleString('zh-CN') : '')

let requestSeq = 0

const loadRecords = async () => {
  const seq = ++requestSeq
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filters.value.floorId) params.floorId = filters.value.floorId
    if (filters.value.roomId) params.roomId = filters.value.roomId
    if (filters.value.comboId) params.comboId = filters.value.comboId
    const result = await comboApi.getRecords(params)
    if (seq !== requestSeq) return
    records.value = result.records || []
    total.value = result.total || 0
  } catch (error) {
    if (seq === requestSeq) ElMessage.error(error.message || '加载套用记录失败')
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

const searchRecords = () => {
  pageNum.value = 1
  loadRecords()
}

const onFloorChange = () => {
  filters.value.roomId = null
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadRecords()
}

const handleCurrentChange = (page) => {
  pageNum.value = page
  loadRecords()
}

const goDetail = (id) => router.push(`/combo-record/${id}`)

onMounted(async () => {
  loadRecords()
  try {
    const [floorList, roomList, comboList] = await Promise.all([
      floorApi.getAll(),
      roomApi.getAll(),
      comboApi.getAll()
    ])
    floors.value = floorList
    allRooms.value = roomList
    combos.value = comboList
  } catch (error) {
    console.error('加载筛选数据失败', error)
  }
})
</script>

<style scoped>
.combo-record-list {
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

.sub-text {
  font-size: 12px;
  color: #999;
}

.count-sep {
  margin: 0 4px;
  color: #c0c4cc;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
