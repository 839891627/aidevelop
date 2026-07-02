package com.example.aidevelop.agent.tool;

import com.example.aidevelop.service.business.RepaymentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RepaymentQueryAgentTool implements AgentTool {

    private final RepaymentQueryService repaymentQueryService;

    @Override
    public String name() {
        return "repayment.query";
    }

    @Override
    public String description() {
        return "repayment.query: 查询用户还款记录。参数: userNo(string,用户编号), status(string,可选,枚举值: INIT|SUCCESS|FAIL|PENDING)";
    }

    @Override
    public Object execute(Map<String, Object> args) {
        String userNo = readString(args, "userNo", "");
        String status = readString(args, "status", null);
        return repaymentQueryService.queryRepaymentRecords(userNo, status);
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
