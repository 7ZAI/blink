package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.base.constans.CommonConstans;
import com.blink.base.constans.RedisKeyConstans;
import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.base.dto.rsp.QueryPermissionIdentityRsp;
import com.blink.base.dto.rsp.QuerySysPermissionRsp;
import com.blink.base.dto.rsp.QueryUserPermissionRsp;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.entity.SysPermissionDO;
import com.blink.base.entity.SysRolePermRelaDO;
import com.blink.base.entity.SysUserRoleRelaDO;
import com.blink.base.mapper.SysPermissionMapper;
import com.blink.base.mapper.SysRolePermRelaMapper;
import com.blink.base.mapper.SysUserRoleRelaMapper;
import com.blink.base.service.SysPermissionService;
import com.blink.datasource.PageUtils;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.CacheComponent;
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
    private CacheComponent cacheComponent;

    /**
     * 保存 权限菜单
     *
     * @param saveParam 入参
     * @return SysPermissionVO
     * @throws BlinkException
     */
    @Override
    public SysPermissionVO saveSysPermission(AddSysPermissionReq saveParam) throws BlinkException {

        var sysPermissionDO = new SysPermissionDO();
        //接口权限
        if(CommonConstans.PERMISSION_API_TYPE.equals(saveParam.getAcType())){

             sysPermissionDO = sysPermissionMapper.selectOne(new LambdaQueryWrapper<SysPermissionDO>()
                    .eq(SysPermissionDO::getUrl, saveParam.getUrl()));

            //url 不允许重复
            if (ObjectUtil.isNotNull(sysPermissionDO)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_REPEAT);
            }
        }

        BeanUtil.copyProperties(saveParam, sysPermissionDO);
        sysPermissionMapper.insert(sysPermissionDO);

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


        //批量删除
        if (deleteParam.isBatchDelete()) {

            Long count = rolePermRelaMapper.selectCount(new LambdaQueryWrapper<SysRolePermRelaDO>()
                    .in(SysRolePermRelaDO::getAcId, deleteParam.getIdList()));

            //存在关联数据 无法删除
            if (count > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_RELA_DATA);
            }

            sysPermissionMapper.deleteBatchIds(deleteParam.getIdList());
        } else {

            Long count = rolePermRelaMapper.selectCount(new LambdaQueryWrapper<SysRolePermRelaDO>()
                    .eq(SysRolePermRelaDO::getAcId, deleteParam.getDeleteId()));

            //存在关联数据 无法删除
            if (count > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_RELA_DATA);
            }

            sysPermissionMapper.deleteById(deleteParam.getDeleteId());
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

        BeanUtil.copyProperties(updateParam, sysPermissionDO);
        sysPermissionMapper.updateById(sysPermissionDO);
    }

    /**
     * 查询 权限菜单 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    @Override
    public QuerySysPermissionRsp<SysPermissionDO> getSysPermissionList(QuerySysPermissionReq queryParam) throws BlinkException {

        var pageRsp = new QuerySysPermissionRsp<SysPermissionDO>();
        PageUtils.queryPage(queryParam, () -> sysPermissionMapper.findSysPermissionList(queryParam), pageRsp);
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
            if (roleIds.contains(CommonConstans.SUPER_ADMIN_ID)) {

                SysPermissionDO superAdmin = new SysPermissionDO();
                superAdmin.setAcIdentity(CommonConstans.SUPER_ADMIN_PERMISSION);
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
     * @param reqDTO 用户id DTO
     * @return 权限集合
     * @throws BlinkException
     */
    @Override
    public QueryUserPermissionRsp getPermissionsByUserId(QueryUserPermissionReq reqDTO) throws BlinkException {
        Integer userId = reqDTO.getUserId();
        List<SysUserRoleRelaDO> roleRela = userRoleRelaMapper.selectList(new LambdaQueryWrapper<SysUserRoleRelaDO>()
                .eq(SysUserRoleRelaDO::getUserId, userId));

        Set<String> permissions = new HashSet<>();
        QueryUserPermissionRsp rspDTO = new QueryUserPermissionRsp();

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
                .eq(SysPermissionDO::getAcType,CommonConstans.PERMISSION_API_TYPE));
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

        String acIdentity = (String) cacheComponent.getFromCacheOrDB(RedisKeyConstans.URL_PERMISSION + url, () -> {
                    SysPermissionDO permissionDO = sysPermissionMapper.selectOne(new LambdaQueryWrapper<SysPermissionDO>().eq(SysPermissionDO::getUrl, url));
                    return Objects.isNull(permissionDO) ? "" : permissionDO.getAcIdentity();
                }
        );

        var rspDTO = new QueryPermissionIdentityRsp();
        rspDTO.setAcIdentity(acIdentity);
        return rspDTO;
    }


}
