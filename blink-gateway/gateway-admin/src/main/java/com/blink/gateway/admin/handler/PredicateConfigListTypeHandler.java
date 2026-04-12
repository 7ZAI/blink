package com.blink.gateway.admin.handler;

import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.admin.entity.PredicateConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * PredicateConfig List 类型 JSON TypeHandler
 * 使用 JacksonUtil 的 ObjectMapper，支持 LocalDateTime 等 Java 8 时间类型
 *
 * @author binblink
 * @since 2026-04-12
 */
@Slf4j
@MappedTypes({List.class})
public class PredicateConfigListTypeHandler extends BaseTypeHandler<List<PredicateConfig>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<PredicateConfig> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, JacksonUtil.toJson(parameter));
        } catch (Exception e) {
            log.error("PredicateConfig List JSON序列化失败", e);
            throw new SQLException("JSON序列化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PredicateConfig> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public List<PredicateConfig> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public List<PredicateConfig> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private List<PredicateConfig> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JacksonUtil.fromJsonToList(json, PredicateConfig.class);
        } catch (Exception e) {
            log.error("PredicateConfig List JSON反序列化失败, json: {}", json, e);
            return Collections.emptyList();
        }
    }
}