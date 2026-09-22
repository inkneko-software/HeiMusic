package com.inkneko.heimusic.errorcode;


public enum LyricServiceErrorCode implements ErrorCode {
    LYRIC_NOT_FOUND(5000, "未查询到相应歌词"),
    LYRIC_MUSIC_NOT_FOUND(5001, "指定音乐不存在"),
    LYRIC_ALREADY_EXISTS(5002, "该音乐已存在相同语言的歌词"),
    LYRIC_MUSIC_MISMATCH(5003, "歌词与音乐不匹配"),
    LYRIC_UPDATE_EMPTY(5004, "歌词内容与格式不能同时为空"),
    LYRIC_FETCH_ALREADY_HAS_LYRIC(5005, "该音乐已存在歌词，拉取功能仅服务无歌词的音乐"),
    LYRIC_SCAN_THROTTLED(5006, "扫描请求提交过于频繁，请稍后再试"),
    ;

    private final int code;
    private final String message;

    LyricServiceErrorCode(int code, String message) {
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
