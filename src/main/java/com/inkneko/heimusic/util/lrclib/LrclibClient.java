package com.inkneko.heimusic.util.lrclib;

import com.inkneko.heimusic.config.HeiMusicConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * LRCLIB API 客户端（https://lrclib.net/docs）
 * <p>
 * LRCLIB 为公开歌词库，无需 API Key，但要求以 User-Agent 标识客户端；
 * 调用方须自行节流（官方建议串行请求、批量间隔 200~500ms），
 * 429 限流与 503 过载时本客户端抛出携带 Retry-After 的 LrclibRateLimitException，由调用方退避
 */
@Component
public class LrclibClient {

    /**
     * 包私有字段，供同包测试注入绑定 MockRestServiceServer 的 RestClient；
     * 生产构造下为带 baseUrl/User-Agent/超时配置的实例
     */
    RestClient restClient;

    public LrclibClient(HeiMusicConfig config, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(config.getLrclibConnectTimeoutMillis());
        requestFactory.setReadTimeout(config.getLrclibReadTimeoutMillis());
        this.restClient = builder
                .baseUrl(config.getLrclibBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, config.getLrclibUserAgent())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 按曲目签名查询歌词（GET /api/get），返回单条最优匹配
     *
     * @param trackName  曲目名，必填
     * @param artistName 艺术家名，必填
     * @param albumName  专辑名，可空，推荐提供以提高匹配精度
     * @param duration   时长秒数，可空，须在 1~3600 之间；服务端仅返回时长差 ±2 秒内的记录
     * @return 最优匹配的歌词记录；LRCLIB 无此曲目时返回 null（404 为正常结局，其后台会补录缺失曲目）
     */
    public LrclibTrack getBySignature(String trackName, String artistName, String albumName, Integer duration) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/get")
                                .queryParam("track_name", trackName)
                                .queryParam("artist_name", artistName);
                        if (albumName != null) {
                            uriBuilder.queryParam("album_name", albumName);
                        }
                        if (duration != null) {
                            uriBuilder.queryParam("duration", duration);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(LrclibTrack.class);
        } catch (HttpStatusCodeException e) {
            return handleHttpError(e);
        }
    }

    /**
     * 关键词搜索歌词记录（GET /api/search），LRCLIB 最多返回 20 条且无分页
     *
     * @param q 关键词，匹配曲目名/艺术家名/专辑名任意字段
     * @return 命中的歌词记录列表，无命中时为空列表
     */
    public List<LrclibTrack> search(String q) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/search").queryParam("q", q).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<LrclibTrack>>() {
                    });
        } catch (HttpStatusCodeException e) {
            return handleHttpError(e);
        }
    }

    /**
     * 统一转换 LRCLIB 的状态码语义：404 表示曲目不存在（返回 null 而非抛错）；
     * 429 限流与 503 过载（ServerOverloaded，文档未记载、实测出现，均为"稍后重试"的瞬时态）
     * 统一转为 LrclibRateLimitException（携带 Retry-After，缺失时由异常侧取默认值）；其余原样抛出
     */
    private <T> T handleHttpError(HttpStatusCodeException e) {
        int status = e.getStatusCode().value();
        if (status == 404) {
            return null;
        }
        if (status == 429 || status == 503) {
            HttpHeaders headers = e.getResponseHeaders();
            throw new LrclibRateLimitException(headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER));
        }
        throw e;
    }
}
