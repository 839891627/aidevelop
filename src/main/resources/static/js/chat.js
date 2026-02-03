/**
 * AI 智能助手 - 聊天应用
 * 采用现代化设计，支持流式输出和 Markdown 渲染
 */

class ChatApp {
    constructor() {
        this.conversationId = null;
        this.messageInput = document.getElementById('messageInput');
        this.sendBtn = document.getElementById('sendBtn');
        this.clearBtn = document.getElementById('clearBtn');
        this.modeSelect = document.getElementById('modeSelect');
        this.chatMessages = document.getElementById('chatMessages');
        this.hasMessages = false;

        this.initEventListeners();
        this.autoResizeTextarea();
    }

    initEventListeners() {
        this.sendBtn.addEventListener('click', () => this.sendMessage());
        this.clearBtn.addEventListener('click', () => this.clearConversation());

        this.messageInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });

        // 输入框自动调整高度
        this.messageInput.addEventListener('input', () => {
            this.autoResizeTextarea();
        });
    }

    autoResizeTextarea() {
        this.messageInput.style.height = 'auto';
        this.messageInput.style.height = Math.min(this.messageInput.scrollHeight, 120) + 'px';
    }

    async sendMessage() {
        const message = this.messageInput.value.trim();
        if (!message) return;

        // 移除空状态
        this.removeEmptyState();

        // 禁用输入
        this.setInputEnabled(false);

        // 显示用户消息
        this.addMessage('user', message);
        this.messageInput.value = '';
        this.autoResizeTextarea();

        const mode = this.modeSelect.value;
        const requestBody = {
            message: message,
            conversationId: this.conversationId
        };

        if (mode === 'stream') {
            await this.sendStreamMessage(requestBody);
        } else {
            await this.sendNormalMessage(requestBody);
        }

        // 恢复输入
        this.setInputEnabled(true);
        this.messageInput.focus();
    }

    async sendNormalMessage(requestBody) {
        try {
            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error('请求失败');
            }

            const data = await response.json();
            this.conversationId = data.conversationId;
            this.addMessage('assistant', data.message);

        } catch (error) {
            console.error('Error:', error);
            this.addMessage('assistant', '抱歉，发生了错误。请稍后重试。');
        }
    }

    async sendStreamMessage(requestBody) {
        // 创建消息容器并添加正在输入动画
        const messageDiv = this.addMessage('assistant', '');
        const contentDiv = messageDiv.querySelector('.message-content');
        contentDiv.innerHTML = '<div class="thinking-dots"><span></span><span></span><span></span></div>';

        console.log('[流式模式] 开始发送请求');

        try {
            const response = await fetch('/api/chat/stream', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error('请求失败');
            }

            console.log('[流式模式] 响应成功，开始读取流');

            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');

            let buffer = '';
            let fullText = '';
            let chunkCount = 0;
            let extractedCount = 0;

            while (true) {
                const { done, value } = await reader.read();
                if (done) {
                    const finalChunk = decoder.decode();
                    if (finalChunk) {
                        buffer += finalChunk;
                    }
                    console.log('[流式模式] 流读取完成，共接收', chunkCount, '个chunk');
                    console.log('[流式模式] 提取到', extractedCount, '个SSE事件');
                    break;
                }

                const chunk = decoder.decode(value, { stream: true });
                chunkCount++;

                buffer += chunk;

                const lines = buffer.split('\n');
                buffer = lines.pop() || '';

                for (const line of lines) {
                    if (!line.trim()) {
                        continue;
                    }

                    let content = null;
                    if (line.startsWith('data: ')) {
                        content = line.slice(6);
                    } else if (line.startsWith('data:')) {
                        content = line.slice(5);
                    }

                    if (content !== null) {
                        fullText += content;
                        extractedCount++;

                        // 实时渲染Markdown
                        this.renderMarkdown(contentDiv, fullText);
                        this.scrollToBottom();
                    }
                }
            }

            // 处理缓冲区中剩余的内容
            if (buffer.trim()) {
                const lines = buffer.split('\n');
                for (const line of lines) {
                    if (!line.trim()) continue;

                    if (line.startsWith('data: ')) {
                        const content = line.slice(6);
                        fullText += content;
                        extractedCount++;
                    } else if (line.startsWith('data:')) {
                        const content = line.slice(5);
                        fullText += content;
                        extractedCount++;
                    }
                }
            }

            console.log('[流式模式] 总共提取到', extractedCount, '个数据块');

            // 最终渲染Markdown
            if (fullText.trim()) {
                this.renderMarkdown(contentDiv, fullText);
                console.log('[流式模式] Markdown最终渲染完成');
            } else {
                contentDiv.textContent = '未收到响应数据，请检查网络连接或稍后重试。';
            }

            this.conversationId = requestBody.conversationId || this.conversationId;

        } catch (error) {
            console.error('[流式模式] 发生错误:', error);
            contentDiv.innerHTML = '抱歉，发生了错误：' + this.escapeHtml(error.message);
        }
    }

    renderMarkdown(contentDiv, text) {
        if (!text || !text.trim()) {
            return;
        }

        // 检查marked库是否已加载
        if (typeof marked === 'undefined') {
            console.warn('[流式模式] marked库未加载，使用纯文本显示');
            contentDiv.textContent = text;
            contentDiv.classList.add('text-only');
            return;
        }

        try {
            // 预处理文本：确保Markdown格式正确
            let processedText = text
                .replace(/\r\n/g, '\n')
                .replace(/\r/g, '\n');

            // 确保标题前后有换行
            processedText = processedText.replace(/([^\n])##/g, '$1\n\n##');
            processedText = processedText.replace(/([^\n])###/g, '$1\n\n###');

            // 确保列表项前后有换行
            processedText = processedText.replace(/([^\n])-\s/g, '$1\n- ');
            processedText = processedText.replace(/([^\n])\*\s/g, '$1\n* ');
            processedText = processedText.replace(/([^\n])\+\s/g, '$1\n+ ');

            // 确保标题和内容之间有换行
            processedText = processedText.replace(/##([^\n]+)-/g, '##$1\n-');

            // 清理多余的换行
            processedText = processedText.replace(/\n{3,}/g, '\n\n');

            // 使用marked解析
            const html = marked.parse(processedText, {
                breaks: true,
                gfm: true,
                headerIds: false,
                mangle: false
            });

            // 移除text-only类
            contentDiv.classList.remove('text-only');
            contentDiv.innerHTML = html;

            // 添加打字机光标效果（仅在流式输出时）
            if (text.endsWith('...') || text.length < 100) {
                contentDiv.innerHTML += '<span class="typing-cursor"></span>';
            }

        } catch (e) {
            console.error('[流式模式] Markdown渲染失败:', e);
            contentDiv.textContent = text;
            contentDiv.classList.add('text-only');
        }
    }

    addMessage(role, content) {
        // 移除空状态
        this.removeEmptyState();

        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${role}`;

        // 添加头像（仅助手消息）
        if (role === 'assistant') {
            const avatar = document.createElement('div');
            avatar.className = 'message-avatar';
            messageDiv.appendChild(avatar);
        }

        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';

        // 如果是assistant消息且包含Markdown，使用渲染方法
        if (role === 'assistant' && content && typeof marked !== 'undefined') {
            this.renderMarkdown(contentDiv, content);
        } else if (content) {
            contentDiv.textContent = content;
            contentDiv.classList.add('text-only');
        }

        messageDiv.appendChild(contentDiv);
        this.chatMessages.appendChild(messageDiv);

        this.hasMessages = true;
        this.scrollToBottom();

        // 触发消息进入动画
        setTimeout(() => {
            messageDiv.style.opacity = '1';
        }, 10);

        return messageDiv;
    }

    removeEmptyState() {
        const emptyState = this.chatMessages.querySelector('.empty-state');
        if (emptyState) {
            emptyState.remove();
        }
    }

    clearConversation() {
        if (this.conversationId) {
            fetch(`/api/chat/${this.conversationId}`, {
                method: 'DELETE'
            }).catch(err => console.error('Clear error:', err));
        }

        this.conversationId = null;
        this.chatMessages.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">✦</div>
                <div class="empty-state-text">
                    <strong>开始新的对话</strong><br>
                    输入您的问题，我将竭诚为您服务
                </div>
            </div>
        `;
        this.hasMessages = false;
    }

    scrollToBottom() {
        this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
    }

    setInputEnabled(enabled) {
        this.messageInput.disabled = !enabled;
        this.sendBtn.disabled = !enabled;

        if (!enabled) {
            this.sendBtn.innerHTML = '<span>发送中</span>';
        } else {
            this.sendBtn.innerHTML = '<span>发送</span>';
        }
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// 初始化应用
document.addEventListener('DOMContentLoaded', () => {
    new ChatApp();
});
