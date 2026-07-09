<template>
  <div class="transfer-list">
    <el-card>
      <div class="toolbar">
        <div class="search-bar">
          <el-input v-model="searchDeviceName" placeholder="设备名称" class="search-input" clearable />
          <el-input v-model="searchOperator" placeholder="操作人" class="search-input" clearable />
          <el-button @click="searchTransfers" type="primary">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
      </div>

      <el-table :data="transfers" v-loading="loading">
        <el-table-column prop="deviceCode" label="设备编号" width="120" />
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column label="流转路径" width="350">
          <template #default="{ row }">
            <div class="transfer-path">
              <div class="path-item">
                <el-icon><OfficeBuilding /></el-icon>
                <span>{{ row.fromFloorName || '无' }}</span>
              </div>
              <el-icon class="arrow"><ArrowRight /></el-icon>
              <div class="path-item">
                <el-icon><OfficeBuilding /></el-icon>
                <span>{{ row.toFloorName || '无' }}</span>
              </div>
            </div>
            <div class="path-detail">
              <span>{{ row.fromRoomName || '无' }} → {{ row.toRoomName || '无' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="transferReason" label="流转原因" width="200" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="transferTime" label="流转时间" width="170">
          <template #default="{ row }">{{ formatTime(row.transferTime) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
      </el-table>

      <div class="pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pageNum"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, OfficeBuilding, ArrowRight } from '@element-plus/icons-vue'
import { deviceApi } from '../api'

const loading = ref(false)
const transfers = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchDeviceName = ref('')
const searchOperator = ref('')

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const loadTransfers = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    const result = await deviceApi.getAllTransferRecords(params)
    transfers.value = result
    total.value = result.length > 0 ? result.length * 10 : 0
  } catch (error) {
    console.error('加载流转记录失败', error)
  } finally {
    loading.value = false
  }
}

const searchTransfers = () => {
  pageNum.value = 1
  loadTransfers()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadTransfers()
}

const handleCurrentChange = (page) => {
  pageNum.value = page
  loadTransfers()
}

onMounted(() => {
  loadTransfers()
})
</script>

<style scoped>
.transfer-list {
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

.transfer-path {
  display: flex;
  align-items: center;
  gap: 10px;
}

.path-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #333;
}

.arrow {
  color: #409eff;
  font-size: 16px;
}

.path-detail {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
