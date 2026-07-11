import request from '@/utils/request'

export const getNotificationList = (params) => request.get('/notification/list', { params })

export const getUnreadCount = () => request.get('/notification/unread-count')

export const markAsRead = (id) => request.put(`/notification/read/${id}`)

export const markAllRead = () => request.put('/notification/read-all')

export const deleteNotification = (id) => request.delete(`/notification/delete/${id}`)
