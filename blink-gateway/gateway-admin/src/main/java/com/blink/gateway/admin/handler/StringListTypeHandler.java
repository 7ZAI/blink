package com.blink.gateway.admin.handler;

import com.blink.framework.common.utils.JacksonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * List<String> 类型 JSON TypeHandler
 *
 * @author binblink
 * @since 2026-04-16
 */
@Slf4j
public class StringListTypeHandler extends BaseTypeHandler<List<String>> {

    private static final TypeReference<List<String>> TYPE_REFERENCE = new TypeReference<List<String>>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, JacksonUtil.toJson(parameter));
        } catch (Exception e) {
            log.error("[StringList] JSON序列化失败", e);
            throw new SQLException("JSON序列化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JacksonUtil.fromJson(json, TYPE_REFERENCE);
        } catch (Exception e) {
            log.error("[StringList] JSON反序列化失败, json: {}", json, e);
            return null;
        }
    }
}
