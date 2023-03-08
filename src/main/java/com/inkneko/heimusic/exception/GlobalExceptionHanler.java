package com.inkneko.heimusic.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHanler {

    @ExceptionHandler(ServiceException.class)
    public String serviceExceptionHandler(){
        return "";
    }


}
