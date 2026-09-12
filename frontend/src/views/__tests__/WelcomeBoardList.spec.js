import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import WelcomeBoardList from '../WelcomeBoardList.vue'
import { welcomeBoardApi, floorApi, roomApi } from '../../api'

vi.mock('../../api', () => ({
  welcomeBoardApi: {
    getPage: vi.fn(),
    create: vi.fn(),
    getById: vi.fn(),
    getOverdue: vi.fn(),
    remove: vi.fn(),
    getRoomAvailability: vi.fn()
  },
  floorApi: { getAll: vi.fn() },
  roomApi: { getByFloor: vi.fn() }
}))

const makeBoard = (id, overrides = {}) => ({
  id,
  boardNo: `YP20260912${String(id).padStart(4, '0')}`,
  boardText: `热烈欢迎考察团${id}莅临指导`,
  floorId: 1,
  floorName: '3F',
  roomId: 10,
  roomName: '301接待室',
  roomCode: 'R301',
  mountTime: '2026-09-12T08:00:00',
  plannedRemoveTime: '2026-09-12T10:00:00',
  registrar: '行政小王',
  status: 1,
  statusText: '已上墙',
  overdueRemove: false,
  removeReceipt: null,
  ...overrides
})

const pageResult = (records, total) => ({
  records,
  total,
  pageNum: 1,
  pageSize: 10,
  pages: Math.ceil(total / 10)
})

const makeRoom = (id, name, overrides = {}) => ({
  id,
  roomName: name,
  roomCode: `R${name}`,
  floorId: 1,
  status: 1,
  ...overrides
})

const mountPage = () => mount(WelcomeBoardList, {
  global: {
    plugins: [ElementPlus]
  }
})

describe('WelcomeBoardList 接待室欢迎牌排期', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    floorApi.getAll.mockResolvedValue([{ id: 1, floorName: '3F' }])
    roomApi.getByFloor.mockResolvedValue([makeRoom(10, '301接待室')])
    welcomeBoardApi.getRoomAvailability.mockResolvedValue([
      { roomId: 10, roomName: '301接待室', floorId: 1, floorName: '3F', roomEnabled: true, available: false, boardText: '在墙文案', overdueRemove: false },
      { roomId: 20, roomName: '302接待室', floorId: 1, floorName: '3F', roomEnabled: true, available: true }
    ])
  })

  it('初始加载分页排期与可接待情况，待撤标记在行与文案上可见', async () => {
    welcomeBoardApi.getPage.mockResolvedValue(pageResult([
      makeBoard(1),
      makeBoard(2, { status: 1, overdueRemove: true, plannedRemoveTime: '2026-09-12T09:00:00' }),
      makeBoard(3, { status: 0, statusText: '待上墙' }),
      makeBoard(4, { status: 2, statusText: '已撤下', overdueRemove: false, removeReceipt: '已取下归库' })
    ], 4))

    const wrapper = mountPage()
    await flushPromises()

    expect(welcomeBoardApi.getPage).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })
    expect(welcomeBoardApi.getRoomAvailability).toHaveBeenCalledWith({})
    expect(wrapper.findAll('.el-table__row')).toHaveLength(4)

    const text = wrapper.text()
    expect(text).toContain('待撤')
    expect(text).toContain('不可接待')
    expect(text).toContain('可接待')
    // 到期未撤的行带待撤背景样式类
    expect(wrapper.findAll('.row-overdue')).toHaveLength(1)
  })

  it('筛选透传楼层/接待室/状态/仅待撤参数并重置页码', async () => {
    welcomeBoardApi.getPage.mockResolvedValue(pageResult([], 0))
    const wrapper = mountPage()
    await flushPromises()

    wrapper.vm.searchFloorId = 1
    await wrapper.vm.onSearchFloorChange(1)
    expect(roomApi.getByFloor).toHaveBeenCalledWith(1)
    wrapper.vm.searchRoomId = 10
    wrapper.vm.searchStatus = 1
    wrapper.vm.overdueOnly = true
    await wrapper.vm.searchBoards()

    expect(welcomeBoardApi.getPage).toHaveBeenLastCalledWith({
      pageNum: 1,
      pageSize: 10,
      floorId: 1,
      roomId: 10,
      status: 1,
      overdueOnly: true
    })
  })

  it('登记欢迎牌前校验必填项，提交文案/接待室/上墙撤下时间/登记人', async () => {
    welcomeBoardApi.getPage.mockResolvedValue(pageResult([], 0))
    welcomeBoardApi.create.mockResolvedValue(makeBoard(99, { status: 0, statusText: '待上墙' }))
    const wrapper = mountPage()
    await flushPromises()

    // 缺字段时拦截
    wrapper.vm.createVisible = true
    await wrapper.vm.confirmCreate()
    expect(welcomeBoardApi.create).not.toHaveBeenCalled()

    wrapper.vm.createForm.floorId = 1
    await wrapper.vm.onCreateFloorChange(1)
    wrapper.vm.createForm.roomId = 10
    wrapper.vm.createForm.boardText = '热烈欢迎考察团莅临指导'
    wrapper.vm.createForm.mountTime = '2026-09-20T08:00:00'
    wrapper.vm.createForm.plannedRemoveTime = '2026-09-20T10:00:00'
    wrapper.vm.createForm.registrar = '行政小王'
    await wrapper.vm.confirmCreate()
    await flushPromises()

    expect(welcomeBoardApi.create).toHaveBeenCalledWith({
      boardText: '热烈欢迎考察团莅临指导',
      roomId: 10,
      mountTime: '2026-09-20T08:00:00',
      plannedRemoveTime: '2026-09-20T10:00:00',
      registrar: '行政小王',
      remark: ''
    })
    // 登记后列表与可接待情况都刷新
    expect(welcomeBoardApi.getPage).toHaveBeenCalledTimes(2)
    expect(welcomeBoardApi.getRoomAvailability).toHaveBeenCalledTimes(2)
  })

  it('撤下不写回执被前端拦住；写回执后才能调用撤下接口', async () => {
    welcomeBoardApi.getPage.mockResolvedValue(pageResult([
      makeBoard(2, { overdueRemove: true })
    ], 1))
    welcomeBoardApi.remove.mockResolvedValue(makeBoard(2, {
      status: 2,
      statusText: '已撤下',
      overdueRemove: false,
      removeReceipt: '欢迎牌已取下，物料归库'
    }))
    const wrapper = mountPage()
    await flushPromises()

    wrapper.vm.openRemove(wrapper.vm.boards[0])
    expect(wrapper.vm.removeVisible).toBe(true)
    expect(wrapper.vm.removeRow.overdueRemove).toBe(true)

    // 回执为空：拦截
    wrapper.vm.removeReceipt = '   '
    await wrapper.vm.confirmRemove()
    expect(welcomeBoardApi.remove).not.toHaveBeenCalled()

    // 写回执后撤下
    wrapper.vm.removeReceipt = '欢迎牌已取下，物料归库'
    wrapper.vm.removeOperator = '李四'
    await wrapper.vm.confirmRemove()
    await flushPromises()

    expect(welcomeBoardApi.remove).toHaveBeenCalledWith(2, {
      receipt: '欢迎牌已取下，物料归库',
      operator: '李四'
    })
    expect(welcomeBoardApi.getPage).toHaveBeenCalledTimes(2)
    expect(welcomeBoardApi.getRoomAvailability).toHaveBeenCalledTimes(2)
  })
})
