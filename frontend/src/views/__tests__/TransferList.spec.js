import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus, { ElInput, ElPagination } from 'element-plus'
import TransferList from '../TransferList.vue'
import { deviceApi } from '../../api'

vi.mock('../../api', () => ({
  deviceApi: {
    getAllTransferRecords: vi.fn()
  }
}))

const makeRecord = (id, overrides = {}) => ({
  id,
  deviceCode: `DEV${String(id).padStart(3, '0')}`,
  deviceName: `设备${id}`,
  fromFloorName: '1F',
  toFloorName: '2F',
  fromRoomName: '101',
  toRoomName: '202',
  transferReason: '日常调配',
  operator: '张三',
  transferTime: '2026-09-01T10:00:00',
  remark: '',
  ...overrides
})

const pageResult = (records, total) => ({
  records,
  total,
  pageNum: 1,
  pageSize: 10,
  pages: Math.ceil(total / 10)
})

const mountPage = () => mount(TransferList, {
  global: { plugins: [ElementPlus] }
})

const findSearchButton = (wrapper) =>
  wrapper.findAll('button').find(btn => btn.text().includes('搜索'))

// jsdom 会缓存 getComputedStyle 结果，isVisible() 在多次显隐后不可靠，直接读内联 display
const isLoading = (wrapper) => {
  const mask = wrapper.find('.el-loading-mask')
  if (!mask.exists()) return false
  return !(mask.attributes('style') || '').includes('display: none')
}

describe('TransferList 设备流转记录页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('初始加载请求第一页并展示后端返回的真实总数', async () => {
    deviceApi.getAllTransferRecords.mockResolvedValue(
      pageResult([makeRecord(1), makeRecord(2)], 23)
    )

    const wrapper = mountPage()
    await flushPromises()

    expect(deviceApi.getAllTransferRecords).toHaveBeenCalledTimes(1)
    expect(deviceApi.getAllTransferRecords).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })
    expect(wrapper.findAll('.el-table__row')).toHaveLength(2)
    // 总数来自接口而非前端估算
    expect(wrapper.find('.el-pagination__total').text()).toContain('23')
    expect(isLoading(wrapper)).toBe(false)
  })

  it('搜索时将设备名称与操作人传给后端并重置到第一页', async () => {
    deviceApi.getAllTransferRecords.mockResolvedValue(pageResult([], 0))
    const wrapper = mountPage()
    await flushPromises()

    // 先切到第 3 页，验证搜索会重置页码
    wrapper.findComponent(ElPagination).vm.$emit('current-change', 3)
    await flushPromises()
    expect(deviceApi.getAllTransferRecords).toHaveBeenLastCalledWith({ pageNum: 3, pageSize: 10 })

    const inputs = wrapper.findAllComponents(ElInput)
    await inputs[0].find('input').setValue(' 投影仪 ')
    await inputs[1].find('input').setValue('张三')
    await findSearchButton(wrapper).trigger('click')
    await flushPromises()

    expect(deviceApi.getAllTransferRecords).toHaveBeenLastCalledWith({
      pageNum: 1,
      pageSize: 10,
      deviceName: '投影仪',
      operator: '张三'
    })
  })

  it('切换页码与每页条数时按最新分页参数查询', async () => {
    deviceApi.getAllTransferRecords.mockResolvedValue(pageResult([makeRecord(1)], 100))
    const wrapper = mountPage()
    await flushPromises()

    wrapper.findComponent(ElPagination).vm.$emit('current-change', 2)
    await flushPromises()
    expect(deviceApi.getAllTransferRecords).toHaveBeenLastCalledWith({ pageNum: 2, pageSize: 10 })

    // 改变每页条数后应回到第一页，避免页码越界出现空数据
    wrapper.findComponent(ElPagination).vm.$emit('size-change', 20)
    await flushPromises()
    expect(deviceApi.getAllTransferRecords).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 20 })
  })

  it('重复搜索时仅采用最后一次请求的结果，加载状态最终复位', async () => {
    const pending = []
    deviceApi.getAllTransferRecords.mockImplementation(
      () => new Promise(resolve => pending.push(resolve))
    )
    const wrapper = mountPage()
    await flushPromises()

    const input = wrapper.findAllComponents(ElInput)[0]
    await input.find('input').setValue('投影仪')
    await findSearchButton(wrapper).trigger('click')
    await input.find('input').setValue('音响')
    await findSearchButton(wrapper).trigger('click')
    await flushPromises()

    expect(pending).toHaveLength(3)
    expect(isLoading(wrapper)).toBe(true)

    // 最后一次请求先返回
    pending[2](pageResult([makeRecord(3, { deviceName: '音响A' })], 5))
    await flushPromises()
    // 较早的请求后返回，其过期结果不应覆盖页面
    pending[1](pageResult([makeRecord(9, { deviceName: '投影仪X' })], 99))
    pending[0](pageResult([makeRecord(8, { deviceName: '旧数据' })], 88))
    await flushPromises()

    const rows = wrapper.findAll('.el-table__row')
    expect(rows).toHaveLength(1)
    expect(wrapper.text()).toContain('音响A')
    expect(wrapper.text()).not.toContain('投影仪X')
    expect(wrapper.text()).not.toContain('旧数据')
    expect(wrapper.find('.el-pagination__total').text()).toContain('5')
    expect(isLoading(wrapper)).toBe(false)
  })
})
