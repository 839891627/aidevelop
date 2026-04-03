package com.example.aidevelop.controller;

import com.example.aidevelop.service.LoanFormService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * 贷款申请表单 AI 填充控制器
 *
 * 核心功能：
 * 1. 接收前端 SSE 请求
 * 2. 调用 AI 解析文档
 * 3. 流式返回填充过程
 * 4. 支持暂停/继续
 */
@RestController
@RequestMapping("/api/loan-form")
@CrossOrigin(origins = "*")
public class LoanFormController {

    @Autowired
    private LoanFormService loanFormService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * SSE 流式填充端点
     *
     * 前端通过 EventSource 连接此端点
     * 后端逐步推送 AI 处理结果
     */
    @GetMapping(value = "/fill-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter fillFormStream(@RequestParam String docId) {
        // 创建 SSE Emitter，设置超时时间为 5 分钟
        SseEmitter emitter = new SseEmitter(300000L);

        // 在新线程中处理，避免阻塞
        new Thread(() -> {
            try {
                // 发送连接成功消息
                sendEvent(emitter, Map.of(
                    "type", "thinking",
                    "content", "正在读取文档内容..."
                ));

                // 调用 Service 处理文档填充
                loanFormService.processDocumentAndFill(docId, emitter);

                // 发送完成消息
                sendEvent(emitter, Map.of(
                    "type", "complete",
                    "message", "所有字段填充完成！"
                ));

            } catch (Exception e) {
                try {
                    sendEvent(emitter, Map.of(
                        "type", "error",
                        "message", "处理过程中出现错误: " + e.getMessage()
                    ));
                } catch (IOException ignored) {}
            } finally {
                emitter.complete();
            }
        }).start();

        return emitter;
    }

    /**
     * 发送 SSE 事件
     */
    private void sendEvent(SseEmitter emitter, Map<String, Object> data) throws IOException {
        String jsonData = objectMapper.writeValueAsString(data);
        emitter.send(SseEmitter.event().data(jsonData));
    }

    /**
     * 测试端点：获取所有文档列表
     */
    @GetMapping("/documents")
    public Map<String, Object> getDocuments() {
        return Map.of(
            "documents", loanFormService.getSampleDocuments()
        );
    }

    /**
     * 提交表单端点
     */
    @PostMapping("/submit")
    public Map<String, Object> submitForm(@RequestBody Map<String, Object> formData) {
        // 这里可以保存到数据库
        return Map.of(
            "success", true,
            "message", "申请表单提交成功",
            "applicationId", "APP" + System.currentTimeMillis()
        );
    }
}
