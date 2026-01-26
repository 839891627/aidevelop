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
        const messageDiv = this.addMessage('assistant', '思考中...');
        const contentDiv = messageDiv.querySelector('.message-content');

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

            let buffer = '';  // 缓冲区，处理不完整的行
            let fullText = ''; // 完整的响应文本
            let chunkCount = 0;
            let extractedCount = 0; // 提取到的事件数量

            // 清空初始提示
            contentDiv.textContent = '';

            while (true) {
                const { done, value } = await reader.read();
                if (done) {
                    // 流结束时，进行最终解码以处理可能残留的多字节字符
                    const finalChunk = decoder.decode();
                    if (finalChunk) {
                        buffer += finalChunk;
                    }
                    console.log('[流式模式] 流读取完成，共接收', chunkCount, '个chunk');
                    console.log('[流式模式] 提取到', extractedCount, '个SSE事件');
                    console.log('[流式模式] 最终buffer长度:', buffer.length);
                    console.log('[流式模式] 最终buffer内容(前200字符):', JSON.stringify(buffer.substring(0, 200)));
                    break;
                }

                // 使用 stream: true 正确处理流式解码中的多字节字符（如emoji）
                const chunk = decoder.decode(value, { stream: true });
                chunkCount++;

                // 调试：打印前几个chunk的原始内容
                if (chunkCount <= 3) {
                    console.log('[流式模式] Chunk', chunkCount, '原始内容:', JSON.stringify(chunk.substring(0, 150)));
                }

                // 将新数据追加到缓冲区
                buffer += chunk;

                // SSE格式解析：Spring Boot自动包装的格式是 "data: <content>\n\n"
                // 但实际传输时，可能一个chunk包含多个事件，或者一个事件被分成多个chunk
                // 更健壮的方法：按行处理，每遇到 "data: " 开头的行就提取内容
                const lines = buffer.split('\n');
                // 保留最后一个可能不完整的行
                buffer = lines.pop() || '';
                
                // 处理完整的行
                for (const line of lines) {
                    // 跳过空行
                    if (!line.trim()) {
                        continue;
                    }
                    
                    // 提取 "data: " 或 "data:" 后面的内容
                    let content = null;
                    if (line.startsWith('data: ')) {
                        content = line.slice(6); // 移除 "data: " 前缀
                    } else if (line.startsWith('data:')) {
                        content = line.slice(5); // 移除 "data:" 前缀
                    }
                    
                    if (content !== null) {
                        fullText += content;
                        extractedCount++;
                        
                        // 调试：打印前几个提取的内容
                        if (extractedCount <= 3) {
                            console.log('[流式模式] 提取内容', extractedCount, ':', JSON.stringify(content));
                        }
                        
                        // 实时渲染Markdown
                        this.renderMarkdown(contentDiv, fullText);
                        this.scrollToBottom();
                    }
                }
            }

            // 处理缓冲区中剩余的内容（最后一个可能不完整的行）
            if (buffer.trim()) {
                console.log('[流式模式] 处理剩余buffer:', JSON.stringify(buffer.substring(0, 200)));
                // 按行处理剩余内容
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

            console.log('[流式模式] 最终文本长度:', fullText.length);
            console.log('[流式模式] 最终文本内容(前200字符):', JSON.stringify(fullText.substring(0, 200)));

            // 流式输出完成后，最终渲染一次Markdown（确保格式正确）
            if (fullText.trim()) {
                this.renderMarkdown(contentDiv, fullText);
                console.log('[流式模式] Markdown最终渲染完成');
            } else {
                contentDiv.textContent = '未收到响应数据，请检查网络连接或稍后重试。';
            }
            
            // 保存conversationId（如果有返回）
            this.conversationId = requestBody.conversationId || this.conversationId;

        } catch (error) {
            console.error('[流式模式] 发生错误:', error);
            contentDiv.textContent = '抱歉，发生了错误：' + error.message;
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
            // 将连续的换行符标准化
            let processedText = text
                .replace(/\r\n/g, '\n')  // 统一换行符
                .replace(/\r/g, '\n');
            
            // 确保标题前后有换行（处理 ##标题 这种情况）
            processedText = processedText.replace(/([^\n])##/g, '$1\n\n##');
            processedText = processedText.replace(/([^\n])###/g, '$1\n\n###');
            
            // 确保列表项前后有换行（处理 文本-列表项 这种情况）
            processedText = processedText.replace(/([^\n])-\s/g, '$1\n- ');
            processedText = processedText.replace(/([^\n])\*\s/g, '$1\n* ');
            processedText = processedText.replace(/([^\n])\+\s/g, '$1\n+ ');
            
            // 确保标题和内容之间有换行（处理 ##标题-内容 这种情况）
            processedText = processedText.replace(/##([^\n]+)-/g, '##$1\n-');
            
            // 清理多余的换行
            processedText = processedText.replace(/\n{3,}/g, '\n\n');
            
            // 使用marked解析，配置breaks选项
            const html = marked.parse(processedText, {
                breaks: true,  // 单个换行符转换为<br>
                gfm: true,     // GitHub Flavored Markdown
                headerIds: false,
                mangle: false  // 不混淆email地址
            });
            
            // 调试：打印文本和HTML
            console.log('[流式模式] 原始文本长度:', text.length);
            console.log('[流式模式] 原始文本(前300字符):', JSON.stringify(text.substring(0, 300)));
            console.log('[流式模式] 处理后文本(前300字符):', JSON.stringify(processedText.substring(0, 300)));
            console.log('[流式模式] 渲染HTML(前500字符):', html.substring(0, 500));
            
            // 移除text-only类（如果存在），因为现在是Markdown渲染
            contentDiv.classList.remove('text-only');
            // 直接设置innerHTML，不使用textContent
            contentDiv.innerHTML = html;
        } catch (e) {
            console.error('[流式模式] Markdown渲染失败:', e);
            // 降级到纯文本显示
            contentDiv.textContent = text;
            contentDiv.classList.add('text-only');
        }
    }

    addMessage(role, content) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${role}`;

        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';
        
        // 如果是assistant消息且包含Markdown，使用渲染方法
        if (role === 'assistant' && typeof marked !== 'undefined') {
            this.renderMarkdown(contentDiv, content);
        } else {
            contentDiv.textContent = content;
            contentDiv.classList.add('text-only');
        }

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