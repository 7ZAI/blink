package com.blink.gateway.admin.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.admin.entity.FilterConfig;
import com.blink.gateway.admin.entity.GaRouteDO;
import com.blink.gateway.admin.entity.PredicateConfig;
import com.blink.gateway.admin.mapper.GaRouteMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.blink.gateway.admin.constants.ErrCodeConstant.*;
import static com.blink.gateway.admin.constants.RouteConstant.*;

/**
 * 路由校验器
 * 校验路由配置的正确性和合法性
 *
 * @author binblink
 * @since 2026-04-12
 */
@Service
@Slf4j
public class RouteValidator {

    @Resource
    private GaRouteMapper gaRouteMapper;

    /**
     * 校验路由配置完整性
     * 包括：URI格式、断言必填、断言/过滤器类型、路由冲突检测
     *
     * @param routeId 路由ID（更新时传入，新增时为null）
     * @param uri 目标URI
     * @param predicates 断言配置列表
     * @param filters 过滤器配置列表
     */
    public void validateRouteConfig(String routeId, String uri, List<PredicateConfig> predicates, List<FilterConfig> filters) {
        // 1. URI格式校验
        validateUriFormat(uri);

        // 2. 断言必填校验
        validatePredicatesRequired(predicates);

        // 3. 断言类型和语法校验
        validatePredicates(predicates);

        // 4. 过滤器类型和语法校验
        validateFilters(filters);

        // 5. 路由冲突检测（Path断言）
        checkRouteConflict(routeId, predicates);

        log.debug("[RouteValidator] 路由配置校验通过 | routeId: {}", routeId);
    }

    /**
     * 校验URI格式
     * 必须以 lb://、http://、https:// 开头
     *
     * @param uri 目标URI
     */
    private void validateUriFormat(String uri) {
        if (StrUtil.isBlank(uri)) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        // URI格式校验：必须以支持的协议开头
        if (!uri.startsWith(URI_PREFIX_LB) && !uri.startsWith(URI_PREFIX_HTTP) && !uri.startsWith(URI_PREFIX_HTTPS)) {
            log.warn("[RouteValidator] URI格式无效 | uri: {}", uri);
            BlinkException.throwBusinessException(URI_FORMAT_INVALID);
        }
    }

    /**
     * 校验断言必填
     * 至少需要一个断言配置
     *
     * @param predicates 断言配置列表
     */
    private void validatePredicatesRequired(List<PredicateConfig> predicates) {
        if (CollUtil.isEmpty(predicates)) {
            log.warn("[RouteValidator] 断言配置为空");
            BlinkException.throwBusinessException(PREDICATE_REQUIRED);
        }
    }

    /**
     * 校验断言类型和语法
     * 验证断言名称是否在支持列表中，参数是否合法
     *
     * @param predicates 断言配置列表
     */
    private void validatePredicates(List<PredicateConfig> predicates) {
        Set<String> supportedPredicates = SUPPORTED_PREDICATES;

        for (PredicateConfig predicate : predicates) {
            String name = predicate.getName();
            if (StrUtil.isBlank(name)) {
                log.warn("[RouteValidator] 断言名称为空");
                BlinkException.throwBusinessException(PREDICATE_SYNTAX_ERROR);
            }

            // 校验断言类型是否支持
            if (!supportedPredicates.contains(name)) {
                log.warn("[RouteValidator] 不支持的断言类型 | name: {}", name);
                BlinkException.throwBusinessException(UNSUPPORTED_PREDICATE_TYPE);
            }

            // 校验断言参数
            Map<String, String> args = predicate.getArgs();
            if (args == null || args.isEmpty()) {
                log.warn("[RouteValidator] 断言参数为空 | name: {}", name);
                BlinkException.throwBusinessException(PREDICATE_SYNTAX_ERROR);
            }

            // Path断言必须有pattern参数
            if ("Path".equals(name) && !args.containsKey("pattern")) {
                log.warn("[RouteValidator] Path断言缺少pattern参数");
                BlinkException.throwBusinessException(PREDICATE_SYNTAX_ERROR);
            }
        }
    }

