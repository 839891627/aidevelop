package com.example.aidevelop.service;

import com.example.aidevelop.model.dto.loanform.LoanFormData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 贷款表单 AI 填充服务
 *
 * 核心职责：
 * 1. 定义示例文档数据
 * 2. 使用 AI 解析文档提取字段
 * 3. 通过 SSE 流式发送填充指令
 * 4. 提供可视化的处理过程
 */
@Service
public class LoanFormService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 示例文档（与前端保持一致） ====================
    private static final Map<String, String> SAMPLE_DOCUMENTS = Map.of(
        "doc1", """
            贷款申请说明

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
            无保证人
            """,
        "doc2", """
            企业经营贷款申请

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
            保证人：无
            """,
        "doc3", """
            房屋抵押贷款申请

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
            保证人电话：13600136000
            """
    );

    public LoanFormService(@Qualifier("openAiChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 处理文档并填充表单（流式）
     *
     * 这是核心方法，展示了 AI 如何逐步处理任务
     */
    public void processDocumentAndFill(String docId, SseEmitter emitter) throws IOException {
        // 1. 获取文档内容
        String documentContent = SAMPLE_DOCUMENTS.get(docId);
        if (documentContent == null) {
            sendError(emitter, "文档不存在");
            return;
        }

        sendThinking(emitter, "✅ 文档读取成功，共 " + documentContent.length() + " 字符");
        sendProgress(emitter, 10, "正在让 AI 分析文档结构...");

        // 2. 使用 AI 解析文档，提取所有字段
        sendThinking(emitter, "🔍 正在使用 AI 智能提取信息...");
        sendProgress(emitter, 20, "AI 正在阅读文档...");

        LoanFormData formData = extractFieldsWithAI(documentContent, emitter);

        if (formData == null) {
            sendError(emitter, "AI 解析失败");
            return;
        }

        sendThinking(emitter, "✅ AI 已完成信息提取，开始填充表单...");
        sendProgress(emitter, 50, "开始填充表单字段...");

        // 3. 逐步填充表单 - 基本信息
        fillBasicInfo(formData, emitter);
        sendProgress(emitter, 70, "已完成基本信息");

        // 4. 填充贷款信息
        fillLoanInfo(formData, emitter);
        sendProgress(emitter, 85, "已完成贷款信息");

        // 5. 填充担保信息
        fillGuaranteeInfo(formData, emitter);
        sendProgress(emitter, 100, "全部完成！");
    }

    /**
     * 使用 AI 从文档中提取结构化字段
     *
     * 这里使用了 Prompt Engineering 技巧：
     * 1. System Prompt 定义角色和任务
     * 2. User Prompt 提供具体文档
     * 3. 要求返回 JSON 格式
     */
    private LoanFormData extractFieldsWithAI(String document, SseEmitter emitter) throws IOException {
        sendThinking(emitter, "🧠 正在构建 AI Prompt...");

        // System Prompt: 定义 AI 的角色和任务
        String systemPrompt = """
            你是一个专业的贷款申请信息提取助手。

            你的任务是从贷款申请文档中提取结构化信息，并返回 JSON 格式。

            请严格按照以下 JSON 格式返回：
            {
              "applicantName": "申请人姓名",
              "idNumber": "身份证号",
              "phone": "联系电话",
              "address": "居住地址",
              "occupation": "职业",
              "income": "年收入（仅数字，不要单位）",
              "loanType": "贷款类型（personal/business/mortgage/car 之一）",
              "loanAmount": "贷款金额（仅数字，不要单位）",
              "loanTerm": "贷款期限（仅数字，不要单位）",
              "loanPurpose": "贷款用途",
              "repaymentSource": "还款来源",
              "guaranteeType": "担保方式（credit/mortgage/pledge/guarantor 之一）",
              "collateral": "抵押物描述（如无则填空字符串）",
              "collateralValue": "抵押物估值（仅数字，如无则填0）",
              "guarantorName": "保证人姓名（如无则填空字符串）",
              "guarantorPhone": "保证人电话（如无则填空字符串）"
            }

            注意事项：
            1. 如果文档中没有某个字段的信息，填空字符串或 0
            2. loanType 映射：个人消费=personal, 企业经营=business, 房屋抵押=mortgage, 汽车=car
            3. guaranteeType 映射：信用=credit, 抵押=mortgage, 质押=pledge, 保证人=guarantor
            4. 只返回 JSON，不要有其他解释文字
            """;

        // User Prompt: 提供具体的文档内容
        String userPrompt = """
            请从以下贷款申请文档中提取信息：

            %s

            请返回 JSON 格式的结构化数据。
            """.formatted(document);

        sendThinking(emitter, "📤 向 AI 发送请求...");
        sendProgress(emitter, 30, "等待 AI 响应...");

        try {
            // 调用 AI 模型
            Prompt prompt = new Prompt(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
            );

            String response = chatModel.call(prompt).getResult().getOutput().getContent();

            sendThinking(emitter, "📥 收到 AI 响应，正在解析...");
            sendProgress(emitter, 40, "解析结果...");

            // 清理响应（去除可能的 markdown 代码块标记）
            response = response.trim();
            if (response.startsWith("```json")) {
                response = response.substring(7);
            }
            if (response.startsWith("```")) {
                response = response.substring(3);
            }
            if (response.endsWith("```")) {
                response = response.substring(0, response.length() - 3);
            }
            response = response.trim();

            // 解析 JSON
            return objectMapper.readValue(response, LoanFormData.class);

        } catch (Exception e) {
            sendThinking(emitter, "❌ AI 解析出错: " + e.getMessage());
            return null;
        }
    }

    /**
     * 填充基本信息
     */
    private void fillBasicInfo(LoanFormData data, SseEmitter emitter) throws IOException {
        sendSwitchTab(emitter, "basic", "基本信息");

        fillField(emitter, "basic", "applicantName", "申请人姓名", data.getApplicantName());
        sleep(300); // 模拟逐个填充的视觉效果

        fillField(emitter, "basic", "idNumber", "身份证号", data.getIdNumber());
        sleep(300);

        fillField(emitter, "basic", "phone", "联系电话", data.getPhone());
        sleep(300);

        fillField(emitter, "basic", "address", "居住地址", data.getAddress());
        sleep(300);

        fillField(emitter, "basic", "occupation", "职业", data.getOccupation());
        sleep(300);

        fillField(emitter, "basic", "income", "年收入", data.getIncome());
    }

    /**
     * 填充贷款信息
     */
    private void fillLoanInfo(LoanFormData data, SseEmitter emitter) throws IOException {
        sendSwitchTab(emitter, "loan", "贷款信息");

        fillField(emitter, "loan", "loanType", "贷款类型", mapLoanType(data.getLoanType()));
        sleep(300);

        fillField(emitter, "loan", "loanAmount", "贷款金额", data.getLoanAmount());
        sleep(300);

        fillField(emitter, "loan", "loanTerm", "贷款期限", data.getLoanTerm());
        sleep(300);

        fillField(emitter, "loan", "loanPurpose", "贷款用途", data.getLoanPurpose());
        sleep(300);

        fillField(emitter, "loan", "repaymentSource", "还款来源", data.getRepaymentSource());
    }

    /**
     * 填充担保信息
     */
    private void fillGuaranteeInfo(LoanFormData data, SseEmitter emitter) throws IOException {
        sendSwitchTab(emitter, "guarantee", "担保信息");

        fillField(emitter, "guarantee", "guaranteeType", "担保方式", mapGuaranteeType(data.getGuaranteeType()));
        sleep(300);

        fillField(emitter, "guarantee", "collateral", "抵押物描述", data.getCollateral());
        sleep(300);

        fillField(emitter, "guarantee", "collateralValue", "抵押物估值", data.getCollateralValue());
        sleep(300);

        fillField(emitter, "guarantee", "guarantorName", "保证人姓名", data.getGuarantorName());
        sleep(300);

        fillField(emitter, "guarantee", "guarantorPhone", "保证人电话", data.getGuarantorPhone());
    }

    // ==================== 辅助方法 ====================

    private String mapLoanType(String type) {
        if (type == null) return "";
        return switch (type) {
            case "personal" -> "personal";
            case "business" -> "business";
            case "mortgage" -> "mortgage";
            case "car" -> "car";
            default -> type;
        };
    }

    private String mapGuaranteeType(String type) {
        if (type == null) return "";
        return switch (type) {
            case "credit" -> "credit";
            case "mortgage" -> "mortgage";
            case "pledge" -> "pledge";
            case "guarantor" -> "guarantor";
            default -> type;
        };
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== SSE 发送方法 ====================

    private void sendEvent(SseEmitter emitter, Map<String, Object> data) throws IOException {
        String jsonData = objectMapper.writeValueAsString(data);
        emitter.send(SseEmitter.event().data(jsonData));
    }

    private void sendThinking(SseEmitter emitter, String content) throws IOException {
        sendEvent(emitter, Map.of("type", "thinking", "content", content));
    }

    private void sendProgress(SseEmitter emitter, int progress, String message) throws IOException {
        sendEvent(emitter, Map.of("type", "progress", "progress", progress, "message", message));
    }

    private void sendSwitchTab(SseEmitter emitter, String tabId, String tabName) throws IOException {
        sendEvent(emitter, Map.of("type", "switch_tab", "tabId", tabId, "tabName", tabName));
    }

    private void fillField(SseEmitter emitter, String tabId, String fieldName,
                          String fieldLabel, String value) throws IOException {
        sendEvent(emitter, Map.of(
            "type", "fill_field",
            "tabId", tabId,
            "fieldName", fieldName,
            "fieldLabel", fieldLabel,
            "value", value != null ? value : ""
        ));
    }

    private void sendError(SseEmitter emitter, String message) throws IOException {
        sendEvent(emitter, Map.of("type", "error", "message", message));
    }

    /**
     * 获取示例文档列表
     */
    public Map<String, String> getSampleDocuments() {
        return new LinkedHashMap<>(SAMPLE_DOCUMENTS);
    }
}
