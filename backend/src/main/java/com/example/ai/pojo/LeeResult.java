package com.example.ai.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeeResult<T> {

    /**
     * 业务状态码
     * 0：成功
     * 非 0：失败
     */
    private int code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    // ================= 成功 =================

    public static <T> LeeResult<T> ok() {
        return new LeeResult<>(0, "success", null);
    }

    public static <T> LeeResult<T> ok(T data) {
        return new LeeResult<>(0, "success", data);
    }

    public static <T> LeeResult<T> ok(String message, T data) {
        return new LeeResult<>(0, message, data);
    }

    // ================= 失败 =================

    public static <T> LeeResult<T> fail(String message) {
        return new LeeResult<>(-1, message, null);
    }

    public static <T> LeeResult<T> fail(int code, String message) {
        return new LeeResult<>(code, message, null);
    }
}
