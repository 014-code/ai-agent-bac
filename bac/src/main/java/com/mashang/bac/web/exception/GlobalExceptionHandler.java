package com.mashang.bac.web.exception;

import com.mashang.bac.web.enums.ErrorCode;
import com.mashang.bac.web.utils.ResultTUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResultTUtil<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return new ResultTUtil<>().error(String.valueOf(e.getCode()), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResultTUtil<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return new ResultTUtil<>().errorCode(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
