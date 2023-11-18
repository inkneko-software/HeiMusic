package com.inkneko.heimusic.errorcode;

/**
 * 业务逻辑异常
 * <p>
 * 各服务的错误码起始：
 * <p>
 * AuthServiceErrorCode        1000
 * UserServiceErrorCode        2000
 * MusicArtistServiceErrorCode 3000
 * MinIOServiceErrorCode       4000
 */
public interface ErrorCode {
    /**
     *
     * @return code 错误码，用于定位产生异常的业务逻辑
     */
    public int getCode();

    /**
     *
     * @return 用户端友好的错误信息
     */
    public String getMessage();
}
