<template>
  <div class="template-list">
    <el-card>
      <div class="toolbar">
        <h3 class="page-title">设备类型规格模板</h3>
        <el-button @click="handleAdd" type="primary">
          <el-icon><Plus /></el-icon>
          新增模板
        </el-button>
      </div>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="停用模板不会清除历史设备已保存的规格；停用后添加/编辑设备不再按该模板强制校验，详情仍按历史模板渲染。"
        class="tip"
      />

      <el-table :data="templates" v-loading="loading" border>
        <el-table-column prop="deviceType" label="设备类型" width="140">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.deviceType)">{{ row.deviceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规格字段" min-width="320">
          <template #default="{ row }">
            <el-tag
              v-for="field in row.fields"
              :key="field.id"
              size="small"
              :type="field.required ? 'danger' : 'info'"
              class="field-tag"
            >
              {{ field.fieldLabel }}{{ field.required ? '*' : '' }}（{{ typeTextMap[field.fieldType] || field.fieldType }}）
            </el-tag>
            <span v-if="!row.fields || row.fields.length === 0" class="no-field">暂无字段</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button @click="handleEdit(row)" size="small" type="primary">编辑</el-button>
            <el-button
              @click="handleToggleStatus(row)"
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑规格模板' : '新增规格模板'"
      width="900px"
      top="6vh"
      @closed="resetForm"
    >
      <el-form :model="form" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备类型" required>
              <el-select
                v-model="form.deviceType"
                placeholder="选择或输入设备类型"
                filterable
                allow-create
                :disabled="isEdit"
                class="full-width"
              >
                <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch
                v-model="form.status"
                :active-value="1"
                :inactive-value="0"
                active-text="启用"
                inactive-text="停用"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">规格字段</el-divider>

        <div class="fields-editor">
          <el-button @click="addField" type="primary" size="small">
            <el-icon><Plus /></el-icon>
            添加字段
          </el-button>

          <el-table :data="form.fields" border class="fields-table">
            <el-table-column label="字段名称" min-width="130">
              <template #default="{ row }">
                <el-input v-model="row.fieldLabel" placeholder="如：屏幕尺寸" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="字段键" min-width="130">
              <template #default="{ row }">
                <el-input v-model="row.fieldKey" placeholder="如：screenSize" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="字段类型" width="130">
              <template #default="{ row }">
                <el-select v-model="row.fieldType" size="small">
                  <el-option v-for="t in fieldTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="必填" width="70" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.required" />
              </template>
            </el-table-column>
            <el-table-column label="排序" width="90">
              <template #default="{ row }">
                <el-input-number v-model="row.sortOrder" :min="0" :controls="false" size="small" class="full-width" />
              </template>
            </el-table-column>
            <el-table-column label="选项（下拉选择用，逗号分隔）" min-width="200">
              <template #default="{ row }">
                <el-input
                  v-if="row.fieldType === 'select'"
                  :model-value="(row.options || []).join(',')"
                  @update:model-value="val => row.options = parseOptions(val)"
                  placeholder="选项1,选项2"
                  size="small"
                />
                <span v-else class="option-placeholder">—</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button @click="removeField($index)" type="danger" size="small" link>删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="form.fields.length === 0" class="no-field-tip">
            暂未配置规格字段，保存后该类型设备将使用自由键值对录入规格。
          </div>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="submitForm" type="primary">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { specTemplateApi } from '../api'

const DEFAULT_TYPES = ['电视', '音响', '麦克风', '投影仪', '其他']

const typeTextMap = {
  text: '单行文本',
  textarea: '多行文本',
  number: '数字',
  select: '下拉选择',
  date: '日期',
  boolean: '开关'
}

const fieldTypeOptions = Object.entries(typeTextMap).map(([value, label]) => ({ value, label }))

const loading = ref(false)
const templates = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)

const form = reactive({
  deviceType: '',
  status: 1,
  fields: []
})

