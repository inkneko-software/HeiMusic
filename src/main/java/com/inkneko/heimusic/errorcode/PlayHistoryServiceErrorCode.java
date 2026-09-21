package com.inkneko.heimusic.errorcode;


public enum PlayHistoryServiceErrorCode implements ErrorCode {
    HISTORY_MUSIC_NOT_FOUND(6000, "指定音乐不存在"),
    HISTORY_NOT_FOUND(6001, "未查询到播放历史"),
    ;

    private final int code;
    private final String message;

    PlayHistoryServiceErrorCode(int code, String message) {
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
