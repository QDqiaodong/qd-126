package com.example.devicemanagement.config;

import com.example.devicemanagement.dto.response.ApiResponse;
import com.example.devicemanagement.exception.InterpreterConflictException;
import com.example.devicemanagement.exception.QuietPeriodConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 业务参数异常统一返回 400，并携带具体校验提示（如规格模板必填项缺失）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 静音时段冲突业务码：前端据此弹出静音原因并拦住提交 */
    public static final int CODE_QUIET_PERIOD_CONFLICT = 460;

    /** 译员时段撞车业务码：前端据此弹出已约接待室并拦住提交 */
    public static final int CODE_INTERPRETER_CONFLICT = 461;

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(QuietPeriodConflictException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleQuietPeriodConflict(QuietPeriodConflictException e) {
        return ApiResponse.error(CODE_QUIET_PERIOD_CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InterpreterConflictException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInterpreterConflict(InterpreterConflictException e) {
        return ApiResponse.error(CODE_INTERPRETER_CONFLICT, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntime(RuntimeException e) {
        return ApiResponse.error(500, e.getMessage() != null ? e.getMessage() : "服务器内部错误");
    }
}
