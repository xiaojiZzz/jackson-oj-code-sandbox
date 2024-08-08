package com.jackson.ojcodesandbox.service;

import com.jackson.ojcodesandbox.model.dto.ExecuteCodeRequest;
import com.jackson.ojcodesandbox.model.dto.ExecuteCodeResponse;

/**
 * 代码沙箱服务
 *
 * @author jackson
 */
public interface CodeSandboxService {

    /**
     * 使用代码沙箱执行代码并获取执行信息
     *
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest);
}
