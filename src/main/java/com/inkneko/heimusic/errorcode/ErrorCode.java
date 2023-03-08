package com.inkneko.heimusic.errorcode;

/**
 * 业务逻辑异常
 * @param code 错误码，用于定位产生异常的业务逻辑
 * @param message 用户端友好的错误信息
 */
public interface ErrorCode {
    public int getCode();
    public String getMessage();
}
