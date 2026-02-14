package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.base.constans.CommonConstans;
import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QuerySysRoleRspDTO;
import com.blink.base.dto.rsp.QueryUserRolesRspDTO;
import com.blink.base.dto.vo.SysRoleVO;
import com.blink.base.entity.SysRoleDO;
import com.blink.base.entity.SysRoleMenuRelaDO;
import com.blink.base.entity.SysRolePermRelaDO;
import com.blink.base.entity.SysUserRoleRelaDO;
import com.blink.base.mapper.*;
import com.blink.base.service.SysRoleService;
import com.blink.datasource.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 系统角色 服务实现类
 *
 * @author binblink
 * @since 2024-01-03
 */
@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
public class SysRoleServiceImpl implements SysRoleService {

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysRolePermRelaMapper rolePermRelaMapper;

    @Resource
    private SysRoleMenuRelaMapper roleMenuRelaMapper;

    @Resource
    private SysUserRoleRelaMapper userRoleRelaMapper;

    @Resource
    private SysPermissionMapper permissionMapper;

    @Resource
    private SysMenuMapper sysMenuMapper;

    /**
     * 保存 系统角色
     *
     * @param saveParam 入参
     * @return SysRoleVO 显示信息
     */
    @Override
    public SysRoleVO saveSysRole(AddSysRoleReqDTO saveParam) throws BlinkException {

        var sysRoleDO = new SysRoleDO();

        BeanUtil.copyProperties(saveParam, sysRoleDO);
        Long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRoleDO>().eq(SysRoleDO::getRoleCode, sysRoleDO.getRoleCode()));

