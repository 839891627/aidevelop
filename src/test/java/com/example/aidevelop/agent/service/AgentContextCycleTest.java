package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.multi.MultiAgentProperties;
import com.example.aidevelop.agent.multi.SubAgentRunner;
import com.example.aidevelop.agent.multi.SubAgentTool;
import com.example.aidevelop.agent.tool.ToolRouter;
import com.example.aidevelop.config.AgentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

class AgentContextCycleTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(AgentToolGraphConfiguration.class)
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withBean(AgentProperties.class, AgentProperties::new)
        .withBean(MultiAgentProperties.class, MultiAgentProperties::new)
        .withBean(AgentToolExecutor.class, () -> new AgentToolExecutor(null, null, null))
        .withBean(AgentReflector.class, () -> new AgentReflector(new ObjectMapper()))
        .withBean(AgentResponder.class, () -> new AgentResponder(new ObjectMapper(), new AgentProperties(), null))
        .withBean("chatClientForOpenAI", ChatClient.class, AgentContextCycleTest::noopChatClient);

    @Test
    void shouldCreateAgentToolGraphWithoutCircularReference() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ToolRouter.class);
            assertThat(context).hasSingleBean(SubAgentTool.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
        ToolRouter.class,
        AgentPolicyEnforcer.class,
        AgentPlanner.class,
        SubAgentRunner.class,
        SubAgentTool.class
    })
    static class AgentToolGraphConfiguration {
    }

    private static ChatClient noopChatClient() {
        return (ChatClient) Proxy.newProxyInstance(
            ChatClient.class.getClassLoader(),
            new Class<?>[]{ChatClient.class},
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> "noopChatClient";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
                throw new UnsupportedOperationException("ChatClient should not be called during context creation");
            }
        );
    }
}
