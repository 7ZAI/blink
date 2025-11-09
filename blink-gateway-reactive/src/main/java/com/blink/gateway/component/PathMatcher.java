package com.blink.gateway.component;

import org.springframework.util.AntPathMatcher;

/**
 * @Author binblink
 * @Date 2025/8/23
 */
//@Component
public class PathMatcher {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public boolean matches(String pattern, String path) {
        return antPathMatcher.match(pattern, path);
    }

    public boolean matchesAny(String path, String... patterns) {
        for (String pattern : patterns) {
            if (antPathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}