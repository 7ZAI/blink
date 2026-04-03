package com.blink.gateway.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.blink.gateway.base.dto.req.*;
import com.blink.gateway.base.dto.req.GetAllApiPermissionsReq;
import com.blink.gateway.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.gateway.base.dto.rsp.QueryUserPermissionRsp;
import com.blink.gateway.base.dto.vo.SysPermissionVO;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.gateway.base.constants.BaseErrCodeConstant;
import com.blink.gateway.base.constants.CommonConstants;
import com.blink.gateway.base.constants.RedisKeyConstants;
import com.blink.gateway.base.dto.rsp.QueryPermissionIdentityRsp;
import com.blink.gateway.base.dto.rsp.QuerySysPermissionRsp;
import com.blink.gateway.base.entity.SysDataFilterDO;
import com.blink.gateway.base.entity.SysPermissionDO;
import com.blink.gateway.base.entity.SysRolePermRelaDO;
import com.blink.gateway.base.entity.SysUserDO;
import com.blink.gateway.base.entity.SysUserRoleRelaDO;
import com.blink.gateway.base.mapper.SysDataFilterMapper;
import com.blink.gateway.base.mapper.SysMenuMapper;
import com.blink.gateway.base.mapper.SysPermissionMapper;
import com.blink.gateway.base.mapper.SysRolePermRelaMapper;
import com.blink.gateway.base.mapper.SysUserMapper;
import com.blink.gateway.base.mapper.SysUserRoleRelaMapper;
import com.blink.gateway.base.service.SysPermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限菜单 服务实现类
 *
 * @author binblink
 * @since 2024-01-13
 */
@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
public class SysPermissionServiceImpl implements SysPermissionService {

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysRolePermRelaMapper rolePermRelaMapper;

    @Resource
    private SysUserRoleRelaMapper userRoleRelaMapper;

    @Resource
    private SysUserMapper userMapper;

    @Resource
    private CacheComponent cacheComponent;

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private SysDataFilterMapper sysDataFilterMapper;

    /**
     * 保存 权限菜单
     *
     * @param saveParam 入参
     * @return SysPermissionVO
     * @throws BlinkException
     */
    @Override
    public SysPermissionVO saveSysPermission(AddSysPermissionReq saveParam) throws BlinkException {

        // 校验权限标识是否重复
        SysPermissionDO existByIdentity = sysPermissionMapper.selectOne(new LambdaQueryWrapper<SysPermissionDO>()
                .eq(SysPermissionDO::getAcIdentity, saveParam.getAcIdentity()));
        if (ObjectUtil.isNotNull(existByIdentity)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_IDENTITY_REPEAT);
        }

