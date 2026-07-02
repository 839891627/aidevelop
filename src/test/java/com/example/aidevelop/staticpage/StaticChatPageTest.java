package com.example.aidevelop.staticpage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class StaticChatPageTest {

    private static String readStaticFile(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/resources/static", relativePath));
    }

    @Test
    void chatPageExposesMainNavigationAndThreeApiModes() throws IOException {
        String html = readStaticFile("index.html");

        assertThat(html).contains("/prompt.html", "/cost.html");
        assertThat(html).contains("value=\"general\"", "value=\"financial_rag\"", "value=\"agent\"");
        assertThat(html).contains("历史聊天", "id=\"conversationHistory\"", "id=\"newChatBtn\"");
        assertThat(html).contains(
            "什么是 RAG？和 Agent 有什么区别？",
            "请根据金融助贷知识库说明借款申请通常需要关注哪些规则",
            "请查询 USER001 的借款记录，并总结当前借款状态",
            "请分析 USER001 的借款和还款信息，指出需要关注的问题",
            "请对 USER001 做风险评估，并给出风险等级、依据和建议",
            "请检索知识库中的风控规则，并结合 USER001 的情况给出判断",
            "data-mode=\"general\"",
            "data-mode=\"financial_rag\"",
            "data-mode=\"agent\"",
            "class=\"chip-tag\""
        );
        assertThat(html).doesNotContain("CUST1001");
    }

    @Test
    void chatScriptRoutesMessagesToSelectedApiMode() throws IOException {
        String script = readStaticFile("js/chat.js");

        assertThat(script).contains("/api/chat/stream", "/api/chat", "/api/agent/chat");
        assertThat(script).contains("sendAgentMessage", "apiModeSelect", "button.dataset.mode", "mode: mode", "USER001");
        assertThat(script).doesNotContain("CUST1001");
    }

    @Test
    void chatScriptLoadsAndRestoresConversationHistory() throws IOException {
        String script = readStaticFile("js/chat.js");

        assertThat(script).contains(
            "loadConversationHistory",
            "renderConversationHistory",
            "loadConversationMessages",
            "/api/chat/conversations",
            "/api/chat/${conversationId}/messages",
            "conversationHistory",
            "newChatBtn"
        );
    }

    @Test
    void chatScriptNormalizesLooseMarkdownBeforeRendering() throws IOException {
        String script = readStaticFile("js/chat.js");

        assertThat(script).contains("normalizeMarkdownText", "(#{1,6})", "(\\s*\\d+\\.)");
    }

    @Test
    void chatStylesDefineChatGptInspiredWorkspaceLayout() throws IOException {
        String css = readStaticFile("css/chat.css");

        assertThat(css).contains(".app-shell", ".sidebar", ".composer-card", ".api-switcher");
    }

    @Test
    void chatMessagesCanShrinkInsideWorkspaceGrid() throws IOException {
        String css = readStaticFile("css/chat.css");

        Pattern chatMessagesBlock = Pattern.compile("\\.chat-messages\\s*\\{[^}]*min-height:\\s*0;", Pattern.DOTALL);
        assertThat(chatMessagesBlock.matcher(css).find()).isTrue();
    }

    @Test
    void chatWorkspaceGridKeepsLongHistoryInsideSidebar() throws IOException {
        String css = readStaticFile("css/chat.css");

        Pattern shellBlock = Pattern.compile("\\.app-shell\\s*\\{[^}]*grid-template-rows:\\s*minmax\\(0,\\s*1fr\\);", Pattern.DOTALL);
        Pattern sidebarBlock = Pattern.compile("\\.sidebar\\s*\\{[^}]*min-height:\\s*0;", Pattern.DOTALL);
        Pattern workspaceBlock = Pattern.compile("\\.chat-workspace\\s*\\{[^}]*min-height:\\s*0;", Pattern.DOTALL);

        assertThat(shellBlock.matcher(css).find()).isTrue();
        assertThat(sidebarBlock.matcher(css).find()).isTrue();
        assertThat(workspaceBlock.matcher(css).find()).isTrue();
    }

    @Test
    void chatSidebarNavigationUsesCompactSpacing() throws IOException {
        String css = readStaticFile("css/chat.css");

        Pattern navItemBlock = Pattern.compile("\\.nav-item\\s*\\{[^}]*padding:\\s*10px\\s+12px;", Pattern.DOTALL);
        Pattern navTitleBlock = Pattern.compile("\\.nav-item span\\s*\\{[^}]*font-size:\\s*14px;", Pattern.DOTALL);
        Pattern navCaptionBlock = Pattern.compile("\\.nav-item small\\s*\\{[^}]*font-size:\\s*11px;", Pattern.DOTALL);

        assertThat(navItemBlock.matcher(css).find()).isTrue();
        assertThat(navTitleBlock.matcher(css).find()).isTrue();
        assertThat(navCaptionBlock.matcher(css).find()).isTrue();
    }

    @Test
    void promptPageUsesIntegratedWorkspaceNavigation() throws IOException {
        String html = readStaticFile("prompt.html");
        String css = readStaticFile("css/prompt.css");
        String script = readStaticFile("js/prompt.js");

        assertThat(html).contains("/index.html", "/cost.html", "class=\"prompt-shell\"");
        assertThat(css).contains(".prompt-shell", ".prompt-nav", ".prompt-grid");
        assertThat(script).contains("chat.general", "chat.financial.rag");
    }

    @Test
    void costPageUsesIntegratedWorkspaceNavigation() throws IOException {
        String html = readStaticFile("cost.html");
        String css = readStaticFile("css/cost.css");
        String script = readStaticFile("js/cost.js");

        assertThat(html).contains("/index.html", "/prompt.html", "class=\"cost-shell\"");
        assertThat(css).contains(".cost-shell", ".cost-nav", ".metric-grid");
        assertThat(script).contains("#0f7b65", "#ebe4d7");
    }
}
