package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.base.constans.CommonConstans;
import com.blink.base.dto.req.AddSysUserReq;
import com.blink.base.dto.req.DeleteSysUserReq;
import com.blink.base.dto.req.QuerySysUserReq;
import com.blink.base.dto.req.UpdateSysUserReq;
import com.blink.base.dto.rsp.SysUserRsp;
import com.blink.base.dto.vo.SysUserVO;
import com.blink.base.entity.*;
import com.blink.base.mapper.*;
import com.blink.base.service.SysUserService;
import com.blink.datasource.PageUtils;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

        if (existOne > CommonConstans.LONG_ZERO) {
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

        //TODO 解密密码 取决于密码是否加密

        //生成盐值
        sysUserDO.setSalt(BCrypt.gensalt());
        String encodePassword = BCrypt.hashpw(sysUserDO.getPassword(), sysUserDO.getSalt());
        sysUserDO.setPassword(encodePassword);

        //TODO 从报文中获取 报文头由网关填充  头像默认图片暂无
        sysUserDO.setAvatar("default");
        sysUserDO.setUsername("default");
        sysUserDO.setUpdateBy(saveParam.getLoginName());

        sysUserMapper.insert(sysUserDO);

        var groupUser = new SysUserGroupRelaDO();
        groupUser.setGroupId(gid);
        groupUser.setUserId(sysUserDO.getUserId());
        sysUserGroupRelaMapper.insert(groupUser);

        List<SysUserRoleRelaDO> roleUsers = new ArrayList<>();

        for (Integer roleId : roles) {
            var roleUser = new SysUserRoleRelaDO();
            roleUser.setRoleId(roleId);
            roleUser.setUserId(sysUserDO.getUserId());
            roleUsers.add(roleUser);
        }

        sysUserRoleRelaMapper.batchInsert(roleUsers);

    }

    /**
     * 删除 系统用户
     *
     * @param deleteParam 删除参数
     */
    @Override
    public void deleteSysUser(DeleteSysUserReq deleteParam) throws BlinkException {

        if (deleteParam.isBatchDelete()) {
            List<Integer> delList = deleteParam.getUserIdList();
            List<SysUserDO> existIds = sysUserMapper.selectBatchIds(delList);

            //存在非法id
            if (delList.size() != existIds.size()) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIST);
            }

            List<SysUserDO> superAdminUser = existIds.stream().filter(u -> u.getSuperFlag().equals(CommonConstans.SUPER_ADMIN_ID)).toList();
            //包含超级管理员 无法删除
            if (!superAdminUser.isEmpty()) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.NOT_ALLOW_DELETE);
            }

            sysUserMapper.deleteBatchIds(delList);
            sysUserRoleRelaMapper.deleteBatchIds(delList);
            sysUserGroupRelaMapper.deleteBatchIds(delList);

        } else {
            Integer userId = deleteParam.getUserId();
            SysUserDO user = sysUserMapper.selectById(userId);
            // 不存在的userId
            if (Objects.isNull(user)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIST);
            }
            //超级管理员 无法删除
            if (user.getSuperFlag().equals(CommonConstans.SUPER_ADMIN_ID)) {
                BlinkException.throwBusinessException(BaseErrCodeConstant.NOT_ALLOW_DELETE);
            }

            sysUserMapper.deleteById(userId);
            sysUserRoleRelaMapper.deleteById(userId);
            sysUserGroupRelaMapper.deleteById(userId);
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
        if (!CollUtil.isEqualList(updateParam.getRoleIdList(), roleIdList)) {
            //删除所有角色关联
            sysUserRoleRelaMapper.delete(new LambdaQueryWrapper<SysUserRoleRelaDO>().eq(SysUserRoleRelaDO::getUserId, userId));
            // 插入新的角色关联
            List<Integer> roleIds = updateParam.getRoleIdList();
            if (Objects.nonNull(roleIds) && !roleIds.isEmpty()) {
                List<SysUserRoleRelaDO> list = new ArrayList<>(roleIds.size());
                roleIds.forEach(roleId -> {

                    SysUserRoleRelaDO newUserRoleRela = new SysUserRoleRelaDO();
                    newUserRoleRela.setUserId(userId);
                    newUserRoleRela.setRoleId(roleId);
                    list.add(newUserRoleRela);
                });
                sysUserRoleRelaMapper.batchInsert(list);
            }

        }

        //判断是否相同 相同则不更新
        if (!CollUtil.isEqualList(updateParam.getGroupIdList(), groupIdList)) {
            //删除所有组关联
            sysUserGroupRelaMapper.delete(new LambdaQueryWrapper<SysUserGroupRelaDO>().eq(SysUserGroupRelaDO::getUserId, userId));
            // 插入新的组关联
            List<Integer> groups = updateParam.getGroupIdList();
            if (Objects.nonNull(groups) && !groups.isEmpty()) {
                groups.forEach(groupId -> {

                    SysUserGroupRelaDO ugRela = new SysUserGroupRelaDO();
                    ugRela.setUserId(userId);
                    ugRela.setGroupId(groupId);
                    sysUserGroupRelaMapper.insert(ugRela);
                });
            }
        }

        sysUserMapper.updateById(sysUserDO);

    }

    /**
     * 查询 系统用户 列表
     *
     * @param queryParam 查询条件参数
     * @return 分页封装 SysUserRspDTO<SysUserVO>
     */
    @Override
    public SysUserRsp getSysUserList(QuerySysUserReq queryParam) throws BlinkException {
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
}
