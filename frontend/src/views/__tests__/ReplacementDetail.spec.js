import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import ReplacementDetail from '../ReplacementDetail.vue'
import { replacementApi } from '../../api'

const routerPush = vi.fn()

vi.mock('../../api', () => ({
  replacementApi: {
    getById: vi.fn(),
    resolve: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1' } }),
  useRouter: () => ({ push: routerPush })
}))

const makeDetail = (overrides = {}) => ({
  id: 1,
  replacementNo: 'TH2026090112000001',
  floorId: 1,
  floorName: '3F',
  roomId: 10,
  roomName: '301接待室',
  roomCode: 'R301',
  faultyDeviceId: 101,
  faultyDeviceCode: 'MIC-001',
  faultyDeviceName: '手持麦克风A',
  faultyDeviceType: '麦克风',
  faultyDeviceStatus: 2,
  faultyDeviceStatusText: '待维修',
  faultyDeviceRoomId: null,
  faultyDeviceRoomName: null,
  spareDeviceId: 201,
  spareDeviceCode: 'MIC-B1',
  spareDeviceName: '备用麦克风',
  spareDeviceType: '麦克风',
  spareDeviceStatus: 1,
  spareDeviceStatusText: '正常',
  spareDeviceRoomId: 10,
  spareDeviceRoomName: '301接待室',
  faultPhenomenon: '发言时无声音',
  operator: '值班员甲',
  replacementTime: '2026-09-01T10:00:00',
  processResult: 1,
  processResultText: '待维修',
  processRemark: null,
  processedBy: null,
  processedAt: null,
  remark: '',
  ...overrides
})

const mountPage = () => mount(ReplacementDetail, {
  global: { plugins: [ElementPlus] }
})

describe('ReplacementDetail 替换记录详情', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('加载详情并对照展示卸下与换上设备、故障现象与原因', async () => {
    replacementApi.getById.mockResolvedValue(makeDetail())
    const wrapper = mountPage()
    await flushPromises()

    expect(replacementApi.getById).toHaveBeenCalledWith('1')
    const text = wrapper.text()
    expect(text).toContain('手持麦克风A')
    expect(text).toContain('备用麦克风')
    expect(text).toContain('发言时无声音')
    expect(text).toContain('值班员甲')
    expect(text).toContain('301接待室')
    // 故障机实时状态为待维修且已不在接待室
    expect(text).toContain('待维修')
    // 待维修状态展示登记入口
    expect(wrapper.findAll('button').some(b => b.text().includes('登记处理结果'))).toBe(true)
  })

  it('提交已修复处理结果并同步更新页面', async () => {
    replacementApi.getById.mockResolvedValue(makeDetail())
    const repaired = makeDetail({
      processResult: 2,
      processResultText: '已修复',
      processRemark: '更换音头后测试正常',
      processedBy: '维修员乙',
      processedAt: '2026-09-02T09:00:00',
      faultyDeviceStatus: 1,
      faultyDeviceStatusText: '正常'
    })
    replacementApi.resolve.mockResolvedValue(repaired)

    const wrapper = mountPage()
    await flushPromises()

    wrapper.vm.resolveForm.processResult = 2
    wrapper.vm.resolveForm.processRemark = '更换音头后测试正常'
    wrapper.vm.resolveForm.processedBy = '维修员乙'
    await wrapper.vm.confirmResolve()
    await flushPromises()

    expect(replacementApi.resolve).toHaveBeenCalledWith('1', {
      processResult: 2,
      processRemark: '更换音头后测试正常',
      processedBy: '维修员乙'
    })
    expect(wrapper.text()).toContain('已修复')
    expect(wrapper.text()).toContain('更换音头后测试正常')
  })

  it('已登记处理结果的记录不再显示登记按钮', async () => {
    replacementApi.getById.mockResolvedValue(makeDetail({
      processResult: 3,
      processResultText: '已报废',
      processRemark: '无维修价值',
      processedBy: '维修员乙',
      processedAt: '2026-09-02T09:00:00'
    }))
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.findAll('button').some(b => b.text().includes('登记处理结果'))).toBe(false)
    expect(wrapper.text()).toContain('已报废')
    expect(wrapper.text()).toContain('无维修价值')
  })

  it('处理备注或登记人为空时拒绝提交', async () => {
    replacementApi.getById.mockResolvedValue(makeDetail())
    const wrapper = mountPage()
    await flushPromises()

    wrapper.vm.resolveForm.processResult = 3
    wrapper.vm.resolveForm.processRemark = '   '
    wrapper.vm.resolveForm.processedBy = '维修员乙'
    await wrapper.vm.confirmResolve()

    expect(replacementApi.resolve).not.toHaveBeenCalled()
  })
})
