import request from '@/utils/request'

export const getOverview = () => request.get('/statistics/overview')

export const getPieChart = () => request.get('/statistics/pie')

export const getTrend = () => request.get('/statistics/trend')

export const getBarChart = () => request.get('/statistics/bar')

export const getEfficiency = () => request.get('/statistics/efficiency')

export const getHighRisk = () => request.get('/statistics/high-risk')

export const getHeatmap = () => request.get('/statistics/heatmap')