    /**
     * 校验过滤器类型和语法
     * 验证过滤器名称是否在支持列表中
     *
     * @param filters 过滤器配置列表
     */
    private void validateFilters(List<FilterConfig> filters) {
        if (CollUtil.isEmpty(filters)) {
            // 过滤器可选，为空时不校验
            return;
        }

        Set<String> supportedFilters = SUPPORTED_FILTERS;

        for (FilterConfig filter : filters) {
            String name = filter.getName();
            if (StrUtil.isBlank(name)) {
                log.warn("[RouteValidator] 过滤器名称为空");
                BlinkException.throwBusinessException(FILTER_SYNTAX_ERROR);
            }

            // 校验过滤器类型是否支持
            if (!supportedFilters.contains(name)) {
                log.warn("[RouteValidator] 不支持的过滤器类型 | name: {}", name);
                BlinkException.throwBusinessException(UNSUPPORTED_FILTER_TYPE);
            }

            // 过滤器参数校验（部分过滤器参数可选）
            Map<String, String> args = filter.getArgs();
            if (args == null) {
                log.warn("[RouteValidator] 过滤器参数为null | name: {}", name);
                BlinkException.throwBusinessException(FILTER_SYNTAX_ERROR);
            }

            // StripPrefix过滤器必须有parts参数
            if ("StripPrefix".equals(name) && !args.containsKey("parts")) {
                log.warn("[RouteValidator] StripPrefix过滤器缺少parts参数");
                BlinkException.throwBusinessException(FILTER_SYNTAX_ERROR);
            }
        }
    }

    /**
     * 检测路由冲突
     * 检查相同Path断言是否已存在
     *
     * @param routeId 当前路由ID（更新时传入，新增时为null）
     * @param predicates 断言配置列表
     */
    private void checkRouteConflict(String routeId, List<PredicateConfig> predicates) {
        // 提取Path断言的pattern值
        String pathPattern = extractPathPattern(predicates);
        if (StrUtil.isBlank(pathPattern)) {
            // 无Path断言，不检测冲突
            return;
        }

        // 查询现有路由中是否存在相同Path
        List<GaRouteDO> existingRoutes = gaRouteMapper.selectList(null);
        for (GaRouteDO route : existingRoutes) {
            // 更新时跳过自身
            if (StrUtil.isNotBlank(routeId) && routeId.equals(route.getRouteId())) {
                continue;
            }

            String existingPath = extractPathPattern(route.getPredicates());
            if (pathPattern.equals(existingPath)) {
                log.warn("[RouteValidator] 路由路径冲突 | newPattern: {}, existingRouteId: {}", pathPattern, route.getRouteId());
                BlinkException.throwBusinessException(ROUTE_PATH_CONFLICT);
            }
        }
    }

    /**
     * 从断言配置中提取Path断言的pattern值
     *
     * @param predicates 断言配置列表
     * @return Path pattern值，不存在时返回null
     */
    private String extractPathPattern(List<PredicateConfig> predicates) {
        if (CollUtil.isEmpty(predicates)) {
            return null;
        }

        for (PredicateConfig predicate : predicates) {
            if ("Path".equals(predicate.getName())) {
                Map<String, String> args = predicate.getArgs();
                if (args != null && args.containsKey("pattern")) {
                    return args.get("pattern");
                }
            }
        }
        return null;
    }

    /**
     * 校验路由ID是否已存在
     *
     * @param routeId 路由ID
     * @param excludeExisting 排除现有路由（用于更新场景）
     */
    public void validateRouteIdUnique(String routeId, boolean excludeExisting) {
        if (StrUtil.isBlank(routeId)) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        if (!excludeExisting) {
            GaRouteDO existingRoute = gaRouteMapper.selectById(routeId);
            if (existingRoute != null) {
                log.warn("[RouteValidator] 路由ID已存在 | routeId: {}", routeId);
                BlinkException.throwBusinessException(ROUTE_ID_EXISTS);
            }
        }
    }
}