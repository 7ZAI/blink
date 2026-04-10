package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.constants.CommonConstants;
import com.blink.base.dto.req.*;
import com.blink.base.entity.*;
import com.blink.base.mapper.*;
import com.blink.base.dto.rsp.QuerySysRoleRsp;
import com.blink.base.dto.rsp.QueryUserRolesRsp;
import com.blink.base.dto.rsp.RoleDetailRsp;
import com.blink.base.dto.vo.SysMenuVO;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.dto.vo.SysRoleVO;
import com.blink.base.dto.vo.SysUserVO;

import com.blink.base.service.OnlineUserService;
import com.blink.base.service.SysRoleService;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private OnlineUserService onlineUserService;

    /**
     * 保存 系统角色
     *
     * @param saveParam 入参
     * @return SysRoleVO 显示信息
     */
    @Override
    public SysRoleVO saveSysRole(AddSysRoleReq saveParam) throws BlinkException {

        var sysRoleDO = new SysRoleDO();

        BeanUtil.copyProperties(saveParam, sysRoleDO);

        // 角色代码不允许重复
        Long codeCount = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRoleDO>()
                .eq(SysRoleDO::getRoleCode, sysRoleDO.getRoleCode()));
        if (codeCount > CommonConstants.LONG_ZERO) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_ALREADY_EXIT);
        }

        // 角色名称不允许重复
        Long nameCount = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRoleDO>()
                .eq(SysRoleDO::getRoleName, sysRoleDO.getRoleName()));
        if (nameCount > CommonConstants.LONG_ZERO) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_NAME_ALREADY_EXIT);
        }

        sysRoleMapper.insert(sysRoleDO);
        Integer roleId = sysRoleDO.getRoleId();

        //分配的权限id集合
        List<Integer> permIds = saveParam.getPermissionIds();
        if (ObjectUtil.isNotEmpty(permIds)) {
            // 验证前端菜单id是否都合法
            if (dataNotExistCheck(permIds, () -> permissionMapper.selectList(new LambdaQueryWrapper<SysPermissionDO>()
                    .in(SysPermissionDO::getAcId, permIds)))) {
                //存在非法权限id
                BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_NOT_EXIST);
            }
            batchInsertPermissions(permIds, roleId);
        }

        List<Integer> menuIds = saveParam.getMenuIds();
        if (ObjectUtil.isNotEmpty(menuIds)) {

            // 验证前端菜单id是否都合法
            if (dataNotExistCheck(menuIds, () -> sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuDO>()
                    .in(SysMenuDO::getMenuId, menuIds)))) {
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
    public void deleteSysRole(DeleteSysRoleReq deleteParam) throws BlinkException {

        // 检查是否包含超级管理员角色
        if (Boolean.TRUE.equals(deleteParam.getBatchDelete())) {
            if (deleteParam.getIdList().contains(CommonConstants.SUPER_ADMIN_ID)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.NOT_ALLOW_DELETE);
            }
        } else {
            if (deleteParam.getDeleteId().equals(CommonConstants.SUPER_ADMIN_ID)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.NOT_ALLOW_DELETE);
            }
        }

        if (Boolean.TRUE.equals(deleteParam.getBatchDelete())) {
            Long count = userRoleRelaMapper.selectCount(new LambdaQueryWrapper<SysUserRoleRelaDO>()
                    .in(SysUserRoleRelaDO::getRoleId, deleteParam.getIdList()));

            //存在关联数据 无法删除 只有未绑定任何用户的角色才能删除 或者将拥有角色的用户全部删除后 才能删除角色
            if (count.compareTo(CommonConstants.LONG_ZERO) > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_RELA_DATA);
            }

            sysRoleMapper.delete(new LambdaQueryWrapper<SysRoleDO>().in(SysRoleDO::getRoleId, deleteParam.getIdList()));
            // 批量删除角色权限关联
            rolePermRelaMapper.delete(new LambdaQueryWrapper<SysRolePermRelaDO>()
                    .in(SysRolePermRelaDO::getRoleId, deleteParam.getIdList()));
            // 批量删除角色菜单关联
            roleMenuRelaMapper.delete(new LambdaQueryWrapper<SysRoleMenuRelaDO>()
                    .in(SysRoleMenuRelaDO::getRoleId, deleteParam.getIdList()));
            log.info("[SysRole] 批量删除角色成功 | roleIds: {}", deleteParam.getIdList());
        } else {
            Long count = userRoleRelaMapper.selectCount(new LambdaQueryWrapper<SysUserRoleRelaDO>()
                    .eq(SysUserRoleRelaDO::getRoleId, deleteParam.getDeleteId()));
            //存在关联数据 无法删除
            if (count.compareTo(CommonConstants.LONG_ZERO) > 0) {
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
    public SysRoleVO modifySysRole(UpdateSysRoleReq updateParam) throws BlinkException {

        // 检查是否为超级管理员角色
        if (updateParam.getRoleId().equals(CommonConstants.SUPER_ADMIN_ID)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.SUPER_ADMIN_ROLE_NOT_ALLOW_UPDATE);
        }

        SysRoleDO sysRoleDold = sysRoleMapper.selectById(updateParam.getRoleId());

        //角色不存在
        if (ObjectUtil.isNull(sysRoleDold)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_NOT_EXIST);
        }

        // 角色名称不允许重复（排除自身）
        Long nameCount = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRoleDO>()
                .eq(SysRoleDO::getRoleName, updateParam.getRoleName())
                .ne(SysRoleDO::getRoleId, updateParam.getRoleId()));
        if (nameCount > CommonConstants.LONG_ZERO) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_NAME_ALREADY_EXIT);
        }

        SysRoleDO sysRoleDO = new SysRoleDO();
        BeanUtil.copyProperties(updateParam, sysRoleDO);

        sysRoleMapper.updateById(sysRoleDO);
        Integer roleId = sysRoleDold.getRoleId();

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
                // 验证新增权限ID是否合法
                if (dataNotExistCheck(addList, () -> permissionMapper.selectList(new LambdaQueryWrapper<SysPermissionDO>()
                        .in(SysPermissionDO::getAcId, addList)))) {
                    BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_NOT_EXIST);
                }
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
                // 验证新增菜单ID是否合法
                if (dataNotExistCheck(addList, () -> sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuDO>()
                        .in(SysMenuDO::getMenuId, addList)))) {
                    BlinkException.throwBusinessException(BaseErrCodeConstant.MENU_NOT_EXIST);
                }
                batchInsertMenus(addList, roleId);
            }

        } else {
            if (ObjectUtil.isNotEmpty(menuIds)) {
                batchInsertMenus(menuIds, roleId);
            }
        }

        // 强制拥有该角色的在线用户重新登录
        kickoutOnlineUsersByRoleId(roleId);
        log.info("[SysRole] 更新角色成功，已强制相关用户重新登录 | roleId: {}", roleId);

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
    public QuerySysRoleRsp getSysRoleList(QuerySysRoleReq queryParam) throws BlinkException {

        var pageRsp = new QuerySysRoleRsp();
        var role = new SysRoleDO();
        BeanUtil.copyProperties(queryParam, role);
        PageUtils.queryPage(queryParam, () -> sysRoleMapper.findSysRoleList(role), pageRsp);

        return pageRsp;
    }

    /**
     * 根据用户信息查询 用户角色
     *
     * @param queryParam 查询参数
     * @return {@link QueryUserRolesRsp}
     */
    @Override
    public QueryUserRolesRsp getSysRolesByUser(QueryUserRolesReq queryParam) throws BlinkException {

        var queryUserRolesRspDTO = new QueryUserRolesRsp();

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

    /**
     * 为角色分配权限
     *
     * @param assignParam 分配参数
     */
    @Override
    public void assignPermissions(AssignPermissionReq assignParam) throws BlinkException {
        Integer roleId = assignParam.getRoleId();

        // 检查是否为超级管理员角色
        if (roleId.equals(CommonConstants.SUPER_ADMIN_ID)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.SUPER_ADMIN_ROLE_NOT_ALLOW_UPDATE);
        }

        List<Integer> permissionIds = Optional.ofNullable(assignParam.getPermissionIds()).orElse(new ArrayList<>());

        // 验证角色是否存在
        SysRoleDO roleDO = sysRoleMapper.selectById(roleId);
        if (ObjectUtil.isNull(roleDO)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_NOT_EXIST);
        }

        // 验证权限ID是否合法
        if (ObjectUtil.isNotEmpty(permissionIds)) {
            if (dataNotExistCheck(permissionIds, () -> permissionMapper.selectList(new LambdaQueryWrapper<SysPermissionDO>()
                    .in(SysPermissionDO::getAcId, permissionIds)))) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_NOT_EXIST);
            }
        }

        // 删除原有权限关联
        rolePermRelaMapper.delete(new LambdaQueryWrapper<SysRolePermRelaDO>()
                .eq(SysRolePermRelaDO::getRoleId, roleId));

        // 插入新的权限关联
        if (ObjectUtil.isNotEmpty(permissionIds)) {
            batchInsertPermissions(permissionIds, roleId);
        }

        // 强制拥有该角色的在线用户重新登录
        kickoutOnlineUsersByRoleId(roleId);
        log.info("[SysRole] 分配权限成功，已强制相关用户重新登录 | roleId: {}", roleId);
    }

    /**
     * 为角色分配菜单
     *
     * @param assignParam 分配参数
     */
    @Override
    public void assignMenus(AssignMenuReq assignParam) throws BlinkException {
        Integer roleId = assignParam.getRoleId();

        // 检查是否为超级管理员角色
        if (roleId.equals(CommonConstants.SUPER_ADMIN_ID)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.SUPER_ADMIN_ROLE_NOT_ALLOW_UPDATE);
        }

        List<Integer> menuIds = Optional.ofNullable(assignParam.getMenuIds()).orElse(new ArrayList<>());

        // 验证角色是否存在
        SysRoleDO roleDO = sysRoleMapper.selectById(roleId);
        if (ObjectUtil.isNull(roleDO)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_NOT_EXIST);
        }

        // 验证菜单ID是否合法
        if (ObjectUtil.isNotEmpty(menuIds)) {
            if (dataNotExistCheck(menuIds, () -> sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuDO>()
                    .in(SysMenuDO::getMenuId, menuIds)))) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.MENU_NOT_EXIST);
            }
        }

        // 删除原有菜单关联
        roleMenuRelaMapper.delete(new LambdaQueryWrapper<SysRoleMenuRelaDO>()
                .eq(SysRoleMenuRelaDO::getRoleId, roleId));

        // 插入新的菜单关联
        if (CollUtil.isNotEmpty(menuIds)) {
            batchInsertMenus(menuIds, roleId);
        }

        // 提取菜单关联的接口权限ID
        List<Integer> permIds = Collections.emptyList();
        if (CollUtil.isNotEmpty(menuIds)) {
            List<SysMenuDO> menus = sysMenuMapper.selectByIds(menuIds);
            permIds = menus.stream()
                    .filter(menu -> ObjectUtil.isNotNull(menu.getPermId()))
                    .map(SysMenuDO::getPermId)
                    .distinct()
                    .toList();
        }

        // 删除原有的接口权限关联（仅 ac_type=1）
        rolePermRelaMapper.deleteApiPermissionsByRoleId(roleId);

        // 插入新的接口权限关联
        if (CollUtil.isNotEmpty(permIds)) {
            batchInsertPermissions(permIds, roleId);
            log.info("[SysRole] 分配菜单自动关联接口权限 | roleId: {}, permIds: {}", roleId, permIds);
        }

        // 强制拥有该角色的在线用户重新登录
        kickoutOnlineUsersByRoleId(roleId);
        log.info("[SysRole] 分配菜单成功，已强制相关用户重新登录 | roleId: {}", roleId);
    }

    /**
     * 查询角色详情
     *
     * @param queryParam 查询参数
     * @return 角色详情
     */
    @Override
    public RoleDetailRsp getRoleDetail(QueryRoleDetailReq queryParam) throws BlinkException {
        Integer roleId = queryParam.getRoleId();

        // 查询角色基本信息
        SysRoleDO roleDO = sysRoleMapper.selectById(roleId);
        if (ObjectUtil.isNull(roleDO)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_NOT_EXIST);
        }

        RoleDetailRsp detailRsp = new RoleDetailRsp();

        // 设置角色基本信息
        SysRoleVO roleVO = new SysRoleVO();
        BeanUtil.copyProperties(roleDO, roleVO);
        detailRsp.setRoleInfo(roleVO);

        // 查询已授权的权限列表
        List<SysRolePermRelaDO> permRelas = rolePermRelaMapper.selectList(
                new LambdaQueryWrapper<SysRolePermRelaDO>().eq(SysRolePermRelaDO::getRoleId, roleId));
        if (ObjectUtil.isNotEmpty(permRelas)) {
            List<Integer> permIds = permRelas.stream().map(SysRolePermRelaDO::getAcId).toList();
            List<SysPermissionDO> permDos = permissionMapper.selectList(new LambdaQueryWrapper<SysPermissionDO>()
                    .in(SysPermissionDO::getAcId, permIds));
            List<SysPermissionVO> permVos = BeanUtil.copyToList(permDos, SysPermissionVO.class);
            detailRsp.setPermissions(permVos);
        }

        // 查询已分配的菜单列表（包含父菜单链）
        List<SysRoleMenuRelaDO> menuRelas = roleMenuRelaMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenuRelaDO>().eq(SysRoleMenuRelaDO::getRoleId, roleId));
        if (ObjectUtil.isNotEmpty(menuRelas)) {
            List<Integer> menuIdList = menuRelas.stream().map(SysRoleMenuRelaDO::getMenuId).toList();
            // 查询所有菜单（包含父菜单链）
            Set<Integer> allMenuIds = new HashSet<>(menuIdList);
            collectParentMenuIds(menuIdList, allMenuIds);
            List<SysMenuDO> menuDos = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuDO>()
                    .in(SysMenuDO::getMenuId, allMenuIds));
            List<SysMenuVO> menuVos = BeanUtil.copyToList(menuDos, SysMenuVO.class);
            detailRsp.setMenus(menuVos);
        }

        // 查询拥有该角色的用户列表
        List<SysUserRoleRelaDO> userRoleRelas = userRoleRelaMapper.selectList(
                new LambdaQueryWrapper<SysUserRoleRelaDO>().eq(SysUserRoleRelaDO::getRoleId, roleId));
        if (ObjectUtil.isNotEmpty(userRoleRelas)) {
            List<Integer> userIds = userRoleRelas.stream().map(SysUserRoleRelaDO::getUserId).toList();
            List<SysUserDO> userDOs = sysUserMapper.selectByIds(userIds);
            List<SysUserVO> userVOs = BeanUtil.copyToList(userDOs, SysUserVO.class);
            detailRsp.setUsers(userVOs);
        }

        return detailRsp;
    }

    /**
     * 递归收集所有父菜单ID
     *
     * @param menuIds    当前菜单ID列表
     * @param allMenuIds 收集到的所有菜单ID集合
     */
    private void collectParentMenuIds(List<Integer> menuIds, Set<Integer> allMenuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }

        // 查询当前菜单的父菜单ID
        List<SysMenuDO> menus = sysMenuMapper.selectByIds(menuIds);
        List<Integer> parentIds = menus.stream()
                .map(SysMenuDO::getParentId)
                .filter(ObjectUtil::isNotNull)
                .filter(id -> id > 0)
                .filter(id -> !allMenuIds.contains(id))
                .toList();

        if (CollUtil.isNotEmpty(parentIds)) {
            allMenuIds.addAll(parentIds);
            // 递归收集父菜单的父菜单
            collectParentMenuIds(parentIds, allMenuIds);
        }
    }

    /**
     * 为用户分配角色
     *
     * @param assignParam 分配参数
     */
    @Override
    public void assignRoleToUsers(AssignRoleToUsersReq assignParam) throws BlinkException {
        Integer roleId = assignParam.getRoleId();
        List<Integer> userIds = Optional.ofNullable(assignParam.getUserIds()).orElse(new ArrayList<>());

        // 验证角色是否存在
        SysRoleDO roleDO = sysRoleMapper.selectById(roleId);
        if (ObjectUtil.isNull(roleDO)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_NOT_EXIST);
        }

        // 验证用户ID是否合法
        if (ObjectUtil.isNotEmpty(userIds)) {
            List<SysUserDO> existUsers = sysUserMapper.selectByIds(userIds);
            if (existUsers.size() != userIds.size()) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIST);
            }
        }

        // 查询已有该角色的用户
        List<SysUserRoleRelaDO> existingRelas = userRoleRelaMapper.selectList(
                new LambdaQueryWrapper<SysUserRoleRelaDO>().eq(SysUserRoleRelaDO::getRoleId, roleId));
        Set<Integer> existingUserIds = existingRelas.stream()
                .map(SysUserRoleRelaDO::getUserId)
                .collect(Collectors.toSet());

        // 过滤出需要新增的用户ID（排除已有该角色的用户）
        List<Integer> newUserIds = userIds.stream()
                .filter(userId -> !existingUserIds.contains(userId))
                .toList();

        // 批量插入新的用户角色关联
        if (CollUtil.isNotEmpty(newUserIds)) {
            List<SysUserRoleRelaDO> relaList = newUserIds.stream()
                    .map(userId -> {
                        SysUserRoleRelaDO rela = new SysUserRoleRelaDO();
                        rela.setUserId(userId);
                        rela.setRoleId(roleId);
                        return rela;
                    })
                    .collect(Collectors.toList());
            userRoleRelaMapper.batchInsert(relaList);

            // 强制新分配角色的用户重新登录以刷新权限
            onlineUserService.kickoutUsersByUserIds(newUserIds);
            log.info("[SysRole] 为用户分配角色成功，已强制用户重新登录 | roleId: {}, userIds: {}", roleId, newUserIds);
        }
    }

    /**
     * 根据角色ID强制拥有该角色的在线用户下线
     *
     * @param roleId 角色ID
     */
    private void kickoutOnlineUsersByRoleId(Integer roleId) {
        // 查询拥有该角色的用户
        List<SysUserRoleRelaDO> userRoleRelas = userRoleRelaMapper.selectList(
                new LambdaQueryWrapper<SysUserRoleRelaDO>().eq(SysUserRoleRelaDO::getRoleId, roleId));
        if (CollUtil.isNotEmpty(userRoleRelas)) {
            List<Integer> userIds = userRoleRelas.stream()
                    .map(SysUserRoleRelaDO::getUserId)
                    .toList();
            onlineUserService.kickoutUsersByUserIds(userIds);
        }
    }
}
