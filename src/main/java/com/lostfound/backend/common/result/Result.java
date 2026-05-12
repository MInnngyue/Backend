// 创建文件：common/result/Result.java

package com.lostfound.backend.common.result;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;    // 状态码
    private String message;  // 提示信息
    private T data;          // 响应数据

    // 成功（无数据）
    public static <T> Result<T> success() {
        return build(200, "操作成功", null);
    }

    // 成功（有数据）
    public static <T> Result<T> success(T data) {
        return build(200, "操作成功", data);
    }

    // 成功（自定义消息）
    public static <T> Result<T> success(String message, T data) {
        return build(200, message, data);
    }

    // 失败
    public static <T> Result<T> fail(String message) {
        return build(500, message, null);
    }

    // 失败（自定义状态码）
    public static <T> Result<T> fail(Integer code, String message) {
        return build(code, message, null);
    }

    private static <T> Result<T> build(Integer code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}