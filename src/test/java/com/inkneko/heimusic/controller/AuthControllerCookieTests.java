package com.inkneko.heimusic.controller;

import com.inkneko.heimusic.config.HeiMusicConfig;
import com.inkneko.heimusic.model.dto.LoginDto;
import com.inkneko.heimusic.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 登录 cookie 写入行为回归测试。
 * 浏览器通过局域网 IP + 前端开发代理访问时（页面主机为 192.168.x.x 等），
 * Set-Cookie 若携带固定的 Domain=localhost 会被浏览器以 Domain 不匹配为由拒绝，
 * 因此未配置 heimusic.domain 时不得写 Domain 属性（host-only cookie）。
 */
class AuthControllerCookieTests {

    @Test
    void loginWritesHostOnlyCookiesWhenDomainNotConfigured() {
        AuthController controller = controllerWithDomain("");

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.login(new LoginDto("a@b.c", "pw", null), response);

        Cookie[] cookies = response.getCookies();
        assertEquals(2, cookies.length);
        for (Cookie cookie : cookies) {
            assertNull(cookie.getDomain(), "未配置 heimusic.domain 时不应写 Domain 属性");
            assertTrue(cookie.isHttpOnly());
            assertEquals("/", cookie.getPath());
            assertEquals(60 * 60 * 24 * 180, cookie.getMaxAge());
        }
    }

    @Test
    void loginWritesDomainScopedCookiesWhenDomainConfigured() {
        AuthController controller = controllerWithDomain("music.example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.login(new LoginDto("a@b.c", "pw", null), response);

        Cookie[] cookies = response.getCookies();
        assertEquals(2, cookies.length);
        for (Cookie cookie : cookies) {
            assertEquals("music.example.com", cookie.getDomain());
        }
    }

    private AuthController controllerWithDomain(String domain) {
        AuthService authService = mock(AuthService.class);
        when(authService.login("a@b.c", "pw")).thenReturn(Map.entry(7, "session-id"));
        HeiMusicConfig config = mock(HeiMusicConfig.class);
        when(config.getDomain()).thenReturn(domain);
        return new AuthController(authService, config);
    }
}
