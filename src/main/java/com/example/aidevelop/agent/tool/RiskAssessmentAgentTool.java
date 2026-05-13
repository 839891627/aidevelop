package com.example.aidevelop.agent.tool;

import com.example.aidevelop.service.function.RiskAssessmentFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RiskAssessmentAgentTool implements AgentTool {

    private final RiskAssessmentFunction riskAssessmentFunction;

    @Override
    public String name() {
        return "risk.assess";
    }

    @Override
    public Object execute(Map<String, Object> args) {
        String userNo = readString(args, "userNo", "");
        return riskAssessmentFunction.assessRisk(new RiskAssessmentFunction.Request(userNo));
    }

    private String readString(Map<String, Object> args, String key, String defaultValue) {
        if (args == null) {
            return defaultValue;
        }
        Object value = args.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }
}
