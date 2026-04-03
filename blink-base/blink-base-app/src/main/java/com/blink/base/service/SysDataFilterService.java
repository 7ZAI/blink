package com.blink.base.service;

import com.blink.base.dto.req.AddDataFilterReq;
import com.blink.base.dto.req.DataFilterIdReq;
import com.blink.base.dto.req.QueryDataFilterReq;
import com.blink.base.dto.req.UpdateDataFilterReq;
import com.blink.base.dto.rsp.EntityFieldsRsp;
import com.blink.base.dto.rsp.EntityListRsp;
import com.blink.base.dto.rsp.MatchTypesRsp;
import com.blink.base.dto.rsp.QueryDataFilterRsp;
import com.blink.base.dto.vo.DataFilterVO;
import com.blink.datasource.data.RuleConfig;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;

import java.util.List;

/**
 * 数据过滤规则服务接口
 *
 * @author binblink
 */
public interface SysDataFilterService {

    /**
     * 查询数据过滤规则列表（分页）
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    ResponseDTO<QueryDataFilterRsp> queryDataFilterList(RequestDTO<QueryDataFilterReq> reqDto);

    /**
     * 新增数据过滤规则
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    ResponseDTO<EmptyBody> addDataFilter(RequestDTO<AddDataFilterReq> reqDto);

    /**
     * 更新数据过滤规则
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    ResponseDTO<EmptyBody> updateDataFilter(RequestDTO<UpdateDataFilterReq> reqDto);

    /**
     * 删除数据过滤规则
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    ResponseDTO<EmptyBody> deleteDataFilter(RequestDTO<DataFilterIdReq> reqDto);

    /**
     * 获取数据过滤规则详情
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    ResponseDTO<DataFilterVO> getDataFilterDetail(RequestDTO<DataFilterIdReq> reqDto);

    /**
     * 获取实体类字段列表
     *
     * @param entityClass 实体类全限定名
     * @return 字段列表响应
     */
    EntityFieldsRsp getEntityFields(String entityClass);

    /**
     * 获取已注册实体列表
     *
     * @return 已注册实体列表响应
     */
    EntityListRsp getEntityList();

    /**
     * 刷新数据权限缓存
     *
     * @return 响应DTO
     */
    ResponseDTO<EmptyBody> refreshCache();

    /**
     * 根据用户ID查询该用户拥有的所有数据过滤规则配置
     * 关联查询：用户 -> 角色 -> 权限 -> 数据过滤规则
     *
     * @param userId 用户ID
     * @return 规则配置列表
     */
    List<RuleConfig> getRuleConfigsByUserId(Integer userId);

    /**
     * 获取匹配类型选项
     * 根据过滤对象和关联关系返回可用的匹配类型
     *
     * @param tableName     过滤对象表名
     * @param relationName  关联关系名称
     * @return 匹配类型选项响应
     */
    MatchTypesRsp getMatchTypes(String tableName, String relationName);
}