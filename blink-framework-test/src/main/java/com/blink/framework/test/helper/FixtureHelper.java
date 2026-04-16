package com.blink.framework.test.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 测试数据 Fixture 管理工具
 * 支持共享测试数据的加载、缓存和复用
 *
 * 使用方式：
 * <pre>
 * // 加载 JSON Fixture 文件
 * AddUserReq userReq = FixtureHelper.loadFixture("fixtures/add-user-request.json", AddUserReq.class);
 *
 * // 注册并复用 Fixture
 * FixtureHelper.registerFixture("defaultUser", () -> createDefaultUser());
 * SysUserDO user = FixtureHelper.getFixture("defaultUser");
 *
 * // 使用缓存 Fixture（避免重复创建）
 * SysUserDO cachedUser = FixtureHelper.getOrCompute("testUser", () -> createUser("test"));
 * </pre>
 *
 * @author binblink
 * @since 2026-04-16
 */
public class FixtureHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Fixture 缓存（测试运行期间共享）
     */
    private static final Map<String, Object> FIXTURE_CACHE = new HashMap<>();

    /**
     * Fixture 注册表（Supplier 模式，延迟创建）
     */
    private static final Map<String, Supplier<?>> FIXTURE_REGISTRY = new HashMap<>();

    // ========== JSON 文件加载 ==========

    /**
     * 从 JSON 文件加载 Fixture
     *
     * @param path  文件路径（相对于 resources 目录）
     * @param clazz 目标类型
     * @return 加载的对象
     */
    public static <T> T loadFixture(String path, Class<T> clazz) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return MAPPER.readValue(resource.getInputStream(), clazz);
        } catch (IOException e) {
            throw new RuntimeException("加载 Fixture 文件失败: " + path, e);
        }
    }

    /**
     * 从 JSON 文件加载 Fixture 列表
     *
     * @param path  文件路径
     * @param clazz 元素类型
     * @return 加载的列表
     */
    public static <T> java.util.List<T> loadFixtureList(String path, Class<T> clazz) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            com.fasterxml.jackson.databind.JavaType type = MAPPER.getTypeFactory()
                    .constructCollectionType(java.util.List.class, clazz);
            return MAPPER.readValue(resource.getInputStream(), type);
        } catch (IOException e) {
            throw new RuntimeException("加载 Fixture 列表文件失败: " + path, e);
        }
    }

    // ========== Fixture 注册与获取 ==========

    /**
     * 注册 Fixture（Supplier 模式，延迟创建）
     *
     * @param name     Fixture 名称
     * @param supplier Fixture 创建函数
     */
    public static <T> void registerFixture(String name, Supplier<T> supplier) {
        FIXTURE_REGISTRY.put(name, supplier);
    }

    /**
     * 获取已注册的 Fixture（首次获取时创建，后续复用）
     *
     * @param name Fixture 名称
     * @return Fixture 对象
     */
    public static <T> T getFixture(String name) {
        if (FIXTURE_CACHE.containsKey(name)) {
            return (T) FIXTURE_CACHE.get(name);
        }
        Supplier<?> supplier = FIXTURE_REGISTRY.get(name);
        if (supplier == null) {
            throw new IllegalArgumentException("未注册的 Fixture: " + name);
        }
        T fixture = (T) supplier.get();
        FIXTURE_CACHE.put(name, fixture);
        return fixture;
    }

    /**
     * 获取或计算 Fixture（缓存模式）
     * 如果缓存存在则返回，否则创建并缓存
     *
     * @param name     Fixture 名称
     * @param supplier Fixture 创建函数
     * @return Fixture 对象
     */
    public static <T> T getOrCompute(String name, Supplier<T> supplier) {
        if (FIXTURE_CACHE.containsKey(name)) {
            return (T) FIXTURE_CACHE.get(name);
        }
        T fixture = supplier.get();
        FIXTURE_CACHE.put(name, fixture);
        return fixture;
    }

    /**
     * 获取或计算 Fixture（每次都创建新实例）
     *
     * @param name     Fixture 名称
     * @param supplier Fixture 创建函数
     * @return 新创建的 Fixture 对象
     */
    public static <T> T computeFresh(String name, Supplier<T> supplier) {
        T fixture = supplier.get();
        FIXTURE_CACHE.put(name, fixture);
        return fixture;
    }

    // ========== Fixture 缓存管理 ==========

    /**
     * 清除所有 Fixture 缓存
     * 通常在 @AfterAll 或测试结束时调用
     */
    public static void clearAll() {
        FIXTURE_CACHE.clear();
    }

    /**
     * 清除指定 Fixture 缓存
     *
     * @param name Fixture 名称
     */
    public static void clear(String name) {
        FIXTURE_CACHE.remove(name);
    }

    /**
     * 检查 Fixture 是否已缓存
     *
     * @param name Fixture 名称
     * @return 是否已缓存
     */
    public static boolean isCached(String name) {
        return FIXTURE_CACHE.containsKey(name);
    }

    /**
     * 检查 Fixture 是否已注册
     *
     * @param name Fixture 名称
     * @return 是否已注册
     */
    public static boolean isRegistered(String name) {
        return FIXTURE_REGISTRY.containsKey(name);
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存中的 Fixture 数量
     */
    public static int cacheSize() {
        return FIXTURE_CACHE.size();
    }

    // ========== Fixture 快捷方法 ==========

    /**
     * 快捷创建并缓存 Fixture
     *
     * @param name     Fixture 名称
     * @param fixture  Fixture 对象
     * @return 缓存的 Fixture 对象
     */
    public static <T> T cache(String name, T fixture) {
        FIXTURE_CACHE.put(name, fixture);
        return fixture;
    }

    /**
     * 更新已缓存的 Fixture
     *
     * @param name     Fixture 名称
     * @param fixture  新的 Fixture 对象
     */
    public static <T> void update(String name, T fixture) {
        FIXTURE_CACHE.put(name, fixture);
    }
}