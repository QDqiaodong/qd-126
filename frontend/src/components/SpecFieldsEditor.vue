<template>
  <div class="spec-fields">
    <template v-if="fields.length > 0">
      <el-form-item
        v-for="field in fields"
        :key="field.fieldKey"
        :label="field.fieldLabel"
        :required="enforceRequired && field.required"
      >
        <el-select
          v-if="field.fieldType === 'select'"
          v-model="values[field.fieldKey]"
          :placeholder="'请选择' + field.fieldLabel"
          clearable
          class="spec-control"
        >
          <el-option v-for="opt in field.options || []" :key="opt" :label="opt" :value="opt" />
        </el-select>

        <el-input-number
          v-else-if="field.fieldType === 'number'"
          v-model="numberValues[field.fieldKey]"
          controls-position="right"
          class="spec-control"
        />

        <el-switch
          v-else-if="field.fieldType === 'boolean'"
          v-model="booleanValues[field.fieldKey]"
        />

        <el-date-picker
          v-else-if="field.fieldType === 'date'"
          v-model="values[field.fieldKey]"
          type="date"
          value-format="YYYY-MM-DD"
          :placeholder="'请选择' + field.fieldLabel"
          class="spec-control"
        />

        <el-input
          v-else-if="field.fieldType === 'textarea'"
          v-model="values[field.fieldKey]"
          type="textarea"
          :rows="2"
          :placeholder="'请输入' + field.fieldLabel"
        />

        <el-input
          v-else
          v-model="values[field.fieldKey]"
          :placeholder="'请输入' + field.fieldLabel"
        />
      </el-form-item>
    </template>

    <!-- 模板未覆盖（字段已删除/类型变化前的旧键）的历史规格，只读回显并随表单原样回传，避免覆盖历史数据 -->
    <el-form-item v-if="extraSpecItems.length > 0" label="历史规格">
      <div class="spec-form">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="以下规格不在当前模板字段中（可能因模板调整），将随设备信息原样保留，不会被清除或覆盖。"
          class="spec-hint-alert"
        />
        <div v-for="item in extraSpecItems" :key="item.key" class="spec-item spec-item-readonly">
          <el-input :model-value="item.key" class="spec-key" readonly />
          <el-input :model-value="item.value" class="spec-value" readonly />
        </div>
      </div>
    </el-form-item>

    <!-- 未配置启用模板时，回退为自由键值对录入，兼容“其他”等无模板类型 -->
    <el-form-item v-if="fields.length === 0" label="规格参数">
      <div class="spec-form">
        <el-button @click="addSpecItem" type="primary" size="small">
          <el-icon><Plus /></el-icon>
          添加参数
        </el-button>
        <div v-for="(item, index) in customItems" :key="index" class="spec-item">
          <el-input v-model="item.key" placeholder="参数名称" class="spec-key" />
          <el-input v-model="item.value" placeholder="参数值" class="spec-value" />
          <el-button @click="removeSpecItem(index)" type="danger" size="small">删除</el-button>
        </div>
        <div v-if="disabledHint" class="spec-hint">{{ disabledHint }}</div>
      </div>
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'

const props = defineProps({
  // 模板字段定义，空数组表示无启用模板
  fields: {
    type: Array,
    default: () => []
  },
  // 已有规格值，用于编辑回显
  modelValue: {
    type: Object,
    default: () => ({})
  },
  disabledHint: {
    type: String,
    default: ''
  },
  // 是否强制必填校验（模板停用时编辑历史设备为 false）
  enforceRequired: {
    type: Boolean,
    default: true
  }
})

const values = reactive({})
const numberValues = reactive({})
const booleanValues = reactive({})
const customItems = ref([])
// 模板字段未覆盖的历史键值（模板删字段/改类型前已保存的数据），原样保留不覆盖
const extraSpecItems = ref([])
// 旧值无法按当前字段类型编辑、已转入历史规格区的键，回写时跳过以免覆盖原始值
const rawHistoricalKeys = new Set()

const isEmptyValue = (v) => v === null || v === undefined || (typeof v === 'string' && v.trim() === '')

