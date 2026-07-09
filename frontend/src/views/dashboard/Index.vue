<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in stats" :key="item.label">
        <el-card><el-statistic :title="item.label" :value="item.value" /></el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12"><div ref="pieChart" style="height:300px"></div></el-col>
      <el-col :span="12"><div ref="lineChart" style="height:300px"></div></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const stats = ref([
  { label: '今日识别', value: 0 },
  { label: '待处理工单', value: 0 },
  { label: '总林区数', value: 0 },
  { label: '本月火情', value: 0 }
])
const pieChart = ref(null)
const lineChart = ref(null)

onMounted(async () => {
  const res = await request.get('/statistics/overview')
  if (res.data.code === 200) {
    const d = res.data.data
    stats.value = [
      { label: '今日识别', value: d.todayDetections || 0 },
      { label: '待处理工单', value: d.pendingOrders || 0 },
      { label: '总林区数', value: d.totalForests || 0 },
      { label: '本月火情', value: d.monthFire || 0 }
    ]
  }
  // 图表渲染...
  initCharts()
})

const initCharts = () => {
  // 饼图
  const pie = echarts.init(pieChart.value)
  pie.setOption({
    title: { text: '隐患类型分布' },
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: '50%', data: [
      { name: '明火', value: 30 }, { name: '烟雾', value: 20 }, { name: '人员', value: 10 }
    ]}]
  })
  // 折线图
  const line = echarts.init(lineChart.value)
  line.setOption({
    title: { text: '月度火情趋势' },
    xAxis: { type: 'category', data: ['1月','2月','3月','4月','5月','6月'] },
    yAxis: { type: 'value' },
    series: [{ type: 'line', data: [5, 8, 12, 9, 6, 4] }]
  })
}
</script>