const typeOptions = computed(() => {
  const used = new Set(templates.value.map(t => t.deviceType))
  return DEFAULT_TYPES.filter(t => !used.has(t)).concat(
    templates.value
      .map(t => t.deviceType)
      .filter(t => !DEFAULT_TYPES.includes(t))
  )
})

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const getTypeTagType = (type) => {
  const map = { 电视: 'primary', 音响: 'success', 麦克风: 'info', 投影仪: 'warning', 其他: 'danger' }
  return map[type] || ''
}

const parseOptions = (val) => {
  return String(val || '')
    .split(',')
    .map(o => o.trim())
    .filter(Boolean)
}

const loadTemplates = async () => {
  loading.value = true
  try {
    templates.value = await specTemplateApi.getAll()
  } catch (error) {
    ElMessage.error(error.message || '加载规格模板失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.deviceType = ''
  form.status = 1
  form.fields = []
  editId.value = null
}

const addField = () => {
  form.fields.push({
    fieldKey: '',
    fieldLabel: '',
    fieldType: 'text',
    required: false,
    options: [],
    sortOrder: form.fields.length
  })
}

const removeField = (index) => {
  form.fields.splice(index, 1)
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  form.deviceType = row.deviceType
  form.status = row.status
  form.fields = (row.fields || []).map(f => ({
    id: f.id,
    fieldKey: f.fieldKey,
    fieldLabel: f.fieldLabel,
    fieldType: f.fieldType,
    required: !!f.required,
    options: [...(f.options || [])],
    sortOrder: f.sortOrder ?? 0
  }))
  dialogVisible.value = true
}

const handleToggleStatus = async (row) => {
  const nextStatus = row.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}【${row.deviceType}】的规格模板吗？${nextStatus === 0 ? '停用后历史设备规格不会被清除。' : ''}`,
      '提示',
      { type: 'warning' }
    )
    await specTemplateApi.updateStatus(row.id, nextStatus)
    ElMessage.success(`已${action}`)
    loadTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const validateFields = () => {
  const keys = new Set()
  for (const field of form.fields) {
    if (!field.fieldLabel.trim() || !field.fieldKey.trim()) {
      return '存在字段名称或字段键为空的字段'
    }
    if (!/^[A-Za-z][A-Za-z0-9_]*$/.test(field.fieldKey.trim())) {
      return `字段键【${field.fieldKey}】需以字母开头，仅允许字母、数字和下划线`
    }
    if (keys.has(field.fieldKey.trim())) {
      return `字段键【${field.fieldKey}】重复`
    }
    keys.add(field.fieldKey.trim())
    if (field.fieldType === 'select' && (!field.options || field.options.length === 0)) {
      return `下拉选择字段【${field.fieldLabel}】必须配置选项`
    }
  }
  return ''
}

const submitForm = async () => {
  if (!form.deviceType || !form.deviceType.trim()) {
    ElMessage.warning('请选择或输入设备类型')
    return
  }
  const fieldError = validateFields()
  if (fieldError) {
    ElMessage.warning(fieldError)
    return
  }

  const payload = {
    deviceType: form.deviceType.trim(),
    status: form.status,
    fields: form.fields.map((f, index) => ({
      id: f.id,
      fieldKey: f.fieldKey.trim(),
      fieldLabel: f.fieldLabel.trim(),
      fieldType: f.fieldType,
      required: f.required,
      options: f.fieldType === 'select' ? f.options : [],
      sortOrder: f.sortOrder ?? index
    }))
  }

  try {
    if (isEdit.value) {
      await specTemplateApi.update(editId.value, payload)
      ElMessage.success('模板更新成功')
    } else {
      await specTemplateApi.create(payload)
      ElMessage.success('模板创建成功')
    }
    dialogVisible.value = false
    loadTemplates()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

onMounted(() => {
  loadTemplates()
})
</script>

<style scoped>
.template-list {
  padding: 10px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
}

.tip {
  margin-bottom: 16px;
}

.field-tag {
  margin: 2px 4px 2px 0;
}

.no-field {
  color: #999;
}

.full-width {
  width: 100%;
}

.fields-editor {
  margin-top: 8px;
}

.fields-table {
  margin-top: 12px;
}

.option-placeholder {
  color: #c0c4cc;
}

.no-field-tip {
  margin-top: 12px;
  color: #999;
  font-size: 13px;
}
</style>
