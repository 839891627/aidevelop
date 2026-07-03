/**
 * Prompt 管理页面 - 版本查看、创建草稿、发布、回滚
 */
class PromptManager {
    constructor() {
        this.prompts = [
            { key: 'system.default', label: 'System Prompt' },
            { key: 'chat.general', label: 'General Chat Prompt' },
            { key: 'chat.financial.rag', label: 'Financial RAG Prompt' },
            { key: 'rag.qa', label: 'RAG QA Prompt' },
            { key: 'function.calling', label: 'Function Calling Prompt' }
        ];
        this.currentEnv = 'dev';
        this.initEventListeners();
        this.renderCards();
        this.loadAllPrompts();
    }

    initEventListeners() {
        document.getElementById('envSelect').addEventListener('change', (e) => {
            this.currentEnv = e.target.value;
            this.loadAllPrompts();
        });
    }

    slug(key) {
        return key.replace(/\./g, '-');
    }

    renderCards() {
        const container = document.getElementById('promptCards');
        container.innerHTML = this.prompts.map(p => {
            const s = this.slug(p.key);
            return `
            <div class="prompt-card" data-key="${p.key}">
                <div class="card-header">
                    <h3>${p.label}</h3>
                    <span class="card-badge" id="badge-${s}">--</span>
                </div>
                <div class="card-meta">
                    <span class="meta-item" id="version-${s}">版本: -</span>
                    <span class="meta-item" id="length-${s}">长度: -</span>
                </div>
                <div class="card-preview" id="preview-${s}">加载中...</div>
                <div class="card-actions">
                    <button class="btn-view" onclick="promptManager.viewDetail('${p.key}')">查看</button>
                    <button class="btn-versions" onclick="promptManager.showVersions('${p.key}')">历史版本</button>
                    <button class="btn-create" onclick="promptManager.showCreateDraft('${p.key}')">新建草稿</button>
                </div>
            </div>`;
        }).join('');
    }

    async loadAllPrompts() {
        for (const p of this.prompts) {
            await this.loadPromptStatus(p.key);
        }
    }

    async loadPromptStatus(promptKey) {
        const s = this.slug(promptKey);
        try {
            const env = this.currentEnv;
            const response = await fetch(`/api/prompts/registry/active?promptKey=${encodeURIComponent(promptKey)}&env=${env}`);
            if (!response.ok) throw new Error('Failed to load');
            const data = await response.json();

            const badge = document.getElementById(`badge-${s}`);
            const version = document.getElementById(`version-${s}`);
            const length = document.getElementById(`length-${s}`);
            const preview = document.getElementById(`preview-${s}`);

            if (data.active) {
                badge.textContent = 'ACTIVE';
                badge.className = 'card-badge badge-active';
                version.textContent = `版本: v${data.version}`;
                length.textContent = `长度: ${data.length} 字符`;
                preview.textContent = data.content
                    ? data.content.substring(0, 200) + (data.content.length > 200 ? '...' : '')
                    : '(空)';
            } else {
                badge.textContent = '未配置';
                badge.className = 'card-badge badge-none';
                version.textContent = '版本: -';
                length.textContent = '长度: -';
                preview.textContent = '当前环境暂无生效版本';
            }
        } catch (error) {
            console.error(`Error loading ${promptKey}:`, error);
            const preview = document.getElementById(`preview-${s}`);
            if (preview) preview.textContent = '加载失败';
        }
    }

    async viewDetail(promptKey) {
        try {
            const env = this.currentEnv;
            const response = await fetch(`/api/prompts/registry/active?promptKey=${encodeURIComponent(promptKey)}&env=${env}`);
            if (!response.ok) throw new Error('Failed to load');
            const data = await response.json();

            document.getElementById('modalTitle').textContent = `${promptKey} (${env})`;
            const textarea = document.getElementById('modalContent');
            textarea.value = data.active ? data.content : '当前环境暂无生效版本';
            textarea.readOnly = true;

            document.getElementById('modalFooter').innerHTML = '';
            document.getElementById('modalOverlay').style.display = 'flex';
        } catch (error) {
            this.toast('加载详情失败', 'error');
        }
    }

    closeModal() {
        document.getElementById('modalOverlay').style.display = 'none';
    }

