package com.example.ai.control;

import com.example.ai.pojo.AiSqlQueryRequest;
import com.example.ai.pojo.AiSqlQueryResponse;
import com.example.ai.pojo.LeeResult;
import com.example.ai.service.AiSqlQueryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiSqlQueryController {

    private final AiSqlQueryService aiSqlQueryService;

    /**
     * 自然语言数据查询入口。
     *
     * 控制器只负责：
     * 1. 读取当前登录用户身份；
     * 2. 接收用户问题和模型名；
     * 3. 调用后端安全查询服务；
     * 4. 返回 SQL 与查询结果。
     */
    @PostMapping("/data-query")
    public LeeResult<AiSqlQueryResponse> query(@RequestBody AiSqlQueryRequest request,
                                               HttpServletRequest servletRequest) {
        Integer userId = currentUserId(servletRequest);
        if (userId == null) {
            return LeeResult.fail(401, "Missing user");
        }
        try {
            AiSqlQueryResponse response = aiSqlQueryService.query(
                    request == null ? null : request.getQuestion(),
                    request == null ? null : request.getModel(),
                    userId,
                    currentRole(servletRequest)
            );
            return LeeResult.ok(response);
        } catch (IllegalArgumentException ex) {
            return LeeResult.fail(ex.getMessage());
        }
    }

    private Integer currentUserId(HttpServletRequest request) {
        String authUserId = (String) request.getAttribute("authUserId");
        if (!StringUtils.hasText(authUserId)) {
            return null;
        }
        return Integer.parseInt(authUserId);
    }

    private String currentRole(HttpServletRequest request) {
        Object authRole = request.getAttribute("authRole");
        return authRole == null ? "" : String.valueOf(authRole).trim().toUpperCase();
    }
}
