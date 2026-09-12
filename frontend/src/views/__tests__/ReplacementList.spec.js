import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import ReplacementList from '../ReplacementList.vue'
import { replacementApi, floorApi, roomApi, deviceApi } from '../../api'

const routerPush = vi.fn()

vi.mock('../../api', () => ({
  replacementApi: {
    getPage: vi.fn(),
    getSpares: vi.fn(),
    create: vi.fn()
  },
  floorApi: { getAll: vi.fn() },
  roomApi: { getAll: vi.fn() },
  deviceApi: { getAll: vi.fn() }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush })
}))

const floors = [
  { id: 1, floorName: '1F' },
  { id: 2, floorName: '2F' }
]
const rooms = [
  { id: 10, floorId: 1, roomName: '101接待室' },
  { id: 20, floorId: 2, roomName: '201接待室' }
]

const makeRecord = (id, overrides = {}) => ({
  id,
  replacementNo: `TH2026090100${id}`,
  floorId: 1,
  floorName: '1F',
  roomId: 10,
  roomName: '101接待室',
  roomCode: 'R101',
  faultyDeviceId: 100 + id,
  faultyDeviceCode: `F${id}`,
  faultyDeviceName: `故障设备${id}`,
  faultyDeviceType: '麦克风',
  spareDeviceId: 200 + id,
  spareDeviceCode: `S${id}`,
  spareDeviceName: `备用设备${id}`,
  spareDeviceType: '麦克风',
  faultPhenomenon: '无声音',
  operator: '值班员甲',
  processResult: 1,
  processResultText: '待维修',
  replacementTime: '2026-09-01T10:00:00',
  ...overrides
})

const pageResult = (records, total) => ({
  records,
  total,
  pageNum: 1,
  pageSize: 10,
  pages: Math.ceil(total / 10)
})

const mountPage = () => mount(ReplacementList, {
  global: {
    plugins: [ElementPlus]
  }
})

const findButton = (wrapper, text) =>
  wrapper.findAll('button').find(btn => btn.text().includes(text))

describe('ReplacementList 故障应急替换列表页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routerPush.mockClear()
    floorApi.getAll.mockResolvedValue(floors)
    roomApi.getAll.mockResolvedValue(rooms)
  })

  it('初始按默认分页加载，楼层/接待室/处理结果过滤传给后端', async () => {
    replacementApi.getPage.mockResolvedValue(pageResult([makeRecord(1)], 1))
    const wrapper = mountPage()
    await flushPromises()

    expect(replacementApi.getPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })
    expect(wrapper.text()).toContain('故障设备1')
    expect(wrapper.text()).toContain('备用设备1')
    expect(wrapper.text()).toContain('无声音')
    expect(wrapper.text()).toContain('值班员甲')
  })

  it('搜索时携带楼层、接待室、处理结果并重置第一页', async () => {
    replacementApi.getPage.mockResolvedValue(pageResult([], 0))
    const wrapper = mountPage()
    await flushPromises()

    // 通过设置响应式 filters 的方式选择筛选（el-select 在 jsdom 下交互不稳定）
    wrapper.vm.filters.floorId = 1
    wrapper.vm.onFilterFloorChange()
    wrapper.vm.filters.roomId = 10
    wrapper.vm.filters.processResult = 1
    findButton(wrapper, '搜索').trigger('click')
    await flushPromises()

    expect(replacementApi.getPage).toHaveBeenLastCalledWith({
      pageNum: 1,
      pageSize: 10,
      floorId: 1,
      roomId: 10,
      processResult: 1
    })
  })

  it('选择故障设备后请求同楼层备用机，确认替换成功并展示新记录', async () => {
    replacementApi.getPage
      .mockResolvedValueOnce(pageResult([], 0))
      .mockResolvedValueOnce(pageResult([makeRecord(5)], 1))
    deviceApi.getAll.mockResolvedValue([
      { id: 101, deviceCode: 'M-1', deviceName: '手持麦克风', deviceType: '麦克风',
        currentFloorId: 1, currentFloorName: '1F', currentRoomId: 10, currentRoomName: '101接待室',
        status: 1, statusText: '正常' }
    ])
    replacementApi.getSpares.mockResolvedValue([
      { id: 201, deviceCode: 'M-B1', deviceName: '备用麦克风', deviceType: '麦克风',
        currentFloorName: '1F', sameDeviceType: true, sameType: false, status: 1, statusText: '正常' }
    ])
    replacementApi.create.mockResolvedValue(makeRecord(5))

    const wrapper = mountPage()
    await flushPromises()

    await wrapper.vm.openCreate()
    await flushPromises()
    expect(deviceApi.getAll).toHaveBeenCalled()

    wrapper.vm.createForm.faultyDeviceId = 101
    await wrapper.vm.onFaultyDeviceChange(101)
    await flushPromises()
    expect(replacementApi.getSpares).toHaveBeenCalledWith(101)

    wrapper.vm.createForm.spareDeviceId = 201
    wrapper.vm.createForm.faultPhenomenon = '  发言时断续无声  '
    wrapper.vm.createForm.operator = ' 值班员乙 '
    await wrapper.vm.confirmCreate()
    await flushPromises()

    expect(replacementApi.create).toHaveBeenCalledWith({
      faultyDeviceId: 101,
      spareDeviceId: 201,
      faultPhenomenon: '发言时断续无声',
      operator: '值班员乙',
      remark: ''
    })
    // 创建后列表刷新
    expect(replacementApi.getPage).toHaveBeenCalledTimes(2)
  })

  it('同楼层无可用备用机时给出空提示且禁止提交', async () => {
    replacementApi.getPage.mockResolvedValue(pageResult([], 0))
    deviceApi.getAll.mockResolvedValue([
      { id: 101, deviceCode: 'M-1', deviceName: '手持麦克风', deviceType: '麦克风',
        currentFloorId: 1, currentFloorName: '1F', currentRoomId: 10, currentRoomName: '101接待室',
        status: 1, statusText: '正常' }
    ])
    replacementApi.getSpares.mockResolvedValue([])

    const wrapper = mountPage()
    await flushPromises()
    await wrapper.vm.openCreate()
    await flushPromises()
    wrapper.vm.createForm.faultyDeviceId = 101
    await wrapper.vm.onFaultyDeviceChange(101)
    await flushPromises()

    expect(wrapper.text()).toContain('同楼层暂无可调用的备用机')

    wrapper.vm.createForm.faultPhenomenon = '故障'
    wrapper.vm.createForm.operator = '值班员'
    await wrapper.vm.confirmCreate()
    expect(replacementApi.create).not.toHaveBeenCalled()
  })

  it('组件挂载时加载楼层与接待室用于过滤', async () => {
    replacementApi.getPage.mockResolvedValue(pageResult([], 0))
    mountPage()
    await flushPromises()
    expect(floorApi.getAll).toHaveBeenCalled()
    expect(roomApi.getAll).toHaveBeenCalled()
  })
})
