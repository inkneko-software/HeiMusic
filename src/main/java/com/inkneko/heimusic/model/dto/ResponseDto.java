package com.inkneko.heimusic.model.dto;

import com.inkneko.heimusic.errorcode.ErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import lombok.Data;

@Data
public class ResponseDto {
    private Integer code;
    private String  message;
    private Object data;

    public ResponseDto(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public ResponseDto(Integer code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public ResponseDto(ErrorCode e){
        this.code = e.getCode();
        this.message = e.getMessage();
    }


    public ResponseDto(ServiceException e){
        this.code = e.getErrorCode().getCode();
        this.message = e.getErrorCode().getMessage();
    }

}