import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import InventoryList from '../InventoryList.vue'
import { inventoryApi, floorApi } from '../../api'

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return { ...actual, ElMessageBox: { confirm: vi.fn().mockResolvedValue(true) } }
})

vi.mock('../../api', () => ({
  inventoryApi: {
    getBatches: vi.fn(),
    createBatch: vi.fn(),
    closeBatch: vi.fn()
  },
  floorApi: {
    getAll: vi.fn()
  },
  roomApi: {
    getByFloor: vi.fn()
  }
}))

const makeBatch = (id, overrides = {}) => ({
  id,
  batchNo: `PD20260901${String(id).padStart(3, '0')}`,
  batchName: `批次${id}`,
  scopeType: 'FLOOR',
  scopeTypeText: '楼层',
  floorName: '3F',
  roomName: null,
  operator: '张三',
  status: 0,
  statusText: '盘点中',
  snapshotTime: '2026-09-01T10:00:00',
  totalCount: 10,
  checkedCount: 4,
  presentCount: 4,
  missingCount: 0,
  mismatchCount: 0,
  repairCount: 0,
  progressPercent: 40,
  pendingDiffCount: 0,
  ...overrides
})

const pageResult = (records, total) => ({
  records,
  total,
  pageNum: 1,
  pageSize: 10,
  pages: Math.ceil(total / 10)
})

const mountPage = () => mount(InventoryList, {
  global: {
    plugins: [ElementPlus],
    mocks: { $router: { push: vi.fn() } }
  }
})

describe('InventoryList 盘点批次列表', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    floorApi.getAll.mockResolvedValue([{ id: 1, floorName: '3F' }])
  })

  it('初始加载批次分页与楼层数据', async () => {
    inventoryApi.getBatches.mockResolvedValue(
      pageResult([makeBatch(1), makeBatch(2, { status: 1, statusText: '已提交', pendingDiffCount: 3 })], 23)
    )

    const wrapper = mountPage()
    await flushPromises()

    expect(inventoryApi.getBatches).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })
    expect(floorApi.getAll).toHaveBeenCalledTimes(1)
    expect(wrapper.findAll('.el-table__row')).toHaveLength(2)
    expect(wrapper.find('.el-pagination__total').text()).toContain('23')
    // 已提交且有待处理差异时展示待处理标记
    expect(wrapper.text()).toContain('待处理')
  })

  it('盘点中与已提交批次均提供关闭入口，已关闭不提供', async () => {
    inventoryApi.getBatches.mockResolvedValue(pageResult([
      makeBatch(1, { status: 0 }),
      makeBatch(2, { status: 1, statusText: '已提交' }),
      makeBatch(3, { status: 2, statusText: '已关闭' })
    ], 3))

    const wrapper = mountPage()
    await flushPromises()

    const rows = wrapper.findAll('.el-table__row')
    const countClose = (row) => row.findAll('button').filter(b => b.text().includes('关闭')).length
    expect(countClose(rows[0])).toBe(1)
    expect(countClose(rows[1])).toBe(1)
    expect(countClose(rows[2])).toBe(0)

    // 盘点中关闭直接结束批次（跳过确认框）
    await wrapper.vm.handleClose({ id: 1, batchName: '批次1', status: 0 })
    expect(inventoryApi.closeBatch).toHaveBeenCalledWith(1)
  })

  it('按状态与名称搜索时重置到第一页并透传查询参数', async () => {
    inventoryApi.getBatches.mockResolvedValue(pageResult([], 0))
    const wrapper = mountPage()
    await flushPromises()

    // 通过组件实例直接设置筛选并触发搜索（el-select 在 jsdom 下交互受限）
    wrapper.vm.searchName = '3F'
    wrapper.vm.searchStatus = 1
    await wrapper.find('.search-bar .el-button').trigger('click')
    await flushPromises()

    expect(inventoryApi.getBatches).toHaveBeenLastCalledWith({
      pageNum: 1,
      pageSize: 10,
      batchName: '3F',
      status: 1
    })
  })
})
