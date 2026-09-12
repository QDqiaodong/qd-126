import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import ComboList from '../ComboList.vue'
import { comboApi, floorApi, roomApi, deviceApi } from '../../api'

const routerPush = vi.fn()

vi.mock('../../api', () => ({
  comboApi: {
    getAll: vi.fn(),
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    apply: vi.fn()
  },
  floorApi: { getAll: vi.fn() },
  roomApi: { getAll: vi.fn() },
  deviceApi: { getAll: vi.fn() }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush })
}))

const floors = [{ id: 1, floorName: '3F' }]
const rooms = [
  { id: 10, floorId: 1, roomName: '301接待室', roomCode: 'R301', equipmentCount: 1, status: 1 }
]
const devices = [
  { id: 101, deviceCode: 'TV-001', deviceName: '液晶电视', deviceType: '电视',
    currentFloorId: 1, currentFloorName: '3F', currentRoomId: null, currentRoomName: null,
    status: 1, statusText: '正常' },
  { id: 102, deviceCode: 'SPK-001', deviceName: '音响', deviceType: '音响',
    currentFloorId: 1, currentFloorName: '3F', currentRoomId: null, currentRoomName: null,
    status: 1, statusText: '正常' },
  { id: 103, deviceCode: 'MIC-001', deviceName: '麦克风', deviceType: '麦克风',
    currentFloorId: 1, currentFloorName: '3F', currentRoomId: 20, currentRoomName: '302接待室',
    status: 1, statusText: '正常' },
  { id: 104, deviceCode: 'MIC-002', deviceName: '故障麦克风', deviceType: '麦克风',
    currentFloorId: 1, currentFloorName: '3F', currentRoomId: null, currentRoomName: null,
    status: 2, statusText: '待维修' }
]

const makeCombo = (id, overrides = {}) => ({
  id,
  comboName: '贵宾接待组合',
  deviceCount: 4,
  status: 1,
  statusText: '启用',
  createdBy: '管理员',
  createdAt: '2026-09-01T10:00:00',
  devices: [
    { deviceId: 101, deviceCode: 'TV-001', deviceName: '液晶电视', deviceType: '电视' },
    { deviceId: 102, deviceCode: 'SPK-001', deviceName: '音响', deviceType: '音响' },
    { deviceId: 103, deviceCode: 'MIC-001', deviceName: '麦克风', deviceType: '麦克风' },
    { deviceId: 104, deviceCode: 'MIC-002', deviceName: '故障麦克风', deviceType: '麦克风' }
  ],
  ...overrides
})

const applyResult = {
  id: 700,
  recordNo: 'TZ20260901010001',
  comboId: 1,
  comboName: '贵宾接待组合',
  roomId: 10,
  roomName: '301接待室',
  roomCode: 'R301',
  floorId: 1,
  floorName: '3F',
  operator: '值班员小王',
  applyTime: '2026-09-01T10:05:00',
  requiredCount: 4,
  appliedCount: 2,
  presentCount: 0,
  skippedCount: 2,
  beforeDevices: [],
  afterDevices: [
    { id: 101, deviceCode: 'TV-001', deviceName: '液晶电视', deviceType: '电视' },
    { id: 102, deviceCode: 'SPK-001', deviceName: '音响', deviceType: '音响' }
  ],
  items: [
    { deviceId: 101, deviceCode: 'TV-001', deviceName: '液晶电视', deviceType: '电视',
      result: 1, resultText: '调入', skipReason: null, fromRoomName: null },
    { deviceId: 102, deviceCode: 'SPK-001', deviceName: '音响', deviceType: '音响',
      result: 1, resultText: '调入', skipReason: null, fromRoomName: null },
    { deviceId: 103, deviceCode: 'MIC-001', deviceName: '麦克风', deviceType: '麦克风',
      result: 3, resultText: '跳过', skipReason: '设备正在接待室「302接待室」使用', fromRoomName: '302接待室' },
    { deviceId: 104, deviceCode: 'MIC-002', deviceName: '故障麦克风', deviceType: '麦克风',
      result: 3, resultText: '跳过', skipReason: '设备待修，已送修不能调配', fromRoomName: null }
  ]
}

