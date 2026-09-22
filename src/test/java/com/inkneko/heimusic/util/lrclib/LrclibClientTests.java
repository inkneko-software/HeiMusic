package com.inkneko.heimusic.util.lrclib;

import com.inkneko.heimusic.config.HeiMusicConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * LrclibClient 单元测试：MockRestServiceServer 绑定 RestClient.Builder，无真实网络请求。
 * 覆盖正常响应解析（含未知字段容错）、404 转 null、429 转 LrclibRateLimitException（读取 Retry-After）、search 空数组。
 */
class LrclibClientTests {

    private static final String TRACK_JSON = """
            {"id":3396226,"name":"I Want to Live","trackName":"I Want to Live",
             "artistName":"Borislav Slavov","albumName":"Baldur's Gate 3 (Original Game Soundtrack)",
             "duration":233,"instrumental":false,
             "plainLyrics":"I feel your breath upon my neck",
             "syncedLyrics":"[00:17.12] I feel your breath upon my neck",
             "lyricsfile":"version: '1.0'"}
            """;

    MockRestServiceServer server;
    LrclibClient client;

    @BeforeEach
    void setUp() {
        //走生产构造器实例化（覆盖构造逻辑），再于同包内注入绑定 mock server 的 RestClient
        HeiMusicConfig config = new HeiMusicConfig();
        config.setLrclibBaseUrl("https://lrclib.net");
        client = new LrclibClient(config, RestClient.builder().baseUrl("https://lrclib.net"));

        RestClient.Builder mockBuilder = RestClient.builder().baseUrl("https://lrclib.net");
        server = MockRestServiceServer.bindTo(mockBuilder).build();
        client.restClient = mockBuilder.build();
    }

    @Test
    void getBySignatureParsesTrackAndToleratesUnknownFields() {
        server.expect(requestTo(allOf(
                        containsString("/api/get?"),
                        containsString("track_name=I%20Want%20to%20Live"),
                        containsString("artist_name=Borislav%20Slavov"),
                        containsString("album_name="),
                        containsString("duration=233"),
                        not(containsString("lyricsfile")))))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(TRACK_JSON, MediaType.APPLICATION_JSON));

        LrclibTrack track = client.getBySignature("I Want to Live", "Borislav Slavov",
                "Baldur's Gate 3 (Original Game Soundtrack)", 233);

        assertNotNull(track);
        assertEquals(3396226, track.getId());
        assertEquals("I Want to Live", track.getTrackName());
        assertEquals(false, track.getInstrumental());
        assertEquals("[00:17.12] I feel your breath upon my neck", track.getSyncedLyrics());
        server.verify();
    }

    @Test
    void getBySignatureOmitsOptionalParamsWhenAbsent() {
        server.expect(requestTo(allOf(
                        containsString("/api/get?"),
                        containsString("track_name="),
                        not(containsString("album_name")),
                        not(containsString("duration")))))
                .andRespond(withSuccess(TRACK_JSON, MediaType.APPLICATION_JSON));

        LrclibTrack track = client.getBySignature("I Want to Live", "Borislav Slavov", null, null);

        assertNotNull(track);
        server.verify();
    }

    @Test
    void getBySignatureReturnsNullOn404() {
        server.expect(requestTo(containsString("/api/get")))
                .andRespond(withResourceNotFound()
                        .body("{\"code\":404,\"name\":\"TrackNotFound\",\"message\":\"Failed to find specified track\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        LrclibTrack track = client.getBySignature("not-exist", "not-exist", null, null);

        assertNull(track);
        server.verify();
    }

    @Test
    void rateLimitThrowsWithRetryAfterSeconds() {
        server.expect(requestTo(containsString("/api/get")))
                .andRespond(withTooManyRequests()
                        .body("{\"code\":429,\"name\":\"TooManyRequests\",\"message\":\"Rate limit exceeded\"}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.RETRY_AFTER, "7"));

        LrclibRateLimitException exception = assertThrows(LrclibRateLimitException.class,
                () -> client.getBySignature("any", "any", null, null));
        assertEquals(7, exception.getRetryAfterSeconds());
        server.verify();
    }

    @Test
    void searchReturnsList() {
        server.expect(requestTo(containsString("/api/search?q=")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<LrclibTrack> tracks = client.search("zzz-not-exist");

        assertNotNull(tracks);
        assertTrue(tracks.isEmpty());
        server.verify();
    }
}
