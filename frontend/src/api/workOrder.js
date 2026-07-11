import request from '@/utils/request'

export const getWorkOrderList = (params) => request.get('/workorder/list', { params })

export const getWorkOrderDetail = (id) => request.get(`/workorder/detail/${id}`)

export const assignWorkOrder = (data) => request.post('/workorder/assign', data)

export const disposeWorkOrder = (data) => request.post('/workorder/dispose', data)

export const reviewWorkOrder = (data) => request.post('/workorder/review', data)

export const getMyOrders = (params) => request.get('/workorder/my-orders', { params })

export const exportWorkOrders = () => request.get('/workorder/export')

export const getWorkOrderStatistics = () => request.get('/workorder/statistics')
