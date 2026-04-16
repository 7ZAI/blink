package com.blink.gateway.admin.handler;

import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.admin.entity.GaRouteDO;
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
 * List<GaRouteDO> 类型 JSON TypeHandler
 * 专门处理路由快照列表的序列化/反序列化
 *
 * @author binblink
 * @since 2026-04-16
 */
@Slf4j
public class GaRouteDOListTypeHandler extends BaseTypeHandler<List<GaRouteDO>> {

    private static final TypeReference<List<GaRouteDO>> TYPE_REFERENCE = new TypeReference<List<GaRouteDO>>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<GaRouteDO> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, JacksonUtil.toJson(parameter));
        } catch (Exception e) {
            log.error("[GaRouteDOList] JSON序列化失败", e);
            throw new SQLException("JSON序列化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GaRouteDO> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public List<GaRouteDO> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public List<GaRouteDO> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private List<GaRouteDO> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JacksonUtil.fromJson(json, TYPE_REFERENCE);
        } catch (Exception e) {
            log.error("[GaRouteDOList] JSON反序列化失败, json: {}", json, e);
            return null;
        }
    }
}
