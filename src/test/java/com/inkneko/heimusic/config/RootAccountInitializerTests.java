package com.inkneko.heimusic.config;

import com.inkneko.heimusic.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * root 账户自动初始化逻辑单元测试（不依赖 Spring 上下文与基础设施）。
 */
class RootAccountInitializerTests {

    @Test
    void createsRootWithPlaceholderEmailWhenAbsent() {
        AuthService authService = mock(AuthService.class);
        when(authService.isRootAccountExists()).thenReturn(false);

        new RootAccountInitializer(authService).run(null);

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(authService).createRootAccount(eq("admin@heimusic.local"), passwordCaptor.capture());
        String password = passwordCaptor.getValue();
        assertEquals(16, password.length(), "初始密码应为 16 位");
        assertTrue(password.matches("[ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789]+"),
                "初始密码应仅包含去除易混淆字符的字母数字，实际: " + password);
    }

    @Test
    void generatesDifferentPasswordsAcrossRuns() {
        AuthService authService = mock(AuthService.class);
        when(authService.isRootAccountExists()).thenReturn(false);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        new RootAccountInitializer(authService).run(null);
        new RootAccountInitializer(authService).run(null);

        verify(authService, times(2)).createRootAccount(anyString(), passwordCaptor.capture());
        assertNotEquals(passwordCaptor.getAllValues().get(0), passwordCaptor.getAllValues().get(1),
                "两次生成的初始密码不应相同");
    }

    @Test
    void skipsCreationWhenRootExists() {
        AuthService authService = mock(AuthService.class);
        when(authService.isRootAccountExists()).thenReturn(true);

        new RootAccountInitializer(authService).run(null);

        verify(authService, never()).createRootAccount(anyString(), anyString());
    }
}
