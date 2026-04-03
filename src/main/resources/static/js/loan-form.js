// ==================== 示例文档数据 ====================
const sampleDocuments = {
    doc1: {
        title: "张三个人贷款申请",
        content: `贷款申请说明

申请人：张三
身份证号码：110101199001011234
联系电话：13800138000
居住地址：北京市朝阳区建国路88号现代城A座1201室
职业：软件工程师
年收入：35万元

贷款类型：个人消费贷款
贷款金额：50万元
贷款期限：36个月
贷款用途：用于家庭装修，包括客厅、卧室、厨房的全面装修，预计总费用50万元左右。
还款来源：主要来源于工资收入，月均收入约3万元，稳定。

担保方式：信用担保
无抵押物
无保证人`
    },
    doc2: {
        title: "李四企业经营贷款",
        content: `企业经营贷款申请

申请人：李四
身份证号码：310101198505205678
联系电话：13900139000
居住地址：上海市浦东新区陆家嘴环路1000号
职业：某科技公司总经理
年收入：80万元

贷款类型：企业经营贷款
贷款金额：200万元
贷款期限：60个月
贷款用途：用于扩大公司业务规模，采购办公设备，招聘技术人员，预计投入200万元。
还款来源：企业经营收入，公司年营收约500万元，利润率约30%。

担保方式：抵押担保
抵押物描述：北京市海淀区中关村软件园内办公楼一层，面积200平方米
抵押物估值：400万元
保证人：无`
    },
    doc3: {
        title: "王五房屋抵押贷款",
        content: `房屋抵押贷款申请

申请人：王五
身份证号码：440101197808109012
联系电话：13700137000
居住地址：广州市天河区珠江新城花城大道123号
职业：自由职业
年收入：25万元

贷款类型：房屋抵押贷款
贷款金额：150万元
贷款期限：120个月
贷款用途：用于购买第二套房产作为投资，以及部分资金用于孩子留学准备。
还款来源：自由职业收入、房产出租收入

担保方式：抵押担保
抵押物描述：广州市天河区珠江新城现有自住房产，三室两厅，面积120平方米
抵押物估值：350万元

保证人：赵六
保证人电话：13600136000`
    }
};

// ==================== 全局状态 ====================
let currentDocument = null;
let isFilling = false;
let isPaused = false;
let eventSource = null;
let currentTab = 'basic';

// ==================== Tab 切换 ====================
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const tabId = btn.dataset.tab;
        switchTab(tabId);
    });
});

function switchTab(tabId) {
    // 更新按钮状态
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === tabId);
    });

    // 更新内容显示
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.toggle('active', content.id === tabId);
    });

    currentTab = tabId;
}

// ==================== 文档选择 ====================
const documentSelect = document.getElementById('documentSelect');
const previewDocBtn = document.getElementById('previewDocBtn');
const documentPreview = document.getElementById('documentPreview');
const documentContent = document.getElementById('documentContent');

documentSelect.addEventListener('change', () => {
    const docKey = documentSelect.value;
    const startBtn = document.getElementById('startFillBtn');

    if (docKey && sampleDocuments[docKey]) {
        currentDocument = sampleDocuments[docKey];
        startBtn.disabled = false;
    } else {
        currentDocument = null;
        startBtn.disabled = true;
        documentPreview.classList.add('hidden');
    }
});

previewDocBtn.addEventListener('click', () => {
    if (currentDocument) {
        documentContent.textContent = currentDocument.content;
        documentPreview.classList.toggle('hidden');
    }
});

// ==================== 按钮事件 ====================
const startFillBtn = document.getElementById('startFillBtn');
const pauseBtn = document.getElementById('pauseBtn');
const resetBtn = document.getElementById('resetBtn');

startFillBtn.addEventListener('click', startAiFill);
pauseBtn.addEventListener('click', togglePause);
resetBtn.addEventListener('click', resetForm);

// ==================== AI 填充主逻辑 ====================
function startAiFill() {
    if (!currentDocument) {
        alert('请先选择文档');
        return;
    }

    isFilling = true;
    isPaused = false;
    updateButtons();
    clearLog();
    addLog('🤖 AI 开始分析文档...', 'thinking');
    addLog(`📄 文档：${currentDocument.title}`, '');
    updateProgress(0, '正在连接 AI 服务...');

    // 使用 SSE 接收流式响应
    const url = `/api/loan-form/fill-stream?docId=${documentSelect.value}`;
    eventSource = new EventSource(url);

    eventSource.onmessage = (event) => {
        if (isPaused) return;

        try {
            const data = JSON.parse(event.data);
            handleAiMessage(data);
        } catch (e) {
            console.error('解析消息失败:', e);
        }
    };

    eventSource.onerror = (error) => {
        console.error('SSE 连接错误:', error);
        addLog('❌ 连接中断，请重试', 'error');
        stopFilling();
    };

    eventSource.onopen = () => {
        addLog('✅ 已连接到 AI 服务', 'success');
    };
}

