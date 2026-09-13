package com.example.devicemanagement.exception;

/**
 * 活动占用时段与接待室静音时段重叠：拦截提交并携带静音原因，
 * 前端按错误码识别后弹出原因提示。
 */
public class QuietPeriodConflictException extends RuntimeException {

    public QuietPeriodConflictException(String message) {
        super(message);
    }
}
