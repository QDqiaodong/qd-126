<template>
  <div class="device-add">
    <el-card>
      <div class="card-header">
        <h3>添加设备档案</h3>
        <el-button @click="goBack">返回列表</el-button>
      </div>

      <el-form :model="deviceForm" label-width="120px" class="device-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备编号" required>
              <el-input v-model="deviceForm.deviceCode" placeholder="请输入设备编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" required>
              <el-input v-model="deviceForm.deviceName" placeholder="请输入设备名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备类型" required>
              <el-select
                v-model="deviceForm.deviceType"
                placeholder="请选择设备类型"
                filterable
                allow-create
                @change="onTypeChange"
              >
                <el-option v-for="t in deviceTypeOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="deviceForm.brand" placeholder="请输入品牌" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="型号">
              <el-input v-model="deviceForm.model" placeholder="请输入型号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备状态">
              <el-select v-model="deviceForm.status" placeholder="请选择状态">
                <el-option label="正常" :value="1" />
                <el-option label="损坏" :value="0" />
                <el-option label="待维修" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购日期">
              <el-date-picker v-model="deviceForm.purchaseDate" type="date" placeholder="选择采购日期" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保修截止日期">
              <el-date-picker v-model="deviceForm.warrantyEndDate" type="date" placeholder="选择保修截止日期" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属楼层">
              <el-select v-model="deviceForm.currentFloorId" @change="onFloorChange" placeholder="请选择楼层">
                <el-option v-for="floor in floors" :key="floor.id" :label="floor.floorName" :value="floor.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属接待室">
              <el-select v-model="deviceForm.currentRoomId" placeholder="请选择接待室">
                <el-option v-for="room in rooms" :key="room.id" :label="room.roomName" :value="room.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="设备图片">
          <el-upload
            class="image-upload"
            action="/api/device/upload"
            :before-upload="handleBeforeUpload"
            :on-success="handleUploadSuccess"
            :file-list="imageFileList"
            accept="image/*"
            :limit="1"
          >
            <el-button type="primary">上传图片</el-button>
          </el-upload>
          <div v-if="deviceForm.imageUrl" class="preview-image">
            <img :src="deviceForm.imageUrl" alt="设备图片" />
          </div>
        </el-form-item>

        <SpecFieldsEditor ref="specEditorRef" :fields="currentFields" :model-value="emptySpec" />

        <el-form-item label="创建人">
          <el-input v-model="deviceForm.createdBy" placeholder="请输入创建人" />
        </el-form-item>

        <el-form-item>
          <el-button @click="goBack">取消</el-button>
          <el-button @click="submitForm" type="primary">提交</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { deviceApi, floorApi, roomApi, specTemplateApi } from '../api'
import SpecFieldsEditor from '../components/SpecFieldsEditor.vue'

const DEFAULT_TYPES = ['电视', '音响', '麦克风', '投影仪', '其他']

const router = useRouter()

const deviceForm = reactive({
  deviceCode: '',
  deviceName: '',
  deviceType: '',
  brand: '',
  model: '',
  specJson: {},
  imageUrl: '',
  currentFloorId: null,
  currentRoomId: null,
  status: 1,
  purchaseDate: null,
  warrantyEndDate: null,
  createdBy: ''
})

const floors = ref([])
const rooms = ref([])
const imageFileList = ref([])
const enabledTemplates = ref([])
const specEditorRef = ref(null)
const emptySpec = Object.freeze({})

const templateMap = computed(() => {
  const map = {}
  enabledTemplates.value.forEach(t => {
    map[t.deviceType] = t.fields || []
  })
  return map
})

const deviceTypeOptions = computed(() => {
  const types = [...DEFAULT_TYPES]
  enabledTemplates.value.forEach(t => {
    if (!types.includes(t.deviceType)) {
      types.push(t.deviceType)
    }
  })
  return types
})

const currentFields = computed(() => templateMap.value[deviceForm.deviceType] || [])

const goBack = () => {
  router.push('/device')
}

const onTypeChange = () => {
  // 字段定义切换后，SpecFieldsEditor 内部会按新模板重置规格值
}

const onFloorChange = async (floorId) => {
  if (floorId) {
    rooms.value = await roomApi.getByFloor(floorId)
  } else {
    rooms.value = []
  }
  deviceForm.currentRoomId = null
}

const handleBeforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('请上传图片文件')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过2MB')
    return false
  }
  return true
}

const handleUploadSuccess = (response) => {
  deviceForm.imageUrl = response.data.url
}

const submitForm = async () => {
  if (!deviceForm.deviceCode || !deviceForm.deviceName || !deviceForm.deviceType) {
    ElMessage.warning('请填写必填字段')
    return
  }

  const specError = specEditorRef.value ? specEditorRef.value.validate() : ''
  if (specError) {
    ElMessage.warning(specError)
    return
  }
  deviceForm.specJson = specEditorRef.value ? specEditorRef.value.getSpecJson() : {}

  try {
    await deviceApi.create(deviceForm)
    ElMessage.success('设备添加成功')
    router.push('/device')
  } catch (error) {
    ElMessage.error(error.message || '设备添加失败')
  }
}

const loadFloors = async () => {
  try {
    floors.value = await floorApi.getAll()
  } catch (error) {
    console.error('加载楼层失败', error)
  }
}

const loadTemplates = async () => {
  try {
    enabledTemplates.value = await specTemplateApi.getEnabled()
  } catch (error) {
    console.error('加载规格模板失败', error)
  }
}

onMounted(() => {
  loadFloors()
  loadTemplates()
})
</script>

<style scoped>
.device-add {
  padding: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
}

.device-form {
  max-width: 800px;
}

.image-upload {
  margin-bottom: 10px;
}

.preview-image {
  max-width: 300px;
}

.preview-image img {
  max-width: 100%;
  border-radius: 4px;
}
</style>
