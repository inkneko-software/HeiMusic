package com.inkneko.heimusic.exception;

import com.inkneko.heimusic.errorcode.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务层异常
 */
public class ServiceException extends RuntimeException{
    private final ErrorCode errorCode;

    private final Integer code;
    private final String message;

    public ServiceException(ErrorCode errorCode){
        this.message = errorCode.getMessage();
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
    }

    public ServiceException(Integer code, String message){
        this.code = code;
        this.message = message;
        this.errorCode = null;
    }

    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
