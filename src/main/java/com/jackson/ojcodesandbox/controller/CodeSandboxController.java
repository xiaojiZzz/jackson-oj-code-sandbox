package com.jackson.ojcodesandbox.controller;

import com.jackson.ojcodesandbox.model.dto.ExecuteCodeRequest;
import com.jackson.ojcodesandbox.model.dto.ExecuteCodeResponse;
import com.jackson.ojcodesandbox.service.CodeSandboxService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 代码沙箱接口
 *
 * @author jackson
 */
@RestController
@RequestMapping("/codesandbox")
public class CodeSandboxController {

    /**
     * 定义鉴权请求头
     */
    private static final String AUTH_REQUEST_HEADER = "auth";

    /**
     * 定义鉴权密钥
     */
    private static final String AUTH_REQUEST_SECRET = "jackson_7788";

    @Resource
    private CodeSandboxService codeSandboxService;

    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request, HttpServletResponse response) {

        // 基本的认证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            response.setStatus(403);
            return null;
        }

        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }

        return codeSandboxService.execute(executeCodeRequest);
    }
}
