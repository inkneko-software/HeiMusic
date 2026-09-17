package com.inkneko.heimusic.errorcode;


public enum UserServiceErrorCode implements ErrorCode{
    USER_NOT_EXISTS(2000, "用户不存在"),
    AVATAR_FILE_EMPTY(2001, "头像文件不能为空"),
    AVATAR_FILE_TOO_LARGE(2002, "头像文件过大，最大10MB"),
    AVATAR_FORMAT_UNSUPPORTED(2003, "头像格式不支持，仅支持 JPEG/PNG/WebP/GIF"),
    AVATAR_UPDATE_OVER_LIMIT(2004, "头像修改过于频繁，请稍后再试"),
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
