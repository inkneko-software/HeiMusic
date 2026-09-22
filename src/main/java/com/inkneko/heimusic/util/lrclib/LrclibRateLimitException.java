package com.inkneko.heimusic.util.lrclib;

/**
 * LRCLIB 限流异常（HTTP 429 Too Many Requests）
 * <p>
 * LRCLIB 超限返回 429 + Retry-After 头（秒），客户端必须遵守否则可能被临时封禁，
 * 该异常携带建议等待秒数供调用方退避重试
 */
public class LrclibRateLimitException extends RuntimeException {

    /**
     * 响应未携带 Retry-After 时的默认等待秒数
     */
    private static final int DEFAULT_RETRY_AFTER_SECONDS = 5;

    private final int retryAfterSeconds;

    public LrclibRateLimitException(String retryAfterHeader) {
        super("LRCLIB rate limit exceeded");
        this.retryAfterSeconds = parseRetryAfter(retryAfterHeader);
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /**
     * 解析 Retry-After 头，缺失或非数值时取默认值
     */
    private static int parseRetryAfter(String header) {
        if (header == null || header.isBlank()) {
            return DEFAULT_RETRY_AFTER_SECONDS;
        }
        try {
            return Math.max(1, Integer.parseInt(header.trim()));
        } catch (NumberFormatException e) {
            return DEFAULT_RETRY_AFTER_SECONDS;
        }
    }
}
