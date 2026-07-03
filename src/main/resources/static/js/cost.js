/**
 * 成本管理页面 - ECharts 图表 + 数据加载
 */
class CostManager {
    constructor() {
        this.currentPeriod = 'today';
        this.charts = {};
        this.initCharts();
        this.initEventListeners();
        this.loadStats('today');
        window.addEventListener('resize', () => this.resizeCharts());
    }

    initCharts() {
        const theme = {
            textStyle: { color: '#5f594f' },
            backgroundColor: 'transparent'
        };
        this.charts.dailyCost = echarts.init(document.getElementById('dailyCostChart'));
        this.charts.modelPie = echarts.init(document.getElementById('modelPieChart'));
        this.charts.dailyCall = echarts.init(document.getElementById('dailyCallChart'));
        this.charts.modelCall = echarts.init(document.getElementById('modelCallChart'));
    }

    resizeCharts() {
        Object.values(this.charts).forEach(chart => chart.resize());
    }

    initEventListeners() {
        document.querySelectorAll('.time-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                document.querySelectorAll('.time-btn').forEach(b => b.classList.remove('active'));
                e.target.classList.add('active');
                const period = e.target.dataset.period;
                if (period === 'custom') {
                    document.querySelector('.custom-range').style.display = 'flex';
                } else {
                    document.querySelector('.custom-range').style.display = 'none';
                    this.loadStats(period);
                }
            });
        });

        document.getElementById('queryBtn')?.addEventListener('click', () => {
            const start = document.getElementById('startDate').value;
            const end = document.getElementById('endDate').value;
            if (start && end) this.loadStatsRange(start, end);
        });
    }

    async loadStats(period) {
        this.currentPeriod = period;
        try {
            const response = await fetch(`/api/cost/${period}`);
            if (!response.ok) throw new Error('Failed to load stats');
            const data = await response.json();
            this.render(data);
        } catch (error) {
            console.error('Error loading stats:', error);
            this.showError('加载数据失败，请确认后端服务已启动');
        }
    }

    async loadStatsRange(start, end) {
        try {
            const startDate = new Date(start).toISOString();
            const endDate = new Date(end).toISOString();
            const response = await fetch(`/api/cost/range?start=${encodeURIComponent(startDate)}&end=${encodeURIComponent(endDate)}`);
            if (!response.ok) throw new Error('Failed to load stats');
            const data = await response.json();
            this.render(data);
        } catch (error) {
            console.error('Error loading stats:', error);
            this.showError('加载数据失败，请确认后端服务已启动');
        }
    }

    render(data) {
        this.updateCards(data);
        this.renderDailyCostChart(data.dailyCosts || []);
        this.renderDailyCallChart(data.dailyCosts || []);
        this.renderModelPieChart(data.modelUsages || []);
        this.renderModelCallChart(data.modelUsages || []);
        this.updateTable(data.dailyCosts || []);
        this.updateModelList(data.modelUsages || []);
    }

    updateCards(data) {
        document.getElementById('totalCalls').textContent = (data.totalCalls || 0).toLocaleString();
        document.getElementById('totalCost').textContent = `¥${(data.totalCost || 0).toFixed(4)}`;
        document.getElementById('successCalls').textContent = `${data.successCalls || 0} / ${data.totalCalls || 0}`;
        document.getElementById('successRate').textContent = `${(data.successRate || 0)}%`;
    }

    renderDailyCostChart(dailyCosts) {
        const dates = dailyCosts.map(d => d.date);
        const costs = dailyCosts.map(d => d.cost || 0);

        this.charts.dailyCost.setOption({
            tooltip: { trigger: 'axis', formatter: '{b}<br/>成本: ¥{c}' },
            grid: { left: 60, right: 20, top: 20, bottom: 30 },
            xAxis: {
                type: 'category',
                data: dates,
                axisLabel: { color: '#948b7d', fontSize: 11 },
                axisLine: { lineStyle: { color: '#ebe4d7' } }
            },
            yAxis: {
                type: 'value',
                axisLabel: { color: '#948b7d', fontSize: 11, formatter: '¥{value}' },
                splitLine: { lineStyle: { color: '#ebe4d7' } }
            },
            series: [{
                type: 'line',
                data: costs,
                smooth: true,
                symbol: 'circle',
                symbolSize: 6,
                lineStyle: { color: '#0f7b65', width: 3 },
                itemStyle: { color: '#0f7b65' },
                areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        { offset: 0, color: 'rgba(15,123,101,0.24)' },
                        { offset: 1, color: 'rgba(15,123,101,0.02)' }
                    ])
                }
            }]
        });
    }

    renderDailyCallChart(dailyCosts) {
        const dates = dailyCosts.map(d => d.date);
        const calls = dailyCosts.map(d => d.callCount || 0);

        this.charts.dailyCall.setOption({
            tooltip: { trigger: 'axis', formatter: '{b}<br/>调用: {c} 次' },
            grid: { left: 50, right: 20, top: 20, bottom: 30 },
            xAxis: {
                type: 'category',
                data: dates,
                axisLabel: { color: '#948b7d', fontSize: 11 },
                axisLine: { lineStyle: { color: '#ebe4d7' } }
            },
            yAxis: {
                type: 'value',
                axisLabel: { color: '#948b7d', fontSize: 11 },
                splitLine: { lineStyle: { color: '#ebe4d7' } }
            },
            series: [{
                type: 'bar',
                data: calls,
                barWidth: '50%',
                itemStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        { offset: 0, color: '#aa6a18' },
                        { offset: 1, color: 'rgba(170,106,24,0.28)' }
                    ]),
                    borderRadius: [8, 8, 0, 0]
                }
            }]
        });
    }

    renderModelPieChart(modelUsages) {
        const data = modelUsages.map(m => ({
            name: m.modelName,
            value: parseFloat((m.totalCost || 0).toFixed(4))
        }));

        this.charts.modelPie.setOption({
            tooltip: { trigger: 'item', formatter: '{b}<br/>成本: ¥{c} ({d}%)' },
            legend: {
                orient: 'vertical',
                right: 10,
                top: 'center',
                textStyle: { color: '#5f594f', fontSize: 11 }
            },
            series: [{
                type: 'pie',
                radius: ['40%', '70%'],
                center: ['35%', '50%'],
                avoidLabelOverlap: true,
                label: { show: false },
                emphasis: {
                    label: { show: true, fontSize: 14, color: '#23211d' }
                },
                data: data,
                itemStyle: {
                    borderColor: '#fffaf1',
                    borderWidth: 2
                },
                color: ['#0f7b65', '#aa6a18', '#7357a6', '#b84b3f', '#5f8f7f', '#c09256']
            }]
        });
    }

    renderModelCallChart(modelUsages) {
        const names = modelUsages.map(m => m.modelName);
        const calls = modelUsages.map(m => m.callCount || 0);
        const tokens = modelUsages.map(m => m.totalTokens || 0);

        this.charts.modelCall.setOption({
            tooltip: { trigger: 'axis' },
            legend: {
                data: ['调用次数', 'Token 消耗'],
                textStyle: { color: '#5f594f', fontSize: 11 },
                top: 0
            },
            grid: { left: 60, right: 60, top: 35, bottom: 40 },
            xAxis: {
                type: 'category',
                data: names,
                axisLabel: { color: '#948b7d', fontSize: 10, rotate: 15 },
                axisLine: { lineStyle: { color: '#ebe4d7' } }
            },
            yAxis: [
                {
                    type: 'value',
                    name: '调用次数',
                    nameTextStyle: { color: '#948b7d', fontSize: 10 },
                    axisLabel: { color: '#948b7d', fontSize: 10 },
                    splitLine: { lineStyle: { color: '#ebe4d7' } }
                },
                {
                    type: 'value',
                    name: 'Token',
                    nameTextStyle: { color: '#948b7d', fontSize: 10 },
                    axisLabel: { color: '#948b7d', fontSize: 10 },
                    splitLine: { show: false }
                }
            ],
            series: [
                {
                    name: '调用次数',
                    type: 'bar',
                    data: calls,
                    barWidth: '35%',
                    itemStyle: { color: '#0f7b65', borderRadius: [8, 8, 0, 0] }
                },
                {
                    name: 'Token 消耗',
                    type: 'bar',
                    yAxisIndex: 1,
                    data: tokens,
                    barWidth: '35%',
                    itemStyle: { color: '#7357a6', borderRadius: [8, 8, 0, 0] }
                }
            ]
        });
    }

    updateTable(dailyCosts) {
        const tbody = document.getElementById('detailsBody');
        if (dailyCosts.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3" class="loading">暂无数据</td></tr>';
            return;
        }
        tbody.innerHTML = dailyCosts.map(day => `
            <tr>
                <td>${day.date}</td>
                <td>${day.callCount} 次</td>
                <td>¥${(day.cost || 0).toFixed(4)}</td>
            </tr>
        `).join('');
    }

    updateModelList(modelUsages) {
        const container = document.getElementById('modelStats');
        if (modelUsages.length === 0) {
            container.innerHTML = '<div class="loading">暂无数据</div>';
            return;
        }
        container.innerHTML = modelUsages.map(stat => `
            <div class="model-item">
                <div class="model-info">
                    <div class="model-name">${stat.modelName}</div>
                    <div class="model-provider">${stat.provider}</div>
                </div>
                <div class="model-stats">
                    <span>调用: ${stat.callCount}</span>
                    <span>Token: ${(stat.totalTokens || 0).toLocaleString()}</span>
                    <span>成本: ¥${(stat.totalCost || 0).toFixed(4)}</span>
                </div>
            </div>
        `).join('');
    }

    showError(message) {
        document.getElementById('totalCalls').textContent = '-';
        document.getElementById('totalCost').textContent = '-';
        document.getElementById('successCalls').textContent = '-';
        document.getElementById('successRate').textContent = '-';
        document.getElementById('detailsBody').innerHTML =
            `<tr><td colspan="3" class="loading">${message}</td></tr>`;
        Object.values(this.charts).forEach(chart => {
            chart.setOption({ graphic: {
                type: 'text',
                left: 'center', top: 'middle',
                style: { text: '暂无数据', fill: '#948b7d', fontSize: 14 }
            }});
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new CostManager();
});
