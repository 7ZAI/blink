package com.blink.gateway.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.base.constants.BaseErrCodeConstant;
import com.blink.gateway.base.constants.CommonConstants;
import com.blink.gateway.base.dto.req.*;
import com.blink.gateway.base.dto.rsp.SysUserRsp;
import com.blink.gateway.base.dto.rsp.UserPermissionRsp;
import com.blink.gateway.base.dto.vo.*;
import com.blink.gateway.base.entity.*;
import com.blink.gateway.base.mapper.*;
import com.blink.gateway.base.service.OnlineUserService;
import com.blink.gateway.base.service.SysConfigService;
import com.blink.gateway.base.service.SysUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 系统用户 服务实现类
 * </p>
 *
 * @author binblink
 */
@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
public class SysUserServiceImpl implements SysUserService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper roleMapper;
    

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private OnlineUserService onlineUserService;

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysRoleMenuRelaMapper sysRoleMenuRelaMapper;

    @Resource
    private SysRolePermRelaMapper sysRolePermRelaMapper;

    @Resource
    private SysUserRoleRelaMapper sysUserRoleRelaMapper;

    /**
     * 保存 系统用户
     *
     * @param saveParam 用户参数
     */
    @Override
    public void saveSysUser(AddSysUserReq saveParam) throws BlinkException {


        var sysUserDO = new SysUserDO();
        BeanUtil.copyProperties(saveParam, sysUserDO);

        //loginName 不能重复
        Long existOne = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserDO>().eq(SysUserDO::getLoginName, sysUserDO.getLoginName()));

        if (existOne > CommonConstants.LONG_ZERO) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.LOGIN_NAME_REPEAT);
        }
        //角色是否都存在
        List<Integer> roles = saveParam.getRoles();
        if (Objects.nonNull(roles) && !roles.isEmpty()) {
            List<SysRoleDO> existRoles = roleMapper.selectList(new LambdaQueryWrapper<SysRoleDO>().in(SysRoleDO::getRoleId, roles));
            if (existRoles.size() != roles.size()) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_NOT_EXIST);
            }
        }

    
        var config = new QueryOneSysConfigReq();
        config.setConfigKey(CommonConstants.SysConfigKeys.USER_INIT_PASSWORD);
        // 从配置中获取默认密码
        SysConfigVO pswConf = Optional.ofNullable(sysConfigService.getOneConfigFromCacheOrDataBase(config)).orElseGet(SysConfigVO::new);

        // 如果配置中没有设置默认密码，使用系统默认值
        String defaultPassword = (pswConf != null && !pswConf.getConfigValue().isEmpty()) ? pswConf.getConfigValue() : CommonConstants.DEFAULT_USER_PASSWORD;

        // 生成盐值并加密密码
        sysUserDO.setSalt(BCrypt.gensalt());
        String encodePassword = BCrypt.hashpw(defaultPassword, sysUserDO.getSalt());
        sysUserDO.setPassword(encodePassword);

        config.setConfigKey(CommonConstants.SysConfigKeys.USER_DEFAULT_AVATAR_STYLE);
        SysConfigVO avatarConf = Optional.ofNullable(sysConfigService.getOneConfigFromCacheOrDataBase(config)).orElseGet(SysConfigVO::new);

        // 从配置中获取默认头像样式
        String defaultAvatarStyle = (avatarConf != null && !avatarConf.getConfigValue().isEmpty()) ? avatarConf.getConfigValue() : CommonConstants.DEFAULT_USER_AVATAR;

        // 设置首次登录需要重置密码标识
        sysUserDO.setPasswordReset(CommonConstants.SUPER_ADMIN_YES);

        sysUserDO.setUpdateBy(saveParam.getLoginName());
        sysUserMapper.insert(sysUserDO);



        List<SysUserRoleRelaDO> roleUsers = new ArrayList<>();

        if (roles != null && !roles.isEmpty()) {
            for (Integer roleId : roles) {
                var roleUser = new SysUserRoleRelaDO();
                roleUser.setRoleId(roleId);
                roleUser.setUserId(sysUserDO.getUserId());
                roleUsers.add(roleUser);
            }
            sysUserRoleRelaMapper.batchInsert(roleUsers);
        }

    }

    /**
     * 删除 系统用户
     *
     * @param deleteParam 删除参数
     */
    @Override
    public void deleteSysUser(DeleteSysUserReq deleteParam) throws BlinkException {

        if (Boolean.TRUE.equals(deleteParam.getBatchDelete())) {
            List<Integer> delList = deleteParam.getUserIdList();
            if (CollUtil.isEmpty(delList)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PARAMETER_NOT_NULL);
            }
            List<SysUserDO> existIds = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserDO>()
                    .in(SysUserDO::getUserId, delList));

            //存在非法id
            if (delList.size() != existIds.size()) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIST);
            }

            List<SysUserDO> superAdminUser = existIds.stream().filter(u -> u.getSuperFlag().equals(CommonConstants.SUPER_ADMIN_ID)).toList();
            //包含超级管理员 无法删除
            if (!superAdminUser.isEmpty()) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.NOT_ALLOW_DELETE);
            }

            // 先踢出在线用户
            onlineUserService.kickoutUsersByUserIds(delList);

            sysUserMapper.delete(new LambdaQueryWrapper<SysUserDO>().in(SysUserDO::getUserId, delList));
            sysUserRoleRelaMapper.delete(new LambdaQueryWrapper<SysUserRoleRelaDO>().in(SysUserRoleRelaDO::getUserId, delList));

            log.info("[SysUser] 批量删除用户成功 | userIdList: {}", delList);

        } else {
            Integer userId = deleteParam.getUserId();
            if (userId == null) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PARAMETER_NOT_NULL);
            }
            SysUserDO user = sysUserMapper.selectById(userId);
            // 不存在的userId
            if (Objects.isNull(user)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIST);
            }
            //超级管理员 无法删除
            if (user.getSuperFlag().equals(CommonConstants.SUPER_ADMIN_ID)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.NOT_ALLOW_DELETE);
            }

            // 先踢出在线用户
            onlineUserService.kickoutUsersByUserIds(List.of(userId));

            sysUserMapper.deleteById(userId);
            sysUserRoleRelaMapper.deleteById(userId);

            log.info("[SysUser] 删除用户成功 | userId: {}, loginName: {}", userId, user.getLoginName());
        }
    }

    /**
     * 更新 系统用户
     *
     * @param updateParam 入参
     */
    @Override
    public void modifySysUser(UpdateSysUserReq updateParam) throws BlinkException {

        Integer userId = updateParam.getUserId();

        SysUserDO sysUserDO = sysUserMapper.selectById(userId);

        //用户不存在
        if (ObjectUtil.isEmpty(sysUserDO)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIST);
        }

        // 如果修改的用户是超级管理员，校验当前登录用户是否为超级管理员
        if (CommonConstants.SUPER_ADMIN_YES.equals(sysUserDO.getSuperFlag())) {
            String currentLoginName = BlinkRequestContextHolder.getLoginName();
            SysUserDO currentUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserDO>()
                    .eq(SysUserDO::getLoginName, currentLoginName));

            if (currentUser == null || !CommonConstants.SUPER_ADMIN_YES.equals(currentUser.getSuperFlag())) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.ONLY_SUPER_ADMIN_CAN_MODIFY);
            }
        }

        // 校验角色是否存在
        List<Integer> updateRoleIdList = updateParam.getRoleIdList();
        if (CollUtil.isNotEmpty(updateRoleIdList)) {
            List<SysRoleDO> existRoles = roleMapper.selectRoleListByIds(updateRoleIdList);
            if (existRoles.size() != updateRoleIdList.size()) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_NOT_EXIST);
            }
        }


        List<SysUserRoleRelaDO> userRolesList = sysUserRoleRelaMapper.selectList(new LambdaQueryWrapper<SysUserRoleRelaDO>()
                .eq(SysUserRoleRelaDO::getUserId, userId));



        BeanUtil.copyProperties(updateParam, sysUserDO);
        sysUserDO.setUpdateBy(BlinkRequestContextHolder.getLoginName());

        List<Integer> roleIdList = new ArrayList<Integer>();
        List<Integer> groupIdList = new ArrayList<>();

        if (CollUtil.isNotEmpty(userRolesList)) {
            roleIdList = userRolesList.stream().map(SysUserRoleRelaDO::getRoleId).collect(Collectors.toList());
        }

        //判断是否相同 相同则不更新
        if (!CollUtil.isEqualList(updateRoleIdList, roleIdList)) {
            //删除所有角色关联
            sysUserRoleRelaMapper.delete(new LambdaQueryWrapper<SysUserRoleRelaDO>().eq(SysUserRoleRelaDO::getUserId, userId));
            // 插入新的角色关联
            if (CollUtil.isNotEmpty(updateRoleIdList)) {
                List<SysUserRoleRelaDO> list = new ArrayList<>(updateRoleIdList.size());
                updateRoleIdList.forEach(roleId -> {

                    SysUserRoleRelaDO newUserRoleRela = new SysUserRoleRelaDO();
                    newUserRoleRela.setUserId(userId);
                    newUserRoleRela.setRoleId(roleId);
                    list.add(newUserRoleRela);
                });
                sysUserRoleRelaMapper.batchInsert(list);
            }

        }



        sysUserMapper.updateById(sysUserDO);
        log.info("[SysUser] 更新用户成功 | userId: {}, loginName: {}", userId, sysUserDO.getLoginName());

    }

    /**
     * 查询 系统用户 列表
     *
     * @param queryParam 查询条件参数
     * @return 分页封装 SysUserRspDTO<SysUserVO>
     */
    @Override
    public SysUserRsp getSysUserList(QuerySysUserReq queryParam) throws BlinkException {

        // 判断当前登录用户是否为超级管理员
        String currentLoginName = BlinkRequestContextHolder.getLoginName();
        SysUserDO currentUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getLoginName, currentLoginName));

        // 如果当前用户不是超级管理员，则排除超级管理员用户
        if (currentUser == null || !CommonConstants.SUPER_ADMIN_YES.equals(currentUser.getSuperFlag())) {
            queryParam.setExcludeSuperAdmin(true);
        }

        var sysUserRspDTO = new SysUserRsp();
        PageUtils.queryPage(queryParam, () -> sysUserMapper.findSysUserList(queryParam), sysUserRspDTO);
        return sysUserRspDTO;
    }





    /**
     * 查询 系统用户 详情
     *
     * @param queryParam 入参
     * @return 用户详情信息
     */
    @Override
    public SysUserVO getSysUserDetail(QuerySysUserReq queryParam) throws BlinkException {
        return sysUserMapper.findUserDetail(queryParam);
    }

    /**
     * 锁定/解锁用户
     *
     * @param userId 用户ID
     * @param locked 锁定状态 0正常 1锁定
     * @throws BlinkException
     */
    @Override
    public void lockUser(Integer userId, Integer locked) throws BlinkException {
        SysUserDO sysUserDO = sysUserMapper.selectById(userId);
        if (ObjectUtil.isEmpty(sysUserDO)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIST);
        }
        if (sysUserDO.getSuperFlag().equals(CommonConstants.SUPER_ADMIN_ID)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.NOT_ALLOW_DELETE);
        }
        sysUserDO.setLocked(locked);
        sysUserDO.setUpdateBy(BlinkRequestContextHolder.getLoginName());
        sysUserMapper.updateById(sysUserDO);
    }

    /**
     * 批量分配用户角色
     *
     * @param assignParam 分配参数
     * @throws BlinkException
     */
    @Override
    public void assignUserRoles(AssignUserRoleReq assignParam) throws BlinkException {
        List<Integer> userIdList = assignParam.getUserIdList();
        List<Integer> roleIdList = assignParam.getRoleIdList();

        // 验证用户是否存在
        List<SysUserDO> users = sysUserMapper.selectUserListByIds(userIdList);
        if (users.size() != userIdList.size()) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIST);
        }

        // 验证角色是否存在
        if (CollUtil.isNotEmpty(roleIdList)) {
            List<SysRoleDO> roles = roleMapper.selectRoleListByIds(roleIdList);
            if (roles.size() != roleIdList.size()) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.ROLE_NOT_EXIST);
            }
        }

        // 检查是否包含超级管理员角色
        if (CollUtil.isNotEmpty(roleIdList) && roleIdList.contains(CommonConstants.SUPER_ADMIN_ID)) {
            // 获取当前登录用户
            String currentLoginName = BlinkRequestContextHolder.getLoginName();
            SysUserDO currentUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserDO>()
                    .eq(SysUserDO::getLoginName, currentLoginName));
            
            // 只有超级管理员才能分配超级管理员角色
            if (currentUser == null || !CommonConstants.SUPER_ADMIN_YES.equals(currentUser.getSuperFlag())) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.ONLY_SUPER_ADMIN_CAN_ASSIGN);
            }
        }

        // 批量删除原有角色关联
        sysUserRoleRelaMapper.delete(new LambdaQueryWrapper<SysUserRoleRelaDO>()
                .in(SysUserRoleRelaDO::getUserId, userIdList));

        // 批量插入新的角色关联
        if (CollUtil.isNotEmpty(roleIdList)) {
            List<SysUserRoleRelaDO> relaList = new ArrayList<>(userIdList.size() * roleIdList.size());
            for (Integer userId : userIdList) {
                for (Integer roleId : roleIdList) {
                    SysUserRoleRelaDO rela = new SysUserRoleRelaDO();
                    rela.setUserId(userId);
                    rela.setRoleId(roleId);
                    relaList.add(rela);
                }
            }
            sysUserRoleRelaMapper.batchInsert(relaList);
        }

        // 强制用户重新登录以刷新权限
        onlineUserService.kickoutUsersByUserIds(userIdList);
        log.info("[SysUser] 分配用户角色成功，已强制用户重新登录 | userIdList: {}, roleIdList: {}", userIdList, roleIdList);
    }

    /**
     * 修改当前登录用户密码
     *
     * @param modifyPasswordParam 修改密码参数
     * @throws BlinkException
     */
    @Override
    public void modifyPassword(ModifyPasswordReq modifyPasswordParam) throws BlinkException {
        // 验证新密码和确认密码是否一致
        if (!modifyPasswordParam.getNewPassword().equals(modifyPasswordParam.getConfirmPassword())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PASSWORD_CONFIRM_ERR);
        }

        // 获取当前登录用户
        String currentLoginName = BlinkRequestContextHolder.getLoginName();
        SysUserDO currentUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getLoginName, currentLoginName));

        if (currentUser == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIST);
        }

        // 验证旧密码是否正确
        String oldPasswordHash = BCrypt.hashpw(modifyPasswordParam.getOldPassword(), currentUser.getSalt());
        if (!oldPasswordHash.equals(currentUser.getPassword())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.INCORRECT_PASSWORD);
        }

        // 生成新盐值并加密新密码
        String newSalt = BCrypt.gensalt();
        String newPasswordHash = BCrypt.hashpw(modifyPasswordParam.getNewPassword(), newSalt);

        // 更新用户密码
        currentUser.setPassword(newPasswordHash);
        currentUser.setSalt(newSalt);
        currentUser.setPasswordReset(CommonConstants.SUPER_ADMIN_NO);
        currentUser.setUpdateBy(currentLoginName);
        sysUserMapper.updateById(currentUser);
    }

    /**
     * 管理员重置用户密码
     *
     * @param resetPasswordParam 重置密码参数
     * @throws BlinkException
     */
    @Override
    public void resetPassword(ResetPasswordReq resetPasswordParam) throws BlinkException {
        Integer userId = resetPasswordParam.getUserId();

        // 查询用户
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIST);
        }

        // 超级管理员不允许被重置密码
        if (CommonConstants.SUPER_ADMIN_YES.equals(user.getSuperFlag())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.SUPER_ADMIN_NOT_ALLOW_RESET);
        }

        // 生成新盐值并加密新密码
        String newSalt = BCrypt.gensalt();
        String newPasswordHash = BCrypt.hashpw(resetPasswordParam.getNewPassword(), newSalt);

        // 更新用户密码
        user.setPassword(newPasswordHash);
        user.setSalt(newSalt);
        user.setUpdateBy(BlinkRequestContextHolder.getLoginName());
        sysUserMapper.updateById(user);
    }

    /**
     * 获取用户权限信息
     *
     * @param reqParam 用户ID请求参数
     * @return 用户权限信息（角色、菜单、权限）
     * @throws BlinkException 业务异常
     */
    @Override
    public UserPermissionRsp getUserPermissions(UserIdReq reqParam) throws BlinkException {
        Integer userId = Integer.valueOf(reqParam.getUserId());

        // 查询用户是否存在
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIST);
        }

        UserPermissionRsp rsp = new UserPermissionRsp();

        // 超级管理员拥有所有权限
        if (CommonConstants.SUPER_ADMIN_YES.equals(user.getSuperFlag())) {
            // 查询所有角色
            List<SysRoleDO> allRoles = roleMapper.selectList(
                    new LambdaQueryWrapper<SysRoleDO>().eq(SysRoleDO::getStatus, 0));
            rsp.setRoles(BeanUtil.copyToList(allRoles, SysRoleVO.class));

            // 查询所有菜单
            List<SysMenuDO> allMenus = sysMenuMapper.selectList(
                    new LambdaQueryWrapper<SysMenuDO>().eq(SysMenuDO::getStatus, 0));
            rsp.setMenus(BeanUtil.copyToList(allMenus, SysMenuVO.class));

            // 查询所有权限
            List<SysPermissionDO> allPermissions = sysPermissionMapper.selectList(null);
            rsp.setPermissions(BeanUtil.copyToList(allPermissions, SysPermissionVO.class));

            return rsp;
        }

        // 查询用户关联的角色
        List<SysUserRoleRelaDO> userRoleRelas = sysUserRoleRelaMapper.selectList(
                new LambdaQueryWrapper<SysUserRoleRelaDO>().eq(SysUserRoleRelaDO::getUserId, userId));

        if (CollUtil.isEmpty(userRoleRelas)) {
            rsp.setRoles(new ArrayList<>());
            rsp.setMenus(new ArrayList<>());
            rsp.setPermissions(new ArrayList<>());
            return rsp;
        }

        List<Integer> roleIds = userRoleRelas.stream()
                .map(SysUserRoleRelaDO::getRoleId)
                .toList();

        // 查询角色信息
        List<SysRoleDO> roles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRoleDO>()
                        .in(SysRoleDO::getRoleId, roleIds)
                        .eq(SysRoleDO::getStatus, 0));
        rsp.setRoles(BeanUtil.copyToList(roles, SysRoleVO.class));

        // 查询角色关联的菜单ID
        List<SysRoleMenuRelaDO> menuRelas = sysRoleMenuRelaMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenuRelaDO>().in(SysRoleMenuRelaDO::getRoleId, roleIds));

        if (CollUtil.isNotEmpty(menuRelas)) {
            Set<Integer> menuIds = new HashSet<>();
            menuRelas.forEach(rela -> menuIds.add(rela.getMenuId()));

            // 收集父菜单ID链
            collectParentMenuIds(new ArrayList<>(menuIds), menuIds);

            // 查询菜单信息
            List<SysMenuDO> menus = sysMenuMapper.selectList(
                    new LambdaQueryWrapper<SysMenuDO>()
                            .in(SysMenuDO::getMenuId, menuIds)
                            .eq(SysMenuDO::getStatus, 0));
            rsp.setMenus(BeanUtil.copyToList(menus, SysMenuVO.class));
        } else {
            rsp.setMenus(new ArrayList<>());
        }

        // 查询角色关联的权限ID
        List<SysRolePermRelaDO> permRelas = sysRolePermRelaMapper.selectList(
                new LambdaQueryWrapper<SysRolePermRelaDO>().in(SysRolePermRelaDO::getRoleId, roleIds));

        if (CollUtil.isNotEmpty(permRelas)) {
            Set<Integer> permIds = permRelas.stream()
                    .map(SysRolePermRelaDO::getAcId)
                    .collect(Collectors.toSet());

            // 查询权限信息
            List<SysPermissionDO> permissions = sysPermissionMapper.selectList(
                    new LambdaQueryWrapper<SysPermissionDO>().in(SysPermissionDO::getAcId, permIds));
            rsp.setPermissions(BeanUtil.copyToList(permissions, SysPermissionVO.class));
        } else {
            rsp.setPermissions(new ArrayList<>());
        }

        return rsp;
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
}
