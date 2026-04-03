package com.blink.gateway.datascope;

import com.blink.base.datascope.handler.DateRangeFilterHandler;
import com.blink.datasource.data.DataScopeParseResult;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.data.UserDataScopeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DateRangeFilterHandler 单元测试类
 * 测试时间范围过滤规则处理器的各项功能
 *
 * @author binblink
 */
@DisplayName("DateRangeFilterHandler 单元测试")
class DateRangeFilterHandlerTest {

    private DateRangeFilterHandler dateRangeFilterHandler;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    void setUp() {
        dateRangeFilterHandler = new DateRangeFilterHandler();
    }

    @Test
    @DisplayName("测试相对时间 - 过去7天")
    void testRelativeTimePast7Days() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_time");
        config.setRangeType("RELATIVE");
        config.setRelativeValue(-7);
        config.setRelativeUnit("DAY");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果 - 应该生成双向范围条件
        String result = sql.toString();
        assertTrue(result.contains("create_time >="));
        assertTrue(result.contains("AND"));
        assertTrue(result.contains("create_time <="));
        assertTrue(result.contains("("));
        assertTrue(result.contains(")"));
    }

    @Test
    @DisplayName("测试相对时间 - 未来30天")
    void testRelativeTimeFuture30Days() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("expire_time");
        config.setRangeType("RELATIVE");
        config.setRelativeValue(30);
        config.setRelativeUnit("DAY");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果 - 应该生成双向范围条件
        String result = sql.toString();
        assertTrue(result.contains("expire_time >="));
        assertTrue(result.contains("AND"));
        assertTrue(result.contains("expire_time <="));
    }

    @Test
    @DisplayName("测试相对时间 - 带表别名")
    void testRelativeTimeWithTableAlias() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_time");
        config.setRangeType("RELATIVE");
        config.setRelativeValue(-7);
        config.setRelativeUnit("DAY");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果
        assertTrue(sql.toString().contains("t.create_time"));
    }

    @Test
    @DisplayName("测试相对时间 - 月单位")
    void testRelativeTimeMonthUnit() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_time");
        config.setRangeType("RELATIVE");
        config.setRelativeValue(-1);
        config.setRelativeUnit("MONTH");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果
        assertTrue(sql.toString().contains("create_time"));
    }

    @Test
    @DisplayName("测试相对时间 - 年单位")
    void testRelativeTimeYearUnit() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_time");
        config.setRangeType("RELATIVE");
        config.setRelativeValue(-1);
        config.setRelativeUnit("YEAR");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果
        assertTrue(sql.toString().contains("create_time"));
    }

    @Test
    @DisplayName("测试绝对时间 - 完整范围")
    void testAbsoluteTimeFullRange() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_time");
        config.setRangeType("ABSOLUTE");
        config.setStartTime("2026-01-01 00:00:00");
        config.setEndTime("2026-12-31 23:59:59");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果
        String result = sql.toString();
        assertTrue(result.contains("create_time >= '2026-01-01 00:00:00'"));
        assertTrue(result.contains("AND"));
        assertTrue(result.contains("create_time <= '2026-12-31 23:59:59'"));
    }

    @Test
    @DisplayName("测试绝对时间 - 只有开始时间")
    void testAbsoluteTimeOnlyStartTime() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_time");
        config.setRangeType("ABSOLUTE");
        config.setStartTime("2026-01-01 00:00:00");
        config.setEndTime(null);

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果
        String result = sql.toString();
        assertTrue(result.contains("create_time >= '2026-01-01 00:00:00'"));
        assertFalse(result.contains("create_time <="));
    }

    @Test
    @DisplayName("测试绝对时间 - 只有结束时间")
    void testAbsoluteTimeOnlyEndTime() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_time");
        config.setRangeType("ABSOLUTE");
        config.setStartTime(null);
        config.setEndTime("2026-12-31 23:59:59");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果
        String result = sql.toString();
        assertFalse(result.contains("create_time >="));
        assertTrue(result.contains("create_time <= '2026-12-31 23:59:59'"));
    }

    @Test
    @DisplayName("测试绝对时间 - 带表别名")
    void testAbsoluteTimeWithTableAlias() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_time");
        config.setRangeType("ABSOLUTE");
        config.setStartTime("2026-01-01 00:00:00");
        config.setEndTime("2026-12-31 23:59:59");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, "t");

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user t WHERE 1=1");

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果
        assertTrue(sql.toString().contains("t.create_time"));
    }

    @Test
    @DisplayName("测试空字段名 - 不生成条件")
    void testEmptyFieldName() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("");
        config.setRangeType("RELATIVE");
        config.setRelativeValue(-7);
        config.setRelativeUnit("DAY");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试空范围类型 - 不生成条件")
    void testEmptyRangeType() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_time");
        config.setRangeType("");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试相对时间 - null相对值不生成条件")
    void testRelativeTimeNullValue() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_time");
        config.setRangeType("RELATIVE");
        config.setRelativeValue(null);
        config.setRelativeUnit("DAY");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试相对时间 - 空单位不生成条件")
    void testRelativeTimeEmptyUnit() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_time");
        config.setRangeType("RELATIVE");
        config.setRelativeValue(-7);
        config.setRelativeUnit("");

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
    }

    @Test
    @DisplayName("测试绝对时间 - 无开始和结束时间不生成条件")
    void testAbsoluteTimeNoRange() {
        // 准备测试数据
        RuleConfig config = new RuleConfig();
        config.setField("create_time");
        config.setRangeType("ABSOLUTE");
        config.setStartTime(null);
        config.setEndTime(null);

        UserDataScopeInfo userInfo = new UserDataScopeInfo();
        userInfo.setUserId(100);

        DataScopeParseResult context = createMockContext(userInfo, null);

        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1");
        String originalSql = sql.toString();

        // 执行测试
        dateRangeFilterHandler.apply(sql, config, context);

        // 验证结果 - SQL不应改变
        assertEquals(originalSql, sql.toString());
    }

    /**
     * 创建模拟的 DataScopeParseResult
     *
     * @param userInfo   用户信息
     * @param tableAlias 表别名
     * @return 模拟的上下文对象
     */
    private DataScopeParseResult createMockContext(UserDataScopeInfo userInfo, String tableAlias) {
        DataScopeParseResult context = new DataScopeParseResult();
        try {
            // 使用反射设置私有字段
            var userInfoField = DataScopeParseResult.class.getDeclaredField("userInfo");
            userInfoField.setAccessible(true);
            userInfoField.set(context, userInfo);

            var tableAliasField = DataScopeParseResult.class.getDeclaredField("tableAlias");
            tableAliasField.setAccessible(true);
            tableAliasField.set(context, tableAlias);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock context", e);
        }
        return context;
    }
}