/**
 * AI 工作台 - ChatGPT 风格聊天页
 * 支持常规聊天、金融 RAG 与 Agent 任务三种能力模式。
 */
function normalizeMarkdownText(text) {
    return text
        .replace(/\r\n/g, '\n')
        .replace(/\r/g, '\n')
        // 兼容模型常见输出：#标题、##一、标题。CommonMark 要求 # 后有空格。
        .replace(/(^|\n)(#{1,6})([^#\s\n])/g, '$1$2 $3')
        // 兼容 1.列表项 / 1.中文列表项，否则 marked 会当普通段落。
        .replace(/(^|\n)(\s*\d+\.)([^\s\n])/g, '$1$2 $3')
        // 兼容 -列表项 / *列表项 / +列表项。
        .replace(/(^|\n)(\s*[-*+])([^\s\n])/g, '$1$2 $3')
        .replace(/\n{3,}/g, '\n\n');
}

class ChatApp {
    constructor() {
        this.conversationIds = {
            general: null,
            financial_rag: null,
            agent: null
        };
        this.hasMessages = false;
        this.activeConversationId = null;

        this.messageInput = document.getElementById('messageInput');
        this.sendBtn = document.getElementById('sendBtn');
        this.clearBtn = document.getElementById('clearBtn');
        this.newChatBtn = document.getElementById('newChatBtn');
        this.apiModeSelect = document.getElementById('apiModeSelect');
        this.chatMessages = document.getElementById('chatMessages');
        this.conversationHistory = document.getElementById('conversationHistory');
        this.modeDescription = document.getElementById('modeDescription');

        this.modeCopy = {
            general: '常规知识、技术解释、写作和方案设计，不套用金融助贷提示词。',
            financial_rag: '金融助贷知识库问答，使用检索资料回答规则、政策和流程问题。',
            agent: '业务任务编排，适合查询借款/还款记录、风险评估和规则结合判断。'
        };

        this.initEventListeners();
        this.updateModeDescription();
        this.autoResizeTextarea();
        this.loadConversationHistory();
    }

    initEventListeners() {
        this.sendBtn.addEventListener('click', () => this.sendMessage());
        this.clearBtn.addEventListener('click', () => this.clearConversation());
        this.newChatBtn.addEventListener('click', () => this.startNewConversation());
        this.apiModeSelect.addEventListener('change', () => this.updateModeDescription());

        this.messageInput.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                this.sendMessage();
            }
        });

        this.messageInput.addEventListener('input', () => this.autoResizeTextarea());

        document.querySelectorAll('[data-prompt]').forEach((button) => {
            button.addEventListener('click', () => {
                if (button.dataset.mode) {
                    this.apiModeSelect.value = button.dataset.mode;
                    this.updateModeDescription();
                }
                this.messageInput.value = button.dataset.prompt;
                this.autoResizeTextarea();
                this.messageInput.focus();
            });
        });
    }

    get currentMode() {
        return this.apiModeSelect.value;
    }

    updateModeDescription() {
        this.modeDescription.textContent = this.modeCopy[this.currentMode];
    }

    autoResizeTextarea() {
        this.messageInput.style.height = 'auto';
        this.messageInput.style.height = `${Math.min(this.messageInput.scrollHeight, 180)}px`;
    }

    async sendMessage() {
        const message = this.messageInput.value.trim();
        if (!message) {
            return;
        }

        const mode = this.currentMode;
        const requestBody = {
            message,
            conversationId: this.conversationIds[mode] || this.activeConversationId,
            mode: mode
        };

        this.removeEmptyState();
        this.addMessage('user', message, this.getModeLabel(mode));
        this.messageInput.value = '';
        this.autoResizeTextarea();
        this.setInputEnabled(false);

        try {
            // Agent 是独立编排链路；常规聊天和金融 RAG 共用 /api/chat/stream，通过 mode 区分能力。
            if (mode === 'agent') {
                await this.sendAgentMessage(requestBody);
            } else {
                await this.sendStreamMessage(requestBody);
            }
        } finally {
            this.setInputEnabled(true);
            this.messageInput.focus();
            this.loadConversationHistory();
        }
    }

    async sendNormalMessage(requestBody) {
        try {
            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error('普通聊天请求失败');
            }

            const data = await response.json();
            this.conversationIds[requestBody.mode] = data.conversationId;
            this.activeConversationId = data.conversationId;
            this.addMessage('assistant', data.message, this.getModeLabel(requestBody.mode));
        } catch (error) {
            this.addErrorMessage(error);
        }
    }

    async sendAgentMessage(requestBody) {
        const messageDiv = this.addMessage('assistant', '', 'Agent 聊天');
        const contentDiv = messageDiv.querySelector('.message-content');
        contentDiv.innerHTML = this.getThinkingMarkup('Agent 正在规划与执行');

        try {
            // Agent 接口返回 traceId 和 steps，前端会把执行步骤格式化出来，便于观察工具链。
            const response = await fetch('/api/agent/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    message: requestBody.message,
                    conversationId: requestBody.conversationId,
                    maxSteps: 5,
                    multiAgent: false
                })
            });

            if (!response.ok) {
                throw new Error('Agent 聊天请求失败');
            }

            const data = await response.json();
            this.conversationIds.agent = data.traceId || requestBody.conversationId;
            this.activeConversationId = this.conversationIds.agent;
            this.renderMarkdown(contentDiv, this.formatAgentResponse(data));
        } catch (error) {
            contentDiv.innerHTML = this.escapeHtml(`抱歉，Agent 处理失败：${error.message}`);
        }
    }

    async sendStreamMessage(requestBody) {
        const messageDiv = this.addMessage('assistant', '', this.getModeLabel(requestBody.mode));
        const contentDiv = messageDiv.querySelector('.message-content');
        contentDiv.innerHTML = this.getThinkingMarkup('正在生成回答');

        try {
            const response = await fetch('/api/chat/stream', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error('流式聊天请求失败');
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let buffer = '';
            let fullText = '';

            const processEventBlock = (eventBlock) => {
                const parsed = this.parseSseEvent(eventBlock);
                if (!parsed) {
                    return;
                }

                if (parsed.event === 'meta') {
                    try {
                        const meta = JSON.parse(parsed.data);
                        if (meta.conversationId) {
                            this.conversationIds[requestBody.mode] = meta.conversationId;
                            this.activeConversationId = meta.conversationId;
                        }
                    } catch (error) {
                        console.warn('解析流式 meta 事件失败:', error);
                    }
                    return;
                }

                if (parsed.data) {
                    fullText += parsed.data;
                    this.renderMarkdown(contentDiv, fullText);
                    this.scrollToBottom();
                }
            };

            while (true) {
                const { done, value } = await reader.read();
                if (done) {
                    const finalChunk = decoder.decode();
                    if (finalChunk) {
                        buffer += finalChunk;
                    }
                    break;
                }

                buffer += decoder.decode(value, { stream: true });
                const eventBlocks = buffer.split('\n\n');
                buffer = eventBlocks.pop() || '';
                eventBlocks.forEach(processEventBlock);
            }

            if (buffer.trim()) {
                processEventBlock(buffer);
            }

            if (fullText.trim()) {
                this.renderMarkdown(contentDiv, fullText);
            } else {
                contentDiv.textContent = '未收到响应数据，请检查网络连接或稍后重试。';
                contentDiv.classList.add('text-only');
            }
        } catch (error) {
            contentDiv.innerHTML = this.escapeHtml(`抱歉，流式处理失败：${error.message}`);
        }
    }

    formatAgentResponse(data) {
        const answer = data.finalAnswer || 'Agent 未返回最终答案。';
        const meta = [
            data.routeType ? `路由类型：${data.routeType}` : null,
            Number.isFinite(data.executedSteps) ? `执行步骤：${data.executedSteps}` : null,
            Number.isFinite(data.responseTimeMs) ? `耗时：${data.responseTimeMs}ms` : null
        ].filter(Boolean);

        const steps = Array.isArray(data.steps) && data.steps.length > 0
            ? `\n\n### 执行轨迹\n${data.steps.map((step) => {
                const title = step.name || step.toolName || `Step ${step.stepIndex || '-'}`;
                const state = step.success === false ? '失败' : '完成';
                return `- ${title}：${state}`;
            }).join('\n')}`
            : '';

        return `${answer}${meta.length ? `\n\n---\n${meta.join(' · ')}` : ''}${steps}`;
    }

    renderMarkdown(contentDiv, text) {
        if (!text || !text.trim()) {
            return;
        }

        if (typeof marked === 'undefined') {
            contentDiv.textContent = text;
            contentDiv.classList.add('text-only');
            return;
        }

        try {
            const processedText = normalizeMarkdownText(text);

            const html = marked.parse(processedText, {
                breaks: true,
                gfm: true,
                headerIds: false,
                mangle: false
            });

            contentDiv.classList.remove('text-only');
            contentDiv.innerHTML = html;
        } catch (error) {
            console.error('Markdown 渲染失败:', error);
            contentDiv.textContent = text;
            contentDiv.classList.add('text-only');
        }
    }

    addMessage(role, content, metaLabel = '') {
        this.removeEmptyState();

        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${role}`;

        const avatar = document.createElement('div');
        avatar.className = 'message-avatar';
        avatar.textContent = role === 'user' ? '你' : 'AI';
        messageDiv.appendChild(avatar);

        const bodyDiv = document.createElement('div');
        bodyDiv.className = 'message-body';

        const metaDiv = document.createElement('div');
        metaDiv.className = 'message-meta';
        metaDiv.textContent = role === 'user' ? '你' : metaLabel || 'AI 助手';
        bodyDiv.appendChild(metaDiv);

        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';
        if (role === 'assistant' && content && typeof marked !== 'undefined') {
            this.renderMarkdown(contentDiv, content);
        } else if (content) {
            contentDiv.textContent = content;
            contentDiv.classList.add('text-only');
        }

        bodyDiv.appendChild(contentDiv);
        messageDiv.appendChild(bodyDiv);
        this.chatMessages.appendChild(messageDiv);

        this.hasMessages = true;
        this.scrollToBottom();

        return messageDiv;
    }

    addErrorMessage(error) {
        console.error(error);
        this.addMessage('assistant', `抱歉，发生了错误：${error.message}`, '系统提示');
    }

    async loadConversationHistory() {
        if (!this.conversationHistory) {
            return;
        }

        try {
            const response = await fetch('/api/chat/conversations');
            if (!response.ok) {
                throw new Error('历史会话加载失败');
            }

            const conversations = await response.json();
            this.renderConversationHistory(Array.isArray(conversations) ? conversations : []);
        } catch (error) {
            console.error(error);
            this.conversationHistory.innerHTML = '<p class="history-empty">历史会话加载失败</p>';
        }
    }

    renderConversationHistory(conversations) {
        if (conversations.length === 0) {
            this.conversationHistory.innerHTML = '<p class="history-empty">暂无历史会话</p>';
            return;
        }

        this.conversationHistory.innerHTML = '';
        conversations.forEach((conversation) => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'history-item';
            if (conversation.conversationId === this.activeConversationId) {
                button.classList.add('active');
            }
            button.dataset.conversationId = conversation.conversationId;
            button.innerHTML = `
                <span>${this.escapeHtml(conversation.title || '未命名会话')}</span>
                <small>${conversation.messageCount || 0} 条消息</small>
            `;
            button.addEventListener('click', () => this.loadConversationMessages(conversation.conversationId));
            this.conversationHistory.appendChild(button);
        });
    }

    async loadConversationMessages(conversationId) {
        if (!conversationId) {
            return;
        }

        this.setInputEnabled(false);
        try {
            const response = await fetch(`/api/chat/${conversationId}/messages`);
            if (!response.ok) {
                throw new Error('历史消息加载失败');
            }

            const messages = await response.json();
            this.activeConversationId = conversationId;
            this.conversationIds[this.currentMode] = conversationId;
            this.chatMessages.innerHTML = '';

            if (!Array.isArray(messages) || messages.length === 0) {
                this.showEmptyState();
                return;
            }

            messages.forEach((message) => {
                const role = message.role === 'USER' ? 'user' : 'assistant';
                const label = role === 'user' ? '你' : (message.model || 'AI 助手');
                this.addMessage(role, message.content, label);
            });
            this.hasMessages = true;
            this.renderConversationHistoryActiveState();
        } catch (error) {
            this.addErrorMessage(error);
        } finally {
            this.setInputEnabled(true);
        }
    }

    renderConversationHistoryActiveState() {
        this.conversationHistory.querySelectorAll('.history-item').forEach((item) => {
            item.classList.toggle('active', item.dataset.conversationId === this.activeConversationId);
        });
    }

    removeEmptyState() {
        const emptyState = this.chatMessages.querySelector('.empty-state');
        if (emptyState) {
            emptyState.remove();
        }
    }

    async clearConversation() {
        const chatConversationIds = Array.from(new Set([
            this.activeConversationId,
            this.conversationIds.general,
            this.conversationIds.financial_rag
        ].filter(Boolean)));

        await Promise.allSettled(chatConversationIds.map((conversationId) => fetch(`/api/chat/${conversationId}`, {
            method: 'DELETE'
        })));

        this.startNewConversation();
        this.loadConversationHistory();
    }

    startNewConversation() {
        this.conversationIds = {
            general: null,
            financial_rag: null,
            agent: null
        };
        this.activeConversationId = null;
        this.showEmptyState();
        this.renderConversationHistoryActiveState();
        this.messageInput.focus();
    }

    showEmptyState() {
        this.chatMessages.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">?</div>
                <div class="empty-state-text">
                    <strong>开始一段新的对话</strong>
                    <span>预制问题会自动切换到对应能力模式：常规聊天、金融 RAG 或 Agent 任务。</span>
                </div>
                <div class="prompt-chips" aria-label="示例问题">
                    <button type="button" data-mode="general" data-prompt="什么是 RAG？和 Agent 有什么区别？"><span class="chip-tag chip-tag-general">常规</span><span>解释 RAG 与 Agent</span></button>
                    <button type="button" data-mode="general" data-prompt="帮我把这个项目的聊天、Prompt、成本管理能力总结成一段产品说明"><span class="chip-tag chip-tag-general">常规</span><span>生成产品说明</span></button>
                    <button type="button" data-mode="financial_rag" data-prompt="请根据金融助贷知识库说明借款申请通常需要关注哪些规则"><span class="chip-tag chip-tag-rag">RAG</span><span>借款规则说明</span></button>
                    <button type="button" data-mode="financial_rag" data-prompt="请根据金融助贷知识库总结还款逾期相关的处理原则"><span class="chip-tag chip-tag-rag">RAG</span><span>逾期处理原则</span></button>
                    <button type="button" data-mode="agent" data-prompt="请查询 USER001 的借款记录，并总结当前借款状态"><span class="chip-tag">Agent</span><span>查询借款记录</span></button>
                    <button type="button" data-mode="agent" data-prompt="请查询 USER001 的还款记录，重点说明是否存在逾期或异常状态"><span class="chip-tag">Agent</span><span>查询还款记录</span></button>
                    <button type="button" data-mode="agent" data-prompt="请分析 USER001 的借款和还款信息，指出需要关注的问题"><span class="chip-tag">Agent</span><span>借还款综合分析</span></button>
                    <button type="button" data-mode="agent" data-prompt="请对 USER001 做风险评估，并给出风险等级、依据和建议"><span class="chip-tag">Agent</span><span>风险评估</span></button>
                    <button type="button" data-mode="agent" data-prompt="请检索知识库中的风控规则，并结合 USER001 的情况给出判断"><span class="chip-tag">Agent</span><span>规则 + 用户判断</span></button>
                </div>
            </div>
        `;
        this.hasMessages = false;
        this.initPromptChipListeners();
    }

    initPromptChipListeners() {
        this.chatMessages.querySelectorAll('[data-prompt]').forEach((button) => {
            button.addEventListener('click', () => {
                if (button.dataset.mode) {
                    this.apiModeSelect.value = button.dataset.mode;
                    this.updateModeDescription();
                }
                this.messageInput.value = button.dataset.prompt;
                this.autoResizeTextarea();
                this.messageInput.focus();
            });
        });
    }

    scrollToBottom() {
        this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
    }

    setInputEnabled(enabled) {
        this.messageInput.disabled = !enabled;
        this.sendBtn.disabled = !enabled;
        this.apiModeSelect.disabled = !enabled;

        this.sendBtn.innerHTML = enabled ? '<span>发送</span>' : '<span>思考中</span>';
    }

    getModeLabel(mode) {
        return {
            general: '常规聊天',
            financial_rag: '金融 RAG',
            agent: 'Agent 任务'
        }[mode] || '聊天';
    }

    getThinkingMarkup(label) {
        return `
            <div class="thinking-line">
                <span></span><span></span><span></span>
                <em>${this.escapeHtml(label)}</em>
            </div>
        `;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    parseSseEvent(eventBlock) {
        if (!eventBlock || !eventBlock.trim()) {
            return null;
        }

        let eventName = 'message';
        const dataLines = [];

        for (const rawLine of eventBlock.split('\n')) {
            const line = rawLine.replace(/\r$/, '');
            if (!line) {
                continue;
            }
            if (line.startsWith('event:')) {
                eventName = line.slice(6).trim() || 'message';
            } else if (line.startsWith('data:')) {
                dataLines.push(line.slice(5).replace(/^ /, ''));
            }
        }

        if (dataLines.length === 0) {
            return null;
        }

        return {
            event: eventName,
            data: dataLines.join('\n')
        };
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new ChatApp();
});
