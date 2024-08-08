package com.jackson.ojcodesandbox.service.impl;

import com.jackson.ojcodesandbox.codesandbox.java.JavaNativeAcmCodeSandbox;
import com.jackson.ojcodesandbox.model.dto.ExecuteCodeRequest;
import com.jackson.ojcodesandbox.model.dto.ExecuteCodeResponse;
import com.jackson.ojcodesandbox.service.CodeSandboxService;
import com.jackson.ojcodesandbox.template.CppCodeSandboxTemplate;
import com.jackson.ojcodesandbox.template.JavaCodeSandboxTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 代码沙箱服务实现
 *
 * @author jackson
 */
@Service
public class CodeSandboxServiceImpl implements CodeSandboxService {

    @Resource
    JavaCodeSandboxTemplate javaNativeAcmCodeSandbox;

    @Resource
    CppCodeSandboxTemplate cppNativeAcmCodeSandbox;

    /**
     * 使用代码沙箱执行代码并获取执行信息
     *
     * @param executeCodeRequest
     * @return
     */
    @Override
    public ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest) {

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        switch (language) {
            case "java":
                return javaNativeAcmCodeSandbox.executeJavaCode(inputList, code);
            case "cpp":
                return cppNativeAcmCodeSandbox.executeCppCode(inputList, code);
            default:
                return null;
        }
    }
}
