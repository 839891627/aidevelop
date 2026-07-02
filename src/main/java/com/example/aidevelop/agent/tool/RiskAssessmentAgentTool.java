package com.example.aidevelop.agent.tool;

import com.example.aidevelop.service.business.RiskAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RiskAssessmentAgentTool implements AgentTool {

    private final RiskAssessmentService riskAssessmentService;

    @Override
    public String name() {
        return "risk.assess";
    }

    @Override
    public String description() {
        return "risk.assess: 评估用户风险等级。参数: userNo(string,用户编号)";
    }

    @Override
    public Object execute(Map<String, Object> args) {
        String userNo = readString(args, "userNo", "");
        return riskAssessmentService.assessRisk(userNo);
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
