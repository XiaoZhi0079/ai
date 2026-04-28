package com.example.ai.control;

import com.example.ai.pojo.LeeResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public LeeResult<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return LeeResult.fail(ex.getMessage());
    }
}
