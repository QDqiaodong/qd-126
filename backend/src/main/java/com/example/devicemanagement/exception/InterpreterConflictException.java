package com.example.devicemanagement.exception;

/**
 * 同一译员时段撞车：该译员在重叠时段已被其他接待室预约，
 * 拦截提交并携带已约接待室与时段，前端按错误码识别后弹出提示。
 */
public class InterpreterConflictException extends RuntimeException {

    public InterpreterConflictException(String message) {
        super(message);
    }
}
