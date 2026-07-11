import request from '@/utils/request'

export const getForestList = (params) => request.get('/forest/list', { params })

export const addForest = (data) => request.post('/forest/add', data)

export const updateForest = (data) => request.put('/forest/update', data)

export const deleteForest = (id) => request.delete(`/forest/delete/${id}`)

export const exportForest = () => request.get('/forest/export')

// 监控点
export const getPoints = (areaId) => request.get(`/forest/${areaId}/points`)

export const addPoint = (data) => request.post('/forest/point/add', data)

export const updatePoint = (data) => request.put('/forest/point/update', data)

export const deletePoint = (id) => request.delete(`/forest/point/delete/${id}`)

export const getPointDetail = (id) => request.get(`/forest/point/detail/${id}`)
