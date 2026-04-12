package com.blink.framework.common.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * JWT相关数据类单元测试
 * <p>
 * 测试覆盖：
 * 1. ValidationResult 验证结果
 * 2. TokenPair Token对
 *
 * @author binblink
 */
@DisplayName("JWT相关数据类测试")
class JwtDataTests {

    // ==================== ValidationResult 测试 ====================

    @Nested
    @DisplayName("ValidationResult 验证结果测试")
    class ValidationResultTests {

        @Test
        @DisplayName("应该成功创建ValidationResult实例")
        void shouldCreateValidationResultInstance() {
            // when
            ValidationResult result = new ValidationResult();

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("应该使用Builder创建ValidationResult")
        void shouldCreateWithBuilder() {
            // when
            ValidationResult result = ValidationResult.builder()
                    .valid(true)
                    .message("验证成功")
                    .reason("OK")
                    .build();

            // then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessage()).isEqualTo("验证成功");
            assertThat(result.getReason()).isEqualTo("OK");
        }

        @Test
        @DisplayName("应该使用全参构造创建ValidationResult")
        void shouldCreateWithAllArgsConstructor() {
            // when
            ValidationResult result = new ValidationResult(false, "验证失败", "EXPIRED");

            // then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).isEqualTo("验证失败");
            assertThat(result.getReason()).isEqualTo("EXPIRED");
        }

        @Test
        @DisplayName("success静态方法应该返回成功的验证结果")
        void shouldReturnSuccessResult() {
            // when
            ValidationResult result = ValidationResult.success("Token有效");

            // then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Token有效");
            assertThat(result.getReason()).isNull();
        }

        @Test
        @DisplayName("expired静态方法应该返回过期的验证结果")
        void shouldReturnExpiredResult() {
            // when
            ValidationResult result = ValidationResult.expired("Token已过期");

            // then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Token已过期");
            assertThat(result.getReason()).isEqualTo("EXPIRED");
        }

        @Test
        @DisplayName("invalid静态方法应该返回无效的验证结果")
        void shouldReturnInvalidResult() {
            // when
            ValidationResult result = ValidationResult.invalid("Token格式错误");

            // then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Token格式错误");
            assertThat(result.getReason()).isEqualTo("INVALID");
        }

        @Test
        @DisplayName("应该能够设置和获取所有属性")
        void shouldSetAndGetProperties() {
            // given
            ValidationResult result = new ValidationResult();

            // when
            result.setValid(true);
            result.setMessage("测试消息");
            result.setReason("测试原因");

            // then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessage()).isEqualTo("测试消息");
            assertThat(result.getReason()).isEqualTo("测试原因");
        }
    }

    // ==================== TokenPair 测试 ====================

    @Nested
    @DisplayName("TokenPair Token对测试")
    class TokenPairTests {

        @Test
        @DisplayName("应该成功创建TokenPair实例")
        void shouldCreateTokenPairInstance() {
            // when
            TokenPair tokenPair = new TokenPair();

            // then
            assertThat(tokenPair).isNotNull();
        }

        @Test
        @DisplayName("应该使用Builder创建TokenPair")
        void shouldCreateWithBuilder() {
            // given
            List<String> roles = Arrays.asList("admin", "user");

            // when
            TokenPair tokenPair = TokenPair.builder()
                    .accessToken("access-token-123")
                    .refreshToken("refresh-token-456")
                    .tokenType("Bearer")
                    .expiresIn(7200L)
                    .username("admin")
                    .roles(roles)
                    .build();

            // then
            assertThat(tokenPair.getAccessToken()).isEqualTo("access-token-123");
            assertThat(tokenPair.getRefreshToken()).isEqualTo("refresh-token-456");
            assertThat(tokenPair.getTokenType()).isEqualTo("Bearer");
            assertThat(tokenPair.getExpiresIn()).isEqualTo(7200L);
            assertThat(tokenPair.getUsername()).isEqualTo("admin");
            assertThat(tokenPair.getRoles()).containsExactly("admin", "user");
        }

        @Test
        @DisplayName("应该使用全参构造创建TokenPair")
        void shouldCreateWithAllArgsConstructor() {
            // given
            List<String> roles = Collections.singletonList("user");

            // when
            TokenPair tokenPair = new TokenPair(
                    "access",
                    "refresh",
                    "Bearer",
                    3600L,
                    "testuser",
                    roles
            );

            // then
            assertThat(tokenPair.getAccessToken()).isEqualTo("access");
            assertThat(tokenPair.getRefreshToken()).isEqualTo("refresh");
            assertThat(tokenPair.getTokenType()).isEqualTo("Bearer");
            assertThat(tokenPair.getExpiresIn()).isEqualTo(3600L);
            assertThat(tokenPair.getUsername()).isEqualTo("testuser");
            assertThat(tokenPair.getRoles()).containsExactly("user");
        }

        @Test
        @DisplayName("应该能够设置和获取所有属性")
        void shouldSetAndGetProperties() {
            // given
            TokenPair tokenPair = new TokenPair();
            List<String> roles = Arrays.asList("role1", "role2");

            // when
            tokenPair.setAccessToken("new-access");
            tokenPair.setRefreshToken("new-refresh");
            tokenPair.setTokenType("Bearer");
            tokenPair.setExpiresIn(1800L);
            tokenPair.setUsername("newuser");
            tokenPair.setRoles(roles);

            // then
            assertThat(tokenPair.getAccessToken()).isEqualTo("new-access");
            assertThat(tokenPair.getRefreshToken()).isEqualTo("new-refresh");
            assertThat(tokenPair.getTokenType()).isEqualTo("Bearer");
            assertThat(tokenPair.getExpiresIn()).isEqualTo(1800L);
            assertThat(tokenPair.getUsername()).isEqualTo("newuser");
            assertThat(tokenPair.getRoles()).containsExactly("role1", "role2");
        }

        @Test
        @DisplayName("空roles列表应该被允许")
        void shouldAllowEmptyRoles() {
            // when
            TokenPair tokenPair = TokenPair.builder()
                    .accessToken("token")
                    .refreshToken("refresh")
                    .roles(Collections.emptyList())
                    .build();

            // then
            assertThat(tokenPair.getRoles()).isEmpty();
        }

        @Test
        @DisplayName("null roles应该被允许")
        void shouldAllowNullRoles() {
            // when
            TokenPair tokenPair = new TokenPair();
            tokenPair.setAccessToken("token");

            // then
            assertThat(tokenPair.getRoles()).isNull();
        }

        @Test
        @DisplayName("expiresIn默认应该为0")
        void shouldHaveDefaultZeroExpiresIn() {
            // when
            TokenPair tokenPair = new TokenPair();

            // then
            assertThat(tokenPair.getExpiresIn()).isEqualTo(0L);
        }
    }
}