const mountPage = () => mount(ComboList, {
  global: {
    plugins: [ElementPlus]
  }
})

const findButton = (wrapper, text) =>
  wrapper.findAll('button').find(btn => btn.text().includes(text))

describe('ComboList 常用影音组合页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routerPush.mockClear()
    floorApi.getAll.mockResolvedValue(floors)
    roomApi.getAll.mockResolvedValue(rooms)
    deviceApi.getAll.mockResolvedValue(devices)
  })

  it('挂载加载组合列表并展示设备类型汇总', async () => {
    comboApi.getAll.mockResolvedValue([makeCombo(1)])
    const wrapper = mountPage()
    await flushPromises()

    expect(comboApi.getAll).toHaveBeenCalledWith(undefined)
    expect(wrapper.text()).toContain('贵宾接待组合')
    expect(wrapper.text()).toContain('电视×1')
    expect(wrapper.text()).toContain('麦克风×2')
  })

  it('新建组合提交设备清单与创建人', async () => {
    comboApi.getAll.mockResolvedValue([])
    comboApi.create.mockResolvedValue(makeCombo(9))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.vm.openEdit(null)
    await flushPromises()

    wrapper.vm.editForm.comboName = '  简配组合  '
    wrapper.vm.editForm.createdBy = '管理员'
    wrapper.vm.editForm.deviceIds = [101, 102]
    await wrapper.vm.submitCombo()
    await flushPromises()

    expect(comboApi.create).toHaveBeenCalledWith({
      comboName: '简配组合',
      remark: null,
      status: 1,
      createdBy: '管理员',
      deviceIds: [101, 102]
    })
    expect(comboApi.getAll).toHaveBeenCalledTimes(2)
  })

  it('未选设备时禁止保存组合', async () => {
    comboApi.getAll.mockResolvedValue([])
    const wrapper = mountPage()
    await flushPromises()
    await wrapper.vm.openEdit(null)
    await flushPromises()

    wrapper.vm.editForm.comboName = '空组合'
    await wrapper.vm.submitCombo()
    expect(comboApi.create).not.toHaveBeenCalled()
  })

  it('一键套用后展示调入/跳过统计、原因及前后对照', async () => {
    comboApi.getAll
      .mockResolvedValueOnce([makeCombo(1)])
      .mockResolvedValueOnce([makeCombo(1)])
    comboApi.apply.mockResolvedValue(applyResult)
    const wrapper = mountPage()
    await flushPromises()

    wrapper.vm.openApply(makeCombo(1))
    wrapper.vm.applyForm.roomId = 10
    wrapper.vm.applyForm.operator = ' 值班员小王 '
    await wrapper.vm.confirmApply()
    await flushPromises()

    expect(comboApi.apply).toHaveBeenCalledWith(1, {
      roomId: 10,
      operator: '值班员小王',
      remark: null
    })

    expect(wrapper.text()).toContain('TZ20260901010001')
    expect(wrapper.text()).toContain('设备正在接待室「302接待室」使用')
    expect(wrapper.text()).toContain('设备待修，已送修不能调配')
    expect(wrapper.text()).toContain('套用后房间设备')
    // 结果中标记的新增数量
    expect(wrapper.text()).toContain('新增 2 台')
  })

  it('停用组合的一键套用按钮不可用', async () => {
    comboApi.getAll.mockResolvedValue([makeCombo(2, { status: 0, statusText: '停用' })])
    const wrapper = mountPage()
    await flushPromises()
    const btn = findButton(wrapper, '一键套用')
    expect(btn.attributes('disabled')).toBeDefined()
  })
})
