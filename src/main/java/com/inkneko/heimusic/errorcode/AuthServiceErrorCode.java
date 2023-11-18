package com.inkneko.heimusic.errorcode;

public enum AuthServiceErrorCode implements ErrorCode {
    EMAIL_CODE_OVER_LIMIT(1000, "验证码发送过于频繁"),
    USER_NOT_EXISTS(1001, "用户不存在"),
    EMAIL_CODE_INCORRECT(1002, "邮箱验证码不正确"),
    PASSWORD_CODE_NOT_PROVIDED(1003, "请提供密码或认证码"),
    PASSWORD_INCORRECT(1004, "密码不正确"),
    EMAIL_REGISTERED(1005, "邮箱已注册"),
    ROOT_ACCOUNT_EXISTS(1006, "管理账户已存在")
    ;

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
