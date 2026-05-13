package com.example.aidevelop.agent.tool;

import com.example.aidevelop.service.function.LoanQueryFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LoanQueryAgentTool implements AgentTool {

    private final LoanQueryFunction loanQueryFunction;

    @Override
    public String name() {
        return "loan.query";
    }

    @Override
    public Object execute(Map<String, Object> args) {
        String userNo = readString(args, "userNo", "");
        String status = readString(args, "status", null);
        return loanQueryFunction.queryLoanRecords(new LoanQueryFunction.UserStatusRequest(userNo, status));
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
