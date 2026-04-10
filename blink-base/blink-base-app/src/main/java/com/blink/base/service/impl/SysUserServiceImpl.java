package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.constants.CommonConstants;
import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.SysUserRsp;
import com.blink.base.dto.rsp.UserPermissionRsp;
import com.blink.base.dto.vo.SysConfigVO;
import com.blink.base.dto.vo.SysMenuVO;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.dto.vo.SysRoleVO;
import com.blink.base.dto.vo.SysUserVO;
import com.blink.base.entity.*;
import com.blink.base.mapper.*;
import com.blink.base.service.OnlineUserService;
import com.blink.base.service.SysConfigService;
import com.blink.base.service.SysUserService;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.exception.BlinkException;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private SysGroupMapper sysGroupMapper;

    @Resource
    private SysUserRoleRelaMapper sysUserRoleRelaMapper;

    @Resource
    private SysUserGroupRelaMapper sysUserGroupRelaMapper;

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

        //组织是否存在
        Integer gid = saveParam.getGroupId();
        if (Objects.nonNull(gid)) {
            boolean existGroup = sysGroupMapper.exists(new LambdaQueryWrapper<SysGroupDO>().eq(SysGroupDO::getGroupId, gid));
            if (!existGroup) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.GROUP_NOT_EXIST);
            }
        }
        var config = new QueryOneSysConfigReq();
        config.setConfigKey(CommonConstants.SysConfigKeys.USER_INIT_PASSWORD);
        // 从配置中获取默认密码
        SysConfigVO pswConf = Optional.ofNullable(sysConfigService.getOneConfigFromCacheOrDataBase(config)).orElseGet(SysConfigVO::new);

        // 如果配置中没有设置默认密码，使用系统默认值
        String defaultPassword = (pswConf != null && !pswConf.getConfigValue().isEmpty()) ? pswConf.getConfigValue() : CommonConstants.DEFAULT_USER_PASSWORD;

        // 使用标准 BCrypt 加密密码（salt 已包含在 hash 结果中）
        String encodePassword = BCrypt.hashpw(defaultPassword, BCrypt.gensalt());
        sysUserDO.setPassword(encodePassword);

        config.setConfigKey(CommonConstants.SysConfigKeys.USER_DEFAULT_AVATAR_STYLE);
        SysConfigVO avatarConf = Optional.ofNullable(sysConfigService.getOneConfigFromCacheOrDataBase(config)).orElseGet(SysConfigVO::new);

        // 从配置中获取默认头像样式
        String defaultAvatarStyle = (avatarConf != null && !avatarConf.getConfigValue().isEmpty()) ? avatarConf.getConfigValue() : CommonConstants.DEFAULT_USER_AVATAR;

        // 设置首次登录需要重置密码标识
        sysUserDO.setPasswordReset(CommonConstants.SUPER_ADMIN_YES);

        sysUserDO.setUpdateBy(saveParam.getLoginName());
        sysUserMapper.insert(sysUserDO);

        if (Objects.nonNull(gid)) {
            var groupUser = new SysUserGroupRelaDO();
            groupUser.setGroupId(gid);
            groupUser.setUserId(sysUserDO.getUserId());
            sysUserGroupRelaMapper.insert(groupUser);
        }

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
            sysUserGroupRelaMapper.delete(new LambdaQueryWrapper<SysUserGroupRelaDO>().in(SysUserGroupRelaDO::getUserId, delList));

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
            sysUserGroupRelaMapper.deleteById(userId);

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

        // 校验组织是否存在
        List<Integer> updateGroupIdList = updateParam.getGroupIdList();
        if (CollUtil.isNotEmpty(updateGroupIdList)) {
            List<SysGroupDO> existGroups = sysGroupMapper.selectList(new LambdaQueryWrapper<SysGroupDO>()
                    .in(SysGroupDO::getGroupId, updateGroupIdList));
            if (existGroups.size() != updateGroupIdList.size()) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.GROUP_NOT_EXIST);
            }
        }

        List<SysUserRoleRelaDO> userRolesList = sysUserRoleRelaMapper.selectList(new LambdaQueryWrapper<SysUserRoleRelaDO>()
                .eq(SysUserRoleRelaDO::getUserId, userId));

        List<SysUserGroupRelaDO> userGroupList = sysUserGroupRelaMapper.selectList(new LambdaQueryWrapper<SysUserGroupRelaDO>()
                .eq(SysUserGroupRelaDO::getUserId, userId));

        BeanUtil.copyProperties(updateParam, sysUserDO);
        sysUserDO.setUpdateBy(BlinkRequestContextHolder.getLoginName());

        List<Integer> roleIdList = new ArrayList<Integer>();
        List<Integer> groupIdList = new ArrayList<>();

        if (CollUtil.isNotEmpty(userRolesList)) {
            roleIdList = userRolesList.stream().map(SysUserRoleRelaDO::getRoleId).collect(Collectors.toList());
        }

        if (CollUtil.isNotEmpty(userGroupList)) {
            groupIdList = userGroupList.stream().map(SysUserGroupRelaDO::getGroupId).collect(Collectors.toList());
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

        //判断是否相同 相同则不更新
        if (!CollUtil.isEqualList(updateGroupIdList, groupIdList)) {
            //删除所有组关联
            sysUserGroupRelaMapper.delete(new LambdaQueryWrapper<SysUserGroupRelaDO>().eq(SysUserGroupRelaDO::getUserId, userId));
            // 插入新的组关联
            if (CollUtil.isNotEmpty(updateGroupIdList)) {
                updateGroupIdList.forEach(groupId -> {

                    SysUserGroupRelaDO ugRela = new SysUserGroupRelaDO();
                    ugRela.setUserId(userId);
                    ugRela.setGroupId(groupId);
                    sysUserGroupRelaMapper.insert(ugRela);
                });
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
        Integer groupId = queryParam.getGroupId();
        if (groupId != null) {
            List<Integer> groupIdList = getAllDescendantGroupIds(groupId);
            queryParam.setGroupIdList(groupIdList);
        }

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

    @SuppressWarnings("unchecked")
    private List<Integer> getAllDescendantGroupIds(Integer parentGroupId) {
        List<Integer> result = new ArrayList<>();
        result.add(parentGroupId);
        
        List<SysGroupDO> allGroups = sysGroupMapper.selectList(null);
        
        collectDescendantGroups(parentGroupId, allGroups, result);
        
        return result;
    }

    @SuppressWarnings("unchecked")
    private void collectDescendantGroups(Integer parentId, List<SysGroupDO> allGroups, List<Integer> result) {
        for (SysGroupDO group : allGroups) {
            if (group.getGroupParentId() != null && group.getGroupParentId().equals(parentId)) {
                result.add(group.getGroupId());
                collectDescendantGroups(group.getGroupId(), allGroups, result);
            }
        }
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

        // 使用标准 BCrypt 验证旧密码
        if (!BCrypt.checkpw(modifyPasswordParam.getOldPassword(), currentUser.getPassword())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.INCORRECT_PASSWORD);
        }

        // 使用标准 BCrypt 加密新密码
        String newPasswordHash = BCrypt.hashpw(modifyPasswordParam.getNewPassword(), BCrypt.gensalt());

        // 更新用户密码
        currentUser.setPassword(newPasswordHash);
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

        // 使用标准 BCrypt 加密新密码
        String newPasswordHash = BCrypt.hashpw(resetPasswordParam.getNewPassword(), BCrypt.gensalt());

        // 更新用户密码
        user.setPassword(newPasswordHash);
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
        Integer userId = reqParam.getUserId();

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
