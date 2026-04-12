package com.blink.gateway.admin.handler;

import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.admin.entity.GaRouteDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * GaRouteDO 类型 JSON TypeHandler
 * 使用 JacksonUtil 的 ObjectMapper，支持 LocalDateTime 等 Java 8 时间类型
 *
 * @author binblink
 * @since 2026-04-12
 */
@Slf4j
@MappedTypes({GaRouteDO.class})
public class GaRouteDOTypeHandler extends BaseTypeHandler<GaRouteDO> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, GaRouteDO parameter, JdbcType jdbcType) throws SQLException {
        try {
            // 使用 JacksonUtil，已配置 JavaTimeModule 支持 LocalDateTime
            ps.setString(i, JacksonUtil.toJson(parameter));
        } catch (Exception e) {
            log.error("GaRouteDO JSON序列化失败", e);
            throw new SQLException("JSON序列化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public GaRouteDO getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public GaRouteDO getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public GaRouteDO getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private GaRouteDO parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JacksonUtil.fromJson(json, GaRouteDO.class);
        } catch (Exception e) {
            log.error("GaRouteDO JSON反序列化失败, json: {}", json, e);
            return null;
        }
    }
}