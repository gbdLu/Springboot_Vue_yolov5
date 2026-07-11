import request from '@/utils/request'

export const getKnowledgeList = (params) => request.get('/knowledge/list', { params })

export const getKnowledgeDetail = (id) => request.get(`/knowledge/detail/${id}`)

export const addKnowledge = (data) => request.post('/knowledge/add', data)

export const updateKnowledge = (data) => request.put('/knowledge/update', data)

export const deleteKnowledge = (id) => request.delete(`/knowledge/delete/${id}`)

export const getCategories = () => request.get('/knowledge/categories')
