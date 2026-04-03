/**
 * 成本管理页面 - 数据加载和交互
 */

class CostManager {
    constructor() {
        this.currentPeriod = 'today';
        this.initEventListeners();
        this.loadStats('today');
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
            if (start && end) {
                this.loadStatsRange(start, end);
            }
        });
    }

    async loadStats(period) {
        this.currentPeriod = period;
        try {
            const response = await fetch(`/api/cost/${period}`);
            if (!response.ok) throw new Error('Failed to load stats');
            const data = await response.json();
            this.updateStatsDisplay(data);
            this.updateDailyCosts(data.dailyCosts || []);
            this.updateModelDistribution(data.modelUsages || []);
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
            this.updateStatsDisplay(data);
            this.updateDailyCosts(data.dailyCosts || []);
            this.updateModelDistribution(data.modelUsages || []);
        } catch (error) {
            console.error('Error loading stats:', error);
            this.showError('加载数据失败，请确认后端服务已启动');
        }
    }

    updateStatsDisplay(data) {
        document.getElementById('totalCalls').textContent = data.totalCalls || 0;
        document.getElementById('totalCost').textContent = `¥${(data.totalCost || 0).toFixed(4)}`;
        document.getElementById('inputTokens').textContent =
            `${data.successCalls || 0} / ${data.totalCalls || 0}`;
        document.getElementById('outputTokens').textContent =
            `${(data.successRate || 0)}%`;
    }

    updateDailyCosts(dailyCosts) {
        const tbody = document.getElementById('detailsBody');

        if (dailyCosts.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="loading">暂无数据</td></tr>';
            return;
        }

        tbody.innerHTML = dailyCosts.map(day => `
            <tr>
                <td>${day.date}</td>
                <td>-</td>
                <td>-</td>
                <td>-</td>
                <td>¥${(day.cost || 0).toFixed(4)}</td>
                <td>${day.callCount} 次</td>
            </tr>
        `).join('');
    }

    updateModelDistribution(modelUsages) {
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
        document.getElementById('inputTokens').textContent = '-';
        document.getElementById('outputTokens').textContent = '-';
        const tbody = document.getElementById('detailsBody');
        tbody.innerHTML = `<tr><td colspan="6" class="loading">${message}</td></tr>`;
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new CostManager();
});
