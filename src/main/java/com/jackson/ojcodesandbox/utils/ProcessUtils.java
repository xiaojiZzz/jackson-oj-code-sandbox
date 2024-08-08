package com.jackson.ojcodesandbox.utils;

import com.jackson.ojcodesandbox.model.dto.ExecuteMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程工具类
 *
 * @author jackson
 */
@Slf4j
public class ProcessUtils {

    /**
     * 执行进程并获取信息
     *
     * @param runProcess
     * @param opName
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        // 记录程序还未执行的内存使用量，这里只是模拟，并不准确，会受到很多因素的影响
        long initialMemory = getUsedMemory();
        try {
            // 计时
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            // 等待程序执行，获取错误码
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);

            if (exitValue == 0) {
                // 正常退出
                log.info(opName + " success");

                // 分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String> outputList = new ArrayList<>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputList.add(compileOutputLine);
                }
                executeMessage.setMessage(StringUtils.join(outputList, "\n"));
            } else {
                // 异常退出
                log.error(opName + "fail，exitValue：" + exitValue);

                // 分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String> outputStrList = new ArrayList<>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputStrList.add(compileOutputLine);
                }
                executeMessage.setMessage(StringUtils.join(outputStrList, "\n"));

                // 分批获取进程的错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                // 逐行读取
                List<String> errorList = new ArrayList<>();
                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorList.add(errorCompileOutputLine);
                }
                executeMessage.setErrorMessage(StringUtils.join(errorList, "\n"));
            }

            stopWatch.stop();
            long finalMemory = getUsedMemory();
            // 计算内存使用量，单位字节，转换成 KB 需要除以 1024
            long memoryUsage = finalMemory - initialMemory;
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
            executeMessage.setMemory(memoryUsage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * 执行交互式进程并获取执行信息
     *
     * @param runProcess
     * @param input
     * @return
     * @throws IOException
     */
    public static ExecuteMessage getAcmProcessMessage(Process runProcess, String input) throws IOException {

        ExecuteMessage executeMessage = new ExecuteMessage();
        StringReader inputReader = new StringReader(input);
        BufferedReader inputBufferedReader = new BufferedReader(inputReader);

        // 记录程序还未执行的内存使用量，这里只是模拟，并不准确，会受到很多因素的影响
        long initialMemory = getUsedMemory();
        // 计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 输入（模拟控制台输入）
        PrintWriter consoleInput = new PrintWriter(runProcess.getOutputStream());
        String line;
        while ((line = inputBufferedReader.readLine()) != null) {
            consoleInput.println(line);
            // 模拟输入数据到控制台后按回车
            consoleInput.flush();
        }
        consoleInput.close();

        // 获取输出
        BufferedReader userCodeOutput = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
        List<String> outputList = new ArrayList<>();
        String outputLine;
        while ((outputLine = userCodeOutput.readLine()) != null) {
            outputList.add(outputLine);
        }
        userCodeOutput.close();

        // 获取错误输出
        BufferedReader errorOutput = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
        List<String> errorList = new ArrayList<>();
        String errorLine;
        while ((errorLine = errorOutput.readLine()) != null) {
            errorList.add(errorLine);
        }
        errorOutput.close();

        stopWatch.stop();

        // 填充返回参数
        long finalMemory = getUsedMemory();
        // 计算内存使用量，单位字节，转换成 KB 需要除以 1024
        long memoryUsage = finalMemory - initialMemory;
        executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        executeMessage.setMemory(memoryUsage);
        executeMessage.setMessage(StringUtils.join(outputList, "\n"));
        executeMessage.setErrorMessage(StringUtils.join(errorList, "\n"));
        runProcess.destroy();

        return executeMessage;
    }

    /**
     * 获取当前已使用的内存量，单位为 byte，使用这个只是模拟，实际会受到很多因素的影响
     *
     * @return
     */
    public static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
