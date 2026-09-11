import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import SpecFieldsEditor from '../SpecFieldsEditor.vue'

const mountEditor = (props) => mount(SpecFieldsEditor, {
  global: { plugins: [ElementPlus] },
  props,
  attachTo: document.body
})

describe('SpecFieldsEditor 历史规格保护', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  it('模板已删除字段的历史值原样保留，编辑保存时不被覆盖或清除', async () => {
    const wrapper = mountEditor({
      fields: [
        { fieldKey: 'screenSize', fieldLabel: '屏幕尺寸', fieldType: 'number', required: false, options: [] }
      ],
      modelValue: {
        screenSize: 55,
        oldResolution: '1080P'
      }
    })

    // 历史规格分区展示被删除字段的值（只读 input 的值需读 value 属性）
    expect(document.body.textContent).toContain('原样保留')
    const readonlyValues = Array.from(document.body.querySelectorAll('.spec-item-readonly input'))
      .map(input => input.value)
    expect(readonlyValues).toEqual(expect.arrayContaining(['oldResolution', '1080P']))

    const spec = wrapper.vm.getSpecJson()
    expect(spec.screenSize).toBe(55)
    expect(spec.oldResolution).toBe('1080P')
    wrapper.unmount()
  })

  it('字段类型变化导致旧值无法按新类型编辑时，旧值随表单回传不丢失', async () => {
    const wrapper = mountEditor({
      fields: [
        { fieldKey: 'resolution', fieldLabel: '分辨率', fieldType: 'number', required: false, options: [] },
        { fieldKey: 'wireless', fieldLabel: '是否无线', fieldType: 'boolean', required: false, options: [] }
      ],
      modelValue: {
        resolution: '4K超高清',
        wireless: '蓝牙5.0'
      }
    })

    const spec = wrapper.vm.getSpecJson()
    // 旧字符串值无法转为数字/布尔，原样保留，避免被 undefined/false 覆盖
    expect(spec.resolution).toBe('4K超高清')
    expect(spec.wireless).toBe('蓝牙5.0')
    wrapper.unmount()
  })

  it('可识别的旧值正常按模板字段编辑', async () => {
    const wrapper = mountEditor({
      fields: [
        { fieldKey: 'brightness', fieldLabel: '亮度', fieldType: 'number', required: false, options: [] },
        { fieldKey: 'wireless', fieldLabel: '是否无线', fieldType: 'boolean', required: false, options: [] }
      ],
      modelValue: {
        brightness: 3000,
        wireless: true
      }
    })

    const spec = wrapper.vm.getSpecJson()
    expect(spec.brightness).toBe(3000)
    expect(spec.wireless).toBe(true)
    // 无额外历史项
    expect(document.body.textContent).not.toContain('原样保留')
    wrapper.unmount()
  })
})
