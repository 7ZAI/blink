package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.blink.base.dto.req.AddSysConfigGroupReq;
import com.blink.base.dto.req.DeleteSysConfigGroupReq;
import com.blink.base.dto.req.QuerySysConfigGroupReq;
import com.blink.base.dto.req.UpdateSysConfigGroupReq;
import com.blink.base.dto.rsp.QuerySysConfigGroupRsp;
import com.blink.base.entity.SysConfigGroupDO;
import com.blink.base.mapper.SysConfigGroupMapper;
import com.blink.base.service.SysConfigGroupService;
import com.blink.datasource.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 参数分组表 服务实现类
 *
 * @author blink
 * @since 2025-10-16
 */
@Transactional(rollbackFor = Exception.class)
@Service
public class SysConfigGroupServiceImpl implements SysConfigGroupService {

    private final Logger logger = LoggerFactory.getLogger(SysConfigGroupServiceImpl.class);

    @Resource
    private SysConfigGroupMapper sysConfigGroupMapper;

    /**
     * 保存 参数分组表
     *
     * @param saveParam
     * @throws BlinkException
     */
    @Override
    public void saveSysConfigGroup(AddSysConfigGroupReq saveParam) throws BlinkException {

        var sysConfigGroupDO = new SysConfigGroupDO();
        BeanUtil.copyProperties(saveParam, sysConfigGroupDO);

        sysConfigGroupMapper.insert(sysConfigGroupDO);
    }

    /**
     * 删除 参数分组表
     *
     * @param deleteParam
     * @throws BlinkException
     */
    @Override
    public void deleteSysConfigGroup(DeleteSysConfigGroupReq deleteParam) throws BlinkException {


        if (deleteParam.isBatchDelete()) {
            sysConfigGroupMapper.deleteBatchIds(deleteParam.getIdList());
        } else {
            sysConfigGroupMapper.deleteById(deleteParam.getDeleteId());
        }

    }

    /**
     * 更新 参数分组表
     *
     * @param updateParam
     * @throws BlinkException
     */
    @Override
    public void modifySysConfigGroup(UpdateSysConfigGroupReq updateParam) throws BlinkException {
        var sysConfigGroupDO = new SysConfigGroupDO();
        BeanUtil.copyProperties(updateParam, sysConfigGroupDO);

        sysConfigGroupMapper.updateById(sysConfigGroupDO);
    }

    /**
     * 查询 参数分组表 列表
     *
     * @param queryParam
     * @return result
     * @throws BlinkException
     */
    @Override
    public QuerySysConfigGroupRsp getSysConfigGroupList(QuerySysConfigGroupReq queryParam) throws BlinkException {

        var pageRsp = new QuerySysConfigGroupRsp();

        QuerySysConfigGroupRsp result = PageUtils.queryPage(queryParam, () -> sysConfigGroupMapper.findSysConfigGroupList(queryParam), pageRsp);

        return result;
    }


}
