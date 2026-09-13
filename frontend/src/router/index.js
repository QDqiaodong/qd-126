import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue')
  },
  {
    path: '/device',
    name: 'DeviceList',
    component: () => import('../views/DeviceList.vue')
  },
  {
    path: '/device/add',
    name: 'DeviceAdd',
    component: () => import('../views/DeviceAdd.vue')
  },
  {
    path: '/device/edit/:id',
    name: 'DeviceEdit',
    component: () => import('../views/DeviceEdit.vue')
  },
  {
    path: '/device/detail/:id',
    name: 'DeviceDetail',
    component: () => import('../views/DeviceDetail.vue')
  },
  {
    path: '/floor',
    name: 'FloorList',
    component: () => import('../views/FloorList.vue')
  },
  {
    path: '/room',
    name: 'RoomList',
    component: () => import('../views/RoomList.vue')
  },
  {
    path: '/transfer',
    name: 'TransferList',
    component: () => import('../views/TransferList.vue')
  },
  {
    path: '/spec-template',
    name: 'SpecTemplateList',
    component: () => import('../views/SpecTemplateList.vue')
  },
  {
    path: '/grouped',
    name: 'GroupedView',
    component: () => import('../views/GroupedView.vue')
  },
  {
    path: '/warranty',
    name: 'WarrantyView',
    component: () => import('../views/WarrantyView.vue')
  },
  {
    path: '/inventory',
    name: 'InventoryList',
    component: () => import('../views/InventoryList.vue')
  },
  {
    path: '/inventory/:id',
    name: 'InventoryDetail',
    component: () => import('../views/InventoryDetail.vue')
  },
  {
    path: '/activity',
    name: 'ActivityList',
    component: () => import('../views/ActivityList.vue')
  },
  {
    path: '/quiet-period',
    name: 'QuietPeriodList',
    component: () => import('../views/QuietPeriodList.vue')
  },
  {
    path: '/interpreter-booking',
    name: 'InterpreterBookingList',
    component: () => import('../views/InterpreterBookingList.vue')
  },
  {
    path: '/welcome-board',
    name: 'WelcomeBoardList',
    component: () => import('../views/WelcomeBoardList.vue')
  },
  {
    path: '/activity/:id',
    name: 'ActivityDetail',
    component: () => import('../views/ActivityDetail.vue')
  },
  {
    path: '/replacement',
    name: 'ReplacementList',
    component: () => import('../views/ReplacementList.vue')
  },
  {
    path: '/replacement/:id',
    name: 'ReplacementDetail',
    component: () => import('../views/ReplacementDetail.vue')
  },
  {
    path: '/combo',
    name: 'ComboList',
    component: () => import('../views/ComboList.vue')
  },
  {
    path: '/combo-record',
    name: 'ComboRecordList',
    component: () => import('../views/ComboRecordList.vue')
  },
  {
    path: '/combo-record/:id',
    name: 'ComboRecordDetail',
    component: () => import('../views/ComboRecordDetail.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
