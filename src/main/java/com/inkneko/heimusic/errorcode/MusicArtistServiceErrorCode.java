package com.inkneko.heimusic.errorcode;

public enum MusicArtistServiceErrorCode implements ErrorCode{
    MUSIC_ARTIST_NAME_DUPLICATED(3000, "艺术家名称重复"),
    MUSIC_ARTIST_NOT_FOUND(3001, "未查询到相应艺术家");


    private final int code;
    private final String message;

    MusicArtistServiceErrorCode(int code, String message) {
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
