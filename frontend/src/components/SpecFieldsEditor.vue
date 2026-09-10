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

    <!-- 未配置启用模板时，回退为自由键值对录入，兼容“其他”等无模板类型 -->
    <el-form-item v-else label="规格参数">
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

const isEmptyValue = (v) => v === null || v === undefined || (typeof v === 'string' && v.trim() === '')

const syncFromModel = () => {
  Object.keys(values).forEach(k => delete values[k])
  Object.keys(numberValues).forEach(k => delete numberValues[k])
  Object.keys(booleanValues).forEach(k => delete booleanValues[k])

  const spec = props.modelValue || {}
  props.fields.forEach(field => {
    const v = spec[fieldKeyOf(field)]
    if (field.fieldType === 'number') {
      numberValues[field.fieldKey] = v === null || v === undefined || v === '' ? undefined : Number(v)
    } else if (field.fieldType === 'boolean') {
      booleanValues[field.fieldKey] = v === true || v === 'true' || v === 1 || v === '1'
    } else {
      values[field.fieldKey] = v === null || v === undefined ? '' : String(v)
    }
  })

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

const fieldKeyOf = (field) => field.fieldKey

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
    const result = {}
    props.fields.forEach(field => {
      if (field.fieldType === 'number') {
        const v = numberValues[field.fieldKey]
        if (v !== undefined && v !== null) {
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
</style>
