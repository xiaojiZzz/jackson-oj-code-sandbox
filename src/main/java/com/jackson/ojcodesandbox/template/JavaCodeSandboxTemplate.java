package com.jackson.ojcodesandbox.template;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.jackson.ojcodesandbox.model.dto.ExecuteCodeResponse;
import com.jackson.ojcodesandbox.model.dto.ExecuteMessage;
import com.jackson.ojcodesandbox.model.dto.JudgeInfo;
import com.jackson.ojcodesandbox.model.enums.ExecuteCodeStatusEnum;
import com.jackson.ojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Java 代码沙箱模板
 *
 * @author jackson
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate {

    /**
     * 存放代码的目录
     */
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    /**
     * Java 统一类名
     */
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    /**
     * 超时时间 5000ms
     */
    private static final long TIME_OUT = 5000L;

    /**
     * 模板方法，定义运行步骤（Java 的）
     */
    public ExecuteCodeResponse executeJavaCode(List<String> inputList, String code) {

        // 1. 把用户代码保存为文件
        String dir = null;
        File userCodeFile = null;
        try {
            dir = System.getProperty("user.dir") + File.separator + GLOBAL_CODE_DIR_NAME + File.separator + UUID.randomUUID();
            userCodeFile = saveCodeToFile(code, dir);
        } catch (Exception e) {
            return ExecuteCodeResponse.builder()
                    .status(ExecuteCodeStatusEnum.COMPILE_FAILED.getValue())
                    .message(ExecuteCodeStatusEnum.COMPILE_FAILED.getMsg())
                    .build();
        }

        // 2. 编译代码，得到 class 文件
        try {
            ExecuteMessage compileMessage = compileFile(userCodeFile);
            // 编译失败
            if (compileMessage.getExitValue() != 0) {
                return ExecuteCodeResponse.builder()
                        .status(ExecuteCodeStatusEnum.COMPILE_FAILED.getValue())
                        .message(compileMessage.getErrorMessage())
                        .build();
            }
        } catch (IOException e) {
            return ExecuteCodeResponse.builder()
                    .status(ExecuteCodeStatusEnum.COMPILE_FAILED.getValue())
                    .message(e.toString())
                    .build();
        }

        // 3. 执行代码，得到输出结果
        List<ExecuteMessage> executeResults;
        try {
            executeResults = runCode(dir, inputList);
            // 如果错误信息不为空，证明有错误
            ExecuteMessage executeMessage = executeResults.get(executeResults.size() - 1);
            if (StrUtil.isNotEmpty(executeMessage.getErrorMessage())) {
                return ExecuteCodeResponse.builder()
                        .status(ExecuteCodeStatusEnum.RUN_FAILED.getValue())
                        .message(executeMessage.getErrorMessage())
                        .build();
            }
        } catch (IOException e) {
            return ExecuteCodeResponse.builder()
                    .status(ExecuteCodeStatusEnum.RUN_FAILED.getValue())
                    .message(e.toString())
                    .build();
        }

        // 4. 收集整理输出的结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeResults);

        // 5. 文件清理
        boolean del = clearFile(userCodeFile, dir);
        if (!del) {
            log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }

        return outputResponse;
    }

    /**
     * 把用户的代码保存为文件
     *
     * @param code 用户代码
     * @return
     */
    protected File saveCodeToFile(String code, String dir) {
        // 把用户的代码隔离存放
        String userCodePath = dir + File.separator + GLOBAL_JAVA_CLASS_NAME;
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    /**
     * 编译代码
     *
     * @param userCodeFile
     * @return
     */
    protected ExecuteMessage compileFile(File userCodeFile) throws IOException {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        Process compileProcess = Runtime.getRuntime().exec(compileCmd);
        return ProcessUtils.runProcessAndGetMessage(compileProcess, "compile");
    }

    /**
     * 运行代码，得到输出结果
     *
     * @param dir
     * @param inputList
     * @return
     * @throws IOException
     */
    protected abstract List<ExecuteMessage> runCode(String dir, List<String> inputList) throws IOException;

    /**
     * 整理输出结果
     *
     * @param executeMessageList
     * @return
     */
    protected ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();

        // 取用时最大值，便于判断是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
            if (maxTime > TIME_OUT) {
                break;
            }
        }

        // 正常运行完成
        if (outputList.size() == executeMessageList.size() && maxTime <= TIME_OUT) {
            executeCodeResponse.setStatus(ExecuteCodeStatusEnum.SUCCESS.getValue());
        } else {
            executeCodeResponse.setStatus(ExecuteCodeStatusEnum.RUN_FAILED.getValue());
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 文件清理
     *
     * @param codeFile
     * @param dir
     */
    protected boolean clearFile(File codeFile, String dir) {
        if (codeFile.getParentFile() != null) {
            boolean del = FileUtil.del(dir);
            log.info("删除{}: {}", del ? "成功" : "失败", dir);
            return del;
        }
        return true;
    }
}