        //角色代码不允许重复
        if (count > CommonConstans.LONG_ZERO) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_ALREADY_EXIT);
        }

        sysRoleMapper.insert(sysRoleDO);
        Integer roleId = sysRoleDO.getRoleId();

        //分配的权限id集合
        List<Integer> permIds = saveParam.getPermissionIds();
        if (ObjectUtil.isNotEmpty(permIds)) {
            // 验证前端菜单id是否都合法
            if (dataNotExistCheck(permIds, () -> permissionMapper.selectBatchIds(permIds))) {
                //存在非法权限id
                BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_NOT_EXIST);
            }
            batchInsertPermissions(permIds, roleId);
        }

        List<Integer> menuIds = saveParam.getMenuIds();
        if (ObjectUtil.isNotEmpty(menuIds)) {

            // 验证前端菜单id是否都合法
            if (dataNotExistCheck(menuIds, () -> sysMenuMapper.selectBatchIds(menuIds))) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.MENU_NOT_EXIST);
            }
            batchInsertMenus(menuIds, roleId);
        }

        var sysRoleVO = new SysRoleVO();
        BeanUtil.copyProperties(sysRoleDO, sysRoleVO);

        return sysRoleVO;
    }


    /**
     * 删除 系统角色
     *
     * @param deleteParam 入参
     */
    @Override
    public void deleteSysRole(DeleteSysRoleReqDTO deleteParam) throws BlinkException {


        if (deleteParam.isBatchDelete()) {
            Long count = userRoleRelaMapper.selectCount(new LambdaQueryWrapper<SysUserRoleRelaDO>()
                    .in(SysUserRoleRelaDO::getRoleId, deleteParam.getIdList()));

            //存在关联数据 无法删除 只有未绑定任何用户的角色才能删除 或者将拥有角色的用户全部删除后 才能删除角色
            if (count.compareTo(CommonConstans.LONG_ZERO) > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_RELA_DATA);
            }

            sysRoleMapper.deleteBatchIds(deleteParam.getIdList());
            rolePermRelaMapper.deleteById(deleteParam.getDeleteId());
            roleMenuRelaMapper.deleteById(deleteParam.getDeleteId());
        } else {
            Long count = userRoleRelaMapper.selectCount(new LambdaQueryWrapper<SysUserRoleRelaDO>()
                    .eq(SysUserRoleRelaDO::getRoleId, deleteParam.getDeleteId()));
            //存在关联数据 无法删除
            if (count.compareTo(CommonConstans.LONG_ZERO) > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_RELA_DATA);
            }
            sysRoleMapper.deleteById(deleteParam.getDeleteId());
            rolePermRelaMapper.deleteById(deleteParam.getDeleteId());
            roleMenuRelaMapper.deleteById(deleteParam.getDeleteId());
        }

    }

    /**
     * 更新 系统角色
     *
     * @param updateParam 入参
     * @return SysRoleVO
     */
    @Override
    public SysRoleVO modifySysRole(UpdateSysRoleReqDTO updateParam) throws BlinkException {


        SysRoleDO sysRoleDOld = sysRoleMapper.selectById(updateParam.getRoleId());

        //角色不存在
        if (ObjectUtil.isNull(sysRoleDOld)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_NOT_EXIST);
        }

        SysRoleDO sysRoleDO = new SysRoleDO();
        BeanUtil.copyProperties(updateParam, sysRoleDO);

        sysRoleMapper.updateById(sysRoleDO);
        Integer roleId = sysRoleDOld.getRoleId();

        //前端传递的权限id
        List<Integer> permIds = Optional.ofNullable(updateParam.getPermissionIds()).orElse(new ArrayList<>());
        List<SysRolePermRelaDO> olderPermIds = Optional.ofNullable(rolePermRelaMapper.selectList(new LambdaQueryWrapper<SysRolePermRelaDO>()
                .eq(SysRolePermRelaDO::getRoleId, roleId))).orElse(new ArrayList<SysRolePermRelaDO>());

        //对比获取删除的权限id集合 和新增的权限id集合
        if (ObjectUtil.isNotEmpty(olderPermIds)) {
            List<Integer> oldData = olderPermIds.stream().map(SysRolePermRelaDO::getAcId).toList();
            //oldData 中存在 permIds不存在
            List<Integer> deleteList = CollUtil.subtract(oldData, permIds).stream().toList();

            if (!deleteList.isEmpty()) {
                rolePermRelaMapper.deleteBatchByPermIds(deleteList);
            }

            List<Integer> addList = CollUtil.subtract(permIds, oldData).stream().toList();
            if (!addList.isEmpty()) {
                batchInsertPermissions(addList, roleId);
            }

        } else {
            if (ObjectUtil.isNotEmpty(permIds)) {
                batchInsertPermissions(permIds, roleId);
            }
        }

        //前端传递的菜单id
        List<Integer> menuIds = Optional.ofNullable(updateParam.getMenuIds()).orElse(new ArrayList<>());
        //数据库中的关联菜单
        List<SysRoleMenuRelaDO> oldRelaMenus = Optional.ofNullable(roleMenuRelaMapper.selectList(new LambdaQueryWrapper<SysRoleMenuRelaDO>().eq(SysRoleMenuRelaDO::getRoleId, roleId)))
                                                            .orElse(new ArrayList<>());
        //对比获取删除的菜单id集合 和新增的菜单id集合
        if (ObjectUtil.isNotEmpty(oldRelaMenus)) {
            List<Integer> oldData = oldRelaMenus.stream().map(SysRoleMenuRelaDO::getMenuId).toList();
            //oldData 中存在 menuIds不存在
            List<Integer> deleteList = CollUtil.subtract(oldData, menuIds).stream().toList();

            if (!deleteList.isEmpty()) {
                roleMenuRelaMapper.deleteBatchByMenuIds(deleteList);
            }

            List<Integer> addList = CollUtil.subtract(menuIds, oldData).stream().toList();
            if (!addList.isEmpty()) {
                batchInsertMenus(addList, roleId);
            }

        } else {
            if (ObjectUtil.isNotEmpty(menuIds)) {
                batchInsertMenus(menuIds, roleId);
            }
        }


        var sysRoleVO = new SysRoleVO();
        BeanUtil.copyProperties(sysRoleDO, sysRoleVO);

        return sysRoleVO;
    }


    /**
     * 查询 系统角色 列表
     *
     * @param queryParam 查询参数
     * @return QuerySysRoleRspDTO
     */
    @Override
    public QuerySysRoleRspDTO getSysRoleList(QuerySysRoleReqDTO queryParam) throws BlinkException {

        var pageRsp = new QuerySysRoleRspDTO();
        var role = new SysRoleDO();
        BeanUtil.copyProperties(queryParam, role);
        PageUtils.queryPage(queryParam, () -> sysRoleMapper.findSysRoleList(role), pageRsp);

        return pageRsp;
    }

    /**
     * 根据用户信息查询 用户角色
     *
     * @param queryParam 查询参数
     * @return {@link QueryUserRolesRspDTO}
     */
    @Override
    public QueryUserRolesRspDTO getSysRolesByUser(QueryUserRolesReqDTO queryParam) throws BlinkException {

        var queryUserRolesRspDTO = new QueryUserRolesRspDTO();

        List<SysRoleDO> roles = sysRoleMapper.findSysRolesByUser(queryParam);
        List<SysRoleVO> vos = new ArrayList<>();

        BeanUtil.copyProperties(roles, vos);
        queryUserRolesRspDTO.setRoles(vos);

        return queryUserRolesRspDTO;
    }

    /**
     * 批量插入 角色权限关联表
     *
     * @param permIds 权限id集合
     * @param roleId  角色id
     */
    private void batchInsertPermissions(List<Integer> permIds, Integer roleId) {
        List<SysRolePermRelaDO> addList = new ArrayList<>(permIds.size());

        permIds.forEach(pid -> {
            var rolePerm = new SysRolePermRelaDO();
            rolePerm.setRoleId(roleId);
            rolePerm.setAcId(pid);
            addList.add(rolePerm);
        });
        rolePermRelaMapper.batchInsert(addList);
    }

    /**
     * 批量插入角色菜单关联表
     *
     * @param menuIds 菜单id集合
     * @param roleId  角色id
     */
    private void batchInsertMenus(List<Integer> menuIds, Integer roleId) {

        List<SysRoleMenuRelaDO> list = menuIds.stream()
                .map(mid -> {
                    var temp = new SysRoleMenuRelaDO();
                    temp.setMenuId(mid);
                    temp.setRoleId(roleId);
                    return temp;
                }).collect(Collectors.toList());

        roleMenuRelaMapper.batchInsert(list);
    }

    /**
     * 判断集合数据 是否合法
     *
     * @param paramFormOutSide 外部传递的参数
     * @param select           sql
     * @return true 存在数据库 不存在的数据 false 合法
     */
    private boolean dataNotExistCheck(List<?> paramFormOutSide, Supplier<List<?>> select) {
        List<?> existList = select.get();
        if (existList != null && !existList.isEmpty()) {
            //存在非法菜单id
            return existList.size() != paramFormOutSide.size();
        }
        return true;
    }

}
