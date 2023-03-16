package com.inkneko.heimusic.model.vo;

import com.inkneko.heimusic.errorcode.ErrorCode;
import com.inkneko.heimusic.exception.ServiceException;
import lombok.Data;

@Data
public class BaseVo {
    private Integer code;
    private String  message;

    public BaseVo(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public BaseVo(ErrorCode e){
        this.code = e.getCode();
        this.message = e.getMessage();
    }


    public BaseVo(ServiceException e){
        this.code = e.getErrorCode().getCode();
        this.message = e.getErrorCode().getMessage();
    }

}