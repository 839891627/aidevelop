class ChatApp {
    constructor() {
        this.conversationId = null;
        this.messageInput = document.getElementById('messageInput');
        this.sendBtn = document.getElementById('sendBtn');
        this.clearBtn = document.getElementById('clearBtn');
        this.modeSelect = document.getElementById('modeSelect');
        this.chatMessages = document.getElementById('chatMessages');

        this.initEventListeners();
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
    }

    async sendMessage() {
        const message = this.messageInput.value.trim();
        if (!message) return;

        // 禁用输入
        this.setInputEnabled(false);

        // 显示用户消息
        this.addMessage('user', message);
        this.messageInput.value = '';

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
        const messageDiv = this.addMessage('assistant', '');
        const contentDiv = messageDiv.querySelector('.message-content');

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

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let fullText = '';

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                const chunk = decoder.decode(value);
                const lines = chunk.split('\n');

                for (const line of lines) {
                    if (line.startsWith('data: ')) {
                        const content = line.slice(6);
                        fullText += content;
                        contentDiv.textContent = fullText;
                        this.scrollToBottom();
                    }
                }
            }

        } catch (error) {
            console.error('Stream error:', error);
            contentDiv.textContent = '抱歉，发生了错误。请稍后重试。';
        }
    }

    addMessage(role, content) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${role}`;

        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';
        contentDiv.textContent = content;

        messageDiv.appendChild(contentDiv);
        this.chatMessages.appendChild(messageDiv);

        this.scrollToBottom();
        return messageDiv;
    }

    clearConversation() {
        if (this.conversationId) {
            fetch(`/api/chat/${this.conversationId}`, {
                method: 'DELETE'
            }).catch(err => console.error('Clear error:', err));
        }
        this.conversationId = null;
        this.chatMessages.innerHTML = '';
    }

    scrollToBottom() {
        this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
    }

    setInputEnabled(enabled) {
        this.messageInput.disabled = !enabled;
        this.sendBtn.disabled = !enabled;
    }
}

// 初始化应用
document.addEventListener('DOMContentLoaded', () => {
    new ChatApp();
});
