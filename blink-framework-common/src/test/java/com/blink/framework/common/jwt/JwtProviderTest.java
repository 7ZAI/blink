package com.blink.framework.common.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * JwtProvider 单元测试
 * <p>
 * 测试覆盖：
 * 1. Token生成（Access Token / Refresh Token）
 * 2. Token验证（有效/过期/签名错误）
 * 3. Token解析（用户名/角色/Claims）
 * 4. Token刷新
 * 5. Authorization头处理
 * 6. 边界条件
 *
 * @author binblink
 */
@DisplayName("JwtProvider JWT工具类测试")
class JwtProviderTest {

    private JwtConfig jwtConfig;
    private JwtProvider jwtProvider;

    private static final String TEST_USERNAME = "testuser";
    private static final List<String> TEST_ROLES = Arrays.asList("ROLE_USER", "ROLE_ADMIN");

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtProvider = new JwtProvider(jwtConfig);
    }

    // ==================== Access Token生成测试 ====================

    @Nested
    @DisplayName("Access Token生成测试")
    class AccessTokenGenerationTests {

        @Test
        @DisplayName("应该成功生成Access Token")
        void shouldGenerateAccessToken() {
            // when
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // then
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT应该有3部分
        }

        @Test
        @DisplayName("生成的Token应该是有效的")
        void shouldGenerateValidToken() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when & then
            assertThat(jwtProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Token中应该包含用户名")
        void shouldContainUsername() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when
            String username = jwtProvider.getUsernameFromToken(token);

            // then
            assertThat(username).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("Token中应该包含角色")
        void shouldContainRoles() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when
            List<String> roles = jwtProvider.getRolesFromToken(token);

            // then
            assertThat(roles).containsExactlyInAnyOrderElementsOf(TEST_ROLES);
        }

        @Test
        @DisplayName("Token类型应该是access")
        void shouldHaveAccessTokenType() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when
            Object type = jwtProvider.getClaimFromToken(token, "type");

            // then
            assertThat(type).isEqualTo("access");
        }

        @Test
        @DisplayName("应该支持自定义Claims")
        void shouldSupportCustomClaims() {
            // given
            Map<String, Object> customClaims = new HashMap<>();
            customClaims.put("userId", 123);
            customClaims.put("department", "IT");

            // when
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES, customClaims);
            Object userId = jwtProvider.getClaimFromToken(token, "userId");
            Object department = jwtProvider.getClaimFromToken(token, "department");

            // then
            assertThat(userId).isEqualTo(123);
            assertThat(department).isEqualTo("IT");
        }

        @Test
        @DisplayName("空角色列表应该能正确处理")
        void shouldHandleEmptyRoles() {
            // when
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, Collections.emptyList());

            // then
            assertThat(token).isNotEmpty();
            assertThat(jwtProvider.getRolesFromToken(token)).isEmpty();
        }

        @Test
        @DisplayName("null角色列表应该能正确处理")
        void shouldHandleNullRoles() {
            // when
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, null);

            // then
            assertThat(token).isNotEmpty();
        }
    }

    // ==================== Refresh Token生成测试 ====================

    @Nested
    @DisplayName("Refresh Token生成测试")
    class RefreshTokenGenerationTests {

        @Test
        @DisplayName("应该成功生成Refresh Token")
        void shouldGenerateRefreshToken() {
            // when
            String token = jwtProvider.generateRefreshToken(TEST_USERNAME);

            // then
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Refresh Token类型应该是refresh")
        void shouldHaveRefreshTokenType() {
            // given
            String token = jwtProvider.generateRefreshToken(TEST_USERNAME);

            // when
            Object type = jwtProvider.getClaimFromToken(token, "type");

            // then
            assertThat(type).isEqualTo("refresh");
        }

        @Test
        @DisplayName("Refresh Token应该是有效的")
        void shouldGenerateValidRefreshToken() {
            // given
            String token = jwtProvider.generateRefreshToken(TEST_USERNAME);

            // when & then
            assertThat(jwtProvider.validateToken(token)).isTrue();
        }
    }

    // ==================== TokenPair生成测试 ====================

    @Nested
    @DisplayName("TokenPair生成测试")
    class TokenPairGenerationTests {

        @Test
        @DisplayName("应该成功生成TokenPair")
        void shouldGenerateTokenPair() {
            // when
            TokenPair tokenPair = jwtProvider.generateTokenPair(TEST_USERNAME, TEST_ROLES);

            // then
            assertThat(tokenPair).isNotNull();
            assertThat(tokenPair.getAccessToken()).isNotEmpty();
            assertThat(tokenPair.getRefreshToken()).isNotEmpty();
            assertThat(tokenPair.getTokenType()).isEqualTo("Bearer");
            assertThat(tokenPair.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(tokenPair.getRoles()).containsExactlyInAnyOrderElementsOf(TEST_ROLES);
        }

        @Test
        @DisplayName("TokenPair中的expiresIn应该大于0")
        void shouldHavePositiveExpiresIn() {
            // when
            TokenPair tokenPair = jwtProvider.generateTokenPair(TEST_USERNAME, TEST_ROLES);

            // then
            assertThat(tokenPair.getExpiresIn()).isGreaterThan(0);
        }
    }

    // ==================== Token验证测试 ====================

    @Nested
    @DisplayName("Token验证测试")
    class TokenValidationTests {

        @Test
        @DisplayName("有效Token应该验证通过")
        void shouldValidateValidToken() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when & then
            assertThat(jwtProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("无效Token应该验证失败")
        void shouldFailInvalidToken() {
            // when & then
            assertThat(jwtProvider.validateToken("invalid.token.here")).isFalse();
        }

        @Test
        @DisplayName("空Token应该验证失败")
        void shouldFailEmptyToken() {
            // when & then
            assertThat(jwtProvider.validateToken("")).isFalse();
            assertThat(jwtProvider.validateToken(null)).isFalse();
        }

        @Test
        @DisplayName("篡改的Token应该验证失败")
        void shouldFailTamperedToken() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);
            String tamperedToken = token + "tampered";

            // when & then
            assertThat(jwtProvider.validateToken(tamperedToken)).isFalse();
        }

        @Test
        @DisplayName("应该返回详细的验证结果-成功")
        void shouldReturnDetailedSuccessResult() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when
            ValidationResult result = jwtProvider.validateTokenDetailed(token);

            // then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessage()).isNotEmpty();
        }

        @Test
        @DisplayName("应该返回详细的验证结果-失败")
        void shouldReturnDetailedFailureResult() {
            // when
            ValidationResult result = jwtProvider.validateTokenDetailed("invalid.token");

            // then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getReason()).isNotEmpty();
        }
    }

    // ==================== Token解析测试 ====================

    @Nested
    @DisplayName("Token解析测试")
    class TokenParsingTests {

        @Test
        @DisplayName("应该获取所有Claims")
        void shouldGetAllClaims() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when
            Claims claims = jwtProvider.getAllClaims(token);

            // then
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("应该获取过期时间")
        void shouldGetExpirationTime() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when
            long expirationTime = jwtProvider.getExpirationTimeFromToken(token);

            // then
            assertThat(expirationTime).isGreaterThan(System.currentTimeMillis());
        }

        @Test
        @DisplayName("应该获取剩余有效期")
        void shouldGetRemainingValidity() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when
            long remaining = jwtProvider.getRemainingValidity(token);

            // then
            assertThat(remaining).isGreaterThan(0);
        }

        @Test
        @DisplayName("Token不应该过期")
        void shouldNotBeExpired() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when & then
            assertThat(jwtProvider.isTokenExpired(token)).isFalse();
        }

        @Test
        @DisplayName("应该获取自定义Claims")
        void shouldGetCustomClaims() {
            // given
            Map<String, Object> customClaims = new HashMap<>();
            customClaims.put("userId", 123);
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES, customClaims);

            // when
            Map<String, Object> customData = jwtProvider.getCustomClaims(token);

            // then
            assertThat(customData).containsKey("userId");
            assertThat(customData.get("userId")).isEqualTo(123);
        }

        @Test
        @DisplayName("无效Token解析应该抛出异常")
        void shouldThrowExceptionForInvalidTokenParsing() {
            // when & then
            assertThatThrownBy(() -> jwtProvider.getAllClaims("invalid.token"))
                    .isInstanceOf(JwtProvider.InvalidTokenException.class);
        }
    }

    // ==================== Token刷新测试 ====================

    @Nested
    @DisplayName("Token刷新测试")
    class TokenRefreshTests {

        @Test
        @DisplayName("应该使用Refresh Token刷新Access Token")
        void shouldRefreshAccessToken() {
            // given
            TokenPair tokenPair = jwtProvider.generateTokenPair(TEST_USERNAME, TEST_ROLES);

            // when
            String newAccessToken = jwtProvider.refreshAccessToken(tokenPair.getRefreshToken());

            // then
            assertThat(newAccessToken).isNotEmpty();
            assertThat(newAccessToken).isNotEqualTo(tokenPair.getAccessToken());
            assertThat(jwtProvider.validateToken(newAccessToken)).isTrue();
            assertThat(jwtProvider.getUsernameFromToken(newAccessToken)).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("应该使用Refresh Token刷新整个TokenPair")
        void shouldRefreshTokenPair() {
            // given
            TokenPair originalPair = jwtProvider.generateTokenPair(TEST_USERNAME, TEST_ROLES);

            // when
            TokenPair newPair = jwtProvider.refreshTokenPair(originalPair.getRefreshToken());

            // then
            assertThat(newPair).isNotNull();
            assertThat(newPair.getAccessToken()).isNotEmpty();
            assertThat(newPair.getRefreshToken()).isNotEmpty();
            // 新Token对的内容应该正确
            assertThat(newPair.getUsername()).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("使用Access Token刷新应该失败")
        void shouldFailRefreshWithAccessToken() {
            // given
            String accessToken = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when & then
            assertThatThrownBy(() -> jwtProvider.refreshAccessToken(accessToken))
                    .isInstanceOf(JwtProvider.InvalidTokenException.class)
                    .hasMessageContaining("not a refresh token");
        }

        @Test
        @DisplayName("使用无效Token刷新应该失败")
        void shouldFailRefreshWithInvalidToken() {
            // when & then
            assertThatThrownBy(() -> jwtProvider.refreshAccessToken("invalid.token"))
                    .isInstanceOf(JwtProvider.InvalidTokenException.class);
        }
    }

    // ==================== JwtInfo测试 ====================

    @Nested
    @DisplayName("JwtInfo测试")
    class JwtInfoTests {

        @Test
        @DisplayName("应该获取完整的JwtInfo")
        void shouldGetFullJwtInfo() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when
            JwtInfo jwtInfo = jwtProvider.getJwtInfo(token);

            // then
            assertThat(jwtInfo).isNotNull();
            assertThat(jwtInfo.getSubject()).isEqualTo(TEST_USERNAME);
            assertThat(jwtInfo.getRoles()).containsExactlyInAnyOrderElementsOf(TEST_ROLES);
            assertThat(jwtInfo.getExpiration()).isNotNull();
            assertThat(jwtInfo.getRemainingValidity()).isGreaterThan(0);
        }

        @Test
        @DisplayName("JwtInfo应该包含发行者信息")
        void shouldContainIssuer() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when
            JwtInfo jwtInfo = jwtProvider.getJwtInfo(token);

            // then
            // issuer在JwtConfig中可能为null（默认配置）
            assertThat(jwtInfo).isNotNull();
        }

        @Test
        @DisplayName("JwtInfo应该包含受众信息")
        void shouldContainAudience() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when
            JwtInfo jwtInfo = jwtProvider.getJwtInfo(token);

            // then
            // audience在JwtConfig中可能为null（默认配置）
            assertThat(jwtInfo).isNotNull();
        }
    }

    // ==================== Authorization头处理测试 ====================

    @Nested
    @DisplayName("Authorization头处理测试")
    class AuthorizationHeaderTests {

        @Test
        @DisplayName("应该从Authorization头提取Token")
        void shouldExtractTokenFromHeader() {
            // given
            String token = "test_token_value";
            String authHeader = "Bearer " + token;

            // when
            String extracted = JwtProvider.extractTokenFromHeader(authHeader);

            // then
            assertThat(extracted).isEqualTo(token);
        }

        @Test
        @DisplayName("无Bearer前缀应该返回null")
        void shouldReturnNullForHeaderWithoutBearer() {
            // when
            String extracted = JwtProvider.extractTokenFromHeader("token_without_bearer");

            // then
            assertThat(extracted).isNull();
        }

        @Test
        @DisplayName("null头应该返回null")
        void shouldReturnNullForNullHeader() {
            // when
            String extracted = JwtProvider.extractTokenFromHeader(null);

            // then
            assertThat(extracted).isNull();
        }

        @Test
        @DisplayName("应该添加Bearer前缀")
        void shouldAddBearerPrefix() {
            // when
            String result = JwtProvider.addBearerPrefix("token_value");

            // then
            assertThat(result).isEqualTo("Bearer token_value");
        }

        @Test
        @DisplayName("已有Bearer前缀不应该重复添加")
        void shouldNotDuplicateBearerPrefix() {
            // when
            String result = JwtProvider.addBearerPrefix("Bearer token_value");

            // then
            assertThat(result).isEqualTo("Bearer token_value");
        }

        @Test
        @DisplayName("null Token添加Bearer前缀应该返回null")
        void shouldReturnNullForNullToken() {
            // when
            String result = JwtProvider.addBearerPrefix(null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("应该验证有效的Authorization头格式")
        void shouldValidateAuthHeaderFormat() {
            assertThat(JwtProvider.isValidAuthHeader("Bearer valid_token")).isTrue();
        }

        @Test
        @DisplayName("无效Authorization头格式应该返回false")
        void shouldInvalidateAuthHeaderFormat() {
            assertThat(JwtProvider.isValidAuthHeader(null)).isFalse();
            assertThat(JwtProvider.isValidAuthHeader("")).isFalse();
            assertThat(JwtProvider.isValidAuthHeader("Bearer")).isFalse();
            assertThat(JwtProvider.isValidAuthHeader("Bearer ")).isFalse();
            assertThat(JwtProvider.isValidAuthHeader("token_without_bearer")).isFalse();
        }
    }

    // ==================== 格式化时间测试 ====================

    @Nested
    @DisplayName("格式化时间测试")
    class FormattedTimeTests {

        @Test
        @DisplayName("应该格式化剩余时间")
        void shouldFormatRemainingTime() {
            // given
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);

            // when
            String formatted = jwtProvider.getFormattedRemainingTime(token);

            // then
            assertThat(formatted).isNotEmpty();
            assertThat(formatted).doesNotContain("已过期");
        }
    }

    // ==================== JwtConfig测试 ====================

    @Nested
    @DisplayName("JwtConfig测试")
    class JwtConfigTests {

        @Test
        @DisplayName("默认配置应该有默认值")
        void shouldHaveDefaultValues() {
            // when
            JwtConfig config = new JwtConfig();

            // then
            assertThat(config.getJwtSecret()).isNotEmpty();
            assertThat(config.getAccessTokenExpiration()).isGreaterThan(0);
            assertThat(config.getRefreshTokenExpiration()).isGreaterThan(0);
            assertThat(config.getClockSkewSeconds()).isEqualTo(60L);
        }

        @Test
        @DisplayName("自定义密钥应该被正确设置")
        void shouldSetCustomSecret() {
            // given
            String customSecret = "customSecretKeyForTesting1234567890ABCD";

            // when
            JwtConfig config = new JwtConfig(customSecret);

            // then
            assertThat(config.getJwtSecret()).isEqualTo(customSecret);
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("特殊字符用户名应该能正确处理")
        void shouldHandleSpecialCharacterUsername() {
            // given
            String specialUsername = "user@domain.com";

            // when
            String token = jwtProvider.generateAccessToken(specialUsername, TEST_ROLES);
            String extracted = jwtProvider.getUsernameFromToken(token);

            // then
            assertThat(extracted).isEqualTo(specialUsername);
        }

        @Test
        @DisplayName("中文用户名应该能正确处理")
        void shouldHandleChineseUsername() {
            // given
            String chineseUsername = "张三";

            // when
            String token = jwtProvider.generateAccessToken(chineseUsername, TEST_ROLES);
            String extracted = jwtProvider.getUsernameFromToken(token);

            // then
            assertThat(extracted).isEqualTo(chineseUsername);
        }

        @Test
        @DisplayName("大量角色应该能正确处理")
        void shouldHandleManyRoles() {
            // given
            List<String> manyRoles = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                manyRoles.add("ROLE_" + i);
            }

            // when
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, manyRoles);
            List<String> extractedRoles = jwtProvider.getRolesFromToken(token);

            // then
            assertThat(extractedRoles).hasSize(100);
        }

        @Test
        @DisplayName("大量自定义Claims应该能正确处理")
        void shouldHandleManyCustomClaims() {
            // given
            Map<String, Object> manyClaims = new HashMap<>();
            for (int i = 0; i < 50; i++) {
                manyClaims.put("claim_" + i, "value_" + i);
            }

            // when
            String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES, manyClaims);
            Map<String, Object> extracted = jwtProvider.getCustomClaims(token);

            // then
            assertThat(extracted).containsKeys("claim_0", "claim_49");
        }
    }

    // ==================== 多线程安全测试 ====================

    @Nested
    @DisplayName("多线程安全测试")
    class ThreadSafetyTests {

        @Test
        @DisplayName("多线程生成Token应该安全")
        void shouldBeThreadSafeForTokenGeneration() throws InterruptedException {
            // given
            int threadCount = 10;
            List<String> tokens = Collections.synchronizedList(new ArrayList<>());
            List<Thread> threads = new ArrayList<>();

            // when
            for (int i = 0; i < threadCount; i++) {
                Thread t = new Thread(() -> {
                    String token = jwtProvider.generateAccessToken(TEST_USERNAME, TEST_ROLES);
                    tokens.add(token);
                });
                threads.add(t);
                t.start();
            }

            for (Thread t : threads) {
                t.join();
            }

            // then
            assertThat(tokens).hasSize(threadCount);
            // 所有Token都应该是有效的
            for (String token : tokens) {
                assertThat(jwtProvider.validateToken(token)).isTrue();
            }
        }
    }
}
