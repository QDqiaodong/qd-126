import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import ActivityList from '../ActivityList.vue'
import { activityApi, floorApi, roomApi, deviceApi } from '../../api'

const routerPush = vi.fn()

vi.mock('../../api', () => ({
  activityApi: {
    getPage: vi.fn(),
    create: vi.fn(),
    finish: vi.fn(),
    getById: vi.fn(),
    getRoomOccupancies: vi.fn()
  },
  floorApi: { getAll: vi.fn() },
  roomApi: { getByFloor: vi.fn() },
  deviceApi: { getByRoom: vi.fn() }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush })
}))

const makeActivity = (id, overrides = {}) => ({
  id,
  activityNo: `HD20260911${String(id).padStart(4, '0')}`,
  activityName: `客户参观${id}`,
  floorId: 1,
  floorName: '3F',
  roomId: 10,
  roomName: '301接待室',
  startTime: '2026-09-11T10:00:00',
  endTime: '2026-09-11T12:00:00',
  deviceCount: 2,
  manager: '张三',
  status: 1,
  statusText: '进行中',
  ...overrides
})

const pageResult = (records, total) => ({
  records,
  total,
  pageNum: 1,
  pageSize: 10,
  pages: Math.ceil(total / 10)
})

const mountPage = () => mount(ActivityList, {
  global: {
    plugins: [ElementPlus]
  }
})

describe('ActivityList 接待室活动占用列表', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routerPush.mockClear()
    floorApi.getAll.mockResolvedValue([{ id: 1, floorName: '3F' }])
    roomApi.getByFloor.mockResolvedValue([{ id: 10, roomName: '301接待室' }])
    deviceApi.getByRoom.mockResolvedValue([
      { id: 101, deviceCode: 'TV-001', deviceName: '电视A', deviceType: '电视', status: 1 }
    ])
  })

  it('初始加载活动分页与楼层数据', async () => {
    activityApi.getPage.mockResolvedValue(
      pageResult([makeActivity(1), makeActivity(2, { status: 2, statusText: '已结束' })], 23)
    )

    const wrapper = mountPage()
    await flushPromises()

    expect(activityApi.getPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })
    expect(floorApi.getAll).toHaveBeenCalledTimes(1)
    expect(wrapper.findAll('.el-table__row')).toHaveLength(2)
    expect(wrapper.find('.el-pagination__total').text()).toContain('23')
    expect(wrapper.text()).toContain('客户参观1')
  })

  it('按日期、楼层、状态筛选时重置到第一页并透传查询参数', async () => {
    activityApi.getPage.mockResolvedValue(pageResult([], 0))
    const wrapper = mountPage()
    await flushPromises()

    wrapper.vm.searchDate = '2026-09-11'
    wrapper.vm.searchFloorId = 1
    wrapper.vm.searchStatus = 1
    await wrapper.find('.search-bar .el-button').trigger('click')
    await flushPromises()

    expect(activityApi.getPage).toHaveBeenLastCalledWith({
      pageNum: 1,
      pageSize: 10,
      date: '2026-09-11',
      floorId: 1,
      status: 1
    })
  })

  it('登记活动时校验必填项并提交活动名称、时段、设备与负责人', async () => {
    activityApi.getPage.mockResolvedValue(pageResult([], 0))
    const created = makeActivity(99, { status: 0, statusText: '待开始' })
    activityApi.create.mockResolvedValue(created)
    const wrapper = mountPage()
    await flushPromises()

    // 未选择楼层直接提交应被拦截
    wrapper.vm.createVisible = true
    await wrapper.vm.confirmCreate()
    expect(activityApi.create).not.toHaveBeenCalled()

    // 完成楼层 -> 接待室 -> 设备级联
    wrapper.vm.createForm.floorId = 1
    await wrapper.vm.onCreateFloorChange(1)
    expect(roomApi.getByFloor).toHaveBeenCalledWith(1)
    wrapper.vm.createForm.roomId = 10
    await wrapper.vm.onCreateRoomChange(10)
    expect(deviceApi.getByRoom).toHaveBeenCalledWith(10)

    wrapper.vm.createForm.activityName = '客户参观'
    wrapper.vm.createForm.timeRange = ['2026-09-20T09:00:00', '2026-09-20T11:00:00']
    wrapper.vm.createForm.deviceIds = [101]
    wrapper.vm.createForm.manager = '张三'
    await wrapper.vm.confirmCreate()
    await flushPromises()

    expect(activityApi.create).toHaveBeenCalledWith({
      activityName: '客户参观',
      roomId: 10,
      startTime: '2026-09-20T09:00:00',
      endTime: '2026-09-20T11:00:00',
      deviceIds: [101],
      manager: '张三',
      remark: ''
    })
    expect(routerPush).toHaveBeenCalledWith('/activity/99')
  })
})
