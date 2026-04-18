package com.blink.gateway.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.admin.dto.req.AddInstanceGroupReq;
import com.blink.gateway.admin.dto.req.DeleteInstanceGroupReq;
import com.blink.gateway.admin.dto.req.GetInstanceGroupReq;
import com.blink.gateway.admin.dto.req.QueryInstanceGroupReq;
import com.blink.gateway.admin.dto.req.UpdateInstanceGroupReq;
import com.blink.gateway.admin.dto.rsp.InstanceGroupListRsp;
import com.blink.gateway.admin.dto.vo.InstanceGroupVO;
import com.blink.gateway.admin.entity.GatewayInstanceDO;
import com.blink.gateway.admin.entity.GatewayInstanceGroupDO;
import com.blink.gateway.admin.mapper.GatewayInstanceGroupMapper;
import com.blink.gateway.admin.mapper.GatewayInstanceMapper;
import com.blink.gateway.admin.service.GatewayInstanceGroupService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_GROUP_HAS_INSTANCES;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_GROUP_KEY_EXISTS;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_GROUP_NOT_EXIST;
import static com.blink.gateway.admin.constants.RouteConstant.STATUS_ENABLE;

/**
 * 实例分组服务实现类
 *
 * @author binblink
 * @since 2026-04-18
 */
@Service
@Slf4j
public class GatewayInstanceGroupServiceImpl implements GatewayInstanceGroupService {

    @Resource
    private GatewayInstanceGroupMapper gatewayInstanceGroupMapper;

    @Resource
    private GatewayInstanceMapper gatewayInstanceMapper;

    @Override
    public ResponseDTO<InstanceGroupListRsp> queryInstanceGroupList(QueryInstanceGroupReq req) {
        // 构建查询条件
        LambdaQueryWrapper<GatewayInstanceGroupDO> queryWrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(req.getGroupKey())) {
            queryWrapper.like(GatewayInstanceGroupDO::getGroupKey, req.getGroupKey());
        }
        if (StrUtil.isNotBlank(req.getGroupName())) {
            queryWrapper.like(GatewayInstanceGroupDO::getGroupName, req.getGroupName());
        }
        if (ObjectUtil.isNotNull(req.getStatus())) {
            queryWrapper.eq(GatewayInstanceGroupDO::getStatus, req.getStatus());
        }

        // 默认按更新时间降序排序
        if (StrUtil.isBlank(req.getOrderBy())) {
            req.setOrderBy("update_time desc");
        }

        // 分页查询
        InstanceGroupListRsp rsp = PageUtils.queryPage(req,
                () -> gatewayInstanceGroupMapper.selectList(queryWrapper),
                new InstanceGroupListRsp());

        // 转换 DO 到 VO（由于泛型擦除，PageUtils 设置的是 List<GatewayInstanceGroupDO>）
        List rawList = rsp.getRows();
        if (CollUtil.isNotEmpty(rawList)) {
            List<InstanceGroupVO> voList = new ArrayList<>();
            for (Object obj : rawList) {
                GatewayInstanceGroupDO groupDO = (GatewayInstanceGroupDO) obj;
                voList.add(BeanUtil.copyProperties(groupDO, InstanceGroupVO.class));
            }
            rsp.setRows(voList);
        }

        log.info("[InstanceGroup] 分页查询分组列表成功 | total: {}, pageNum: {}, pageSize: {}",
                rsp.getTotal(), req.getPageNum(), req.getPageSize());