const syncFromModel = () => {
  Object.keys(values).forEach(k => delete values[k])
  Object.keys(numberValues).forEach(k => delete numberValues[k])
  Object.keys(booleanValues).forEach(k => delete booleanValues[k])

  const spec = props.modelValue || {}
  const definedKeys = new Set(props.fields.map(f => f.fieldKey))
  const extras = []
  rawHistoricalKeys.clear()

  props.fields.forEach(field => {
    const v = spec[field.fieldKey]
    if (field.fieldType === 'number') {
      if (v === null || v === undefined || v === '') {
        numberValues[field.fieldKey] = undefined
      } else if (Number.isFinite(Number(v))) {
        numberValues[field.fieldKey] = Number(v)
      } else {
        // 旧值无法按当前（可能已变更的）类型编辑时，作为历史规格原样保留
        numberValues[field.fieldKey] = undefined
        extras.push({ key: field.fieldKey, value: String(v) })
        rawHistoricalKeys.add(field.fieldKey)
      }
    } else if (field.fieldType === 'boolean') {
      if (v === true || v === 'true' || v === 1 || v === '1' || v === '是') {
        booleanValues[field.fieldKey] = true
      } else if (v === false || v === 'false' || v === 0 || v === '0' || v === '否') {
        booleanValues[field.fieldKey] = false
      } else if (v === null || v === undefined || v === '') {
        booleanValues[field.fieldKey] = false
      } else {
        // 旧值无法识别为布尔（多为字段类型变化前的历史值），原样保留，避免被 false 覆盖
        booleanValues[field.fieldKey] = false
        extras.push({ key: field.fieldKey, value: String(v) })
        rawHistoricalKeys.add(field.fieldKey)
      }
    } else {
      values[field.fieldKey] = v === null || v === undefined ? '' : String(v)
    }
  })

  // 模板未定义的历史键值（字段被删除等）也原样保留，避免编辑设备时覆盖历史规格
  Object.entries(spec).forEach(([key, value]) => {
    if (!definedKeys.has(key) && value !== null && value !== undefined && value !== '') {
      extras.push({ key, value: String(value) })
    }
  })
  extraSpecItems.value = extras

  customItems.value = []
  Object.entries(spec).forEach(([key, value]) => {
    if (value !== null && value !== undefined) {
      customItems.value.push({ key, value: String(value) })
    }
  })
  if (customItems.value.length === 0) {
    customItems.value.push({ key: '', value: '' })
  }
}

watch(() => props.fields, syncFromModel, { immediate: true, deep: false })
watch(() => props.modelValue, syncFromModel, { deep: false })

const addSpecItem = () => {
  customItems.value.push({ key: '', value: '' })
}

const removeSpecItem = (index) => {
  customItems.value.splice(index, 1)
}

/**
 * 返回当前规格键值对象
 */
const getSpecJson = () => {
  if (props.fields.length > 0) {
    // 先放入模板未覆盖/不可按新类型编辑的历史键值，模板字段值再覆盖，确保历史规格不丢失
    const result = {}
    extraSpecItems.value.forEach(item => {
      if (item.key) {
        result[item.key] = item.value
      }
    })
    props.fields.forEach(field => {
      // 已转入历史规格区的旧值（类型不兼容）不被表单默认值覆盖
      if (rawHistoricalKeys.has(field.fieldKey)) {
        return
      }
      if (field.fieldType === 'number') {
        const v = numberValues[field.fieldKey]
        if (v !== undefined && v !== null && v !== '' && !Number.isNaN(v)) {
          result[field.fieldKey] = v
        }
      } else if (field.fieldType === 'boolean') {
        result[field.fieldKey] = booleanValues[field.fieldKey] === true
      } else {
        const v = values[field.fieldKey]
        if (v !== undefined && v !== null && String(v).trim() !== '') {
          result[field.fieldKey] = v
        }
      }
    })
    return result
  }

  const result = {}
  customItems.value.forEach(item => {
    if (item.key && item.value) {
      result[item.key] = item.value
    }
  })
  return result
}

/**
 * 必填校验，返回错误消息；无错误返回空串
 */
const validate = () => {
  if (!props.enforceRequired) return ''
  for (const field of props.fields) {
    if (!field.required) continue
    if (field.fieldType === 'number') {
      if (isEmptyValue(numberValues[field.fieldKey])) {
        return `规格参数【${field.fieldLabel}】为必填项`
      }
    } else if (field.fieldType === 'boolean') {
      // 开关类字段默认 false 视为已填写，不拦截
      continue
    } else if (isEmptyValue(values[field.fieldKey])) {
      return `规格参数【${field.fieldLabel}】为必填项`
    }
  }
  return ''
}

defineExpose({ getSpecJson, validate })
</script>

<style scoped>
.spec-control {
  width: 100%;
}

.spec-form {
  width: 100%;
}

.spec-item {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.spec-key,
.spec-value {
  width: 200px;
}

.spec-hint {
  margin-top: 8px;
  color: #e6a23c;
  font-size: 12px;
}

.spec-hint-alert {
  margin-bottom: 8px;
}

.spec-item-readonly :deep(.el-input__wrapper) {
  background-color: #f5f7fa;
}
</style>
