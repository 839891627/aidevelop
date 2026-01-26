package com.example.aidevelop.service.rag;

import com.example.aidevelop.model.entity.Conversation;
import com.example.aidevelop.model.entity.Message;
import com.example.aidevelop.model.entity.MessageRole;
import com.example.aidevelop.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询重写服务
 *
 * 功能：基于对话历史重写用户查询，实现指代消解和上下文补全
 *
 * 适用场景：
 * - 用户使用代词（"它"、"这个"、"那个"）需要替换为具体实体
 * - 用户提问不完整（"那怎么办"、"然后呢"）需要补全
 * - 用户口语化表达需要规范化
 */
@Service
@Slf4j
public class QueryRewriteService {

    private final ChatModel chatModel;
    private final ConversationRepository conversationRepository;

    public QueryRewriteService(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            ConversationRepository conversationRepository) {
        this.chatModel = chatModel;
        this.conversationRepository = conversationRepository;
    }

    /**
     * 重写查询（基于对话历史）
     *
     * @param originalQuery 原始查询
     * @param conversationId 对话 ID
     * @return 重写后的查询
     */
    public String rewriteQuery(String originalQuery, String conversationId) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return originalQuery;
        }

        // 1. 如果没有对话 ID 或是新对话，直接返回原查询
        if (conversationId == null || conversationId.isEmpty()) {
            log.debug("新对话，无需重写查询: {}", originalQuery);
            return originalQuery;
        }

        // 2. 获取对话历史
        var conversationOpt = conversationRepository.findById(conversationId);
        if (conversationOpt.isEmpty()) {
            log.debug("未找到对话历史，直接返回原查询: {}", originalQuery);
            return originalQuery;
        }

        Conversation conversation = conversationOpt.get();
        List<Message> history = conversation.getMessages();

        // 3. 如果历史消息少于 2 条（一轮对话），无需重写
        if (history.size() < 2) {
            log.debug("对话历史不足，无需重写: {}", originalQuery);
            return originalQuery;
        }

        // 4. 检查是否需要重写（简单规则判断）
        if (!needsRewrite(originalQuery)) {
            log.debug("查询不需要重写: {}", originalQuery);
            return originalQuery;
        }

        // 5. 构建对话历史文本
        String historyText = buildHistoryText(history);

        // 6. 调用 LLM 重写查询
        String rewrittenQuery = callLLMForRewrite(originalQuery, historyText);

        log.info("查询重写: {} -> {}", originalQuery, rewrittenQuery);
        return rewrittenQuery;
    }

    /**
     * 判断查询是否需要重写
     * 基于简单规则：包含代词、省略主语、口语化表达等
     */
    private boolean needsRewrite(String query) {
        if (query == null) return false;

        // 规则 1: 包含代词
        if (query.contains("它") || query.contains("这个") || query.contains("那个") ||
            query.contains("它们") || query.contains("这些") || query.contains("那些")) {
            return true;
        }

        // 规则 2: 省略主语（常见模式）
        if (query.matches("^(那|那么|然后|接着|还有).*")) {
            return true;
        }

        // 规则 3: 过于简短的查询（少于 5 个字）
        if (query.length() < 5 && !query.contains("?") && !query.contains("？")) {
            return true;
        }

        // 规则 4: 包含"怎么办"、"怎么样"等需要上下文的问题
        if (query.contains("怎么办") || query.contains("怎么样") || query.contains("如何")) {
            return true;
        }

        return false;
    }

    /**
     * 构建对话历史文本（用于发送给 LLM）
     */
    private String buildHistoryText(List<Message> history) {
        return history.stream()
                .filter(m -> m.getRole() != MessageRole.SYSTEM)
                .map(m -> {
                    String roleLabel = m.getRole() == MessageRole.USER ? "用户" : "助手";
                    return roleLabel + ": " + m.getContent();
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * 调用 LLM 重写查询
     */
    private String callLLMForRewrite(String originalQuery, String historyText) {
        String prompt = String.format("""
                你是查询重写专家。请根据对话历史，重写用户的查询，使其更清晰、更具体、更规范。

                ## 对话历史
                %s

                ## 当前查询
                %s

                ## 重写规则
                1. **指代消解**：如果查询包含代词（它、这个、那个），根据对话历史替换为具体实体
                   - 示例："它支持提前还款吗" → "产品A支持提前还款吗"

                2. **补全省略**：如果查询不完整，根据上下文补充完整
                   - 示例："那怎么办" → "如果借款人逾期，应该采取什么催收措施"

                3. **口语化规范化**：将口语化表达转换为规范的书面语
                   - 示例："要是我不还钱会咋样" → "借款人逾期未还款会有哪些后果和影响"

                4. **保持原意**：不要改变用户原本想问的问题，只是让表达更清晰

                ## 输出格式
                只返回重写后的查询，不要添加任何解释、引号或其他内容。

                ## 重写后的查询
                """,
                historyText,
                originalQuery
        );

        try {
            String response = chatModel.call(prompt);
            // 清理响应（去除可能的引号、换行等）
            return response.trim()
                    .replaceAll("^\"|\"$", "")  // 去除首尾引号
                    .replaceAll("\n", " ")       // 替换换行为空格
                    .trim();
        } catch (Exception e) {
            log.error("LLM 查询重写失败，返回原查询: {}", e.getMessage());
            return originalQuery;
        }
    }

    /**
     * 获取查询重写的详细信息（用于调试）
     *
     * @param originalQuery 原始查询
     * @param conversationId 对话 ID
     * @return 重写详情
     */
    public RewriteDetail getRewriteDetail(String originalQuery, String conversationId) {
        String rewrittenQuery = rewriteQuery(originalQuery, conversationId);
        boolean changed = !originalQuery.equals(rewrittenQuery);

        String reason = null;
        if (changed) {
            if (originalQuery.contains("它") || originalQuery.contains("这个") || originalQuery.contains("那个")) {
                reason = "指代消解：将代词替换为具体实体";
            } else if (originalQuery.matches("^(那|那么|然后|接着).*")) {
                reason = "补全省略：根据上下文补充完整的问题";
            } else if (originalQuery.contains("怎么办") || originalQuery.contains("怎么样")) {
                reason = "上下文补全：补充具体场景";
            } else {
                reason = "规范化：口语化转书面语";
            }
        }

        return new RewriteDetail(
                originalQuery,
                rewrittenQuery,
                changed,
                reason
        );
    }

    /**
     * 批量查询重写
     *
     * @param queries 查询列表
     * @param conversationId 对话 ID
     * @return 重写后的查询映射（原始查询 → 重写查询）
     */
    public java.util.Map<String, String> batchRewrite(java.util.List<String> queries, String conversationId) {
        return queries.stream()
                .collect(java.util.stream.Collectors.toMap(
                        q -> q,
                        q -> rewriteQuery(q, conversationId)
                ));
    }

    /**
     * 查询重写详情
     */
    public record RewriteDetail(
            String originalQuery,
            String rewrittenQuery,
            boolean changed,
            String reason
    ) {
        public String getOriginalQuery() { return originalQuery; }
        public String getRewrittenQuery() { return rewrittenQuery; }
        public Boolean getChanged() { return changed; }
        public String getReason() { return reason; }
    }
}
