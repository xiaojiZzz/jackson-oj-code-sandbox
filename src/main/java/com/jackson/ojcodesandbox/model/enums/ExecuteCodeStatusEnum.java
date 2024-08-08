package com.jackson.ojcodesandbox.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 执行代码状态枚举类
 *
 * @author jackson
 */
public enum ExecuteCodeStatusEnum {

    /**
     * 执行代码状态枚举
     */
    SUCCESS("成功", 0),
    COMPILE_FAILED("编译失败", 1),
    RUN_FAILED("运行失败", 2),
    NO_AUTH("无权限", 3);

    private final String msg;

    private final Integer value;

    ExecuteCodeStatusEnum(String msg, Integer value) {
        this.msg = msg;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static ExecuteCodeStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (ExecuteCodeStatusEnum anEnum : ExecuteCodeStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public String getMsg() {
        return msg;
    }

    public Integer getValue() {
        return value;
    }
}
