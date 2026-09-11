import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import SpecTemplateList from '../SpecTemplateList.vue'
import { specTemplateApi } from '../../api'

vi.mock('../../api', () => ({
  specTemplateApi: {
    getAll: vi.fn(),
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    preview: vi.fn(),
    updateStatus: vi.fn()
  }
}))

const makeField = (id, overrides = {}) => ({
  id,
  fieldKey: `key${id}`,
  fieldLabel: `字段${id}`,
  fieldType: 'text',
  required: false,
  options: [],
  sortOrder: id,
  ...overrides
})

const makeTemplate = (id, overrides = {}) => ({
  id,
  deviceType: `类型${id}`,
  status: 1,
  fields: [makeField(id * 10 + 1)],
  createdAt: '2026-09-01T10:00:00',
  updatedAt: '2026-09-02T10:00:00',
  ...overrides
})

const mountPage = () => mount(SpecTemplateList, {
  global: { plugins: [ElementPlus] },
  attachTo: document.body
})

const findButtonByText = (root, text) =>
  Array.from(root.querySelectorAll('button')).find(btn => btn.textContent.includes(text))

const openEditDialog = async (wrapper) => {
  findButtonByText(wrapper.element, '编辑').click()
  await flushPromises()
}

describe('SpecTemplateList 规格模板管理', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''
  })

  it('加载后展示模板列表及其状态', async () => {
    specTemplateApi.getAll.mockResolvedValue([makeTemplate(1), makeTemplate(2, { status: 0 })])
    const wrapper = mountPage()
    await flushPromises()

    expect(specTemplateApi.getAll).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('类型1')
    expect(wrapper.text()).toContain('类型2')
    expect(wrapper.text()).toContain('启用')
    expect(wrapper.text()).toContain('停用')
  })

  it('编辑保存前先请求变更预览，展示新增/删除/类型变化与受影响设备数，确认后才更新', async () => {
    specTemplateApi.getAll.mockResolvedValue([makeTemplate(1, {
      fields: [
        makeField(1, { fieldKey: 'screenSize', fieldLabel: '屏幕尺寸', fieldType: 'number', sortOrder: 1 }),
        makeField(2, { fieldKey: 'resolution', fieldLabel: '分辨率', fieldType: 'select', options: ['1080P', '4K'], sortOrder: 2 })
      ]
    })])
    specTemplateApi.preview.mockResolvedValue({
      templateId: 1,
      deviceType: '类型1',
      totalDeviceCount: 5,
      affectedDeviceCount: 3,
      addedFields: [{ fieldKey: 'brightness', fieldLabel: '亮度', fieldType: 'number', required: true, options: [], sortOrder: 3 }],
      removedFields: [{ fieldKey: 'resolution', fieldLabel: '分辨率', fieldType: 'select' }],
      typeChangedFields: [{ fieldKey: 'screenSize', fieldLabel: '屏幕尺寸', oldFieldType: 'number', newFieldType: 'text' }],
      changed: true
    })
    specTemplateApi.update.mockResolvedValue(makeTemplate(1))

    const wrapper = mountPage()
    await flushPromises()
    await openEditDialog(wrapper)

    // 点击“预览变更并保存”前不应调用预览或更新
    expect(specTemplateApi.preview).not.toHaveBeenCalled()
    expect(specTemplateApi.update).not.toHaveBeenCalled()

    findButtonByText(document.body, '预览变更并保存').click()
    await flushPromises()

    expect(specTemplateApi.preview).toHaveBeenCalledTimes(1)
    expect(specTemplateApi.preview.mock.calls[0][0]).toBe(1)
    expect(specTemplateApi.update).not.toHaveBeenCalled()

    // 预览弹窗展示差异与影响
    expect(document.body.textContent).toContain('受影响设备数量')
    expect(document.body.textContent).toContain('3 台')
    expect(document.body.textContent).toContain('亮度')
    expect(document.body.textContent).toContain('分辨率')
    expect(document.body.textContent).toContain('屏幕尺寸')
    expect(document.body.textContent).toContain('不会覆盖历史设备已保存的规格')

    // 确认保存才真正更新
    findButtonByText(document.body, '确认保存').click()
    await flushPromises()

    expect(specTemplateApi.update).toHaveBeenCalledTimes(1)
    expect(specTemplateApi.update.mock.calls[0][0]).toBe(1)
    // 保存成功后刷新列表
    expect(specTemplateApi.getAll).toHaveBeenCalledTimes(2)
    wrapper.unmount()
  })

  it('保存失败时预览弹窗保留并可重试，不产生半成品', async () => {
    specTemplateApi.getAll.mockResolvedValue([makeTemplate(1)])
    specTemplateApi.preview.mockResolvedValue({
      templateId: 1,
      deviceType: '类型1',
      totalDeviceCount: 0,
      affectedDeviceCount: 0,
      addedFields: [],
      removedFields: [],
      typeChangedFields: [],
      changed: false
    })
    specTemplateApi.update
      .mockRejectedValueOnce(new Error('网络异常'))
      .mockResolvedValueOnce(makeTemplate(1))

    const wrapper = mountPage()
    await flushPromises()
    await openEditDialog(wrapper)

    findButtonByText(document.body, '预览变更并保存').click()
    await flushPromises()
    findButtonByText(document.body, '确认保存').click()
    await flushPromises()

    // 首次失败：展示错误与重试入口，弹窗仍在
    expect(specTemplateApi.update).toHaveBeenCalledTimes(1)
    expect(document.body.textContent).toContain('网络异常')
    const retryButton = findButtonByText(document.body, '重试保存')
    expect(retryButton).toBeTruthy()

    retryButton.click()
    await flushPromises()

    expect(specTemplateApi.update).toHaveBeenCalledTimes(2)
    expect(specTemplateApi.getAll).toHaveBeenCalledTimes(2)
    wrapper.unmount()
  })

  it('新增模板无需预览，保存失败时编辑弹窗保留可重试', async () => {
    specTemplateApi.getAll.mockResolvedValue([])
    specTemplateApi.create
      .mockRejectedValueOnce(new Error('服务器开小差'))
      .mockResolvedValueOnce(makeTemplate(9))

    const wrapper = mountPage()
    await flushPromises()

    findButtonByText(wrapper.element, '新增模板').click()
    await flushPromises()

    // 从类型下拉中选择一个预置类型
    const typeSelect = document.body.querySelector('.el-select input')
    typeSelect.click()
    await flushPromises()
    const typeOption = Array.from(document.body.querySelectorAll('.el-select-dropdown__item'))
      .find(item => item.textContent.includes('电视'))
    typeOption.click()
    await flushPromises()

    const saveButton = findButtonByText(document.body, '保存')
    saveButton.click()
    await flushPromises()

    expect(specTemplateApi.create).toHaveBeenCalledTimes(1)
    expect(specTemplateApi.preview).not.toHaveBeenCalled()
    // 失败后弹窗不关闭，仍可再次点击保存重试
    expect(findButtonByText(document.body, '保存')).toBeTruthy()

    findButtonByText(document.body, '保存').click()
    await flushPromises()
    expect(specTemplateApi.create).toHaveBeenCalledTimes(2)
    wrapper.unmount()
  })

  it('模板详情展示字段顺序、必填与选项', async () => {
    specTemplateApi.getAll.mockResolvedValue([makeTemplate(1)])
    specTemplateApi.getById.mockResolvedValue(makeTemplate(1, {
      fields: [
        makeField(1, {
          fieldKey: 'resolution',
          fieldLabel: '分辨率',
          fieldType: 'select',
          required: true,
          options: ['1080P', '4K'],
          sortOrder: 2
        })
      ]
    }))

    const wrapper = mountPage()
    await flushPromises()

    findButtonByText(wrapper.element, '详情').click()
    await flushPromises()

    expect(specTemplateApi.getById).toHaveBeenCalledWith(1)
    const bodyText = document.body.textContent
    expect(bodyText).toContain('分辨率')
    expect(bodyText).toContain('resolution')
    expect(bodyText).toContain('必填')
    expect(bodyText).toContain('1080P')
    expect(bodyText).toContain('4K')
    wrapper.unmount()
  })
})
