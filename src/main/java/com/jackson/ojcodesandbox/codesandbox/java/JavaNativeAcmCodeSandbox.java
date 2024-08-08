package com.jackson.ojcodesandbox.codesandbox.java;

import cn.hutool.core.util.StrUtil;
import com.jackson.ojcodesandbox.model.dto.ExecuteMessage;
import com.jackson.ojcodesandbox.template.JavaCodeSandboxTemplate;
import com.jackson.ojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Java 原生代码沙箱，ACM 形式
 *
 * @author jackson
 */
@Component
@Slf4j
public class JavaNativeAcmCodeSandbox extends JavaCodeSandboxTemplate {

    /**
     * 超时时间 5000ms
     */
    private static final long TIME_OUT = 5000L;

    /**
     * 运行代码，得到输出结果
     *
     * @param dir
     * @param inputList
     * @return
     * @throws IOException
     */
    @Override
    protected List<ExecuteMessage> runCode(String dir, List<String> inputList) throws IOException {
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        // 执行代码，获取输出结果
        for (String input : inputList) {
            // 执行的命令
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main", dir);

            // 开始计时
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            // 开始执行命令
            Process runProcess = Runtime.getRuntime().exec(runCmd);

            // 超时控制
            @SuppressWarnings({"all"})
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(TIME_OUT);
                    // 发生了超时
                    runProcess.destroy();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            thread.start();

            ExecuteMessage executeMessage = null;
            try {
                executeMessage = ProcessUtils.getAcmProcessMessage(runProcess, input);
            } catch (IOException e) {
                log.error("执行出错：{}", e.toString());
            }

            stopWatch.stop();
            if (!thread.isAlive()) {
                executeMessage = new ExecuteMessage();
                executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
                executeMessage.setErrorMessage("超出时间限制");
            }

            if (executeMessage != null) {
                executeMessageList.add(executeMessage);
            }

            // 发生了错误
            if (executeMessage != null && StrUtil.isNotEmpty(executeMessage.getErrorMessage())) {
                break;
            }
        }
        return executeMessageList;
    }
}