function handleAiMessage(data) {
    switch (data.type) {
        case 'thinking':
            addLog(`💭 ${data.content}`, 'thinking');
            break;

        case 'progress':
            updateProgress(data.progress, data.message);
            break;

        case 'switch_tab':
            addLog(`📑 切换到标签页：${data.tabName}`, 'highlight');
            switchTab(data.tabId);
            break;

        case 'fill_field':
            fillField(data.tabId, data.fieldName, data.value);
            addLog(`✏️ 填充字段 [${data.fieldLabel}]: ${data.value}`, 'success');
            break;

        case 'complete':
            updateProgress(100, '填充完成！');
            addLog('🎉 所有字段填充完成！', 'success');
            stopFilling();
            break;

        case 'error':
            addLog(`❌ 错误：${data.message}`, 'error');
            break;
    }
}

function fillField(tabId, fieldName, value) {
    const tabContent = document.getElementById(tabId);
    if (!tabContent) return;

    const formGroup = tabContent.querySelector(`[data-field="${fieldName}"]`);
    if (!formGroup) return;

    const input = formGroup.querySelector('input, select, textarea');
    if (!input) return;

    // 添加填充动画效果
    formGroup.classList.add('filling');
    input.value = value;

    // 延迟后标记为已完成
    setTimeout(() => {
        formGroup.classList.remove('filling');
        formGroup.classList.add('filled');
    }, 500);
}

function togglePause() {
    isPaused = !isPaused;
    pauseBtn.textContent = isPaused ? '▶️ 继续' : '⏸️ 暂停';
    updateButtons();

    if (isPaused) {
        addLog('⏸️ 已暂停，点击继续恢复', 'warning');
    } else {
        addLog('▶️ 继续填充...', 'success');
    }
}

function stopFilling() {
    isFilling = false;
    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }
    updateButtons();
}

function resetForm() {
    // 停止当前填充
    stopFilling();

    // 重置所有表单
    document.querySelectorAll('form').forEach(form => form.reset());

    // 移除状态样式
    document.querySelectorAll('.form-group').forEach(group => {
        group.classList.remove('filled', 'filling');
    });

    // 重置选择
    documentSelect.value = '';
    currentDocument = null;

    // 重置UI
    documentPreview.classList.add('hidden');
    updateProgress(0, '等待开始...');
    clearLog();
    addLog('🔄 已重置，请重新选择文档', '');
    updateButtons();
}

// ==================== UI 更新函数 ====================
function updateButtons() {
    startFillBtn.disabled = !currentDocument || isFilling;
    pauseBtn.disabled = !isFilling;
    resetBtn.disabled = isFilling;
    documentSelect.disabled = isFilling;
}

function updateProgress(percent, message) {
    const progressBar = document.getElementById('progressBar');
    const progressText = document.getElementById('progressText');

    progressBar.style.width = `${percent}%`;
    progressText.textContent = message;
}

function addLog(message, type = '') {
    const log = document.getElementById('aiThinkingLog');
    const entry = document.createElement('p');
    entry.className = `log-entry ${type}`;
    entry.textContent = message;

    // 移除占位符
    const placeholder = log.querySelector('.log-placeholder');
    if (placeholder) {
        placeholder.remove();
    }

    log.appendChild(entry);
    log.scrollTop = log.scrollHeight;
}

function clearLog() {
    const log = document.getElementById('aiThinkingLog');
    log.innerHTML = '<p class="log-placeholder">AI 准备就绪，选择文档后点击"开始 AI 填充"</p>';
}

// ==================== 提交处理 ====================
document.getElementById('submitBtn').addEventListener('click', () => {
    const formData = collectFormData();
    console.log('提交的表单数据:', formData);

    // 这里可以发送到后端
    alert('申请表单已提交！\n\n请查看控制台了解详情。');
});

function collectFormData() {
    const data = {
        basic: {},
        loan: {},
        guarantee: {}
    };

    // 基本信息
    document.querySelectorAll('#basicForm .form-group').forEach(group => {
        const fieldName = group.dataset.field;
        const input = group.querySelector('input');
        if (input && input.value) {
            data.basic[fieldName] = input.value;
        }
    });

    // 贷款信息
    document.querySelectorAll('#loanForm .form-group').forEach(group => {
        const fieldName = group.dataset.field;
        const input = group.querySelector('input, select, textarea');
        if (input && input.value) {
            data.loan[fieldName] = input.value;
        }
    });

    // 担保信息
    document.querySelectorAll('#guaranteeForm .form-group').forEach(group => {
        const fieldName = group.dataset.field;
        const input = group.querySelector('input, select, textarea');
        if (input && input.value) {
            data.guarantee[fieldName] = input.value;
        }
    });

    return data;
}

// ==================== 初始化 ====================
updateButtons();
