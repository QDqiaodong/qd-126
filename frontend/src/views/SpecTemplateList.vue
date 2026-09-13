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
        title="编辑模板保存前需确认字段变更、受影响设备数量与正在引用的设备名单，确认后才写入；停用或修改模板不会覆盖历史设备已保存的规格，重新启用后新设备按最新字段校验。"
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
              {{ field.sortOrder }}. {{ field.fieldLabel }}{{ field.required ? '*' : '' }}（{{ typeTextMap[field.fieldType] || field.fieldType }}）
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
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button @click="handleEdit(row)" size="small" type="primary">编辑</el-button>
            <el-button @click="handleDetail(row)" size="small">详情</el-button>
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

        <el-divider content-position="left">规格字段（按排序号展示）</el-divider>

        <div class="fields-editor">
          <el-button @click="addField" type="primary" size="small">
            <el-icon><Plus /></el-icon>
            添加字段
          </el-button>
          <el-button @click="moveField(-1)" size="small" :disabled="activeFieldIndex < 0 || activeFieldIndex === 0">上移</el-button>
          <el-button @click="moveField(1)" size="small" :disabled="activeFieldIndex < 0 || activeFieldIndex === form.fields.length - 1">下移</el-button>

          <el-table
            :data="form.fields"
            border
            class="fields-table"
            @current-change="handleCurrentFieldChange"
            highlight-current-row
          >
            <el-table-column label="顺序" width="60" align="center">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
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
        <el-button @click="submitForm" type="primary" :loading="previewLoading">
          {{ isEdit ? '预览变更并保存' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 模板详情：字段顺序、必填、选项完整只读展示 -->
    <el-dialog v-model="detailVisible" title="模板详情" width="820px">
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border v-if="detailData">
          <el-descriptions-item label="设备类型">
            <el-tag :type="getTypeTagType(detailData.deviceType)">{{ detailData.deviceType }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detailData.status === 1 ? 'success' : 'warning'">
              {{ detailData.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detailData.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(detailData.updatedAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="detailData ? detailData.fields : []" border class="detail-table">
          <el-table-column label="顺序" width="70" align="center">
            <template #default="{ row }">{{ row.sortOrder }}</template>
          </el-table-column>
          <el-table-column prop="fieldLabel" label="字段名称" min-width="120" />
          <el-table-column prop="fieldKey" label="字段键" min-width="120" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">{{ typeTextMap[row.fieldType] || row.fieldType }}</template>
          </el-table-column>
          <el-table-column label="必填" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.required ? 'danger' : 'info'" size="small">
                {{ row.required ? '必填' : '选填' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="选项" min-width="180">
            <template #default="{ row }">
              <template v-if="row.fieldType === 'select' && row.options && row.options.length">
                <el-tag v-for="opt in row.options" :key="opt" size="small" type="info" class="option-tag">{{ opt }}</el-tag>
              </template>
              <span v-else class="option-placeholder">—</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 变更预览与影响确认：保存前必须确认 -->
    <el-dialog
      v-model="previewVisible"
      title="变更预览与影响确认"
      width="760px"
      :close-on-click-modal="false"
    >
      <div v-loading="previewLoading">
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="本次修改只更新模板定义，不会覆盖历史设备已保存的规格；保存并启用后，新添加/编辑的设备将按最新字段校验。"
          class="preview-banner"
        />

        <template v-if="previewData">
          <el-descriptions :column="2" border class="preview-summary">
            <el-descriptions-item label="设备类型">{{ previewData.deviceType }}</el-descriptions-item>
            <el-descriptions-item label="同类型设备总数">{{ previewData.totalDeviceCount }} 台</el-descriptions-item>
            <el-descriptions-item label="受影响设备数量">
              <span :class="previewData.affectedDeviceCount > 0 ? 'affected-num' : ''">
                {{ previewData.affectedDeviceCount }} 台
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="字段定义是否变化">
              <el-tag :type="previewData.changed ? 'warning' : 'success'" size="small">
                {{ previewData.changed ? '有变化' : '无变化' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <el-alert
            v-if="statusChanged"
            type="info"
            :closable="false"
            title="模板状态将由「{{ originalStatus === 1 ? '启用' : '停用' }}」变为「{{ form.status === 1 ? '启用' : '停用' }}」，停用后历史规格仍保留且不再强制校验。"
            class="preview-banner"
          />

          <div class="change-section">
            <h4 class="change-title device-title">正在引用该模板的设备（{{ referencingNames.length }} 台）— 确认后才写入，取消保持原规格</h4>
            <el-text v-if="referencingNames.length === 0" type="info" size="small">暂无设备引用该模板</el-text>
            <div v-else class="device-names">
              <el-tag
                v-for="name in referencingNames"
                :key="name"
                type="info"
                class="change-tag"
              >
                {{ name }}
              </el-tag>
            </div>
          </div>

          <div class="change-section">
            <h4 class="change-title add-title">新增字段（{{ previewData.addedFields.length }}）</h4>
            <el-text v-if="previewData.addedFields.length === 0" type="info" size="small">无</el-text>
            <el-tag
              v-for="f in previewData.addedFields"
              :key="f.fieldKey"
              type="success"
              class="change-tag"
            >
              {{ f.fieldLabel }}（{{ f.fieldKey }}，{{ typeTextMap[f.fieldType] || f.fieldType }}{{ f.required ? '，必填' : '' }}）
            </el-tag>
          </div>

          <div class="change-section">
            <h4 class="change-title remove-title">删除字段（{{ previewData.removedFields.length }}）— 历史设备已保存的值仍保留</h4>
            <el-text v-if="previewData.removedFields.length === 0" type="info" size="small">无</el-text>
            <el-tag
              v-for="f in previewData.removedFields"
              :key="f.fieldKey"
              type="danger"
              class="change-tag"
            >
              {{ f.fieldLabel }}（{{ f.fieldKey }}，{{ typeTextMap[f.fieldType] || f.fieldType }}）
            </el-tag>
          </div>

          <div class="change-section">
            <h4 class="change-title type-title">类型变化（{{ previewData.typeChangedFields.length }}）</h4>
            <el-text v-if="previewData.typeChangedFields.length === 0" type="info" size="small">无</el-text>
            <div v-for="f in previewData.typeChangedFields" :key="f.fieldKey" class="type-change-row">
              <el-tag type="info" size="small">{{ f.fieldLabel }}（{{ f.fieldKey }}）</el-tag>
              <span class="type-arrow">{{ typeTextMap[f.oldFieldType] || f.oldFieldType }}</span>
              <el-icon><ArrowRight /></el-icon>
              <span class="type-arrow new">{{ typeTextMap[f.newFieldType] || f.newFieldType }}</span>
            </div>
          </div>

          <el-alert
            v-if="saveError"
            type="error"
            :closable="false"
            :title="'保存失败：' + saveError + '。模板未被修改，可直接重试。'"
            class="preview-banner"
          />
        </template>
      </div>

      <template #footer>
        <el-button @click="previewVisible = false" :disabled="saving">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmSave">
          {{ saveError ? '重试保存' : '确认保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, ArrowRight } from '@element-plus/icons-vue'
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
const originalStatus = ref(null)
const activeFieldIndex = ref(-1)

const form = reactive({
  deviceType: '',
  status: 1,
  fields: []
})

// 变更预览
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewData = ref(null)
const saving = ref(false)
const saveError = ref('')

// 模板详情
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)

const typeOptions = computed(() => {
  const used = new Set(templates.value.map(t => t.deviceType))
  return DEFAULT_TYPES.filter(t => !used.has(t)).concat(
    templates.value
      .map(t => t.deviceType)
      .filter(t => !DEFAULT_TYPES.includes(t))
  )
})

const statusChanged = computed(() => isEdit.value && originalStatus.value !== null && originalStatus.value !== form.status)

// 正在引用该模板的设备名称，保存前弹窗逐台展示
const referencingNames = computed(() => previewData.value?.referencingDeviceNames || [])

const formatTime = (time) => {
  if (!time) return '-'
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
  originalStatus.value = null
  activeFieldIndex.value = -1
}

const addField = () => {
  form.fields.push({
    fieldKey: '',
    fieldLabel: '',
    fieldType: 'text',
    required: false,
    options: [],
    sortOrder: form.fields.length + 1
  })
}

const removeField = (index) => {
  form.fields.splice(index, 1)
  normalizeSortOrder()
}

const handleCurrentFieldChange = (row) => {
  activeFieldIndex.value = row ? form.fields.indexOf(row) : -1
}

const moveField = (delta) => {
  const from = activeFieldIndex.value
  const to = from + delta
  if (from < 0 || to < 0 || to >= form.fields.length) return
  const moved = form.fields.splice(from, 1)[0]
  form.fields.splice(to, 0, moved)
  activeFieldIndex.value = to
  normalizeSortOrder()
}

// 排序号统一从 1 开始，与模板详情/字段回显保持一致
const normalizeSortOrder = () => {
  form.fields.forEach((f, index) => {
    f.sortOrder = index + 1
  })
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  originalStatus.value = row.status
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
  activeFieldIndex.value = -1
  dialogVisible.value = true
}

const handleDetail = async (row) => {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    // 每次打开都拉取最新数据，保证刷新/变更后详情与模板状态一致
    detailData.value = await specTemplateApi.getById(row.id)
  } catch (error) {
    ElMessage.error(error.message || '加载模板详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleToggleStatus = async (row) => {
  const nextStatus = row.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}【${row.deviceType}】的规格模板吗？${
        nextStatus === 0
          ? '停用后历史设备已保存的规格不会被覆盖或清除，仅不再强制校验；重新启用后新设备按最新字段校验。'
          : '启用后，新添加/编辑的设备将按当前最新字段校验，历史设备规格保持不变。'
      }`,
      '提示',
      { type: 'warning', confirmButtonText: `确认${action}`, cancelButtonText: '取消' }
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

const buildPayload = () => ({
  deviceType: form.deviceType.trim(),
  status: form.status,
  fields: form.fields.map((f, index) => ({
    id: f.id,
    fieldKey: f.fieldKey.trim(),
    fieldLabel: f.fieldLabel.trim(),
    fieldType: f.fieldType,
    required: f.required,
    options: f.fieldType === 'select' ? f.options : [],
    sortOrder: f.sortOrder ?? index + 1
  }))
})

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

  const payload = buildPayload()

  // 新增模板没有存量设备，直接创建；编辑必须先预览变更与影响，确认后才保存
  if (!isEdit.value) {
    try {
      await specTemplateApi.create(payload)
      ElMessage.success('模板创建成功')
      dialogVisible.value = false
      loadTemplates()
    } catch (error) {
      // 保存失败不关弹窗，表单保留，可直接重试，不会产生半成品
      ElMessage.error(error.message || '保存失败，可修改后重试')
    }
    return
  }

  previewLoading.value = true
  saveError.value = ''
  previewData.value = null
  try {
    previewData.value = await specTemplateApi.preview(editId.value, payload)
    previewVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '变更预览计算失败，请检查字段配置后重试')
  } finally {
    previewLoading.value = false
  }
}

const confirmSave = async () => {
  saving.value = true
  saveError.value = ''
  try {
    await specTemplateApi.update(editId.value, buildPayload())
    ElMessage.success('模板更新成功')
    previewVisible.value = false
    dialogVisible.value = false
    loadTemplates()
  } catch (error) {
    // 后端整事务回滚 + 先全量校验后落库，失败不会产生半成品；弹窗保留，允许重试
    saveError.value = error.message || '保存失败'
  } finally {
    saving.value = false
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

.detail-table {
  margin-top: 16px;
}

.option-tag {
  margin: 2px 4px 2px 0;
}

.preview-banner {
  margin-bottom: 12px;
}

.preview-summary {
  margin-bottom: 12px;
}

.affected-num {
  color: #f56c6c;
  font-weight: 600;
}

.change-section {
  margin-top: 12px;
}

.change-title {
  margin: 0 0 8px 0;
  font-size: 14px;
}

.add-title {
  color: #67c23a;
}

.remove-title {
  color: #f56c6c;
}

.type-title {
  color: #e6a23c;
}

.change-tag {
  margin: 2px 6px 2px 0;
}

.device-title {
  color: #606266;
}

.device-names {
  max-height: 160px;
  overflow-y: auto;
}

.type-change-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.type-arrow.new {
  color: #e6a23c;
  font-weight: 600;
}
</style>
