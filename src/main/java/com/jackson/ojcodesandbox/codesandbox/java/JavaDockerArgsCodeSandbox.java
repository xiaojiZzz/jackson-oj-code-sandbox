package com.jackson.ojcodesandbox.codesandbox.java;

import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.jackson.ojcodesandbox.model.dto.ExecuteMessage;
import com.jackson.ojcodesandbox.template.JavaCodeSandboxTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Java Docker 代码沙箱，Args 形式
 *
 * @author jackson
 */
@Component
@Slf4j
public class JavaDockerArgsCodeSandbox extends JavaCodeSandboxTemplate {

    /**
     * 判断是否是第一次创建镜像
     */
    public static boolean FIRST_INIT = true;

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

        // 获取默认的 DockerClient 对象
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        // 拉取镜像
        String imageName = "openjdk:8-alpine";
        if (FIRST_INIT) {
            FIRST_INIT = false;
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(imageName);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    log.info("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                log.error("拉取镜像异常：" + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        log.info("下载完成");

        // 创建容器
        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(imageName);
        // 设置配置信息
        HostConfig hostConfig = new HostConfig();
        // 设置最大内存 10 MB
        hostConfig.withMemory(10 * 1024 * 1024L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        // 本机目录挂载到容器的 /app 目录下
        hostConfig.setBinds(new Bind(dir, new Volume("/app")));
        CreateContainerResponse createContainerResponse = createContainerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        String containerId = createContainerResponse.getId();

        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();

        // 执行命令 docker exec containerId java -cp /app Main Args
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String input : inputList) {
            // 执行的命令
            String[] inputArgsArray = input.split(" ");
            String[] runCmd = ArrayUtil.append(new String[]{"java", "-Xmx256m", "-Dfile.encoding=UTF-8", "-cp", "/app", "Main"}, inputArgsArray);

            // 创建执行命令
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(runCmd)
                    .withAttachStdin(true)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .exec();
            String execId = execCreateCmdResponse.getId();

            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null};
            final String[] errorMessage = {null};
            long time = 0L;
            final boolean[] timeout = {true};
            final long[] minMemory = {Long.MAX_VALUE};
            final long[] maxMemory = {0L};

            // 执行 java 的命令
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            // 记录内存使用情况
            // 获取执行 java 命令过程中最小内存使用
            StatsCmd statsCmdGetMinMemory = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> getMinMemory = new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    Long memoryUsage = statistics.getMemoryStats().getUsage();
                    minMemory[0] = Math.min(minMemory[0], Optional.ofNullable(memoryUsage).orElse(0L));
                }

                @Override
                public void close() throws IOException {
                }

                @Override
                public void onStart(Closeable closeable) {
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onComplete() {
                }
            };
            // 获取执行 java 命令过程中最大内存使用
            StatsCmd statsCmdGetMaxMemory = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> getMaxMemory = new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    Long memoryUsage = statistics.getMemoryStats().getUsage();
                    maxMemory[0] = Math.max(maxMemory[0], Optional.ofNullable(memoryUsage).orElse(0L));
                }

                @Override
                public void close() throws IOException {
                }

                @Override
                public void onStart(Closeable closeable) {
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onComplete() {
                }
            };
            statsCmdGetMinMemory.exec(getMinMemory);
            statsCmdGetMaxMemory.exec(getMaxMemory);

            // 执行命令并处理输入输出
            StopWatch stopWatch = new StopWatch();
            try {
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            } finally {
                statsCmdGetMinMemory.close();
                statsCmdGetMaxMemory.close();
            }

            // 填充返回参数
            long memory = maxMemory[0] - minMemory[0];
            executeMessage.setMemory(memory);
            executeMessage.setTime(time);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setMessage(message[0]);
            executeMessageList.add(executeMessage);
        }

        return executeMessageList;
    }
}