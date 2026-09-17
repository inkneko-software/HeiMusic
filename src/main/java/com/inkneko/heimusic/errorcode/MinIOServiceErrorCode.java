package com.inkneko.heimusic.errorcode;

public enum MinIOServiceErrorCode implements ErrorCode{
    DELETE_FAILED(4000, "删除失败"),
    UPLOAD_FAILED(4001, "上传失败"),
    ;

    private final int code;
    private final String message;

    MinIOServiceErrorCode(int code, String message) {
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
