package com.blink.gateway.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.gateway.base.constants.BaseErrCodeConstant;
import com.blink.gateway.base.constants.CommonConstants;
import com.blink.gateway.base.dto.req.*;
import com.blink.gateway.base.dto.rsp.CheckMenuRoleRsp;
import com.blink.gateway.base.dto.rsp.QueryShowMenuRsp;
import com.blink.gateway.base.dto.rsp.QuerySysMenuRsp;
import com.blink.gateway.base.dto.vo.SysMenuVO;
import com.blink.gateway.base.dto.vo.SysRoleVO;
import com.blink.gateway.base.entity.SysMenuDO;
import com.blink.gateway.base.entity.SysPermissionDO;
import com.blink.gateway.base.entity.SysRolePermRelaDO;
import com.blink.gateway.base.mapper.SysMenuMapper;
import com.blink.gateway.base.mapper.SysPermissionMapper;
import com.blink.gateway.base.mapper.SysRoleMapper;
import com.blink.gateway.base.mapper.SysRoleMenuRelaMapper;
import com.blink.gateway.base.mapper.SysRolePermRelaMapper;
import com.blink.gateway.base.service.SysMenuService;

import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统菜单 服务实现类
 *
 * @author binblink
 */
@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
public class SysMenuServiceImpl implements SysMenuService {

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;


    @Resource
    private SysRoleMapper roleMapper;

    @Resource
    private SysRoleMenuRelaMapper sysRoleMenuRelaMapper;

    @Resource
    private SysRolePermRelaMapper sysRolePermRelaMapper;

