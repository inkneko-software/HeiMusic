package com.inkneko.heimusic.exception;

import com.inkneko.heimusic.errorcode.ErrorCode;
import lombok.Data;

/**
 * 业务层异常
 */
public class ServiceException extends RuntimeException{
    @Deprecated
    private ErrorCode errorCode;

    private Integer code;
    private String message;

    @Deprecated
    public ServiceException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.code = 400;
        this.message = errorCode.getMessage();
        this.errorCode = errorCode;
    }

    @Deprecated
    public ErrorCode getErrorCode() {
        return this.errorCode;
    }


    public ServiceException(Integer code, String message){
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
