import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus, { ElMessageBox } from 'element-plus'
import InterpreterBookingList from '../InterpreterBookingList.vue'
import { interpreterBookingApi, floorApi, roomApi } from '../../api'

vi.mock('../../api', () => ({
  interpreterBookingApi: {
    getPage: vi.fn(),
    getById: vi.fn(),
    getByRoom: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  },
  floorApi: { getAll: vi.fn() },
  roomApi: { getByFloor: vi.fn() }
}))

const makeBooking = (id, overrides = {}) => ({
  id,
  bookingNo: `FY20260912${String(id).padStart(4, '0')}`,
  roomId: 10,
  roomName: '301接待室',
  roomCode: 'R301',
  floorId: 1,
  floorName: '3F',
  language: '英语',
  interpreterName: '王芳',
  startTime: '2026-09-13T14:00:00',
  endTime: '2026-09-13T16:00:00',
  status: 0,
  statusText: '待开始',
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

const mountPage = () => mount(InterpreterBookingList, {
  global: {
    plugins: [ElementPlus]
  }
})

describe('InterpreterBookingList 接待室随行翻译预约', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    floorApi.getAll.mockResolvedValue([{ id: 1, floorName: '3F' }])
    roomApi.getByFloor.mockResolvedValue([{ id: 10, roomName: '301接待室' }])
  })

  it('初始加载翻译预约分页并展示语种、译员与时段', async () => {
    interpreterBookingApi.getPage.mockResolvedValue(
      pageResult([makeBooking(1), makeBooking(2, { interpreterName: '李娜', language: '日语' })], 12))

    const wrapper = mountPage()
    await flushPromises()

    expect(interpreterBookingApi.getPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })
    expect(wrapper.findAll('.el-table__row')).toHaveLength(2)
    expect(wrapper.find('.el-pagination__total').text()).toContain('12')
    expect(wrapper.text()).toContain('王芳')
    expect(wrapper.text()).toContain('李娜')
    expect(wrapper.text()).toContain('英语')
    expect(wrapper.text()).toContain('日语')
  })

  it('按楼层筛选时重置到第一页并透传 floorId', async () => {
    interpreterBookingApi.getPage.mockResolvedValue(pageResult([], 0))
    const wrapper = mountPage()
    await flushPromises()

    wrapper.vm.searchFloorId = 1
    await wrapper.find('.search-bar .el-button').trigger('click')
    await flushPromises()

    expect(interpreterBookingApi.getPage).toHaveBeenLastCalledWith({
      pageNum: 1,
      pageSize: 10,
      floorId: 1
    })
  })

  it('预约时校验必填项并提交接待室、语种、译员与时段', async () => {
    interpreterBookingApi.getPage.mockResolvedValue(pageResult([], 0))
    interpreterBookingApi.create.mockResolvedValue(makeBooking(9))
    const wrapper = mountPage()
    await flushPromises()

    // 未选楼层直接提交应被拦截
    wrapper.vm.openCreate()
    await wrapper.vm.confirmSubmit()
    expect(interpreterBookingApi.create).not.toHaveBeenCalled()

    wrapper.vm.form.floorId = 1
    await wrapper.vm.onFormFloorChange(1)
    expect(roomApi.getByFloor).toHaveBeenCalledWith(1)
    wrapper.vm.form.roomId = 10
    wrapper.vm.form.language = '英语'
    wrapper.vm.form.interpreterName = '王芳'
    wrapper.vm.form.timeRange = ['2026-09-13T14:00:00', '2026-09-13T16:00:00']
    await wrapper.vm.confirmSubmit()
    await flushPromises()

    expect(interpreterBookingApi.create).toHaveBeenCalledWith({
      roomId: 10,
      language: '英语',
      interpreterName: '王芳',
      startTime: '2026-09-13T14:00:00',
      endTime: '2026-09-13T16:00:00',
      remark: ''
    })
    expect(wrapper.vm.formVisible).toBe(false)
  })

  it('同一译员时段撞车（业务码461）弹出已约接待室并拦住提交', async () => {
    interpreterBookingApi.getPage.mockResolvedValue(pageResult([], 0))
    const clash = new Error('译员「王芳」时段撞车，同一时段已被以下接待室预约：301接待室（09-13 14:00 ~ 09-13 16:00，语种：英语）')
    clash.code = 461
    interpreterBookingApi.create.mockRejectedValue(clash)
    const alertSpy = vi.spyOn(ElMessageBox, 'alert').mockResolvedValue('ok')

    const wrapper = mountPage()
    await flushPromises()

    wrapper.vm.openCreate()
    wrapper.vm.form.floorId = 1
    await wrapper.vm.onFormFloorChange(1)
    wrapper.vm.form.roomId = 11
    wrapper.vm.form.language = '日语'
    wrapper.vm.form.interpreterName = '王芳'
    wrapper.vm.form.timeRange = ['2026-09-13T15:00:00', '2026-09-13T17:00:00']
    await wrapper.vm.confirmSubmit()
    await flushPromises()

    expect(alertSpy).toHaveBeenCalledTimes(1)
    expect(alertSpy.mock.calls[0][0]).toContain('301接待室')
    expect(alertSpy.mock.calls[0][1]).toBe('译员时段撞车')
    // 拦住提交：弹窗保留，可改译员/时段后重试
    expect(wrapper.vm.formVisible).toBe(true)
    alertSpy.mockRestore()
  })

  it('编辑待开始预约时预填表单并调用更新接口', async () => {
    interpreterBookingApi.getPage.mockResolvedValue(pageResult([makeBooking(3)], 1))
    interpreterBookingApi.update.mockResolvedValue(makeBooking(3, { interpreterName: '李娜' }))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.vm.openEdit(makeBooking(3))
    await flushPromises()

    expect(wrapper.vm.isEdit).toBe(true)
    expect(wrapper.vm.form.roomId).toBe(10)
    expect(wrapper.vm.form.language).toBe('英语')
    expect(wrapper.vm.form.interpreterName).toBe('王芳')
    expect(wrapper.vm.form.timeRange).toEqual(['2026-09-13T14:00:00', '2026-09-13T16:00:00'])

    wrapper.vm.form.interpreterName = '李娜'
    await wrapper.vm.confirmSubmit()
    await flushPromises()

    expect(interpreterBookingApi.update).toHaveBeenCalledWith(3, expect.objectContaining({
      roomId: 10,
      interpreterName: '李娜'
    }))
    expect(interpreterBookingApi.create).not.toHaveBeenCalled()
  })

  it('活动已开始（进行中/已结束）的预约只可查看，不显示编辑与删除', async () => {
    interpreterBookingApi.getPage.mockResolvedValue(pageResult([
      makeBooking(5, { status: 1, statusText: '进行中' }),
      makeBooking(6, { status: 2, statusText: '已结束' }),
      makeBooking(7, { status: 0, statusText: '待开始' })
    ], 3))
    const wrapper = mountPage()
    await flushPromises()

    const rows = wrapper.findAll('.el-table__row')
    expect(rows).toHaveLength(3)
    // 进行中行只有“查看”
    const ongoingButtons = rows[0].findAll('button').map(b => b.text())
    expect(ongoingButtons).toContain('查看')
    expect(ongoingButtons).not.toContain('编辑')
    expect(ongoingButtons).not.toContain('删除')
    // 已结束行同样只有“查看”
    const endedButtons = rows[1].findAll('button').map(b => b.text())
    expect(endedButtons).toContain('查看')
    expect(endedButtons).not.toContain('编辑')
    expect(endedButtons).not.toContain('删除')
    // 待开始行有完整操作
    const pendingButtons = rows[2].findAll('button').map(b => b.text())
    expect(pendingButtons).toContain('查看')
    expect(pendingButtons).toContain('编辑')
    expect(pendingButtons).toContain('删除')
  })

  it('点击查看时拉取详情并打开只读详情弹窗', async () => {
    interpreterBookingApi.getPage.mockResolvedValue(pageResult([makeBooking(8)], 1))
    interpreterBookingApi.getById.mockResolvedValue(
      makeBooking(8, { status: 1, statusText: '进行中' }))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.vm.openDetail(makeBooking(8))
    await flushPromises()

    expect(interpreterBookingApi.getById).toHaveBeenCalledWith(8)
    expect(wrapper.vm.detailVisible).toBe(true)
    expect(wrapper.vm.detail.status).toBe(1)
    expect(wrapper.text()).toContain('活动已开始，该预约只允许查看')
  })

  it('删除待开始预约需确认后调用删除接口', async () => {
    interpreterBookingApi.getPage.mockResolvedValue(pageResult([makeBooking(4)], 1))
    interpreterBookingApi.delete.mockResolvedValue()
    const confirmSpy = vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.vm.handleDelete(makeBooking(4))
    await flushPromises()

    expect(confirmSpy).toHaveBeenCalled()
    expect(interpreterBookingApi.delete).toHaveBeenCalledWith(4)
    confirmSpy.mockRestore()
  })
})
