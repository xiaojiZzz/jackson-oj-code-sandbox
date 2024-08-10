package com.jackson.ojcodesandbox.codesandbox.java;

import com.jackson.ojcodesandbox.model.dto.ExecuteMessage;
import com.jackson.ojcodesandbox.template.JavaCodeSandboxTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Cpp Docker 代码沙箱，ACM 形式
 *
 * @author jackson
 */
@Component
@Slf4j
public class JavaDockerAcmCodeSandbox extends JavaCodeSandboxTemplate {
    @Override
    protected List<ExecuteMessage> runCode(String dir, List<String> inputList) throws IOException {

        // 未完待续
        return null;
    }
}
