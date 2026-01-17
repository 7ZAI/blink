package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.blink.base.dto.req.AddSysConfigGroupReqDTO;
import com.blink.base.dto.req.DeleteSysConfigGroupReqDTO;
import com.blink.base.dto.req.QuerySysConfigGroupReqDTO;
import com.blink.base.dto.req.UpdateSysConfigGroupReqDTO;
import com.blink.base.dto.rsp.QuerySysConfigGroupRspDTO;
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
    public void saveSysConfigGroup(AddSysConfigGroupReqDTO saveParam) throws BlinkException {

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
    public void deleteSysConfigGroup(DeleteSysConfigGroupReqDTO deleteParam) throws BlinkException {


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
    public void modifySysConfigGroup(UpdateSysConfigGroupReqDTO updateParam) throws BlinkException {
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
    public QuerySysConfigGroupRspDTO getSysConfigGroupList(QuerySysConfigGroupReqDTO queryParam) throws BlinkException {

        var pageRsp = new QuerySysConfigGroupRspDTO();

        QuerySysConfigGroupRspDTO result = PageUtils.queryPage(queryParam, () -> sysConfigGroupMapper.findSysConfigGroupList(queryParam), pageRsp);

        return result;
    }


}
