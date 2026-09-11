package com.inkneko.heimusic.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CORS 白名单回归测试。
 * 复现前端 dev server 通过局域网 IP 访问（页面 Origin 为 http://192.168.x.x:8888）、
 * /api 由 dev server 代理转发到后端的场景：请求到达后端时 Host 与 Origin 不同，
 * Spring 会按跨域请求校验 Origin 白名单。
 */
class WebConfigCorsTests {

    private static CorsConfiguration apiCors;

    @BeforeAll
    static void setUp() {
        ExposedCorsRegistry registry = new ExposedCorsRegistry();
        new WebConfig(null).addCorsMappings(registry);
        apiCors = registry.expose().get("/api/**");
    }

    @Test
    void lanIpDevServerOriginIsAllowed() {
        assertNotNull(apiCors, "未注册 /api/** 的 CORS 映射");
        assertEquals("http://192.168.28.87:8888", apiCors.checkOrigin("http://192.168.28.87:8888"));
        assertEquals("http://10.20.30.40:8888", apiCors.checkOrigin("http://10.20.30.40:8888"));
        assertEquals("http://127.0.0.1:8888", apiCors.checkOrigin("http://127.0.0.1:8888"));
    }

    @Test
    void existingOriginsRemainAllowed() {
        List<String> origins = List.of(
                "http://localhost:8888",
                "http://localhost:3000",
                "http://localhost",
                "https://localhost",
                "capacitor://localhost"
        );
        for (String origin : origins) {
            assertEquals(origin, apiCors.checkOrigin(origin), "白名单中原有的 origin 被移除: " + origin);
        }
    }

    @Test
    void unrelatedOriginIsRejected() {
        assertNull(apiCors.checkOrigin("https://evil.example.com"));
        assertNull(apiCors.checkOrigin("http://192.168.28.87:9999"), "非 dev server 端口不应放行");
    }

    @Test
    void proxiedLoginRequestIsAccepted() throws Exception {
        DefaultCorsProcessor processor = new DefaultCorsProcessor();

        // 复现登录请求：同源 POST 携带 Origin，代理转发到后端后 Host(localhost:8081) 与 Origin 不同
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServerName("localhost");
        request.setServerPort(8081);
        request.addHeader(HttpHeaders.ORIGIN, "http://192.168.28.87:8888");
        assertTrue(processor.processRequest(apiCors, request, new MockHttpServletResponse()),
                "局域网 IP 访问 dev server 时的登录请求不应再被 403 拒绝");
    }

    @Test
    void lanOriginPreflightWithAuthHeadersIsAccepted() throws Exception {
        DefaultCorsProcessor processor = new DefaultCorsProcessor();

        MockHttpServletRequest preflight = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
        preflight.setServerName("localhost");
        preflight.setServerPort(8081);
        preflight.addHeader(HttpHeaders.ORIGIN, "http://192.168.28.87:8888");
        preflight.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        preflight.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                "content-type, x-heimusic-auth-userid, x-heimusic-auth-sessionid");

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertTrue(processor.processRequest(apiCors, preflight, response),
                "携带自定义认证头的预检请求不应被拒绝");
    }

    private static class ExposedCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> expose() {
            return getCorsConfigurations();
        }
    }
}
