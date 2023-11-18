package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.errorcode.ErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import lombok.Data;

@Data
public class Response<T> {
    private Integer code;
    private String  message;
    private T data;

    public Response() {
    }

    public Response(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Response(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Response(ErrorCode e){
        this.code = e.getCode();
        this.message = e.getMessage();
    }


    public Response(ServiceException e){
        this.code = e.getErrorCode().getCode();
        this.message = e.getErrorCode().getMessage();
    }

}