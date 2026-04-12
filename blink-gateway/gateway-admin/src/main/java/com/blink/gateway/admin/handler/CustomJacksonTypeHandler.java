package com.blink.gateway.admin.handler;

import com.blink.framework.common.utils.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 自定义 Jackson TypeHandler
 * 使用 Common 模块的 JacksonUtil，支持 LocalDateTime 等 Java 8 时间类型
 *
 * @author binblink
 * @since 2026-04-12
 */
@Slf4j
public class CustomJacksonTypeHandler extends BaseTypeHandler<Object> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        try {
            // 使用 JacksonUtil 的 ObjectMapper，支持 LocalDateTime
            ps.setString(i, JacksonUtil.toJson(parameter));
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            throw new SQLException("JSON序列化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    private Object parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            // 返回 JsonNode 或根据实际情况转换
            return JacksonUtil.readTree(json);
        } catch (Exception e) {
            log.error("JSON反序列化失败, json: {}", json, e);
            return null;
        }
    }
}