package com.inkneko.heimusic.errorcode;

public enum AuthServiceErrorCode implements ErrorCode {
    AUTH_EMAIL_CODE_OVER_LIMIT(1001, "验证码发送过于频繁"),
    AUTH_EMAIL_REGISTERED(1002, "邮箱已注册");

    private final int code;
    private final String message;

    AuthServiceErrorCode(int code, String message) {
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
