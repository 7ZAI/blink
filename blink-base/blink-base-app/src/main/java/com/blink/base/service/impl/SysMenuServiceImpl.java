package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.base.constans.CommonConstans;
import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QueryShowMenuRspDTO;
import com.blink.base.dto.rsp.QuerySysMenuRspDTO;
import com.blink.base.dto.vo.SysMenuVO;
import com.blink.base.entity.SysMenuDO;
import com.blink.base.entity.SysPermissionDO;
import com.blink.base.mapper.SysMenuMapper;
import com.blink.base.mapper.SysPermissionMapper;
import com.blink.base.mapper.SysRoleMapper;
import com.blink.base.service.SysMenuService;
import com.blink.datasource.PageUtils;
import com.blink.framework.common.constrant.SysConstant;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统菜单 服务实现类
 *
 * @author binblink
 * @since 2024-01-05
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

    /**
     * 保存 系统菜单
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    @Override
    public SysMenuVO saveSysMenu(AddSysMenuReqDTO saveParam) throws BlinkException {


        //父节点不存在
        if (ObjectUtil.isNotNull(saveParam.getParentId())) {
            SysMenuDO parentMenu = sysMenuMapper.selectById(saveParam.getParentId());
            if (ObjectUtil.isNull(parentMenu)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.MENU_PARENT_NOT_EXIST);
            }
        }

        var sysMenuDO = new SysMenuDO();
        var sysMenuVO = new SysMenuVO();
        BeanUtil.copyProperties(saveParam, sysMenuDO);
        sysMenuMapper.insert(sysMenuDO);
        BeanUtil.copyProperties(sysMenuDO, sysMenuVO);

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
    public void deleteSysMenu(DeleteSysMenuReqDTO deleteParam) throws BlinkException {


        //TODO 暂时缺少 删除菜单后权限变化逻辑

        if (deleteParam.isBatchDelete()) {

            Long count = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenuDO>()
                    .in(SysMenuDO::getParentId, deleteParam.getIdList()));

            //存在子节点数据 无法删除
            if (count.compareTo(CommonConstans.LONG_ZERO) > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_SON_DATA);
            }

            sysMenuMapper.deleteBatchIds(deleteParam.getIdList());
        } else {

            Long count = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenuDO>()
                    .eq(SysMenuDO::getParentId, deleteParam.getDeleteId()));

            //存在子节点数据 无法删除
            if (count.compareTo(CommonConstans.LONG_ZERO) > 0) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.HAVE_SON_DATA);
            }

            sysMenuMapper.deleteById(deleteParam.getDeleteId());
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
    public SysMenuVO modifySysMenu(UpdateSysMenuReqDTO updateParam) throws BlinkException {


        SysMenuDO sysMenuDO = sysMenuMapper.selectById(updateParam.getMenuId());
        //菜单不存在
        if (ObjectUtil.isNull(sysMenuDO)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.MENU_NOT_EXIST);
        }
        //更换父节点
        if(Objects.nonNull(updateParam.getParentId())){
            SysMenuDO sysMenuParent = sysMenuMapper.selectById(updateParam.getParentId());
            //父节点不存在
            if (ObjectUtil.isNull(sysMenuParent)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.MENU_PARENT_NOT_EXIST);
            }
        }

        BeanUtil.copyProperties(updateParam, sysMenuDO);
        sysMenuMapper.updateById(sysMenuDO);
        var sysMenuVO = new SysMenuVO();
        BeanUtil.copyProperties(sysMenuDO, sysMenuVO);

        return sysMenuVO;
    }

    /**
     * 查询 系统菜单 列表
     *
     * @param param
     * @return
     * @throws BlinkException
     */
    @Override
    public QuerySysMenuRspDTO getSysMenuList(QuerySysMenuReqDTO param) throws BlinkException {

        var pageRsp = new QuerySysMenuRspDTO();
        var queryParam = new SysMenuDO();
        BeanUtil.copyProperties(param, queryParam);
        PageUtils.queryPage(param, () -> sysMenuMapper.findSysMenuList(queryParam), pageRsp);
        return pageRsp;
    }

    /**
     * 根据用户查询其菜单 （登入成功）
     *
     * @param queryParam
     * @return {@link QueryShowMenuRspDTO }
     * @throws BlinkException
     */
    @Override
    public QueryShowMenuRspDTO getSysMenusByRoles(QueryShowMenuReqDTO queryParam) throws BlinkException {

        //菜单权限(包含功能权限)
        List<SysMenuVO> menuVos = sysMenuMapper.findSysMenuListByRole(queryParam);

        List<SysMenuVO> menus = menuVos.stream()
                .filter(menu->menu.getType().equals(CommonConstans.MENU_ORIGIN))
                .toList();
        List<SysMenuVO> functionMenus = menuVos.stream()
                .filter(menu->menu.getType().equals(CommonConstans.MENU_FUNCTION))
                .toList();

        var queryShowMenuRspDTO = new QueryShowMenuRspDTO();
        queryShowMenuRspDTO.setFunctionMenu(functionMenus);
        queryShowMenuRspDTO.setMenus(menus);

        return queryShowMenuRspDTO;
    }


}
