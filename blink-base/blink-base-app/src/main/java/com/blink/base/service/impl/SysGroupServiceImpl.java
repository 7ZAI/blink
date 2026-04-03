package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.constants.CommonConstans;
import com.blink.base.dto.req.AddSysGroupReq;
import com.blink.base.dto.req.DeleteSysGroupReq;
import com.blink.base.dto.req.QuerySysGroupReq;
import com.blink.base.dto.req.UpdateSysGroupReq;
import com.blink.base.dto.rsp.QuerySysGroupRsp;
import com.blink.base.dto.rsp.SysGroupRsp;
import com.blink.base.dto.vo.SysGroupVO;
import com.blink.base.entity.SysGroupDO;
import com.blink.base.entity.SysUserGroupRelaDO;
import com.blink.base.mapper.SysGroupMapper;
import com.blink.base.mapper.SysUserGroupRelaMapper;
import com.blink.base.service.SysGroupService;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public SysGroupRsp saveSysGroup(AddSysGroupReq saveParam) throws BlinkException {


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

        log.info("[SysGroup] 新增组织成功 | groupId: {}, groupName: {}, parentId: {}",
                sysGroupDO.getGroupId(), sysGroupDO.getGroupName(), sysGroupDO.getGroupParentId());

        var rspDTO = new SysGroupRsp();
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
    public void deleteSysGroup(DeleteSysGroupReq deleteParam) throws BlinkException {


        if (Boolean.TRUE.equals(deleteParam.getBatchDelete())) {

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

            sysGroupMapper.deleteByIds(deleteParam.getIdList());
            log.info("[SysGroup] 批量删除组织成功 | groupIds: {}", deleteParam.getIdList());
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
            log.info("[SysGroup] 删除组织成功 | groupId: {}", deleteParam.getDeleteId());
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
    public SysGroupRsp modifySysGroup(UpdateSysGroupReq updateParam) throws BlinkException {

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

        log.info("[SysGroup] 更新组织成功 | groupId: {}, groupName: {}, parentId: {}",
                sysGroupNew.getGroupId(), sysGroupNew.getGroupName(), sysGroupNew.getGroupParentId());

        var rspDTO = new SysGroupRsp();
        BeanUtil.copyProperties(sysGroupNew, rspDTO);

        return rspDTO;
    }

    /**
     * 查询 组 列表（树形数据不分页）
     *
     * @param queryParam 查询参数
     * @return QuerySysGroupRsp
     * @throws BlinkException
     */
    @Override
    public QuerySysGroupRsp getSysGroupList(QuerySysGroupReq queryParam) throws BlinkException {

        QuerySysGroupRsp rsp = new QuerySysGroupRsp();

        LambdaQueryWrapper<SysGroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(SysGroupDO::getGroupLevel, SysGroupDO::getGroupId);

        // 条件查询
        if (queryParam != null) {
            if (cn.hutool.core.util.StrUtil.isNotBlank(queryParam.getGroupName())) {
                queryWrapper.like(SysGroupDO::getGroupName, queryParam.getGroupName());
            }
            if (queryParam.getGroupParentId() != null) {
                queryWrapper.eq(SysGroupDO::getGroupParentId, queryParam.getGroupParentId());
            }
            if (cn.hutool.core.util.StrUtil.isNotBlank(queryParam.getGroupLeader())) {
                queryWrapper.like(SysGroupDO::getGroupLeader, queryParam.getGroupLeader());
            }
        }

        List<SysGroupDO> groupDOList = sysGroupMapper.selectList(queryWrapper);

        // 转换为 VO 列表
        List<SysGroupVO> voList = new ArrayList<>();
        for (SysGroupDO groupDO : groupDOList) {
            SysGroupVO vo = new SysGroupVO();
            cn.hutool.core.bean.BeanUtil.copyProperties(groupDO, vo);
            voList.add(vo);
        }

        rsp.setList(voList);

        log.info("[SysGroup] 查询组织列表完成 | size: {}", voList.size());

        return rsp;
    }


}