    async showVersions(promptKey) {
        try {
            const env = this.currentEnv;
            const response = await fetch(`/api/prompts/registry/versions?promptKey=${encodeURIComponent(promptKey)}&env=${env}`);
            if (!response.ok) throw new Error('Failed to load');
            const data = await response.json();

            document.getElementById('versionsTitle').textContent = `${promptKey} 版本历史 (${env})`;
            const list = document.getElementById('versionsList');

            if (!data.items || data.items.length === 0) {
                list.innerHTML = '<div style="color:var(--text-muted);text-align:center;padding:24px;">暂无版本记录</div>';
            } else {
                list.innerHTML = data.items.map(item => `
                    <div class="version-item">
                        <div class="version-info">
                            <span class="version-number">v${item.version}</span>
                            <span class="version-status status-${item.status.toLowerCase()}">${item.status}</span>
                            <span class="version-meta">${item.createdBy || '-'} | ${this.formatDate(item.updatedAt)}</span>
                            <span class="version-meta">${item.length} 字符</span>
                        </div>
                        <div class="version-actions">
                            ${item.status === 'DRAFT' ? `<button class="btn-publish" onclick="promptManager.publish('${promptKey}', ${item.version})">发布</button>` : ''}
                            ${item.status !== 'ACTIVE' ? `<button class="btn-rollback" onclick="promptManager.rollback('${promptKey}', ${item.version})">回滚</button>` : ''}
                        </div>
                    </div>
                `).join('');
            }

            document.getElementById('versionsSection').style.display = 'block';
            document.getElementById('versionsSection').scrollIntoView({ behavior: 'smooth' });
        } catch (error) {
            this.toast('加载版本列表失败', 'error');
        }
    }

    closeVersions() {
        document.getElementById('versionsSection').style.display = 'none';
    }

    showCreateDraft(promptKey) {
        document.getElementById('draftKey').value = promptKey;
        document.getElementById('draftEnv').value = this.currentEnv;
        document.getElementById('draftContent').value = '';
        document.getElementById('draftOperator').value = '';
        document.getElementById('draftOverlay').style.display = 'flex';
    }

    closeDraft() {
        document.getElementById('draftOverlay').style.display = 'none';
    }

    async submitDraft() {
        const promptKey = document.getElementById('draftKey').value;
        const env = document.getElementById('draftEnv').value;
        const content = document.getElementById('draftContent').value;
        const operator = document.getElementById('draftOperator').value;

        if (!content.trim()) {
            this.toast('请输入 Prompt 内容', 'error');
            return;
        }

        try {
            const response = await fetch('/api/prompts/registry/drafts', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ promptKey, env, content, operator })
            });

            if (!response.ok) {
                const err = await response.json();
                throw new Error(err.message || 'Failed');
            }

            const data = await response.json();
            this.toast(`草稿创建成功 (v${data.version})`, 'success');
            this.closeDraft();
            this.loadAllPrompts();
        } catch (error) {
            this.toast(`创建失败: ${error.message}`, 'error');
        }
    }

    async publish(promptKey, version) {
        const operator = prompt('请输入操作人:');
        if (operator === null) return;

        try {
            const response = await fetch('/api/prompts/registry/publish', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    promptKey,
                    env: this.currentEnv,
                    version,
                    operator: operator || 'anonymous',
                    remark: '从管理页面发布'
                })
            });

            if (!response.ok) {
                const err = await response.json();
                throw new Error(err.message || 'Failed');
            }

            this.toast('发布成功', 'success');
            this.loadAllPrompts();
            this.showVersions(promptKey);
        } catch (error) {
            this.toast(`发布失败: ${error.message}`, 'error');
        }
    }

    async rollback(promptKey, version) {
        if (!confirm(`确认回滚到 v${version}?`)) return;
        const operator = prompt('请输入操作人:');
        if (operator === null) return;

        try {
            const response = await fetch('/api/prompts/registry/rollback', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    promptKey,
                    env: this.currentEnv,
                    version,
                    operator: operator || 'anonymous',
                    remark: '从管理页面回滚'
                })
            });

            if (!response.ok) {
                const err = await response.json();
                throw new Error(err.message || 'Failed');
            }

            this.toast('回滚成功', 'success');
            this.loadAllPrompts();
            this.showVersions(promptKey);
        } catch (error) {
            this.toast(`回滚失败: ${error.message}`, 'error');
        }
    }

    formatDate(dateStr) {
        if (!dateStr) return '-';
        try {
            const d = new Date(dateStr);
            return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
        } catch {
            return dateStr;
        }
    }

    toast(message, type) {
        const existing = document.querySelector('.toast');
        if (existing) existing.remove();

        const el = document.createElement('div');
        el.className = `toast toast-${type}`;
        el.textContent = message;
        document.body.appendChild(el);
        setTimeout(() => el.remove(), 3000);
    }
}

let promptManager;
document.addEventListener('DOMContentLoaded', () => {
    promptManager = new PromptManager();
});
