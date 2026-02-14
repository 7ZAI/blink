package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.base.constans.CommonConstans;
import com.blink.base.constans.RedisKeyConstans;
import com.blink.base.dto.req.AddSysPermissionReqDTO;
import com.blink.base.dto.req.DeleteSysPermissionReqDTO;
import com.blink.base.dto.req.QuerySysPermissionReqDTO;
import com.blink.base.dto.req.UpdateSysPermissionReqDTO;
import com.blink.base.dto.rsp.QueryPermissionIdentityRspDTO;
import com.blink.base.dto.rsp.QuerySysPermissionRspDTO;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.entity.SysPermissionDO;
import com.blink.base.entity.SysRolePermRelaDO;
import com.blink.base.mapper.SysPermissionMapper;
import com.blink.base.mapper.SysRolePermRelaMapper;
import com.blink.base.service.SysPermissionService;
import com.blink.datasource.PageUtils;
import com.blink.framework.common.constrant.SysConstant;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.CacheComponent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private CacheComponent cacheComponent;

    /**
     * 保存 权限菜单
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    @Override
    public SysPermissionVO saveSysPermission(AddSysPermissionReqDTO saveParam) throws BlinkException {


        SysPermissionDO sysPermissionDO = sysPermissionMapper.selectOne(new LambdaQueryWrapper<SysPermissionDO>()
                .eq(SysPermissionDO::getAcIdentity, saveParam.getAcIdentity()));
        //权限标识重复
        if (ObjectUtil.isNotNull(sysPermissionDO)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PERMISSION_REPEAT);
        }

        sysPermissionDO = new SysPermissionDO();
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
    public void deleteSysPermission(DeleteSysPermissionReqDTO deleteParam) throws BlinkException {


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
    public void modifySysPermission(UpdateSysPermissionReqDTO updateParam) throws BlinkException {

        SysPermissionDO sysPermissionDO = sysPermissionMapper.selectById(updateParam.getAcId());
        //不存在
        if(Objects.isNull(sysPermissionDO)){
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
    public QuerySysPermissionRspDTO<SysPermissionDO> getSysPermissionList(QuerySysPermissionReqDTO queryParam) throws BlinkException {

        var pageRsp = new QuerySysPermissionRspDTO<SysPermissionDO>();
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
     * 根据url 查询 权限标识
     * 从缓存中获取 如果取不到去数据获取
     *
     * @param queryParam
     * @return {@link QueryPermissionIdentityRspDTO}
     * @throws Throwable
     */
    @Override
    public QueryPermissionIdentityRspDTO getPermissionByUrl(QuerySysPermissionReqDTO queryParam) throws BlinkException {

        String url = queryParam.getUrl();

        String acIden = (String) cacheComponent.getFromCacheOrDB(RedisKeyConstans.URL_PERMISSION + url, () -> {
                    SysPermissionDO permissionDO = sysPermissionMapper.selectOne(new LambdaQueryWrapper<SysPermissionDO>().eq(SysPermissionDO::getUrl, url));
                    return Objects.isNull(permissionDO) ? "" : permissionDO.getAcIdentity();
                }
        );

        var rspDTO = new QueryPermissionIdentityRspDTO();
        rspDTO.setAcIdentity(acIden);
        return rspDTO;
    }


}
