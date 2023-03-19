package com.inkneko.heimusic.errorcode;


public enum UserServiceErrorCode implements ErrorCode{
    EMAIL_REGISTERED(2000, "邮箱已注册"),
    EMAIL_CODE_OVER_LIMIT(2001, "邮箱验证码发送过于频繁"),
    EMAIL_CODE_INCORRENT(2002, "邮箱验证码不正确"),
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
