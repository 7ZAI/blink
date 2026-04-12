package com.blink.gateway.admin.handler;

import com.blink.framework.common.utils.JacksonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

/**
 * Map 类型 JSON TypeHandler
 * 使用 JacksonUtil 的 ObjectMapper，支持 LocalDateTime 等 Java 8 时间类型
 *
 * @author binblink
 * @since 2026-04-12
 */
@Slf4j
@MappedTypes({Map.class})
public class MapTypeHandler extends BaseTypeHandler<Map<String, Object>> {

    private final TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String, Object>>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, JacksonUtil.toJson(parameter));
        } catch (Exception e) {
            log.error("Map JSON序列化失败", e);
            throw new SQLException("JSON序列化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return JacksonUtil.fromJson(json, typeReference);
        } catch (Exception e) {
            log.error("Map JSON反序列化失败, json: {}", json, e);
            return Collections.emptyMap();
        }
    }
}