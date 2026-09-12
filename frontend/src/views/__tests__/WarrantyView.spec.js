import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import WarrantyView from '../WarrantyView.vue'
import { deviceApi, floorApi, roomApi } from '../../api'

const routerPush = vi.fn()

vi.mock('../../api', () => ({
  deviceApi: { getWarrantyOverview: vi.fn() },
  floorApi: { getAll: vi.fn() },
  roomApi: { getAll: vi.fn(), getByFloor: vi.fn() }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush })
}))

const overviewData = () => ({
  expiringSoonDays: 15,
  groups: [
    {
      floorId: 1,
      floorName: '3F',
      devices: [
        {
          id: 1, deviceCode: 'TV-001', deviceName: '大厅电视', deviceType: '电视',
          currentRoomName: '301接待室', warrantyEndDate: '2026-09-01',
          daysRemaining: -11, warrantyStatus: 'EXPIRED', warrantyStatusText: '已过期'
        },
        {
          id: 2, deviceCode: 'TV-002', deviceName: '会议电视', deviceType: '电视',
          currentRoomName: '302接待室', warrantyEndDate: '2026-09-20',
          daysRemaining: 8, warrantyStatus: 'EXPIRING', warrantyStatusText: '临期'
        },
        {
          id: 3, deviceCode: 'SP-001', deviceName: '功放音响', deviceType: '音响',
          currentRoomName: '301接待室', warrantyEndDate: '2027-09-01',
          daysRemaining: 354, warrantyStatus: 'NORMAL', warrantyStatusText: '正常'
        }
      ]
    }
  ],
  noWarrantyDevices: [
    {
      id: 4, deviceCode: 'MIC-001', deviceName: '无线麦克风', deviceType: '麦克风',
      currentFloorName: '3F', currentRoomName: '303接待室', warrantyEndDate: null,
      daysRemaining: null, warrantyStatus: 'NONE', warrantyStatusText: '未设置'
    }
  ]
})

const mountPage = () => mount(WarrantyView, {
  global: {
    plugins: [ElementPlus]
  }
})

describe('WarrantyView 影音设备保修到期视图', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routerPush.mockClear()
    floorApi.getAll.mockResolvedValue([{ id: 1, floorName: '3F' }])
    roomApi.getAll.mockResolvedValue([{ id: 10, roomName: '301接待室' }])
    roomApi.getByFloor.mockResolvedValue([{ id: 10, roomName: '301接待室' }])
    deviceApi.getWarrantyOverview.mockResolvedValue(overviewData())
  })

  it('初始加载按楼层分组展示，已过期标红、临期标黄，无日期设备单独列出', async () => {
    const wrapper = mountPage()
    await flushPromises()

    expect(deviceApi.getWarrantyOverview).toHaveBeenCalledWith({})
    expect(wrapper.text()).toContain('3F')
    expect(wrapper.text()).toContain('大厅电视')

    // 已过期整行标红、临期整行标黄
    expect(wrapper.findAll('tr.warranty-row-expired')).toHaveLength(1)
    expect(wrapper.findAll('tr.warranty-row-expiring')).toHaveLength(1)
    expect(wrapper.text()).toContain('已过期11天')
    expect(wrapper.text()).toContain('剩8天')

    // 无截止日期的设备在独立分组中列出
    const undated = wrapper.find('.undated-table')
    expect(undated.exists()).toBe(true)
    expect(wrapper.text()).toContain('未设置保修截止日期')
    expect(undated.text()).toContain('MIC-001')
    expect(undated.text()).toContain('未设置')
  })

  it('按楼层和接待室筛选时透传查询参数', async () => {
    const wrapper = mountPage()
    await flushPromises()

    wrapper.vm.searchFloorId = 1
    await wrapper.vm.onFloorChange(1)
    expect(roomApi.getByFloor).toHaveBeenCalledWith(1)
    expect(deviceApi.getWarrantyOverview).toHaveBeenLastCalledWith({ floorId: 1 })

    wrapper.vm.searchRoomId = 10
    const filterButton = wrapper.findAll('button').find(b => b.text().includes('筛选'))
    await filterButton.trigger('click')
    await flushPromises()

    expect(deviceApi.getWarrantyOverview).toHaveBeenLastCalledWith({ floorId: 1, roomId: 10 })
  })

  it('点击设备行进入设备档案页', async () => {
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.warranty-table tbody tr').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/device/detail/1')
  })

  it('刷新后重新从后端获取数据，无数据时展示空状态', async () => {
    const wrapper = mountPage()
    await flushPromises()

    deviceApi.getWarrantyOverview.mockResolvedValue({
      expiringSoonDays: 15,
      groups: [],
      noWarrantyDevices: []
    })
    const refreshButton = wrapper.findAll('button').find(b => b.text().includes('刷新'))
    await refreshButton.trigger('click')
    await flushPromises()

    expect(deviceApi.getWarrantyOverview).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('暂无设备数据')
  })
})
