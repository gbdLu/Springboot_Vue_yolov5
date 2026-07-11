import request from '@/utils/request'

export const login = (data) => request.post('/auth/login', data)

export const logout = () => request.post('/auth/logout')

export const changePassword = (data) => request.post('/auth/change-password', data)

export const getUserInfo = (id) => request.get('/user/info', { params: id ? { id } : {} })

export const register = (data) => request.post('/auth/register', data)

export const getGuards = () => request.get('/user/guards')
