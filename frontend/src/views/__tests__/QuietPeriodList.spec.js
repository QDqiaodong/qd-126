import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus, { ElMessageBox } from 'element-plus'
import QuietPeriodList from '../QuietPeriodList.vue'
import { quietPeriodApi, floorApi, roomApi } from '../../api'

vi.mock('../../api', () => ({
  quietPeriodApi: {
    getPage: vi.fn(),
    getByRoom: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  },
  floorApi: { getAll: vi.fn() },
  roomApi: { getByFloor: vi.fn() }
}))

const makeQuiet = (id, overrides = {}) => ({
  id,
  quietNo: `JY20260912${String(id).padStart(4, '0')}`,
  roomId: 10,
  roomName: '301接待室',
  roomCode: 'R301',
  floorId: 1,
  floorName: '3F',
  startTime: '2026-09-13T14:00:00',
  endTime: '2026-09-13T16:00:00',
  reason: '设备检修',
  remark: '',
  createdAt: '2026-09-12T10:00:00',
  ...overrides
})

const pageResult = (records, total) => ({
  records,
  total,
  pageNum: 1,
  pageSize: 10,
  pages: Math.ceil(total / 10)
})

const mountPage = () => mount(QuietPeriodList, {
  global: {
    plugins: [ElementPlus]
  }
})

describe('QuietPeriodList 接待室静音时段', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    floorApi.getAll.mockResolvedValue([{ id: 1, floorName: '3F' }])
    roomApi.getByFloor.mockResolvedValue([{ id: 10, roomName: '301接待室' }])
  })

  it('初始加载静音时段分页与楼层数据', async () => {
    quietPeriodApi.getPage.mockResolvedValue(pageResult([makeQuiet(1), makeQuiet(2)], 12))

    const wrapper = mountPage()
    await flushPromises()

    expect(quietPeriodApi.getPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })
    expect(wrapper.findAll('.el-table__row')).toHaveLength(2)
    expect(wrapper.find('.el-pagination__total').text()).toContain('12')
    expect(wrapper.text()).toContain('301接待室')
    expect(wrapper.text()).toContain('设备检修')
  })

  it('按楼层筛选时重置到第一页并透传查询参数', async () => {
    quietPeriodApi.getPage.mockResolvedValue(pageResult([], 0))
    const wrapper = mountPage()
    await flushPromises()

    wrapper.vm.searchFloorId = 1
    await wrapper.find('.search-bar .el-button').trigger('click')
    await flushPromises()

    expect(quietPeriodApi.getPage).toHaveBeenLastCalledWith({
      pageNum: 1,
      pageSize: 10,
      floorId: 1
    })
  })

  it('登记静音时段时校验必填项并提交接待室、起止与原因', async () => {
    quietPeriodApi.getPage.mockResolvedValue(pageResult([], 0))
    quietPeriodApi.create.mockResolvedValue(makeQuiet(9))
    const wrapper = mountPage()
    await flushPromises()

    // 未选楼层直接提交应被拦截
    wrapper.vm.openCreate()
    await wrapper.vm.confirmSubmit()
    expect(quietPeriodApi.create).not.toHaveBeenCalled()

    wrapper.vm.form.floorId = 1
    await wrapper.vm.onFormFloorChange(1)
    expect(roomApi.getByFloor).toHaveBeenCalledWith(1)
    wrapper.vm.form.roomId = 10
    wrapper.vm.form.timeRange = ['2026-09-13T14:00:00', '2026-09-13T16:00:00']
    wrapper.vm.form.reason = '设备检修'
    await wrapper.vm.confirmSubmit()
    await flushPromises()

    expect(quietPeriodApi.create).toHaveBeenCalledWith({
      roomId: 10,
      startTime: '2026-09-13T14:00:00',
      endTime: '2026-09-13T16:00:00',
      reason: '设备检修',
      remark: ''
    })
    expect(wrapper.vm.formVisible).toBe(false)
  })

  it('编辑静音时段时预填表单并提交更新', async () => {
    quietPeriodApi.getPage.mockResolvedValue(pageResult([makeQuiet(3)], 1))
    quietPeriodApi.update.mockResolvedValue(makeQuiet(3, { reason: '重要会议保障' }))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.vm.openEdit(makeQuiet(3))
    await flushPromises()

    expect(wrapper.vm.isEdit).toBe(true)
    expect(wrapper.vm.form.roomId).toBe(10)
    expect(wrapper.vm.form.reason).toBe('设备检修')
    expect(wrapper.vm.form.timeRange).toEqual(['2026-09-13T14:00:00', '2026-09-13T16:00:00'])

    wrapper.vm.form.reason = '重要会议保障'
    await wrapper.vm.confirmSubmit()
    await flushPromises()

    expect(quietPeriodApi.update).toHaveBeenCalledWith(3, {
      roomId: 10,
      startTime: '2026-09-13T14:00:00',
      endTime: '2026-09-13T16:00:00',
      reason: '重要会议保障',
      remark: ''
    })
    expect(quietPeriodApi.create).not.toHaveBeenCalled()
  })

  it('删除静音时段需确认后调用删除接口', async () => {
    quietPeriodApi.getPage.mockResolvedValue(pageResult([makeQuiet(4)], 1))
    quietPeriodApi.delete.mockResolvedValue()
    const confirmSpy = vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.vm.handleDelete(makeQuiet(4))
    await flushPromises()

    expect(confirmSpy).toHaveBeenCalled()
    expect(quietPeriodApi.delete).toHaveBeenCalledWith(4)
    confirmSpy.mockRestore()
  })
})
