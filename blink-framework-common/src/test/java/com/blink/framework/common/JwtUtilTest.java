package com.blink.framework.common;

import com.blink.framework.common.jwt.JwtConfig;
import com.blink.framework.common.jwt.JwtInfo;
import com.blink.framework.common.jwt.JwtProvider;
import com.blink.framework.common.jwt.TokenPair;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtConfig jwtConfig = new JwtConfig();
    
    private JwtProvider jwtUtil = new JwtProvider(jwtConfig);
    
    private String testUsername = "testuser";
    private List<String> testRoles = Arrays.asList("ROLE_USER", "ROLE_ADMIN");
    

    @Test
    void testGenerateAccessToken() {
        String token = jwtUtil.generateAccessToken(testUsername, testRoles);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));  // JWT应该有3部分
    }
    
    @Test
    void testGenerateTokenPair() {
        TokenPair tokenPair = jwtUtil.generateTokenPair(testUsername, testRoles);
        
        assertNotNull(tokenPair);
        assertNotNull(tokenPair.getAccessToken());
        assertNotNull(tokenPair.getRefreshToken());
        assertEquals(testUsername, tokenPair.getUsername());
        assertEquals(testRoles, tokenPair.getRoles());
    }
    
    @Test
    void testValidateToken() {
        String token = jwtUtil.generateAccessToken(testUsername, testRoles);
        
        assertTrue(jwtUtil.validateToken(token));
    }
    
    @Test
    void testInvalidToken() {
        String invalidToken = "invalid.token.here";
        
        assertFalse(jwtUtil.validateToken(invalidToken));
    }
    
    @Test
    void testGetUsernameFromToken() {
        String token = jwtUtil.generateAccessToken(testUsername, testRoles);
        
        String username = jwtUtil.getUsernameFromToken(token);
        
        assertEquals(testUsername, username);
    }
    
    @Test
    void testGetRolesFromToken() {
        String token = jwtUtil.generateAccessToken(testUsername, testRoles);
        
        List<String> roles = jwtUtil.getRolesFromToken(token);
        
        assertEquals(testRoles.size(), roles.size());
        assertTrue(roles.containsAll(testRoles));
    }
    
    @Test
    void testCustomClaims() {
        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("userId", 123);
        customClaims.put("department", "IT");
        
        String token = jwtUtil.generateAccessToken(testUsername, testRoles, customClaims);
        
        Object userId = jwtUtil.getClaimFromToken(token, "userId");
        assertEquals(123, userId);
        
        Object department = jwtUtil.getClaimFromToken(token, "department");
        assertEquals("IT", department);
    }
    
    @Test
    void testRefreshToken() {
        TokenPair tokenPair = jwtUtil.generateTokenPair(testUsername, testRoles);
        
        String newAccessToken = jwtUtil.refreshAccessToken(
            tokenPair.getRefreshToken());
        
        assertNotNull(newAccessToken);
        assertNotEquals(tokenPair.getAccessToken(), newAccessToken);
        assertTrue(jwtUtil.validateToken(newAccessToken));
    }
    
    @Test
    void testTokenInfo() {
        String token = jwtUtil.generateAccessToken(testUsername, testRoles);
        
        JwtInfo tokenInfo = jwtUtil.getJwtInfo(token);
        
        assertNotNull(tokenInfo);
        assertEquals(testUsername, tokenInfo.getSubject());
        assertEquals(testRoles, tokenInfo.getCustomData().get("roles"));
        assertNotNull(tokenInfo.getExpiration());

        assertTrue(tokenInfo.getRemainingValidity() > 0);
    }
    
    @Test
    void testExtractTokenFromHeader() {
        String token = "test_token_xyz";
        String authHeader = "Bearer " + token;
        
        String extracted = jwtUtil.extractTokenFromHeader(authHeader);
        
        assertEquals(token, extracted);
    }
}