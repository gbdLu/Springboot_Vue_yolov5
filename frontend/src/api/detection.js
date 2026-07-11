import request from '@/utils/request'

export const uploadAndDetect = (formData) =>
  request.post('/detection/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })

export const getDetectionHistory = (params) => request.get('/detection/history', { params })

export const getDetectionDetail = (id) => request.get(`/detection/detail/${id}`)

export const deleteDetection = (id) => request.delete(`/detection/delete/${id}`)
