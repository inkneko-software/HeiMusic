package com.inkneko.heimusic.exception;

import com.inkneko.heimusic.errorcode.ErrorCode;
import lombok.Data;

/**
 * 业务层异常
 */
public class ServiceException extends Exception{
    private final ErrorCode errorCode;

    public ServiceException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }
}
