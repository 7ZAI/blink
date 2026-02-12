package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.base.constans.CommonConstans;
import com.blink.base.dto.req.AddSysGroupReqDTO;
import com.blink.base.dto.req.DeleteSysGroupReqDTO;
import com.blink.base.dto.req.QuerySysGroupReqDTO;
import com.blink.base.dto.req.UpdateSysGroupReqDTO;
import com.blink.base.dto.rsp.QuerySysGroupRspDTO;
import com.blink.base.dto.rsp.SysGroupRspDTO;
import com.blink.base.entity.SysGroupDO;
import com.blink.base.entity.SysUserGroupRelaDO;
import com.blink.base.mapper.SysGroupMapper;
import com.blink.base.mapper.SysUserGroupRelaMapper;
import com.blink.base.service.SysGroupService;
import com.blink.datasource.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 组 服务实现类
 *
 * @author binblink
 * @since 2024-01-04
 */
@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
public class SysGroupServiceImpl implements SysGroupService {

    @Resource
    private SysGroupMapper sysGroupMapper;

    @Resource
    private SysUserGroupRelaMapper userGroupRelaMapper;

    /**
     * 保存 组
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    @Override
    public SysGroupRspDTO saveSysGroup(AddSysGroupReqDTO saveParam) throws BlinkException {


        List<SysGroupDO> groupDOList = sysGroupMapper.selectList(new LambdaQueryWrapper<SysGroupDO>()
                .eq(SysGroupDO::getGroupName, saveParam.getGroupName())
                .eq(SysGroupDO::getGroupParentId, saveParam.getGroupParentId()));
        //存在同名
        if (!CollUtil.isEmpty(groupDOList)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.GROUP_ALREADY_EXIST);
        }

        //父节点
        SysGroupDO parentGroup = sysGroupMapper.selectById(saveParam.getGroupParentId());

        //父节点判断
        if (ObjectUtil.isNull(parentGroup)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.GROUP_PARENT_NOT_EXIST);
        }

        //父是叶子节点 则更新为否
        if (CommonConstans.IS_LEAF.equals(parentGroup.getIsLeaf())) {
            parentGroup.setIsLeaf(CommonConstans.NOT_LEAF);
            sysGroupMapper.updateById(parentGroup);
        }

        var sysGroupDO = new SysGroupDO();
        BeanUtil.copyProperties(saveParam, sysGroupDO);
        sysGroupDO.setGroupLevel(parentGroup.getGroupLevel() + 1);


        sysGroupMapper.insert(sysGroupDO);

        var rspDTO = new SysGroupRspDTO();
        BeanUtil.copyProperties(sysGroupDO, rspDTO);

        return rspDTO;
    }

    /**
     * 删除 组
     *
     * @param deleteParam
     * @throws BlinkException
     */
    @Override
    public void deleteSysGroup(DeleteSysGroupReqDTO deleteParam) throws BlinkException {


        if (deleteParam.isBatchDelete()) {

            Long count = userGroupRelaMapper.selectCount(new LambdaQueryWrapper<SysUserGroupRelaDO>()
                    .in(SysUserGroupRelaDO::getGroupId, deleteParam.getIdList()));

            //存在关联数据 无法删除
            if (count.compareTo(CommonConstans.LONG_ZERO) > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_RELA_DATA);
            }

            count = sysGroupMapper.selectCount(new LambdaQueryWrapper<SysGroupDO>()
                    .in(SysGroupDO::getGroupParentId, deleteParam.getIdList()));

            //存在子节点数据 无法删除
            if (count.compareTo(CommonConstans.LONG_ZERO) > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_SON_DATA);
            }

            sysGroupMapper.deleteBatchIds(deleteParam.getIdList());
        } else {

            Long count = userGroupRelaMapper.selectCount(new LambdaQueryWrapper<SysUserGroupRelaDO>()
                    .eq(SysUserGroupRelaDO::getGroupId, deleteParam.getDeleteId()));

            //存在关联数据 无法删除
            if (count.compareTo(CommonConstans.LONG_ZERO) > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_RELA_DATA);
            }

            count = sysGroupMapper.selectCount(new LambdaQueryWrapper<SysGroupDO>()
                    .eq(SysGroupDO::getGroupParentId, deleteParam.getDeleteId()));

            //存在子节点数据 无法删除
            if (count.compareTo(CommonConstans.LONG_ZERO) > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_SON_DATA);
            }

            sysGroupMapper.deleteById(deleteParam.getDeleteId());
        }

    }

    /**
     * 更新 组
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    @Override
    public SysGroupRspDTO modifySysGroup(UpdateSysGroupReqDTO updateParam) throws BlinkException {

        SysGroupDO sysGroupOld = sysGroupMapper.selectById(updateParam.getGroupId());

        //节点不存在
        if (ObjectUtil.isNull(sysGroupOld)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.GROUP_NOT_EXIST);
        }

        //修改了名称
        if (!sysGroupOld.getGroupName().equals(updateParam.getGroupName())) {

            List<SysGroupDO> groupDOList = sysGroupMapper.selectList(new LambdaQueryWrapper<SysGroupDO>()
                    .eq(SysGroupDO::getGroupName, updateParam.getGroupName())
                    .eq(SysGroupDO::getGroupParentId, updateParam.getGroupParentId()));
            //存在同名
            if (!CollUtil.isEmpty(groupDOList)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.GROUP_ALREADY_EXIST);
            }
        }

        //父节点
        SysGroupDO parentGroup = sysGroupMapper.selectById(updateParam.getGroupParentId());

        //父节点判断
        if (ObjectUtil.isNull(parentGroup)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.GROUP_PARENT_NOT_EXIST);
        }

        //父是叶子节点 则更新为否
        if (CommonConstans.IS_LEAF.equals(parentGroup.getIsLeaf())) {
            parentGroup.setIsLeaf(CommonConstans.NOT_LEAF);
            sysGroupMapper.updateById(parentGroup);
        }


        var sysGroupNew = new SysGroupDO();
        BeanUtil.copyProperties(updateParam, sysGroupNew);
        sysGroupNew.setGroupLevel(parentGroup.getGroupLevel() + 1);

        sysGroupMapper.updateById(sysGroupNew);

        var rspDTO = new SysGroupRspDTO();
        BeanUtil.copyProperties(sysGroupNew, rspDTO);

        return rspDTO;
    }

    /**
     * 查询 组 列表
     *
     * @param queryParam
     * @return QuerySysGroupRspDTO
     * @throws BlinkException
     */
    @Override
    public QuerySysGroupRspDTO getSysGroupList(QuerySysGroupReqDTO queryParam) throws BlinkException {

        var pageRsp = new QuerySysGroupRspDTO();
        var param = new SysGroupDO();
        BeanUtil.copyProperties(queryParam, param);
        PageUtils.queryPage(queryParam, () -> sysGroupMapper.findSysGroupList(param), pageRsp);

        return pageRsp;
    }


}
