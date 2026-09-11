import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import ActivityDetail from '../ActivityDetail.vue'
import { activityApi } from '../../api'

const routerPush = vi.fn()

vi.mock('../../api', () => ({
  activityApi: {
    getById: vi.fn(),
    finish: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: 1 } }),
  useRouter: () => ({ push: routerPush })
}))

const makeDetail = (overrides = {}) => ({
  id: 1,
  activityNo: 'HD20260911001',
  activityName: '客户参观',
  floorId: 1,
  floorName: '3F',
  roomId: 10,
  roomName: '301接待室',
  startTime: '2026-09-11T10:00:00',
  endTime: '2026-09-11T12:00:00',
  releasedAt: null,
  manager: '张三',
  status: 1,
  statusText: '进行中',
  deviceCount: 2,
  remark: '',
  devices: [
    {
      id: 1, deviceId: 101, deviceCode: 'TV-001', deviceName: '电视A', deviceType: '电视',
      deviceStatus: 1, deviceStatusText: '正常',
      currentFloorName: '3F', currentRoomName: '301接待室', conflict: null
    },
    {
      id: 2, deviceId: 102, deviceCode: 'MIC-001', deviceName: '麦克风B', deviceType: '麦克风',
      deviceStatus: 1, deviceStatusText: '正常',
      currentFloorName: '4F', currentRoomName: '401接待室',
      conflict: '设备已调配至 401接待室，不在本接待室'
    }
  ],
  conflicts: ['设备「麦克风B」：设备已调配至 401接待室，不在本接待室'],
  ...overrides
})

const mountPage = () => mount(ActivityDetail, {
  global: {
    plugins: [ElementPlus]
  }
})

describe('ActivityDetail 活动详情', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('展示活动信息、占用设备清单与冲突提示', async () => {
    activityApi.getById.mockResolvedValue(makeDetail())
    const wrapper = mountPage()
    await flushPromises()

    expect(activityApi.getById).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('客户参观')
    expect(wrapper.text()).toContain('301接待室')
    expect(wrapper.findAll('.el-table__row')).toHaveLength(2)
    // 冲突提示区与设备行内冲突文案
    expect(wrapper.text()).toContain('检测到 1 条冲突提示')
    expect(wrapper.text()).toContain('设备已调配至 401接待室')
    // 进行中活动展示设备锁定告警与结束释放按钮
    expect(wrapper.text()).toContain('不可调配到其他接待室')
    expect(wrapper.findComponent({ name: 'ElPopconfirm' }).exists()).toBe(true)
  })

  it('已结束活动展示释放态且无结束按钮', async () => {
    activityApi.getById.mockResolvedValue(makeDetail({
      status: 2,
      statusText: '已结束',
      releasedAt: '2026-09-11T12:00:00',
      conflicts: []
    }))
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('活动已结束，设备占用已释放')
    expect(wrapper.findComponent({ name: 'ElPopconfirm' }).exists()).toBe(false)
    expect(wrapper.text()).toContain('暂无冲突')
  })

  it('点击结束并释放调用结束接口并刷新详情', async () => {
    const ended = makeDetail({ status: 2, statusText: '已结束', conflicts: [] })
    activityApi.getById.mockResolvedValue(makeDetail())
    activityApi.finish.mockResolvedValue(ended)

    const wrapper = mountPage()
    await flushPromises()

    // popconfirm 确认回调直接触发 finish
    await wrapper.vm.handleFinish()
    await flushPromises()

    expect(activityApi.finish).toHaveBeenCalledWith(1)
    expect(activityApi.getById).toHaveBeenCalledTimes(2)
  })
})
