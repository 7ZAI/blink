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
import com.blink.framework.common.constrant.RedisCacheKeyConstant;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.component.NacosConfigComponent;
import com.blink.gateway.admin.constants.RouteConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.ErrCodeConstant.CREATE_ROUTE_CONFIG_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.DELETE_ROUTE_CONFIG_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.STORAGE_MODE_INVALID;
import static com.blink.gateway.admin.constants.ErrCodeConstant.PARAMETER_NOT_NULL;
import static com.blink.gateway.admin.constants.RouteConstant.STORAGE_MODE_NACOS;
import static com.blink.gateway.admin.constants.RouteConstant.STORAGE_MODE_REDIS;
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

    @Resource
    private NacosConfigComponent nacosConfigComponent;

    @Resource
    private RedisClient redisClient;

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
        // 校验存储方式
        validateStorageMode(req.getStorageMode());

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

        // 创建路由配置（Nacos/Redis）
        createRouteConfig(req.getGroupKey(), req.getStorageMode());

        log.info("[RouteGroup] 新增分组成功 | groupId: {}, groupKey: {}, groupName: {}, storageMode: {}",
                groupDO.getGroupId(), groupDO.getGroupKey(), groupDO.getGroupName(), groupDO.getStorageMode());

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

        // 先删除路由配置（Nacos/Redis）
        deleteRouteConfig(existGroup.getGroupKey(), existGroup.getStorageMode());

        // 删除分组
        gatewayRouteGroupMapper.deleteById(req.getGroupId());

        log.info("[RouteGroup] 删除分组成功 | groupId: {}, groupKey: {}, storageMode: {}",
                req.getGroupId(), existGroup.getGroupKey(), existGroup.getStorageMode());

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
     * 校验存储方式是否合法
     *
     * @param storageMode 存储方式
     */
    private void validateStorageMode(String storageMode) {
        if (StrUtil.isBlank(storageMode)) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }
        if (!STORAGE_MODE_NACOS.equals(storageMode)
                && !STORAGE_MODE_REDIS.equals(storageMode)) {
            log.warn("[RouteGroup] 存储方式不合法 | storageMode: {}", storageMode);
            BlinkException.throwBusinessException(STORAGE_MODE_INVALID);
        }
    }

    /**
     * 创建路由配置
     *
     * @param groupKey    分组标识
     * @param storageMode 存储方式
     */
    private void createRouteConfig(String groupKey, String storageMode) {
        try {
            if (STORAGE_MODE_NACOS.equals(storageMode)) {
                // Nacos: 发布空数组配置
                String dataId = RouteConstant.NACOS_ROUTE_CONFIG_PREFIX + "-" + groupKey + RouteConstant.NACOS_ROUTE_CONFIG_SUFFIX;
                nacosConfigComponent.configPublisher(dataId, RouteConstant.NACOS_ROUTE_CONFIG_GROUP, "[]");
                log.info("[RouteGroup] 创建 Nacos 路由配置成功 | dataId: {}, group: {}", dataId, RouteConstant.NACOS_ROUTE_CONFIG_GROUP);
            } else if (STORAGE_MODE_REDIS.equals(storageMode)) {
                // Redis: 创建空 Hash（通过删除后重建确保干净状态）
                String routeKey = RedisCacheKeyConstant.GATEWAY_DYNAMIC_ROUTES_PREFIX + groupKey + ":default";
                redisClient.delete(routeKey);
                log.info("[RouteGroup] 创建 Redis 路由配置成功 | routeKey: {}", routeKey);
            }
        } catch (Exception e) {
            log.error("[RouteGroup] 创建路由配置失败 | groupKey: {}, storageMode: {}, error: {}",
                    groupKey, storageMode, e.getMessage(), e);
            BlinkException.throwBusinessException(CREATE_ROUTE_CONFIG_FAILED);
        }
    }

    /**
     * 删除路由配置
     *
     * @param groupKey    分组标识
     * @param storageMode 存储方式
     */
    private void deleteRouteConfig(String groupKey, String storageMode) {
        try {
            if (STORAGE_MODE_NACOS.equals(storageMode)) {
                // Nacos: 删除配置文件
                String dataId = RouteConstant.NACOS_ROUTE_CONFIG_PREFIX + "-" + groupKey + RouteConstant.NACOS_ROUTE_CONFIG_SUFFIX;
                nacosConfigComponent.deleteConfig(dataId, RouteConstant.NACOS_ROUTE_CONFIG_GROUP);
                log.info("[RouteGroup] 删除 Nacos 路由配置成功 | dataId: {}", dataId);
            } else if (STORAGE_MODE_REDIS.equals(storageMode)) {
                // Redis: 删除 Hash Key
                String routeKey = RedisCacheKeyConstant.GATEWAY_DYNAMIC_ROUTES_PREFIX + groupKey + ":default";
                redisClient.delete(routeKey);
                log.info("[RouteGroup] 删除 Redis 路由配置成功 | routeKey: {}", routeKey);
            }
        } catch (Exception e) {
            log.error("[RouteGroup] 删除路由配置失败 | groupKey: {}, storageMode: {}, error: {}",
                    groupKey, storageMode, e.getMessage(), e);
            BlinkException.throwBusinessException(DELETE_ROUTE_CONFIG_FAILED);
        }
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
