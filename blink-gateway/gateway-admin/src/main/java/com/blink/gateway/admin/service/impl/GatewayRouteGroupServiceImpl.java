package com.blink.gateway.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.admin.dto.req.AddRouteGroupReq;
import com.blink.gateway.admin.dto.req.DeleteRouteGroupReq;
import com.blink.gateway.admin.dto.req.GetRouteGroupReq;
import com.blink.gateway.admin.dto.req.QueryRouteGroupReq;
import com.blink.gateway.admin.dto.req.UpdateRouteGroupReq;
import com.blink.gateway.admin.dto.rsp.RouteGroupListRsp;
import com.blink.gateway.admin.dto.vo.RouteGroupVO;
import com.blink.gateway.admin.entity.GatewayInstanceDO;
import com.blink.gateway.admin.entity.GatewayRouteGroupDO;
import com.blink.gateway.admin.mapper.GatewayRouteGroupMapper;
import com.blink.gateway.admin.mapper.GatewayInstanceMapper;
import com.blink.gateway.admin.service.GatewayRouteGroupService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.ErrCodeConstant.ROUTE_GROUP_HAS_INSTANCES;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ROUTE_GROUP_KEY_EXISTS;
import static com.blink.gateway.admin.constants.ErrCodeConstant.ROUTE_GROUP_NOT_EXIST;
import static com.blink.gateway.admin.constants.RouteConstant.STATUS_ENABLE;

/**
 * 路由分组服务实现类
 *
 * @author binblink
 * @since 2026-04-18
 */
@Service
@Slf4j
public class GatewayRouteGroupServiceImpl implements GatewayRouteGroupService {

    @Resource
    private GatewayRouteGroupMapper gatewayRouteGroupMapper;

    @Resource
    private GatewayInstanceMapper gatewayInstanceMapper;

    @Override
    public ResponseDTO<RouteGroupListRsp> queryRouteGroupList(QueryRouteGroupReq req) {
        // 构建查询条件
        LambdaQueryWrapper<GatewayRouteGroupDO> queryWrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(req.getGroupKey())) {
            queryWrapper.like(GatewayRouteGroupDO::getGroupKey, req.getGroupKey());
        }
        if (StrUtil.isNotBlank(req.getGroupName())) {
            queryWrapper.like(GatewayRouteGroupDO::getGroupName, req.getGroupName());
        }

        // 默认按更新时间降序排序
        if (StrUtil.isBlank(req.getOrderBy())) {
            req.setOrderBy("update_time desc");
        }

        // 分页查询
        RouteGroupListRsp rsp = PageUtils.queryPage(req,
                () -> gatewayRouteGroupMapper.selectList(queryWrapper),
                new RouteGroupListRsp());

        // 转换 DO 到 VO 并计算每个分组的实例数量
        List rawList = rsp.getRows();
        if (CollUtil.isNotEmpty(rawList)) {
            List<RouteGroupVO> voList = new ArrayList<>();
            for (Object obj : rawList) {
                GatewayRouteGroupDO groupDO = (GatewayRouteGroupDO) obj;
                RouteGroupVO vo = BeanUtil.copyProperties(groupDO, RouteGroupVO.class);

                // 查询该分组绑定的实例数量
                LambdaQueryWrapper<GatewayInstanceDO> instanceQuery = new LambdaQueryWrapper<>();
                instanceQuery.eq(GatewayInstanceDO::getGroupKey, groupDO.getGroupKey());
                Long instanceCount = gatewayInstanceMapper.selectCount(instanceQuery);
                vo.setInstanceCount(instanceCount.intValue());

                voList.add(vo);
            }
            rsp.setRows(voList);
        }

        log.info("[RouteGroup] 分页查询分组列表成功 | total: {}, pageNum: {}, pageSize: {}",
                rsp.getTotal(), req.getPageNum(), req.getPageSize());

