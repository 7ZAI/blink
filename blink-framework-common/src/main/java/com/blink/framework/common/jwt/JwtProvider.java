package com.blink.framework.common.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JJWT封装工具类
 * 支持JWS（签名）、JWE（加密）两种JWT生成/解析
 *
 * @author binblink
 */
@Slf4j
public class JwtProvider {


    private JwtConfig jwtConfig;


    public JwtConfig getJwtConfig() {
        return jwtConfig;
    }

    public void setJwtConfig(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    public JwtProvider() {

    }

    public JwtProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }


    /**
     * 获取签名密钥
     */
    public SecretKey getSecretKey() {
        try {
            String jwtSecret = jwtConfig.getJwtSecret();
            // 验证Secret Key长度是否足够
            if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
                log.warn("JWT secret key is too short (< 256 bits), using default");
            }
            SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
            log.info("JWT key initialized successfully, default algorithm: HS256");
            // 创建签名密钥
            return secretKey;
        } catch (Exception e) {
            log.error("Failed to initialize JWT key", e);
            throw new RuntimeException("JWT initialization failed", e);
        }
    }


    // ==================== Token生成 ====================

    /**
     * 生成Access Token (最常用)
     *
     * @param username 用户名
     * @param roles    用户角色列表
     * @return JWT Token字符串
     */
    public String generateAccessToken(String username, List<String> roles) {
        return generateAccessToken(username, roles, null);
    }

    /**
     * 生成Access Token (支持自定义Claims)
     *
     * @param username         用户名
     * @param roles            用户角色列表
     * @param additionalClaims 额外的自定义Claims
     * @return JWT Token字符串
     */
    public String generateAccessToken(String username, List<String> roles,
                                      Map<String, Object> additionalClaims) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());

            JwtBuilder builder = Jwts.builder()
                    // 标准Claims
                    .subject(username)
                    .issuedAt(now)
                    .expiration(expiryDate)
                    // 自定义Claims
                    .claim("roles", roles)
                    .claim("type", "access");

            // 添加额外的Claims
            if (additionalClaims != null) {
                additionalClaims.forEach(builder::claim);
            }

            // 签名和紧凑化
            String token = builder.signWith(getSecretKey()).compact();

            log.debug("Generated access token for user: {}", username);
            return token;
        } catch (Exception e) {
            log.error("Error generating access token for user: {}", username, e);
            throw new JwtGenerationException("Failed to generate access token", e);
        }
    }

    /**
     * 生成Refresh Token
     *
     * @param username 用户名
     * @return JWT Refresh Token字符串
     */
    public String generateRefreshToken(String username) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());

            String token = Jwts.builder()
                    .subject(username)
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .claim("type", "refresh")
                    .signWith(getSecretKey())
                    .compact();

            log.debug("Generated refresh token for user: {}", username);
            return token;
        } catch (Exception e) {
            log.error("Error generating refresh token for user: {}", username, e);
            throw new JwtGenerationException("Failed to generate refresh token", e);
        }
    }

    /**
     * 生成两个Token (Access Token + Refresh Token)
     *
     * @param username 用户名
     * @param roles    用户角色
     * @return TokenPair对象，包含两个Token
     */
    public TokenPair generateTokenPair(String username, List<String> roles) {
        return TokenPair.builder()
                .accessToken(generateAccessToken(username, roles))
                .refreshToken(generateRefreshToken(username))
                .tokenType("Bearer")
                // 转换为秒
                .expiresIn(jwtConfig.getAccessTokenExpiration() / 1000)
                .username(username)
                .roles(roles)
                .build();
    }

    // ==================== Token验证 ====================

    /**
     * 验证Token是否有效
     *
     * @param token JWT Token字符串
     * @return true: Token有效，false: Token无效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .clockSkewSeconds(jwtConfig.getAccessTokenExpiration())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证Token并返回详细结果
     *
     * @param token JWT Token字符串
     * @return ValidationResult对象，包含验证结果和错误信息
     */
    public ValidationResult validateTokenDetailed(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .clockSkewSeconds(jwtConfig.getAccessTokenExpiration())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return ValidationResult.success("Token is valid");
        } catch (ExpiredJwtException e) {
            return ValidationResult.expired("Token has expired");
        } catch (UnsupportedJwtException e) {
            return ValidationResult.invalid("Unsupported token");
        } catch (MalformedJwtException e) {
            return ValidationResult.invalid("Malformed token");
        } catch (io.jsonwebtoken.security.SignatureException e) {
            return ValidationResult.invalid("Invalid signature");
        } catch (IllegalArgumentException e) {
            return ValidationResult.invalid("Empty JWT claims");
        } catch (Exception e) {
            return ValidationResult.invalid("Token validation failed: " + e.getMessage());
        }
    }

    // ==================== Token解析 ====================

    /**
     * 获取Token中的所有Claims
     *
     * @param token JWT Token字符串
     * @return Claims对象
     * @throws InvalidTokenException 如果token无效
     */
    public Claims getAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.error("Failed to parse JWT token", e);
            throw new InvalidTokenException("Invalid or expired token", e);
        }
    }

    /**
     * 获取Token中的用户名
     *
     * @param token JWT Token字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            return getAllClaims(token).getSubject();
        } catch (Exception e) {
            log.error("Failed to extract username from token", e);
            return null;
        }
    }

    /**
     * 获取Token中的角色列表
     *
     * @param token JWT Token字符串
     * @return 角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = getAllClaims(token);
            Object rolesObj = claims.get("roles");

            if (rolesObj instanceof List) {
                return (List<String>) rolesObj;
            }

            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to extract roles from token", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取Token中的指定Claim
     *
     * @param token     JWT Token字符串
     * @param claimName Claim名称
     * @return Claim值
     */
    public Object getClaimFromToken(String token, String claimName) {
        try {
            return getAllClaims(token).get(claimName);
        } catch (Exception e) {
            log.error("Failed to extract claim {} from token", claimName, e);
            return null;
        }
    }

    /**
     * 获取Token中的所有自定义Claims
     *
     * @param token JWT Token字符串
     * @return 自定义Claims的Map
     */
    public Map<String, Object> getCustomClaims(String token) {
        try {
            Claims claims = getAllClaims(token);

            // 移除标准Claims，只返回自定义的
            Map<String, Object> customData = new HashMap<>();
            // 官方核心字段列表（对应jti、iss、sub、aud、exp、nbf、iat）
            String[] officialKeys = {"jti", "iss", "sub", "aud", "exp", "nbf", "iat"};
            Set<String> officialKeySet = new HashSet<>(Arrays.asList(officialKeys));
            claims.forEach((key, value) -> {
                // 仅保留非官方核心字段，作为自定义业务数据
                if (!officialKeySet.contains(key)) {
                    customData.put(key, value);
                }
            });

            return customData;
        } catch (Exception e) {
            log.error("Failed to extract custom claims from token", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取Token的过期时间
     *
     * @param token JWT Token字符串
     * @return 过期时间戳
     */
    public long getExpirationTimeFromToken(String token) {
        try {
            return getAllClaims(token).getExpiration().getTime();
        } catch (Exception e) {
            log.error("Failed to extract expiration from token", e);
            return 0;
        }
    }

    /**
     * 判断Token是否过期
     *
     * @param token JWT Token字符串
     * @return true: 已过期，false: 未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getAllClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            log.error("Failed to check token expiration", e);
            return true;  // 异常情况认为已过期
        }
    }

    /**
     * 获取Token剩余的有效期（毫秒）
     *
     * @param token JWT Token字符串
     * @return 剩余毫秒数，如果已过期返回0或负数
     */
    public long getRemainingValidity(String token) {
        try {
            Date expiration = getAllClaims(token).getExpiration();
            long remainingMs = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remainingMs, 0);
        } catch (Exception e) {
            log.error("Failed to get remaining validity", e);
            return 0;
        }
    }

    private long getRemainingValidity(Claims claims) {
        try {
            Date expiration = claims.getExpiration();
            long remainingMs = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remainingMs, 0);
        } catch (Exception e) {
            log.error("Failed to get remaining validity", e);
            return 0;
        }
    }

    // ==================== Token刷新 ====================

    /**
     * 使用Refresh Token生成新的Access Token
     *
     * @param refreshToken Refresh Token
     * @return 新的Access Token
     * @throws InvalidTokenException 如果Token无效或类型不是refresh
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            // 验证Refresh Token有效性
            if (!validateToken(refreshToken)) {
                throw new InvalidTokenException("Refresh token is invalid or expired");
            }

            // 验证Token类型
            Claims claims = getAllClaims(refreshToken);
            String tokenType = (String) claims.get("type");

            if (!"refresh".equals(tokenType)) {
                throw new InvalidTokenException("This token is not a refresh token");
            }

            // 获取用户名和角色
            String username = claims.getSubject();
            List<String> roles = getRolesFromToken(refreshToken);

            // 生成新的Access Token
            log.info("Refreshing access token for user: {}", username);
            return generateAccessToken(username, roles);
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to refresh access token", e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    /**
     * 使用Refresh Token生成新的Token对
     *
     * @param refreshToken Refresh Token
     * @return 新的TokenPair对象
     */
    public TokenPair refreshTokenPair(String refreshToken) {
        try {
            Claims claims = getAllClaims(refreshToken);
            String username = claims.getSubject();
            List<String> roles = getRolesFromToken(refreshToken);

            return generateTokenPair(username, roles);
        } catch (Exception e) {
            log.error("Failed to refresh token pair", e);
            throw new RuntimeException("Token pair refresh failed", e);
        }
    }

    // ==================== Token信息获取 ====================

    /**
     * 获取Token的完整信息
     *
     * @param token JWT Token字符串
     * @return TokenInfo对象
     */
    public JwtInfo getJwtInfo(String token) {
        try {
            Claims claims = getAllClaims(token);
            var jwtDto = transformToJwtPayloadDTO(claims);
            jwtDto.setRemainingValidity(getRemainingValidity(claims));
            return jwtDto;

        } catch (Exception e) {

            log.error("Failed to get token info", e);
            return null;
        }
    }

    /**
     * 转换Claims为自己封装的JwtInfo
     * @param claims 验签得到的结果
     * @return JwtInfo
     */
    private JwtInfo transformToJwtPayloadDTO(Claims claims) {
        var jwtDto = new JwtInfo();

        jwtDto.setJwtId(claims.getId());
        jwtDto.setSubject(claims.getSubject());
        jwtDto.setAudience(claims.getAudience());
        jwtDto.setIssuer(claims.getIssuer());
        jwtDto.setIssuedAt(claims.getIssuedAt());
        jwtDto.setExpiration(claims.getExpiration());
        jwtDto.setNotBefore(claims.getNotBefore());
        jwtDto.setTokenType(claims.get("tokenType", String.class));

        Object rolesObj = claims.get("roles");
        List<String> roles = Collections.emptyList();
        if (rolesObj instanceof List) {
            roles = ((List<?>) rolesObj).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
        }
        jwtDto.setRoles(roles);

        Map<String, Object> customData = new HashMap<>();
        // 官方核心字段列表（对应jti、iss、sub、aud、exp、nbf、iat）
        String[] officialKeys = {"jti", "iss", "sub", "aud", "exp", "nbf", "iat"};
        Set<String> officialKeySet = new HashSet<>(Arrays.asList(officialKeys));

        claims.forEach((key, value) -> {
            // 仅保留非官方核心字段，作为自定义业务数据
            if (!officialKeySet.contains(key)) {
                customData.put(key, value);
            }
        });
        jwtDto.setCustomData(customData);

        return jwtDto;
    }



    /**
     * 获取Token的过期剩余时间（友好格式）
     *
     * @param token JWT Token字符串
     * @return 格式化的剩余时间，如 "1天2小时30分钟"
     */
    public String getFormattedRemainingTime(String token) {
        long remainingMs = getRemainingValidity(token);

        if (remainingMs <= 0) {
            return "已过期";
        }

        long seconds = remainingMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%d天%d小时%d分钟",
                    days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d小时%d分钟%d秒",
                    hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒",
                    minutes, seconds % 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    // ==================== Token编码/解码 ====================

    /**
     * 从Authorization头中提取Token
     *
     * @param authHeader Authorization头的值，格式: "Bearer xxx"
     * @return Token字符串
     */
    public static String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 添加Bearer前缀到Token
     *
     * @param token Token字符串
     * @return 带Bearer前缀的Token，格式: "Bearer xxx"
     */
    public static String addBearerPrefix(String token) {
        if (token != null && !token.startsWith("Bearer ")) {
            return "Bearer " + token;
        }
        return token;
    }

    /**
     * 验证Authorization头格式
     *
     * @param authHeader Authorization头的值
     * @return true: 格式正确，false: 格式不正确
     */
    public static boolean isValidAuthHeader(String authHeader) {
        return authHeader != null &&
                authHeader.startsWith("Bearer ") &&
                authHeader.length() > 7;
    }

    // ==================== 异常类 ====================

    /**
     * JWT生成异常
     */
    public static class JwtGenerationException extends RuntimeException {
        public JwtGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Token无效异常
     */
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }

        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }


}