package com.blink.base.service;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QuerySysConfigRsp;
import com.blink.base.dto.vo.SysConfigVO;
import com.blink.base.entity.SysConfigDO;
import com.blink.framework.common.exception.BlinkException;

import java.util.List;

/**
 * <p>
 *  参数配置表 服务类
 * </p>
 *
 * @author blink
 * @since 2025-09-05
 */
public interface SysConfigService {

    /**
     * 保存 参数配置表
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    void saveSysConfig(AddSysConfigReq saveParam) throws BlinkException;

    /**
     * 删除 参数配置表
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    void deleteSysConfig(DeleteSysConfigReq deleteParam) throws BlinkException;

    /**
     * 更新 参数配置表
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    void modifySysConfig(UpdateSysConfigReq updateParam) throws BlinkException;

    /**
     * 查询 参数配置表 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    QuerySysConfigRsp getSysConfigList(QuerySysConfigReq queryParam) throws BlinkException;

    /**
     * 根据分组Id 查询该分组下所有子配置
     * @return
     * @throws BlinkException
     */
    List<SysConfigDO> getSysConfigsByGroupId(Integer gid) throws BlinkException;

    /**
     * 根据key或者id查询单个配置
     * @return
     * @throws BlinkException
     */
    SysConfigVO getOneConfigFromDataBase(QueryOneSysConfigReq param) throws BlinkException;

    /**
     * 根据查询条件查询
     * 缓存或者数据库获取单个参数配置
     *
     * @param body
     * @return SysConfigVO
     * @throws BlinkException
     */
    SysConfigVO getOneConfigFromCacheOrDataBase(QueryOneSysConfigReq body) throws BlinkException;
}
