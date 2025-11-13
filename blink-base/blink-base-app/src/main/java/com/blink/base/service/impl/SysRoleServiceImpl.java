package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.base.constans.CommonConstans;
import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QuerySysRoleRspDTO;
import com.blink.base.dto.rsp.QueryUserRolesRspDTO;
import com.blink.base.dto.vo.SysRoleVO;
import com.blink.base.entity.SysRoleDO;
import com.blink.base.entity.SysUserRoleRelaDO;
import com.blink.base.mapper.SysRoleMapper;
import com.blink.base.mapper.SysUserRoleRelaMapper;
import com.blink.base.service.SysRoleService;
import com.blink.datasource.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统角色 服务实现类
 *
 * @author binblink
 * @since 2024-01-03
 */
@Transactional(rollbackFor = Exception.class)
@Service
public class SysRoleServiceImpl implements SysRoleService {

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysUserRoleRelaMapper userRoleRelaMapper;

    /**
     * 保存 系统角色
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    @Override
    public SysRoleVO saveSysRole(AddSysRoleReqDTO saveParam) throws BlinkException {

        SysRoleDO sysRoleDO = new SysRoleDO();

        BeanUtil.copyProperties(saveParam, sysRoleDO);
        Long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRoleDO>().eq(SysRoleDO::getRoleCode, sysRoleDO.getRoleCode()));

        //角色代码不允许重复
        if (count > CommonConstans.LONG_ZERO) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_ALREADY_EXIT);
        }

        sysRoleMapper.insert(sysRoleDO);

        SysRoleVO sysRoleVO = new SysRoleVO();
        BeanUtil.copyProperties(sysRoleDO, sysRoleVO);

        return sysRoleVO;
    }

    /**
     * 删除 系统角色
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    @Override
    public void deleteSysRole(DeleteSysRoleReqDTO deleteParam) throws BlinkException {


        if (deleteParam.isBatchDelete()) {
            Long count = userRoleRelaMapper.selectCount(new LambdaQueryWrapper<SysUserRoleRelaDO>()
                    .in(SysUserRoleRelaDO::getRoleId, deleteParam.getIdList()));

            //存在关联数据 无法删除
            if (count.compareTo(CommonConstans.LONG_ZERO) > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_RELA_DATA);
            }

            sysRoleMapper.deleteBatchIds(deleteParam.getIdList());
        } else {

            Long count = userRoleRelaMapper.selectCount(new LambdaQueryWrapper<SysUserRoleRelaDO>()
                    .eq(SysUserRoleRelaDO::getRoleId, deleteParam.getDeleteId()));

            //存在关联数据 无法删除
            if (count.compareTo(CommonConstans.LONG_ZERO) > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_RELA_DATA);
            }

            sysRoleMapper.deleteById(deleteParam.getDeleteId());
        }

    }

    /**
     * 更新 系统角色
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    @Override
    public SysRoleVO modifySysRole(UpdateSysRoleReqDTO updateParam) throws BlinkException {


        SysRoleDO sysRoleDOld = sysRoleMapper.selectById(updateParam.getRoleId());

        //节点不存在
        if (ObjectUtil.isNull(sysRoleDOld)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_NOT_EXIST);
        }

        SysRoleDO sysRoleDO = new SysRoleDO();
        BeanUtil.copyProperties(updateParam, sysRoleDO);

        sysRoleMapper.updateById(sysRoleDO);

        SysRoleVO sysRoleVO = new SysRoleVO();
        BeanUtil.copyProperties(sysRoleDO, sysRoleVO);

        return sysRoleVO;
    }

    /**
     * 查询 系统角色 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    @Override
    public QuerySysRoleRspDTO getSysRoleList(QuerySysRoleReqDTO queryParam) throws BlinkException {

        QuerySysRoleRspDTO pageRsp = new QuerySysRoleRspDTO();
        PageUtils.queryPage(queryParam, () -> sysRoleMapper.findSysRoleList(queryParam), pageRsp);

        return pageRsp;
    }

    /**
     * 根据用户信息查询 用户角色
     *
     * @param queryParam
     * @return {@link QueryUserRolesRspDTO}
     * @throws BlinkException
     */
    @Override
    public QueryUserRolesRspDTO getSysRolesByUser(QueryUserRolesReqDTO queryParam) throws BlinkException {

        QueryUserRolesRspDTO queryUserRolesRspDTO = new QueryUserRolesRspDTO();

        List<SysRoleDO> roles = sysRoleMapper.findSysRolesByUser(queryParam);
        List<SysRoleVO> vos = new ArrayList<>();

        BeanUtil.copyProperties(roles, vos);
        queryUserRolesRspDTO.setRoles(vos);

        return queryUserRolesRspDTO;
    }


}
