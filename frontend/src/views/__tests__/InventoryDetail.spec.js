import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { createRouter, createMemoryHistory } from 'vue-router'
import InventoryDetail from '../InventoryDetail.vue'
import { inventoryApi, floorApi } from '../../api'

vi.mock('../../api', () => ({
  inventoryApi: {
    getBatch: vi.fn(),
    getItems: vi.fn(),
    checkItem: vi.fn(),
    submitBatch: vi.fn(),
    closeBatch: vi.fn(),
    transferItem: vi.fn(),
    resolveItem: vi.fn()
  },
  floorApi: { getAll: vi.fn() },
  roomApi: { getByFloor: vi.fn() }
}))

const makeBatch = (overrides = {}) => ({
  id: 7,
  batchNo: 'PD202609010101',
  batchName: '3F盘点',
  scopeType: 'FLOOR',
  scopeTypeText: '楼层',
  floorName: '3F',
  roomName: null,
  operator: '张三',
  status: 0,
  statusText: '盘点中',
  snapshotTime: '2026-09-01T10:00:00',
  submittedAt: null,
  totalCount: 2,
  checkedCount: 0,
  presentCount: 0,
  missingCount: 0,
  mismatchCount: 0,
  repairCount: 0,
  progressPercent: 0,
  pendingDiffCount: 0,
  ...overrides
})

const makeItem = (id, overrides = {}) => ({
  id,
  batchId: 7,
  deviceId: 100 + id,
  deviceCode: `DEV${id}`,
  deviceName: `设备${id}`,
  deviceType: '电视',
  snapshotFloorId: 1,
  snapshotFloorName: '3F',
  snapshotRoomId: 10,
  snapshotRoomName: '301',
  checkResult: null,
  checkResultText: '未盘点',
  remark: '',
  ledgerFloorId: null,
  ledgerRoomId: null,
  locationMismatch: false,
  processStatus: null,
  processStatusText: null,
  ...overrides
})

const mountRoute = async () => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/inventory/:id', component: InventoryDetail }, { path: '/inventory', component: { template: '<div />' } }]
  })
  await router.push('/inventory/7')
  await router.isReady()
  return mount(InventoryDetail, {
    global: { plugins: [ElementPlus, router] }
  })
}

describe('InventoryDetail 盘点工作台', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    floorApi.getAll.mockResolvedValue([{ id: 1, floorName: '3F' }])
  })

  it('盘点中逐台标记调用 checkItem 并刷新汇总', async () => {
    inventoryApi.getBatch.mockResolvedValue(makeBatch())
    inventoryApi.getItems.mockResolvedValue([makeItem(1), makeItem(2)])
    inventoryApi.checkItem.mockImplementation(async (batchId, itemId, data) => ({
      ...makeItem(itemId),
      checkResult: data.checkResult,
      remark: data.remark
    }))
    // 初次加载 + 标记后刷新，批次进度推进到 50%
    inventoryApi.getBatch
      .mockResolvedValueOnce(makeBatch())
      .mockResolvedValue(makeBatch({ checkedCount: 1, presentCount: 1, progressPercent: 50 }))

    const wrapper = await mountRoute()
    await flushPromises()

    // 第一台设备点击“在场”单选
    const radioInputs = wrapper.findAll('input[type=radio]')
    expect(radioInputs.length).toBeGreaterThanOrEqual(4)
    await radioInputs[0].setValue()
    await flushPromises()

    expect(inventoryApi.checkItem).toHaveBeenCalledWith('7', 1, { checkResult: 1, remark: '' })
  })

  it('盘点中可直接关闭，关闭后批次结束且未盘设备不再有盘点中操作', async () => {
    inventoryApi.getBatch
      .mockResolvedValueOnce(makeBatch({ status: 0, checkedCount: 1, totalCount: 2, progressPercent: 50 }))
      .mockResolvedValue(makeBatch({
        status: 2,
        statusText: '已关闭',
        checkedCount: 1,
        presentCount: 1,
        totalCount: 2,
        progressPercent: 50
      }))
    inventoryApi.getItems.mockResolvedValue([
      makeItem(1, { checkResult: 1, checkResultText: '在场' }),
      makeItem(2)
    ])
    inventoryApi.closeBatch.mockResolvedValue(makeBatch({ status: 2, statusText: '已关闭' }))

    const wrapper = await mountRoute()
    await flushPromises()

    // 盘点中同时提供提交与关闭入口
    expect(wrapper.text()).toContain('提交盘点')
    expect(wrapper.text()).toContain('关闭批次')

    // 直接走关闭处理（popconfirm 气泡在 jsdom 下不渲染）
    await wrapper.vm.handleClose()
    await flushPromises()

    expect(inventoryApi.closeBatch).toHaveBeenCalledWith('7')

    // 关闭后只读：不再有提交/盘点单选，未盘设备仍展示为未盘点
    expect(wrapper.text()).toContain('批次已关闭，只读')
    expect(wrapper.text()).not.toContain('提交盘点')
    expect(wrapper.findAll('input[type=radio]')).toHaveLength(0)
    expect(wrapper.text()).toContain('未盘点')
  })

  it('已关闭批次不展示提交按钮与差异处理操作', async () => {
    inventoryApi.getBatch.mockResolvedValue(makeBatch({
      status: 2,
      statusText: '已关闭',
      checkedCount: 2,
      progressPercent: 100
    }))
    inventoryApi.getItems.mockResolvedValue([
      makeItem(1, { checkResult: 2, checkResultText: '缺失', ledgerFloorId: 1, ledgerFloorName: '3F', ledgerRoomId: 10, ledgerRoomName: '301', processStatus: 2, processStatusText: '已处理' }),
      makeItem(2, { checkResult: 1, checkResultText: '在场', ledgerFloorId: 1, ledgerFloorName: '3F', ledgerRoomId: 10, ledgerRoomName: '301' })
    ])

    const wrapper = await mountRoute()
    await flushPromises()

    expect(wrapper.text()).toContain('批次已关闭，只读')
    // 不应出现去调配/登记处理等可写操作按钮
    expect(wrapper.text()).not.toContain('去调配')
    expect(wrapper.text()).not.toContain('登记处理')
    expect(wrapper.text()).not.toContain('提交盘点')
    // 仍展示冻结的差异结果
    expect(wrapper.text()).toContain('缺失')
  })

  it('已提交批次的位置不符设备展示“去调配”入口', async () => {
    inventoryApi.getBatch.mockResolvedValue(makeBatch({
      status: 1,
      statusText: '已提交',
      checkedCount: 2,
      totalCount: 2,
      progressPercent: 100,
      mismatchCount: 1,
      presentCount: 1,
      pendingDiffCount: 1
    }))
    inventoryApi.getItems.mockResolvedValue([
      makeItem(1, { checkResult: 3, checkResultText: '位置不符', ledgerFloorId: 2, ledgerFloorName: '4F', ledgerRoomId: 20, ledgerRoomName: '401', locationMismatch: true, processStatus: 1, processStatusText: '待处理' }),
      makeItem(2, { checkResult: 1, checkResultText: '在场', ledgerFloorId: 1, ledgerFloorName: '3F', ledgerRoomId: 10, ledgerRoomName: '301' })
    ])

    const wrapper = await mountRoute()
    await flushPromises()

    expect(wrapper.text()).toContain('去调配')
    expect(wrapper.text()).toContain('待处理差异')
  })
})
