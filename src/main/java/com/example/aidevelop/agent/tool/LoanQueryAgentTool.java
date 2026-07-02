package com.example.aidevelop.agent.tool;

import com.example.aidevelop.service.business.LoanQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LoanQueryAgentTool implements AgentTool {

    private final LoanQueryService loanQueryService;

    @Override
    public String name() {
        return "loan.query";
    }

    @Override
    public String description() {
        return "loan.query: 查询用户借款记录。参数: userNo(string,用户编号), status(string,可选,枚举值: INIT|SUCCESS|FAIL|PENDING)";
    }

    @Override
    public Object execute(Map<String, Object> args) {
        String userNo = readString(args, "userNo", "");
        String status = readString(args, "status", null);
        return loanQueryService.queryLoanRecords(userNo, status);
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
