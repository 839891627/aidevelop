package com.example.aidevelop.agent.tool;

import com.example.aidevelop.service.function.RepaymentQueryFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RepaymentQueryAgentTool implements AgentTool {

    private final RepaymentQueryFunction repaymentQueryFunction;

    @Override
    public String name() {
        return "repayment.query";
    }

    @Override
    public Object execute(Map<String, Object> args) {
        String userNo = readString(args, "userNo", "");
        String status = readString(args, "status", null);
        return repaymentQueryFunction.queryRepaymentRecords(new RepaymentQueryFunction.Request(userNo, status));
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