        return ResponseDTO.newSuccessInstance(rsp);
    }

    @Override
    public ResponseDTO<RouteGroupVO> getRouteGroupDetail(GetRouteGroupReq req) {
        GatewayRouteGroupDO groupDO = gatewayRouteGroupMapper.selectById(req.getGroupId());

        if (ObjectUtil.isNull(groupDO)) {
            BlinkException.throwBusinessException(ROUTE_GROUP_NOT_EXIST);
        }

        RouteGroupVO vo = convertToVO(groupDO);

        log.info("[RouteGroup] 获取分组详情成功 | groupId: {}, groupKey: {}", req.getGroupId(), vo.getGroupKey());

        return ResponseDTO.newSuccessInstance(vo);
    }

    @Override
    public ResponseDTO<Void> addRouteGroup(AddRouteGroupReq req) {
        // 检查 groupKey 是否已存在
        LambdaQueryWrapper<GatewayRouteGroupDO> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(GatewayRouteGroupDO::getGroupKey, req.getGroupKey());
        Long count = gatewayRouteGroupMapper.selectCount(existQuery);

        if (count > 0) {
            log.warn("[RouteGroup] 新增分组失败，groupKey 已存在 | groupKey: {}", req.getGroupKey());
            BlinkException.throwBusinessException(ROUTE_GROUP_KEY_EXISTS);
        }

        // 构建实体并保存
        GatewayRouteGroupDO groupDO = BeanUtil.copyProperties(req, GatewayRouteGroupDO.class);
        gatewayRouteGroupMapper.insert(groupDO);

        log.info("[RouteGroup] 新增分组成功 | groupId: {}, groupKey: {}, groupName: {}",
                groupDO.getGroupId(), groupDO.getGroupKey(), groupDO.getGroupName());

        return ResponseDTO.newSuccessInstance(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Void> updateRouteGroup(UpdateRouteGroupReq req) {
        // 检查分组是否存在
        GatewayRouteGroupDO existGroup = gatewayRouteGroupMapper.selectById(req.getGroupId());

        if (ObjectUtil.isNull(existGroup)) {
            BlinkException.throwBusinessException(ROUTE_GROUP_NOT_EXIST);
        }

        // 如果 groupKey 有变更，检查新的 groupKey 是否已被其他分组使用
        if (!existGroup.getGroupKey().equals(req.getGroupKey())) {
            LambdaQueryWrapper<GatewayRouteGroupDO> keyQuery = new LambdaQueryWrapper<>();
            keyQuery.eq(GatewayRouteGroupDO::getGroupKey, req.getGroupKey());
            keyQuery.ne(GatewayRouteGroupDO::getGroupId, req.getGroupId());
            Long count = gatewayRouteGroupMapper.selectCount(keyQuery);

            if (count > 0) {
                log.warn("[RouteGroup] 更新分组失败，groupKey 已被占用 | groupId: {}, groupKey: {}",
                        req.getGroupId(), req.getGroupKey());
                BlinkException.throwBusinessException(ROUTE_GROUP_KEY_EXISTS);
            }
        }

        // 更新分组
        GatewayRouteGroupDO updateDO = BeanUtil.copyProperties(req, GatewayRouteGroupDO.class);
        gatewayRouteGroupMapper.updateById(updateDO);

        log.info("[RouteGroup] 更新分组成功 | groupId: {}, groupKey: {}", req.getGroupId(), req.getGroupKey());

        return ResponseDTO.newSuccessInstance(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Void> deleteRouteGroup(DeleteRouteGroupReq req) {
        // 检查分组是否存在
        GatewayRouteGroupDO existGroup = gatewayRouteGroupMapper.selectById(req.getGroupId());

        if (ObjectUtil.isNull(existGroup)) {
            BlinkException.throwBusinessException(ROUTE_GROUP_NOT_EXIST);
        }

        // 检查分组下是否有关联的实例
        LambdaQueryWrapper<GatewayInstanceDO> instanceQuery = new LambdaQueryWrapper<>();
        instanceQuery.eq(GatewayInstanceDO::getGroupKey, existGroup.getGroupKey());
        Long instanceCount = gatewayInstanceMapper.selectCount(instanceQuery);

        if (instanceCount > 0) {
            log.warn("[RouteGroup] 删除分组失败，分组下存在关联实例 | groupId: {}, instanceCount: {}",
                    req.getGroupId(), instanceCount);
            BlinkException.throwBusinessException(ROUTE_GROUP_HAS_INSTANCES);
        }

        // 删除分组
        gatewayRouteGroupMapper.deleteById(req.getGroupId());

        log.info("[RouteGroup] 删除分组成功 | groupId: {}, groupKey: {}",
                req.getGroupId(), existGroup.getGroupKey());

        return ResponseDTO.newSuccessInstance(null);
    }

    @Override
    public ResponseDTO<List<RouteGroupVO>> getEnabledRouteGroups() {
        // 查询所有启用的分组
        LambdaQueryWrapper<GatewayRouteGroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GatewayRouteGroupDO::getStatus, STATUS_ENABLE);
        queryWrapper.orderByAsc(GatewayRouteGroupDO::getGroupName);

        List<GatewayRouteGroupDO> groupList = gatewayRouteGroupMapper.selectList(queryWrapper);

        // 转换为 VO
        List<RouteGroupVO> voList = CollUtil.isEmpty(groupList)
                ? List.of()
                : groupList.stream()
                        .map(this::convertToVO)
                        .collect(Collectors.toList());

        log.info("[RouteGroup] 获取启用分组列表成功 | count: {}", voList.size());

        return ResponseDTO.newSuccessInstance(voList);
    }

    /**
     * 将 DO 转换为 VO
     *
     * @param groupDO 分组实体
     * @return 分组视图对象
     */
    private RouteGroupVO convertToVO(GatewayRouteGroupDO groupDO) {
        RouteGroupVO vo = BeanUtil.copyProperties(groupDO, RouteGroupVO.class);
        return vo;
    }
}
