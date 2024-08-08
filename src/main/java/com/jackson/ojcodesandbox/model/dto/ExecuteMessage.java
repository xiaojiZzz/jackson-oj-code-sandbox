package com.jackson.ojcodesandbox.model.dto;

import lombok.Data;

/**
 * 进程执行信息
 *
 * @author jackson
 */
@Data
public class ExecuteMessage {

    /**
     * 退出码
     */
    private Integer exitValue;

    /**
     * 执行信息
     */
    private String message;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行时间
     */
    private Long time;

    /**
     * 占用内存
     */
    private Long memory;
}
