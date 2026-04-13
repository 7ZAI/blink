package com.blink.base.service;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.SysUserRsp;
import com.blink.base.dto.rsp.UserPermissionRsp;
import com.blink.base.dto.vo.SysConfigVO;
import com.blink.base.dto.vo.SysUserVO;
import com.blink.base.entity.*;
import com.blink.base.mapper.*;
import com.blink.base.service.impl.SysUserServiceImpl;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.exception.BlinkException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * SysUserService 单元测试类
 * 测试系统用户服务的核心业务逻辑
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysUserService 单元测试")
class SysUserServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysGroupMapper sysGroupMapper;

    @Mock
    private SysUserRoleRelaMapper sysUserRoleRelaMapper;

    @Mock
    private SysUserGroupRelaMapper sysUserGroupRelaMapper;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private OnlineUserService onlineUserService;

    @Mock
    private SysMenuMapper sysMenuMapper;

    @Mock
    private SysPermissionMapper sysPermissionMapper;

    @Mock
    private SysRoleMenuRelaMapper sysRoleMenuRelaMapper;

    @Mock
    private SysRolePermRelaMapper sysRolePermRelaMapper;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @Nested
    @DisplayName("saveSysUser 测试")
    class SaveSysUserTests {

        @Test
        @DisplayName("保存用户 - 正常场景")
        void testSaveSysUser_Success() {
            // Given
            AddSysUserReq req = new AddSysUserReq();
            req.setLoginName("testuser");
            req.setUsername("测试用户");
            req.setGroupId(1);
            req.setRoles(List.of(1, 2));

            // Mock 登录名不重复
            when(sysUserMapper.selectCount(any())).thenReturn(0L);

            // Mock 角色存在
            SysRoleDO role1 = new SysRoleDO();
            role1.setRoleId(1);
            SysRoleDO role2 = new SysRoleDO();
            role2.setRoleId(2);
            when(roleMapper.selectList(any())).thenReturn(List.of(role1, role2));

            // Mock 组织存在
            when(sysGroupMapper.exists(any())).thenReturn(true);

            // Mock 配置
            SysConfigVO configVO = new SysConfigVO();
            configVO.setConfigValue("123456");
            when(sysConfigService.getOneConfigFromCacheOrDataBase(any())).thenReturn(configVO);

            // Mock 插入
            when(sysUserMapper.insert(any(SysUserDO.class))).thenAnswer(invocation -> {
                SysUserDO user = invocation.getArgument(0);
                user.setUserId(1);
                return 1;
            });

            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> sysUserService.saveSysUser(req));

            // 验证调用
            verify(sysUserMapper, times(1)).insert(any(SysUserDO.class));
            verify(sysUserRoleRelaMapper, times(1)).batchInsert(anyList());
            verify(sysUserGroupRelaMapper, times(1)).insert(any(SysUserGroupRelaDO.class));
        }

        @Test
        @DisplayName("保存用户 - 登录名重复")
        void testSaveSysUser_DuplicateLoginName() {
            // Given
            AddSysUserReq req = new AddSysUserReq();
            req.setLoginName("existinguser");

            // Mock 登录名已存在
            when(sysUserMapper.selectCount(any())).thenReturn(1L);

            // When & Then
            assertThrows(BlinkException.class, () -> sysUserService.saveSysUser(req));
            verify(sysUserMapper, never()).insert(any(SysUserDO.class));
        }

        @Test
        @DisplayName("保存用户 - 角色不存在")
        void testSaveSysUser_RoleNotExist() {
            // Given
            AddSysUserReq req = new AddSysUserReq();
            req.setLoginName("testuser");
            req.setRoles(List.of(999));

            // Mock 登录名不重复
            when(sysUserMapper.selectCount(any())).thenReturn(0L);

            // Mock 角色不存在
            when(roleMapper.selectList(any())).thenReturn(Collections.emptyList());

            // When & Then
            assertThrows(BlinkException.class, () -> sysUserService.saveSysUser(req));
        }

        @Test
        @DisplayName("保存用户 - 组织不存在")
        void testSaveSysUser_GroupNotExist() {
            // Given
            AddSysUserReq req = new AddSysUserReq();
            req.setLoginName("testuser");
            req.setGroupId(999);

            // Mock 登录名不重复
            when(sysUserMapper.selectCount(any())).thenReturn(0L);

            // Mock 组织不存在
            when(sysGroupMapper.exists(any())).thenReturn(false);

            // When & Then
            assertThrows(BlinkException.class, () -> sysUserService.saveSysUser(req));
        }
    }

    @Nested
    @DisplayName("deleteSysUser 测试")
    class DeleteSysUserTests {

        @Test
        @DisplayName("删除用户 - 单个删除正常场景")
        void testDeleteSysUser_SingleSuccess() {
            // Given
            DeleteSysUserReq req = new DeleteSysUserReq();
            req.setUserId(2);
            req.setBatchDelete(false);

            SysUserDO user = new SysUserDO();
            user.setUserId(2);
            user.setLoginName("testuser");
            user.setSuperFlag(0);

            when(sysUserMapper.selectById(2)).thenReturn(user);
            doNothing().when(onlineUserService).kickoutUsersByUserIds(anyList());
            when(sysUserMapper.deleteById(2)).thenReturn(1);
            when(sysUserRoleRelaMapper.deleteById(2)).thenReturn(1);
            when(sysUserGroupRelaMapper.deleteById(2)).thenReturn(1);

            // When & Then
            assertDoesNotThrow(() -> sysUserService.deleteSysUser(req));

            verify(sysUserMapper, times(1)).deleteById(2);
            verify(onlineUserService, times(1)).kickoutUsersByUserIds(List.of(2));
        }

        @Test
        @DisplayName("删除用户 - 用户不存在")
        void testDeleteSysUser_UserNotExist() {
            // Given
            DeleteSysUserReq req = new DeleteSysUserReq();
            req.setUserId(999);
            req.setBatchDelete(false);

            when(sysUserMapper.selectById(999)).thenReturn(null);

            // When & Then
            assertThrows(BlinkException.class, () -> sysUserService.deleteSysUser(req));
            verify(sysUserMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("删除用户 - 禁止删除超级管理员")
        void testDeleteSysUser_CannotDeleteSuperAdmin() {
            // Given
            DeleteSysUserReq req = new DeleteSysUserReq();
            req.setUserId(1);
            req.setBatchDelete(false);

            SysUserDO superAdmin = new SysUserDO();
            superAdmin.setUserId(1);
            superAdmin.setLoginName("admin");
            superAdmin.setSuperFlag(1);

            when(sysUserMapper.selectById(1)).thenReturn(superAdmin);

            // When & Then
            assertThrows(BlinkException.class, () -> sysUserService.deleteSysUser(req));
            verify(sysUserMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("删除用户 - 批量删除正常场景")
        void testDeleteSysUser_BatchSuccess() {
            // Given
            DeleteSysUserReq req = new DeleteSysUserReq();
            req.setUserIdList(List.of(2, 3));
            req.setBatchDelete(true);

            SysUserDO user2 = new SysUserDO();
            user2.setUserId(2);
            user2.setSuperFlag(0);

            SysUserDO user3 = new SysUserDO();
            user3.setUserId(3);
            user3.setSuperFlag(0);

            when(sysUserMapper.selectList(any())).thenReturn(List.of(user2, user3));
            doNothing().when(onlineUserService).kickoutUsersByUserIds(anyList());
            when(sysUserMapper.delete(any())).thenReturn(2);

            // When & Then
            assertDoesNotThrow(() -> sysUserService.deleteSysUser(req));

            verify(sysUserMapper, times(1)).delete(any());
            verify(onlineUserService, times(1)).kickoutUsersByUserIds(List.of(2, 3));
        }

        @Test
        @DisplayName("删除用户 - 批量删除包含超级管理员")
        void testDeleteSysUser_BatchContainsSuperAdmin() {
            // Given
            DeleteSysUserReq req = new DeleteSysUserReq();
            req.setUserIdList(List.of(1, 2));
            req.setBatchDelete(true);

            SysUserDO superAdmin = new SysUserDO();
            superAdmin.setUserId(1);
            superAdmin.setSuperFlag(1);

            SysUserDO normalUser = new SysUserDO();
            normalUser.setUserId(2);
            normalUser.setSuperFlag(0);

            when(sysUserMapper.selectList(any())).thenReturn(List.of(superAdmin, normalUser));

            // When & Then
            assertThrows(BlinkException.class, () -> sysUserService.deleteSysUser(req));
            verify(sysUserMapper, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("lockUser 测试")
    class LockUserTests {

        @Test
        @DisplayName("锁定用户 - 正常场景")
        void testLockUser_Success() {
            // Given
            SysUserDO user = new SysUserDO();
            user.setUserId(2);
            user.setSuperFlag(0);

            when(sysUserMapper.selectById(2)).thenReturn(user);
            when(sysUserMapper.updateById(any(SysUserDO.class))).thenReturn(1);

            try (MockedStatic<BlinkRequestContextHolder> mockedStatic = mockStatic(BlinkRequestContextHolder.class)) {
                mockedStatic.when(BlinkRequestContextHolder::getLoginName).thenReturn("admin");

                // When & Then
                assertDoesNotThrow(() -> sysUserService.lockUser(2, 1));
                verify(sysUserMapper, times(1)).updateById(any(SysUserDO.class));
            }
        }

        @Test
        @DisplayName("锁定用户 - 用户不存在")
        void testLockUser_UserNotExist() {
            // Given
            when(sysUserMapper.selectById(999)).thenReturn(null);

            // When & Then
            assertThrows(BlinkException.class, () -> sysUserService.lockUser(999, 1));
        }

        @Test
        @DisplayName("锁定用户 - 禁止锁定超级管理员")
        void testLockUser_CannotLockSuperAdmin() {
            // Given
            SysUserDO superAdmin = new SysUserDO();
            superAdmin.setUserId(1);
            superAdmin.setSuperFlag(1);

            when(sysUserMapper.selectById(1)).thenReturn(superAdmin);

            // When & Then
            assertThrows(BlinkException.class, () -> sysUserService.lockUser(1, 1));
        }
    }

    @Nested
    @DisplayName("getUserPermissions 测试")
    class GetUserPermissionsTests {

        @Test
        @DisplayName("获取用户权限 - 超级管理员获取所有权限")
        void testGetUserPermissions_SuperAdmin() {
            // Given
            UserIdReq req = new UserIdReq();
            req.setUserId(1);

            SysUserDO superAdmin = new SysUserDO();
            superAdmin.setUserId(1);
            superAdmin.setSuperFlag(1);

            when(sysUserMapper.selectById(1)).thenReturn(superAdmin);
            when(roleMapper.selectList(any())).thenReturn(new ArrayList<>());
            when(sysMenuMapper.selectList(any())).thenReturn(new ArrayList<>());
            when(sysPermissionMapper.selectList(any())).thenReturn(new ArrayList<>());

            // When
            UserPermissionRsp rsp = sysUserService.getUserPermissions(req);

            // Then
            assertNotNull(rsp);
            verify(roleMapper, times(1)).selectList(any());
            verify(sysMenuMapper, times(1)).selectList(any());
            verify(sysPermissionMapper, times(1)).selectList(any());
        }

        @Test
        @DisplayName("获取用户权限 - 普通用户")
        void testGetUserPermissions_NormalUser() {
            // Given
            UserIdReq req = new UserIdReq();
            req.setUserId(2);

            SysUserDO normalUser = new SysUserDO();
            normalUser.setUserId(2);
            normalUser.setSuperFlag(0);

            when(sysUserMapper.selectById(2)).thenReturn(normalUser);

            // Mock 用户角色关联
            SysUserRoleRelaDO userRole = new SysUserRoleRelaDO();
            userRole.setUserId(2);
            userRole.setRoleId(1);
            when(sysUserRoleRelaMapper.selectList(any())).thenReturn(List.of(userRole));

            // Mock 角色信息
            SysRoleDO role = new SysRoleDO();
            role.setRoleId(1);
            role.setStatus((byte) 0);
            when(roleMapper.selectList(any())).thenReturn(List.of(role));

            // Mock 菜单关联为空
            when(sysRoleMenuRelaMapper.selectList(any())).thenReturn(Collections.emptyList());
            // Mock 权限关联为空
            when(sysRolePermRelaMapper.selectList(any())).thenReturn(Collections.emptyList());

            // When
            UserPermissionRsp rsp = sysUserService.getUserPermissions(req);

            // Then
            assertNotNull(rsp);
            assertNotNull(rsp.getRoles());
        }

        @Test
        @DisplayName("获取用户权限 - 用户不存在")
        void testGetUserPermissions_UserNotExist() {
            // Given
            UserIdReq req = new UserIdReq();
            req.setUserId(999);

            when(sysUserMapper.selectById(999)).thenReturn(null);

            // When & Then
            assertThrows(BlinkException.class, () -> sysUserService.getUserPermissions(req));
        }
    }
}