    /**
     * 保存 系统菜单
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    @Override
    public SysMenuVO saveSysMenu(AddSysMenuReq saveParam) throws BlinkException {

        // 校验菜单类型
        Integer menuType = saveParam.getType();
        if (menuType == null || (menuType != 1 && menuType != 2 && menuType != 3)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PARAMETER_OUT_RANGE);
        }

        // 目录和页面菜单URL必填
        if ((menuType == 1 || menuType == 2) && StrUtil.isBlank(saveParam.getUrl())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PARAMETER_NOT_NULL);
        }

        // 计算菜单层级
        Integer menuLevel = 1;
        // 统一处理 parentId：null 或 0 都表示根菜单
        Integer parentId = saveParam.getParentId();
        if (parentId == null) {
            parentId = 0;
        }

        //父节点存在（非根菜单）
        if (parentId > 0) {
            SysMenuDO parentMenu = sysMenuMapper.selectById(parentId);
            if (ObjectUtil.isNull(parentMenu)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.MENU_PARENT_NOT_EXIST);
            }
            menuLevel = parentMenu.getMenuLevel() + 1;
        }

        // 校验permId是否存在（仅页面和按钮菜单）
        if ((menuType == 2 || menuType == 3) && ObjectUtil.isNotNull(saveParam.getPermId())) {
            Long permCount = sysPermissionMapper.selectCount(
                    new LambdaQueryWrapper<SysPermissionDO>()
                            .eq(SysPermissionDO::getAcId, saveParam.getPermId()));
            if (permCount == null || permCount == 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_NOT_EXIST);
            }
        }

        var sysMenuDO = new SysMenuDO();
        var sysMenuVO = new SysMenuVO();
        BeanUtil.copyProperties(saveParam, sysMenuDO);
        // 统一设置 parentId，根菜单为 0
        sysMenuDO.setParentId(parentId);
        // 设置菜单层级
        sysMenuDO.setMenuLevel(menuLevel);
        // 处理关联权限（仅页面和按钮菜单）
        if ((menuType == 2 || menuType == 3)
                && ObjectUtil.isNotNull(saveParam.getPermId())) {
            sysMenuDO.setPermId(saveParam.getPermId());
        }
        sysMenuMapper.insert(sysMenuDO);
        BeanUtil.copyProperties(sysMenuDO, sysMenuVO);

        log.info("[SysMenu] 新增菜单成功 | menuId: {}, menuName: {}, type: {}, level: {}, parentId: {}",
                sysMenuDO.getMenuId(), sysMenuDO.getMenuName(), menuType, menuLevel, parentId);
        return sysMenuVO;
    }

    /**
     * 删除 系统菜单
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    @Override
    public void deleteSysMenu(DeleteSysMenuReq deleteParam) throws BlinkException {

        if (Boolean.TRUE.equals(deleteParam.getBatchDelete())) {

            // 检查是否包含首页菜单
            List<SysMenuDO> menusToDelete = sysMenuMapper.selectByIds(deleteParam.getIdList());
            boolean hasHomeMenu = menusToDelete.stream()
                    .anyMatch(menu -> "/dashboard".equals(menu.getUrl()) || "首页".equals(menu.getMenuName()));
            if (hasHomeMenu) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HOME_MENU_NOT_ALLOW_DELETE);
            }

            Long count = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenuDO>()
                    .in(SysMenuDO::getParentId, deleteParam.getIdList()));

            //存在子节点数据 无法删除
            if (count.compareTo(CommonConstants.LONG_ZERO) > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_SON_DATA);
            }

            // 删除角色-菜单关联
            sysRoleMenuRelaMapper.deleteBatchByMenuIds(deleteParam.getIdList());

            // 收集需要删除权限关联的菜单ID（有绑定权限的菜单）
            List<Integer> menuIdsWithPerm = menusToDelete.stream()
                    .filter(menu -> ObjectUtil.isNotNull(menu.getPermId()))
                    .map(SysMenuDO::getMenuId)
                    .toList();

            // 删除角色-权限关联（仅删除绑定了权限的菜单对应的角色权限）
            if (CollUtil.isNotEmpty(menuIdsWithPerm)) {
                // 查询这些菜单关联的角色
                for (Integer menuId : menuIdsWithPerm) {
                    List<SysRoleVO> roles = sysRoleMenuRelaMapper.selectRolesByMenuId(menuId);
                    if (CollUtil.isNotEmpty(roles)) {
                        // 获取菜单信息
                        SysMenuDO menu = sysMenuMapper.selectById(menuId);
                        if (menu != null && menu.getPermId() != null) {
                            List<Integer> roleIds = roles.stream().map(SysRoleVO::getRoleId).toList();
                            sysRolePermRelaMapper.deleteByRoleIdsAndPermId(roleIds, menu.getPermId());
                            log.info("[SysMenu] 删除菜单时同步删除角色权限关联 | menuId: {}, permId: {}, roleIds: {}",
                                    menuId, menu.getPermId(), roleIds);
                        }
                    }
                }
            }

            sysMenuMapper.deleteByIds(deleteParam.getIdList());
            log.info("[SysMenu] 批量删除菜单成功 | menuIds: {}", deleteParam.getIdList());
        } else {

            // 检查是否为首页菜单
            SysMenuDO menuToDelete = sysMenuMapper.selectById(deleteParam.getDeleteId());
            if (menuToDelete != null &&
                ("/dashboard".equals(menuToDelete.getUrl()) || "首页".equals(menuToDelete.getMenuName()))) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HOME_MENU_NOT_ALLOW_DELETE);
            }

            Long count = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenuDO>()
                    .eq(SysMenuDO::getParentId, deleteParam.getDeleteId()));

            //存在子节点数据 无法删除
            if (count.compareTo(CommonConstants.LONG_ZERO) > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_SON_DATA);
            }

            // 删除角色-菜单关联
            sysRoleMenuRelaMapper.deleteBatchByMenuIds(Collections.singletonList(deleteParam.getDeleteId()));

            // 删除角色-权限关联（如果有绑定权限）
            if (menuToDelete != null && menuToDelete.getPermId() != null) {
                List<SysRoleVO> roles = sysRoleMenuRelaMapper.selectRolesByMenuId(deleteParam.getDeleteId());
                if (CollUtil.isNotEmpty(roles)) {
                    List<Integer> roleIds = roles.stream().map(SysRoleVO::getRoleId).toList();
                    sysRolePermRelaMapper.deleteByRoleIdsAndPermId(roleIds, menuToDelete.getPermId());
                    log.info("[SysMenu] 删除菜单时同步删除角色权限关联 | menuId: {}, permId: {}, roleIds: {}",
                            deleteParam.getDeleteId(), menuToDelete.getPermId(), roleIds);
                }
            }

            sysMenuMapper.deleteById(deleteParam.getDeleteId());
            log.info("[SysMenu] 删除菜单成功 | menuId: {}", deleteParam.getDeleteId());
        }

    }

    /**
     * 更新 系统菜单
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    @Override
    public SysMenuVO modifySysMenu(UpdateSysMenuReq updateParam) throws BlinkException {

        SysMenuDO sysMenuDO = sysMenuMapper.selectById(updateParam.getMenuId());
        //菜单不存在
        if (ObjectUtil.isNull(sysMenuDO)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.MENU_NOT_EXIST);
        }

        // 校验菜单类型
        Integer menuType = updateParam.getType();
        if (menuType == null || (menuType != 1 && menuType != 2 && menuType != 3)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PARAMETER_OUT_RANGE);
        }

        // 目录和页面菜单URL必填
        if ((menuType == 1 || menuType == 2) && StrUtil.isBlank(updateParam.getUrl())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PARAMETER_NOT_NULL);
        }

        // 校验permId是否存在（仅页面和按钮菜单）
        if ((menuType == 2 || menuType == 3) && ObjectUtil.isNotNull(updateParam.getPermId())) {
            Long permCount = sysPermissionMapper.selectCount(
                    new LambdaQueryWrapper<SysPermissionDO>()
                            .eq(SysPermissionDO::getAcId, updateParam.getPermId()));
            if (permCount == null || permCount == 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_NOT_EXIST);
            }
        }

        // 保存旧的权限ID用于比较
        Integer oldPermId = sysMenuDO.getPermId();
        Integer oldParentId = sysMenuDO.getParentId();

        // 统一处理 parentId：null 转 0
        Integer newParentId = updateParam.getParentId();
        if (newParentId == null) {
            newParentId = 0;
        }

        //更换父节点
        if(!newParentId.equals(oldParentId)){
            // 不能将自己设为父节点
            if (newParentId.equals(updateParam.getMenuId())) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PARAMETER_OUT_RANGE);
            }
            // 根菜单（parentId=0）不需要查询父节点
            if (newParentId > 0) {
                SysMenuDO sysMenuParent = sysMenuMapper.selectById(newParentId);
                //父节点不存在
                if (ObjectUtil.isNull(sysMenuParent)) {
                    BlinkException.throwBusinessException(BaseErrCodeConstant.MENU_PARENT_NOT_EXIST);
                }
                // 更新当前菜单层级
                sysMenuDO.setMenuLevel(sysMenuParent.getMenuLevel() + 1);
            } else {
                // 设置为根菜单，层级为1
                sysMenuDO.setMenuLevel(1);
            }
        }

        BeanUtil.copyProperties(updateParam, sysMenuDO);
        // 确保 parentId 正确设置
        sysMenuDO.setParentId(newParentId);
        // 处理关联权限（仅页面和按钮菜单）
        if (menuType == 2 || menuType == 3) {
            sysMenuDO.setPermId(updateParam.getPermId());
        } else {
            sysMenuDO.setPermId(null);
        }
        sysMenuMapper.updateById(sysMenuDO);

        // 如果父节点变更，递归更新子菜单层级
        if (!newParentId.equals(oldParentId)) {
            updateChildrenMenuLevel(sysMenuDO.getMenuId(), sysMenuDO.getMenuLevel());
        }

        // 同步更新角色权限关联（当菜单绑定权限变更时）
        Integer newPermId = sysMenuDO.getPermId();
        boolean permChanged = !Objects.equals(oldPermId, newPermId);

        if (permChanged) {
            // 查询该菜单关联的角色
            List<SysRoleVO> roles = sysRoleMenuRelaMapper.selectRolesByMenuId(updateParam.getMenuId());

            if (CollUtil.isNotEmpty(roles)) {
                List<Integer> roleIds = roles.stream().map(SysRoleVO::getRoleId).toList();

                // 删除旧权限关联
                if (oldPermId != null) {
                    sysRolePermRelaMapper.deleteByRoleIdsAndPermId(roleIds, oldPermId);
                    log.info("[SysMenu] 更新菜单时删除旧权限关联 | menuId: {}, oldPermId: {}, roleIds: {}",
                            updateParam.getMenuId(), oldPermId, roleIds);
                }

                // 添加新权限关联
                if (newPermId != null) {
                    List<SysRolePermRelaDO> newRelations = roleIds.stream()
                            .map(roleId -> {
                                SysRolePermRelaDO rela = new SysRolePermRelaDO();
                                rela.setRoleId(roleId);
                                rela.setAcId(newPermId);
                                return rela;
                            })
                            .toList();
                    sysRolePermRelaMapper.batchInsertIgnore(newRelations);
                    log.info("[SysMenu] 更新菜单时添加新权限关联 | menuId: {}, newPermId: {}, roleIds: {}",
                            updateParam.getMenuId(), newPermId, roleIds);
                }
            }
        }

        var sysMenuVO = new SysMenuVO();
        BeanUtil.copyProperties(sysMenuDO, sysMenuVO);

        log.info("[SysMenu] 更新菜单成功 | menuId: {}, menuName: {}", sysMenuDO.getMenuId(), sysMenuDO.getMenuName());
        return sysMenuVO;
    }

    /**
     * 递归更新子菜单层级
     *
     * @param parentId 父菜单ID
     * @param parentLevel 父菜单层级
     */
    private void updateChildrenMenuLevel(Integer parentId, Integer parentLevel) {
        // 查询所有子菜单
        List<SysMenuDO> children = sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenuDO>()
                        .eq(SysMenuDO::getParentId, parentId));

