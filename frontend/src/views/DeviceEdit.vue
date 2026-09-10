<template>
  <div class="device-edit">
    <el-card>
      <div class="card-header">
        <h3>编辑设备档案</h3>
        <el-button @click="goBack">返回列表</el-button>
      </div>

      <el-form :model="deviceForm" label-width="120px" class="device-form" v-loading="loading">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备编号">
              <el-input v-model="deviceForm.deviceCode" disabled />
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

        <SpecFieldsEditor
          ref="specEditorRef"
          :fields="currentFields"
          :model-value="currentSpec"
          :enforce-required="templateEnabled"
          :disabled-hint="templateDisabledHint"
        />

        <el-form-item>
          <el-button @click="goBack">取消</el-button>
          <el-button @click="submitForm" type="primary">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { deviceApi, floorApi, roomApi, specTemplateApi } from '../api'
import SpecFieldsEditor from '../components/SpecFieldsEditor.vue'

const DEFAULT_TYPES = ['电视', '音响', '麦克风', '投影仪', '其他']

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const deviceId = ref(route.params.id)

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
  warrantyEndDate: null
})

const floors = ref([])
const rooms = ref([])
const imageFileList = ref([])
const enabledTemplates = ref([])
const deviceSpecFields = ref([])
const specEditorRef = ref(null)

const enabledMap = computed(() => {
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

// 当前选中类型启用中则用启用模板；否则回退设备自身携带的字段定义（可能来自已停用模板）
const currentFields = computed(() => {
  const enabled = enabledMap.value[deviceForm.deviceType]
  if (enabled && enabled.length > 0) {
    return enabled
  }
  if (deviceForm.deviceType === originalDeviceType.value) {
    return deviceSpecFields.value || []
  }
  return []
})

const templateEnabled = computed(() => {
  const enabled = enabledMap.value[deviceForm.deviceType]
  return !!(enabled && enabled.length > 0)
})

const templateDisabledHint = computed(() => {
  if (currentFields.value.length > 0 && !templateEnabled.value) {
    return '该设备类型的规格模板已停用，历史规格仍可查看与修改，但不再强制必填。'
  }
  return ''
})

// 切换类型后规格值重置（不同类型字段不同）；未切换时回显设备已有规格
const originalDeviceType = ref('')
const EMPTY_SPEC = Object.freeze({})
const currentSpec = computed(() => {
  return deviceForm.deviceType === originalDeviceType.value ? deviceForm.specJson : EMPTY_SPEC
})

const goBack = () => {
  router.push('/device')
}

const onTypeChange = () => {
  // SpecFieldsEditor 监听 fields/modelValue 变化自动重置
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

const loadDevice = async () => {
  loading.value = true
  try {
    const device = await deviceApi.getById(deviceId.value)
    deviceForm.deviceCode = device.deviceCode
    deviceForm.deviceName = device.deviceName
    deviceForm.deviceType = device.deviceType
    originalDeviceType.value = device.deviceType
    deviceForm.brand = device.brand
    deviceForm.model = device.model
    deviceForm.imageUrl = device.imageUrl
    deviceForm.currentFloorId = device.currentFloorId
    deviceForm.currentRoomId = device.currentRoomId
    deviceForm.status = device.status
    deviceForm.purchaseDate = device.purchaseDate
    deviceForm.warrantyEndDate = device.warrantyEndDate
    deviceForm.specJson = device.specJson || {}
    deviceSpecFields.value = device.specFields || []

    if (device.currentFloorId) {
      rooms.value = await roomApi.getByFloor(device.currentFloorId)
    }
  } catch (error) {
    console.error('加载设备信息失败', error)
  } finally {
    loading.value = false
  }
}

const submitForm = async () => {
  if (!deviceForm.deviceName || !deviceForm.deviceType) {
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
    await deviceApi.update(deviceId.value, deviceForm)
    ElMessage.success('设备更新成功')
    router.push('/device')
  } catch (error) {
    ElMessage.error(error.message || '设备更新失败')
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
  loadDevice()
})
</script>

<style scoped>
.device-edit {
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
