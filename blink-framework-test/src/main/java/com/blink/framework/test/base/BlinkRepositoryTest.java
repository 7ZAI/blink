package com.blink.framework.test.base;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Repository/Mapper 测试基类
 * 使用 H2 内存数据库，适合数据层单元测试
 *
 * 使用方式：
 * <pre>
 * @RepositoryTest
 * class MyMapperTest extends BlinkRepositoryTest {
 *
 *     @Autowired
 *     private MyMapper mapper;
 *
 *     @Test
 *     @Sql("/test-data/users.sql")  // 加载测试数据
 *     void shouldFindUser_byId() {
 *         UserDO user = mapper.selectById(1);
 *         assertThat(user).isNotNull();
 *     }
 * }
 * </pre>
 *
 * @author binblink
 * @since 2026-04-16
 */
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class BlinkRepositoryTest {

    /**
     * 数据源
     * 用于手动执行 SQL 脚本
     */
    @Autowired
    protected DataSource dataSource;

    /**
     * 执行 SQL 初始化脚本
     * 可在测试前手动加载测试数据
     *
     * @param scriptPath SQL 脚本路径（相对于 resources 目录）
     */
    protected void executeInitScript(String scriptPath) {
        try (Connection connection = dataSource.getConnection()) {
            ClassPathResource resource = new ClassPathResource(scriptPath);
            ScriptUtils.executeSqlScript(connection, resource);
        } catch (SQLException e) {
            throw new RuntimeException("执行 SQL 脚本失败: " + scriptPath, e);
        }
    }

    /**
     * 清空指定表的数据
     * 用于测试后清理数据
     *
     * @param tableName 表名
     */
    protected void clearTable(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("DELETE FROM " + tableName);
        } catch (SQLException e) {
            throw new RuntimeException("清空表数据失败: " + tableName, e);
        }
    }

    /**
     * 重置表的自增 ID
     *
     * @param tableName 表名
     */
    protected void resetAutoIncrement(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("ALTER TABLE " + tableName + " AUTO_INCREMENT = 1");
        } catch (SQLException e) {
            throw new RuntimeException("重置自增 ID 失败: " + tableName, e);
        }
    }
}