        if (CollUtil.isEmpty(children)) {
            return;
        }

        // 更新子菜单层级
        for (SysMenuDO child : children) {
            child.setMenuLevel(parentLevel + 1);
            sysMenuMapper.updateById(child);
            // 递归更新孙子菜单
            updateChildrenMenuLevel(child.getMenuId(), child.getMenuLevel());
        }

        log.info("[SysMenu] 更新子菜单层级 | parentId: {}, childCount: {}, newLevel: {}",
                parentId, children.size(), parentLevel + 1);
    }

    /**
     * 查询 系统菜单 列表
     *
     * @param param
     * @return
     * @throws BlinkException
     */
    @Override
    public QuerySysMenuRsp getSysMenuList(QuerySysMenuReq param) throws BlinkException {

        var pageRsp = new QuerySysMenuRsp();
        var queryParam = new SysMenuDO();
        BeanUtil.copyProperties(param, queryParam);

        // 查询所有菜单（不分页，用于构建树形结构），已关联权限信息
        List<SysMenuVO> allMenus = sysMenuMapper.findSysMenuList(queryParam);

        // 构建树形结构
        List<SysMenuVO> menuTree = buildMenuTree(allMenus, null);

        // 设置返回数据
        pageRsp.setRows(menuTree);
        pageRsp.setTotal(allMenus.size());

        return pageRsp;
    }
    
    /**
     * 构建菜单树形结构
     *
     * @param menuList 菜单列表
     * @param parentId 父菜单ID
     * @return 树形结构的菜单列表
     */
    private List<SysMenuVO> buildMenuTree(List<SysMenuVO> menuList, Integer parentId) {
        List<SysMenuVO> result = new ArrayList<>();
        for (SysMenuVO menu : menuList) {
            // 判断是否为当前父节点的子节点
            // 根节点：parentId为null或0
            boolean isRoot = (parentId == null || parentId == 0)
                    && (menu.getParentId() == null || menu.getParentId() == 0);
            boolean isChild = isRoot
                    || (parentId != null && parentId.equals(menu.getParentId()));

            if (isChild) {
                // 递归查找子节点
                List<SysMenuVO> children = buildMenuTree(menuList, menu.getMenuId());
                if (!children.isEmpty()) {
                    menu.setChildren(children);
                }
                result.add(menu);
            }
        }
        // 按排序号排序
        result.sort(Comparator.comparing(menu -> menu.getOrderNumber() != null ? menu.getOrderNumber() : 0));
        return result;
    }

    /**
     * 根据用户查询其菜单 （登入成功）
     *
     * @param queryParam
     * @return {@link QueryShowMenuRsp }
     * @throws BlinkException
     */
    @Override
    public QueryShowMenuRsp getSysMenusByRoles(QueryShowMenuReq queryParam) throws BlinkException {

        //菜单权限(包含功能权限)
        List<SysMenuVO> menuVos = sysMenuMapper.findSysMenuListByRole(queryParam);

        // 收集所有菜单ID（包括父菜单）
        Set<Integer> allMenuIds = menuVos.stream()
                .map(SysMenuVO::getMenuId)
                .collect(Collectors.toSet());

        // 收集父菜单ID链
        List<Integer> menuIdList = new ArrayList<>(allMenuIds);
        collectParentMenuIds(menuIdList, allMenuIds);

        // 如果有缺失的父菜单，需要重新查询补充
        List<SysMenuVO> finalMenuVos = menuVos;
        List<Integer> missingMenuIds = allMenuIds.stream()
                .filter(id -> finalMenuVos.stream().noneMatch(menu -> menu.getMenuId().equals(id)))
                .toList();

        if (!missingMenuIds.isEmpty()) {
            List<SysMenuDO> parentMenus = sysMenuMapper.selectByIds(missingMenuIds);
            List<SysMenuVO> parentMenuVOs = parentMenus.stream().map(menu -> {
                SysMenuVO vo = new SysMenuVO();
                BeanUtil.copyProperties(menu, vo);
                return vo;
            }).toList();
            // 合并父菜单到结果列表
            List<SysMenuVO> mergedMenus = new ArrayList<>(menuVos);
            mergedMenus.addAll(parentMenuVOs);
            menuVos = mergedMenus;
        }

        // 目录和页面菜单 (type=1 目录, type=2 页面)
        List<SysMenuVO> menus = menuVos.stream()
                .filter(menu -> menu.getType().equals(CommonConstants.MENU_DIRECTORY)
                        || menu.getType().equals(CommonConstants.MENU_PAGE))
                .toList();
        // 按钮/功能菜单 (type=3)
        List<SysMenuVO> functionMenus = menuVos.stream()
                .filter(menu -> menu.getType().equals(CommonConstants.MENU_FUNCTION))
                .toList();

        var queryShowMenuRspDTO = new QueryShowMenuRsp();
        queryShowMenuRspDTO.setFunctionMenu(functionMenus);
        queryShowMenuRspDTO.setMenus(menus);

        return queryShowMenuRspDTO;
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

        if (!parentIds.isEmpty()) {
            allMenuIds.addAll(parentIds);
            // 递归收集父菜单的父菜单
            collectParentMenuIds(parentIds, allMenuIds);
        }
    }

    /**
     * 获取所有菜单（不限制角色）
     * 用于超级管理员获取全部菜单
     *
     * @return {@link QueryShowMenuRsp }
     * @throws BlinkException
     */
    @Override
    public QueryShowMenuRsp getAllMenus() throws BlinkException {
        //查询所有菜单（不限制角色）
        List<SysMenuDO> allMenus = Optional.ofNullable(sysMenuMapper.selectList(null)).orElse(new ArrayList<>());
        List<SysMenuVO> menuVos = allMenus.stream()
                .map(menu -> {
                    SysMenuVO vo = new SysMenuVO();
                    BeanUtil.copyProperties(menu, vo);
                    return vo;
                })
                .toList();

        // 目录和页面菜单 (type=1 目录, type=2 页面)
        List<SysMenuVO> menus = menuVos.stream()
                .filter(menu -> menu.getType().equals(CommonConstants.MENU_DIRECTORY)
                        || menu.getType().equals(CommonConstants.MENU_PAGE))
                .toList();
        // 按钮/功能菜单 (type=3)
        List<SysMenuVO> functionMenus = menuVos.stream()
                .filter(menu -> menu.getType().equals(CommonConstants.MENU_FUNCTION))
                .toList();

        var queryShowMenuRspDTO = new QueryShowMenuRsp();
        queryShowMenuRspDTO.setFunctionMenu(functionMenus);
        queryShowMenuRspDTO.setMenus(menus);

        return queryShowMenuRspDTO;
    }

    /**
     * 检查菜单是否已分配给角色
     *
     * @param reqParam 检查请求参数
     * @return 检查结果
     * @throws BlinkException 异常
     */
    @Override
    public CheckMenuRoleRsp checkMenuRoleAssignment(CheckMenuRoleReq reqParam) throws BlinkException {
        var rsp = new CheckMenuRoleRsp();

        // 查询菜单信息
        SysMenuDO menu = sysMenuMapper.selectById(reqParam.getMenuId());
        if (ObjectUtil.isNull(menu)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.MENU_NOT_EXIST);
        }

        // 查询菜单关联的角色列表
        List<SysRoleVO> roles = sysRoleMenuRelaMapper.selectRolesByMenuId(reqParam.getMenuId());

        rsp.setAssigned(CollUtil.isNotEmpty(roles));
        rsp.setRoles(roles);
        rsp.setCurrentPermId(menu.getPermId());

        // 检查权限是否发生变更
        if (ObjectUtil.isNotNull(reqParam.getNewPermId()) || ObjectUtil.isNotNull(menu.getPermId())) {
            rsp.setPermChanged(!Objects.equals(menu.getPermId(), reqParam.getNewPermId()));
        } else {
            rsp.setPermChanged(false);
        }

        log.info("[SysMenu] 检查菜单角色分配 | menuId: {}, assigned: {}, permChanged: {}",
                reqParam.getMenuId(), rsp.getAssigned(), rsp.getPermChanged());
        return rsp;
    }


}
