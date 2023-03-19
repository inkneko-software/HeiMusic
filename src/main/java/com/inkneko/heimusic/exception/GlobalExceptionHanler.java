package com.inkneko.heimusic.exception;

import com.inkneko.heimusic.model.dto.ResponseDto;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHanler {

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public ResponseDto serviceExceptionHandler(ServiceException e){
        return new ResponseDto(e);
    }
}