        // 接口权限类型需要检查URL是否重复
        if (CommonConstants.PERMISSION_API_TYPE.equals(saveParam.getAcType())) {
            if (StrUtil.isBlank(saveParam.getUrl())) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PARAMETER_NOT_NULL);
            }
            SysPermissionDO existPermission = sysPermissionMapper.selectOne(new LambdaQueryWrapper<SysPermissionDO>()
                    .eq(SysPermissionDO::getUrl, saveParam.getUrl()));

            // url 不允许重复
            if (ObjectUtil.isNotNull(existPermission)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_REPEAT);
            }
        }

        // 数据权限类型需要检查dataFilterId是否存在
        if (CommonConstants.PERMISSION_DATA_TYPE.equals(saveParam.getAcType())) {
            if (ObjectUtil.isNull(saveParam.getDataFilterId())) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PARAMETER_NOT_NULL);
            }
            SysDataFilterDO dataFilter = sysDataFilterMapper.selectById(saveParam.getDataFilterId());
            if (ObjectUtil.isNull(dataFilter)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_FILTER_NOT_EXIST);
            }
        }

        // 创建新的权限对象
        SysPermissionDO sysPermissionDO = new SysPermissionDO();
        BeanUtil.copyProperties(saveParam, sysPermissionDO);
        sysPermissionMapper.insert(sysPermissionDO);

        log.info("[SysPermission] 新增权限成功 | acId: {}, acIdentity: {}, acType: {}",
                sysPermissionDO.getAcId(), sysPermissionDO.getAcIdentity(), sysPermissionDO.getAcType());

        // 处理关联菜单（仅接口权限）
        if (CommonConstants.PERMISSION_API_TYPE.equals(saveParam.getAcType())
                && CollUtil.isNotEmpty(saveParam.getMenuIds())) {
            updateMenuPermissionRelation(sysPermissionDO.getAcId(), saveParam.getMenuIds());
        }

        var permissionVO = new SysPermissionVO();
        BeanUtil.copyProperties(sysPermissionDO, permissionVO);
        return permissionVO;
    }

    /**
     * 删除 权限菜单
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    @Override
    public void deleteSysPermission(DeleteSysPermissionReq deleteParam) throws BlinkException {

        // 清空关联菜单的perm_id
        List<Integer> permIds = Boolean.TRUE.equals(deleteParam.getBatchDelete())
            ? deleteParam.getIdList()
            : Collections.singletonList(deleteParam.getDeleteId());
        clearMenuPermissionRelation(permIds);

        //批量删除
        if (Boolean.TRUE.equals(deleteParam.getBatchDelete())) {

            Long count = rolePermRelaMapper.selectCount(new LambdaQueryWrapper<SysRolePermRelaDO>()
                    .in(SysRolePermRelaDO::getAcId, deleteParam.getIdList()));

            //存在关联数据 无法删除
            if (count > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_RELA_DATA);
            }

            sysPermissionMapper.deleteByIds(deleteParam.getIdList());
            log.info("[SysPermission] 批量删除权限成功 | permIds: {}", deleteParam.getIdList());
        } else {

            Long count = rolePermRelaMapper.selectCount(new LambdaQueryWrapper<SysRolePermRelaDO>()
                    .eq(SysRolePermRelaDO::getAcId, deleteParam.getDeleteId()));

            //存在关联数据 无法删除
            if (count > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_RELA_DATA);
            }

            sysPermissionMapper.deleteById(deleteParam.getDeleteId());
            log.info("[SysPermission] 删除权限成功 | permId: {}", deleteParam.getDeleteId());
        }

    }

    /**
     * 更新 权限菜单
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    @Override
    public void modifySysPermission(UpdateSysPermissionReq updateParam) throws BlinkException {

        SysPermissionDO sysPermissionDO = sysPermissionMapper.selectById(updateParam.getAcId());
        //不存在
        if (Objects.isNull(sysPermissionDO)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_NOT_EXIST);
        }

        // 校验权限标识是否重复（排除自身）
        if (!sysPermissionDO.getAcIdentity().equals(updateParam.getAcIdentity())) {
            SysPermissionDO existByIdentity = sysPermissionMapper.selectOne(new LambdaQueryWrapper<SysPermissionDO>()
                    .eq(SysPermissionDO::getAcIdentity, updateParam.getAcIdentity()));
            if (ObjectUtil.isNotNull(existByIdentity)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_IDENTITY_REPEAT);
            }
        }

        // 接口权限类型需要检查URL是否重复（排除自身）
        if (CommonConstants.PERMISSION_API_TYPE.equals(updateParam.getAcType())) {
            if (StrUtil.isBlank(updateParam.getUrl())) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PARAMETER_NOT_NULL);
            }
            if (!updateParam.getUrl().equals(sysPermissionDO.getUrl())) {
                SysPermissionDO existPermission = sysPermissionMapper.selectOne(new LambdaQueryWrapper<SysPermissionDO>()
                        .eq(SysPermissionDO::getUrl, updateParam.getUrl()));
                if (ObjectUtil.isNotNull(existPermission)) {
                    BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_REPEAT);
                }
            }
        }

        // 数据权限类型需要检查dataFilterId是否存在
        if (CommonConstants.PERMISSION_DATA_TYPE.equals(updateParam.getAcType())) {
            if (ObjectUtil.isNull(updateParam.getDataFilterId())) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PARAMETER_NOT_NULL);
            }
            SysDataFilterDO dataFilter = sysDataFilterMapper.selectById(updateParam.getDataFilterId());
            if (ObjectUtil.isNull(dataFilter)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.DATA_FILTER_NOT_EXIST);
            }
        }

        BeanUtil.copyProperties(updateParam, sysPermissionDO);
        sysPermissionMapper.updateById(sysPermissionDO);

        log.info("[SysPermission] 更新权限成功 | acId: {}, acIdentity: {}, acType: {}",
                sysPermissionDO.getAcId(), sysPermissionDO.getAcIdentity(), sysPermissionDO.getAcType());

        // 处理关联菜单（仅接口权限）
        if (CommonConstants.PERMISSION_API_TYPE.equals(updateParam.getAcType())) {
            updateMenuPermissionRelation(sysPermissionDO.getAcId(),
                Optional.ofNullable(updateParam.getMenuIds()).orElse(Collections.emptyList()));
        }
    }

    /**
     * 查询 权限菜单 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    @Override
    public QuerySysPermissionRsp<SysPermissionVO> getSysPermissionList(QuerySysPermissionReq queryParam) throws BlinkException {

        var pageRsp = new QuerySysPermissionRsp<SysPermissionVO>();
        PageUtils.queryPage(queryParam, () -> sysPermissionMapper.findSysPermissionList(queryParam), pageRsp);

        // 查询每个权限关联的菜单ID列表
        if (CollUtil.isNotEmpty(pageRsp.getRows())) {
            pageRsp.getRows().forEach(permissionVO -> {
                List<Integer> menuIds = sysMenuMapper.findMenuIdsByPermId(permissionVO.getAcId());
                permissionVO.setMenuIds(menuIds);
            });
        }

        return pageRsp;
    }

    /**
     * 根据角色获取权限集合 取角色权限交集
     *
     * @param roleIds 角色id
     * @return 取角色权限交集
     * @throws BlinkException
     */
    @Override
    public Set<String> getPermissionsByRoles(List<Integer> roleIds) throws BlinkException {

        List<SysPermissionDO> permissions = new ArrayList<SysPermissionDO>();
        if (CollUtil.isNotEmpty(roleIds)) {
            //包含 超级管理员 查询所有
            if (roleIds.contains(CommonConstants.SUPER_ADMIN_ID)) {

                SysPermissionDO superAdmin = new SysPermissionDO();
                superAdmin.setAcIdentity(CommonConstants.SUPER_ADMIN_PERMISSION);
                permissions.add(superAdmin);
            } else {
                permissions = sysPermissionMapper.findRolesPermissions(roleIds);
            }
        }
        return permissions.stream().map(SysPermissionDO::getAcIdentity).collect(Collectors.toSet());

    }

    /**
     * 根据角色获取权限集合 取角色权限交集
     *
     * @param reqDTO 用户id 或url DTO
     * @return 权限集合
     * @throws BlinkException
     */
    @Override
    public QueryUserPermissionRsp getPermissions(QueryUserPermissionReq reqDTO) throws BlinkException {

        Set<String> permissions = new HashSet<>();
        QueryUserPermissionRsp rspDTO = new QueryUserPermissionRsp();

        Integer userId = reqDTO.getUserId();
        String url = reqDTO.getUrl();

        //根据useId查询
        if (Objects.nonNull(userId) && StrUtil.isBlank(url)) {

            SysUserDO userDO = userMapper.selectById(userId);

            if (Objects.isNull(userDO)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIST);
            }
            //添加超级管理员权限
            if (CommonConstants.SUPER_ADMIN_ID.equals(userDO.getSuperFlag())) {
                permissions.add(CommonConstants.SUPER_ADMIN_PERMISSION);
            }

            List<SysUserRoleRelaDO> roleRela = userRoleRelaMapper.selectList(new LambdaQueryWrapper<SysUserRoleRelaDO>()
                    .eq(SysUserRoleRelaDO::getUserId, userId));
            if (roleRela.isEmpty()) {
                log.warn("用户未分配角色！ userId:{}", userId);
                rspDTO.setPermissions(permissions);
                return rspDTO;
            }

            List<Integer> roleIds = roleRela.stream().map(SysUserRoleRelaDO::getRoleId).toList();
            List<SysPermissionDO> permissionDOList = sysPermissionMapper.findRolesPermissions(roleIds);

            permissionDOList.stream().map(SysPermissionDO::getAcIdentity).forEach(permissions::add);

            rspDTO.setPermissions(permissions);
            return rspDTO;
        }

        //根据url查询
        if (Objects.isNull(userId) && StrUtil.isNotBlank(url)) {
            SysPermissionDO permission = sysPermissionMapper.selectOne(new LambdaQueryWrapper<SysPermissionDO>().eq(SysPermissionDO::getUrl, url)
                    .eq(SysPermissionDO::getAcType, CommonConstants.PERMISSION_API_TYPE));

            if (Objects.nonNull(permission)) {
                permissions.add(permission.getAcIdentity());
            }
            rspDTO.setPermissions(permissions);
        }

        return rspDTO;
    }

    /**
     * 获取所有接口权限
     *
     * @param body 空实体参数
     * @return {@link SysPermissionVO <SysPermissionVO>}
     * @throws BlinkException
     */
    @Override
    public GetAllApiPermissionsRsp getAllApiPermission(GetAllApiPermissionsReq body) throws BlinkException {

        List<SysPermissionDO> permissionList = sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermissionDO>()
                .eq(SysPermissionDO::getAcType, CommonConstants.PERMISSION_API_TYPE));
        var rsp = new GetAllApiPermissionsRsp();
        List<SysPermissionVO> list = BeanUtil.copyToList(permissionList, SysPermissionVO.class);
        rsp.setPermissionList(list);
        return rsp;
    }

    /**
     * 根据url 查询 权限标识
     * 从缓存中获取 如果取不到去数据获取
     *
     * @param queryParam
     * @return {@link QueryPermissionIdentityRsp}
     * @throws Throwable
     */
    @Override
    public QueryPermissionIdentityRsp getPermissionByUrl(QueryPermissionIdentityReq queryParam) throws BlinkException {

        String url = queryParam.getUrl();

        String acIdentity = (String) cacheComponent.getFromCacheOrDB(RedisKeyConstants.URL_PERMISSION + url, () -> {
                    SysPermissionDO permissionDO = sysPermissionMapper.selectOne(new LambdaQueryWrapper<SysPermissionDO>().eq(SysPermissionDO::getUrl, url));
                    return Objects.isNull(permissionDO) ? "" : permissionDO.getAcIdentity();
                }
        );

        var rspDTO = new QueryPermissionIdentityRsp();
        rspDTO.setAcIdentity(acIdentity);
        return rspDTO;
    }

    /**
     * 更新菜单与权限的关联关系
     *
     * @param permId  权限ID
     * @param menuIds 菜单ID列表
     */
    private void updateMenuPermissionRelation(Integer permId, List<Integer> menuIds) {
        // 先清空所有关联此权限的菜单
        sysMenuMapper.updatePermIdToNullByPermId(permId);

        // 更新选中菜单的perm_id
        if (CollUtil.isNotEmpty(menuIds)) {
            sysMenuMapper.updatePermIdByMenuIds(permId, menuIds);
            log.info("[SysPermission] 更新菜单权限关联 | permId: {}, menuIds: {}", permId, menuIds);
        }
    }

    /**
     * 清空菜单与权限的关联关系
     *
     * @param permIds 权限ID列表
     */
    private void clearMenuPermissionRelation(List<Integer> permIds) {
        if (CollUtil.isEmpty(permIds)) {
            return;
        }
        permIds.forEach(permId -> sysMenuMapper.updatePermIdToNullByPermId(permId));
        log.info("[SysPermission] 清空菜单权限关联 | permIds: {}", permIds);
    }


}
