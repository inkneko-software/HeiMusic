package com.inkneko.heimusic.errorcode;


public enum UserServiceErrorCode implements ErrorCode{
    USER_NOT_EXISTS(2000, "用户不存在"),
    ;

    private final int code;
    private final String message;

    UserServiceErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