        return ResponseDTO.newSuccessInstance(rsp);
    }

    @Override
    public ResponseDTO<InstanceGroupVO> getInstanceGroupDetail(GetInstanceGroupReq req) {
        GatewayInstanceGroupDO groupDO = gatewayInstanceGroupMapper.selectById(req.getGroupId());

        if (ObjectUtil.isNull(groupDO)) {
            BlinkException.throwBusinessException(INSTANCE_GROUP_NOT_EXIST);
        }

        InstanceGroupVO vo = convertToVO(groupDO);

        log.info("[InstanceGroup] 获取分组详情成功 | groupId: {}, groupKey: {}", req.getGroupId(), vo.getGroupKey());

        return ResponseDTO.newSuccessInstance(vo);
    }

    @Override
    public ResponseDTO<Void> addInstanceGroup(AddInstanceGroupReq req) {
        // 检查 groupKey 是否已存在
        LambdaQueryWrapper<GatewayInstanceGroupDO> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(GatewayInstanceGroupDO::getGroupKey, req.getGroupKey());
        Long count = gatewayInstanceGroupMapper.selectCount(existQuery);

        if (count > 0) {
            log.warn("[InstanceGroup] 新增分组失败，groupKey 已存在 | groupKey: {}", req.getGroupKey());
            BlinkException.throwBusinessException(INSTANCE_GROUP_KEY_EXISTS);
        }

        // 构建实体并保存
        GatewayInstanceGroupDO groupDO = BeanUtil.copyProperties(req, GatewayInstanceGroupDO.class);
        gatewayInstanceGroupMapper.insert(groupDO);

        log.info("[InstanceGroup] 新增分组成功 | groupId: {}, groupKey: {}, groupName: {}",
                groupDO.getGroupId(), groupDO.getGroupKey(), groupDO.getGroupName());

        return ResponseDTO.newSuccessInstance(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Void> updateInstanceGroup(UpdateInstanceGroupReq req) {
        // 检查分组是否存在
        GatewayInstanceGroupDO existGroup = gatewayInstanceGroupMapper.selectById(req.getGroupId());

        if (ObjectUtil.isNull(existGroup)) {
            BlinkException.throwBusinessException(INSTANCE_GROUP_NOT_EXIST);
        }

        // 如果 groupKey 有变更，检查新的 groupKey 是否已被其他分组使用
        if (!existGroup.getGroupKey().equals(req.getGroupKey())) {
            LambdaQueryWrapper<GatewayInstanceGroupDO> keyQuery = new LambdaQueryWrapper<>();
            keyQuery.eq(GatewayInstanceGroupDO::getGroupKey, req.getGroupKey());
            keyQuery.ne(GatewayInstanceGroupDO::getGroupId, req.getGroupId());
            Long count = gatewayInstanceGroupMapper.selectCount(keyQuery);

            if (count > 0) {
                log.warn("[InstanceGroup] 更新分组失败，groupKey 已被占用 | groupId: {}, groupKey: {}",
                        req.getGroupId(), req.getGroupKey());
                BlinkException.throwBusinessException(INSTANCE_GROUP_KEY_EXISTS);
            }
        }

        // 更新分组
        GatewayInstanceGroupDO updateDO = BeanUtil.copyProperties(req, GatewayInstanceGroupDO.class);
        gatewayInstanceGroupMapper.updateById(updateDO);

        log.info("[InstanceGroup] 更新分组成功 | groupId: {}, groupKey: {}", req.getGroupId(), req.getGroupKey());

        return ResponseDTO.newSuccessInstance(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Void> deleteInstanceGroup(DeleteInstanceGroupReq req) {
        // 检查分组是否存在
        GatewayInstanceGroupDO existGroup = gatewayInstanceGroupMapper.selectById(req.getGroupId());

        if (ObjectUtil.isNull(existGroup)) {
            BlinkException.throwBusinessException(INSTANCE_GROUP_NOT_EXIST);
        }

        // 检查分组下是否有关联的实例
        LambdaQueryWrapper<GatewayInstanceDO> instanceQuery = new LambdaQueryWrapper<>();
        instanceQuery.eq(GatewayInstanceDO::getGroupKey, existGroup.getGroupKey());
        Long instanceCount = gatewayInstanceMapper.selectCount(instanceQuery);

        if (instanceCount > 0) {
            log.warn("[InstanceGroup] 删除分组失败，分组下存在关联实例 | groupId: {}, instanceCount: {}",
                    req.getGroupId(), instanceCount);
            BlinkException.throwBusinessException(INSTANCE_GROUP_HAS_INSTANCES);
        }

        // 删除分组
        gatewayInstanceGroupMapper.deleteById(req.getGroupId());

        log.info("[InstanceGroup] 删除分组成功 | groupId: {}, groupKey: {}",
                req.getGroupId(), existGroup.getGroupKey());

        return ResponseDTO.newSuccessInstance(null);
    }

    @Override
    public ResponseDTO<List<InstanceGroupVO>> getEnabledInstanceGroups() {
        // 查询所有启用的分组
        LambdaQueryWrapper<GatewayInstanceGroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GatewayInstanceGroupDO::getStatus, STATUS_ENABLE);
        queryWrapper.orderByAsc(GatewayInstanceGroupDO::getGroupName);

        List<GatewayInstanceGroupDO> groupList = gatewayInstanceGroupMapper.selectList(queryWrapper);

        // 转换为 VO
        List<InstanceGroupVO> voList = CollUtil.isEmpty(groupList)
                ? List.of()
                : groupList.stream()
                        .map(this::convertToVO)
                        .collect(Collectors.toList());

        log.info("[InstanceGroup] 获取启用分组列表成功 | count: {}", voList.size());

        return ResponseDTO.newSuccessInstance(voList);
    }

    /**
     * 将 DO 转换为 VO
     *
     * @param groupDO 分组实体
     * @return 分组视图对象
     */
    private InstanceGroupVO convertToVO(GatewayInstanceGroupDO groupDO) {
        InstanceGroupVO vo = BeanUtil.copyProperties(groupDO, InstanceGroupVO.class);
        return vo;
    }
}
