<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in stats" :key="item.label">
        <el-card shadow="hover">
          <el-statistic :title="item.label" :value="item.value">
            <template #prefix>
              <span>{{ item.icon }}</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="8">
        <el-card>
          <template #header>📊 隐患类型分布</template>
          <div ref="pieChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>📈 近30天识别趋势</template>
          <div ref="lineChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>📊 近6月工单统计</template>
          <div ref="barChart" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <template #header>⚡ 处置效率</template>
          <div style="display:flex;gap:40px;padding:20px">
            <el-statistic title="平均处置时长(h)" :value="efficiency.avgHandleHours || 0" />
            <el-statistic title="处置率" :value="(efficiency.handleRate || 0) + '%'" />
            <el-statistic title="审核通过率" :value="(efficiency.passRate || 0) + '%'" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>🔴 高风险林区 TOP5</template>
          <el-table :data="highRisk" stripe>
            <el-table-column prop="areaName" label="林区" />
            <el-table-column prop="orderCount" label="工单数" />
            <el-table-column label="火险等级">
              <template #default="{ row }">
                <el-tag :type="row.fireRiskLevel === 3 ? 'danger' : row.fireRiskLevel === 2 ? 'warning' : 'success'">
                  {{ ['', '低', '中', '高'][row.fireRiskLevel] }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getOverview, getPieChart, getTrend, getBarChart, getEfficiency, getHighRisk } from '@/api/statistics'

const stats = ref([
  { icon: '🔥', label: '今日识别', value: 0 },
  { icon: '📋', label: '待处理工单', value: 0 },
  { icon: '🌲', label: '总林区数', value: 0 },
  { icon: '⚠️', label: '本月火情', value: 0 }
])
const efficiency = ref({})
const highRisk = ref([])
const pieChart = ref(null)
const lineChart = ref(null)
const barChart = ref(null)

const initPieChart = (data) => {
  if (!pieChart.value) return
  const chart = echarts.init(pieChart.value)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      label: { formatter: '{b}\n{d}%' },
      data: data.length ? data : [{ name: '暂无数据', value: 0 }]
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

const initLineChart = (data) => {
  if (!lineChart.value) return
  const chart = echarts.init(lineChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.date), axisLabel: { rotate: 45 } },
    yAxis: { type: 'value', minInterval: 1 },
    grid: { left: 50, right: 20, bottom: 60, top: 20 },
    series: [{
      type: 'line', data: data.map(d => d.count), smooth: true,
      areaStyle: { color: 'rgba(64,158,255,0.2)' },
      lineStyle: { color: '#409EFF' },
      itemStyle: { color: '#409EFF' }
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

const initBarChart = (data) => {
  if (!barChart.value) return
  const chart = echarts.init(barChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.month) },
    yAxis: { type: 'value', minInterval: 1 },
    grid: { left: 50, right: 20, bottom: 30, top: 20 },
    series: [{
      type: 'bar', data: data.map(d => d.count),
      itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: '#409EFF' }, { offset: 1, color: '#79bbff' }
      ]), borderRadius: [4, 4, 0, 0] }
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

onMounted(async () => {
  try {
    const [overviewRes, pieRes, trendRes, barRes, effRes, highRes] = await Promise.all([
      getOverview(), getPieChart(), getTrend(), getBarChart(), getEfficiency(), getHighRisk()
    ])

    if (overviewRes.data.code === 200) {
      const d = overviewRes.data.data
      stats.value = [
        { icon: '🔥', label: '今日识别', value: d.todayDetections || 0 },
        { icon: '📋', label: '待处理工单', value: d.pendingOrders || 0 },
        { icon: '🌲', label: '总林区数', value: d.totalForests || 0 },
        { icon: '⚠️', label: '本月火情', value: d.monthFire || 0 }
      ]
    }

    if (effRes.data.code === 200) efficiency.value = effRes.data.data
    if (highRes.data.code === 200) highRisk.value = highRes.data.data

    await nextTick()
    if (pieRes.data.code === 200) initPieChart(pieRes.data.data)
    if (trendRes.data.code === 200) initLineChart(trendRes.data.data)
    if (barRes.data.code === 200) initBarChart(barRes.data.data)
  } catch (e) {
    console.error('Dashboard load error:', e)
  }
})
</script>

<style scoped>
.dashboard { padding: 20px; }
</style>
