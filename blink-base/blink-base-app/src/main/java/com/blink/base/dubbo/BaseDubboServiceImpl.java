package com.blink.base.dubbo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.dto.req.GetAllApiPermissionsReq;
import com.blink.base.dto.req.QueryErrMsgReq;
import com.blink.base.dto.req.QueryOneSysConfigReq;
import com.blink.base.dto.req.QuerySimpleUserReq;
import com.blink.base.dto.req.QueryUserPermissionReq;
import com.blink.base.dto.req.UserIdReq;
import com.blink.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.base.dto.rsp.QuerySimpleUserRsp;
import com.blink.base.dto.rsp.QueryUserPermissionRsp;
import com.blink.base.dto.rsp.UserPermissionDetailRsp;
import com.blink.base.dto.vo.SysConfigVO;
import com.blink.base.dto.vo.SysRoleVO;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.dto.vo.DataFilterVO;
import com.blink.base.dubbo.service.BaseDubboService;
import com.blink.base.entity.SysUserDO;
import com.blink.base.entity.SysRoleDO;
import com.blink.base.entity.SysPermissionDO;
import com.blink.base.entity.SysUserRoleRelaDO;
import com.blink.base.entity.SysRolePermRelaDO;
import com.blink.base.entity.SysDataFilterDO;
import com.blink.base.mapper.SysUserMapper;
import com.blink.base.mapper.SysRoleMapper;
import com.blink.base.mapper.SysPermissionMapper;
import com.blink.base.mapper.SysUserRoleRelaMapper;
import com.blink.base.mapper.SysRolePermRelaMapper;
import com.blink.base.mapper.SysDataFilterMapper;
import com.blink.base.service.SysConfigService;
import com.blink.base.service.SysErrorMsgService;
import com.blink.base.service.SysPermissionService;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.blink.base.constants.BaseErrCodeConstant.*;
import static com.blink.base.constants.CommonConstans.*;
import static com.blink.framework.core.data.CoreConstant.IO_THREADPOOL;

/**
 * Dubbo 基础服务实现类
 * <p>
 * 作为 Dubbo Provider，为其他服务提供基础数据查询能力。
 * 本类仅做服务包装和对象转换，不涉及具体业务逻辑实现。
 * </p>
 *
 * @author blink
 * @since 1.0.0
 */
@Slf4j
@Service
@DubboService(interfaceClass = BaseDubboService.class)
public class BaseDubboServiceImpl implements BaseDubboService {

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private SysPermissionService sysPermissionService;

    @Resource
    private SysErrorMsgService sysErrorMsgService;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysDataFilterMapper sysDataFilterMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysUserRoleRelaMapper sysUserRoleRelaMapper;

    @Resource
    private SysRolePermRelaMapper sysRolePermRelaMapper;

    /**
     * IO密集型线程池，用于 Dubbo 异步调用（可选注入）
     */
    private Executor ioThreadPool;

    /**
     * 尝试获取 IO 线程池 Bean，如果不存在则使用默认 ForkJoinPool
     */
    @Autowired
    public void setIoThreadPool(ApplicationContext applicationContext) {
        try {
            this.ioThreadPool = applicationContext.getBean(IO_THREADPOOL, Executor.class);
        } catch (Exception e) {
            log.info("IO线程池未配置，将使用默认 ForkJoinPool");
        }
    }

    /**
     * 根据配置key获取单个系统配置
     * <p>
     * 优先从缓存获取，缓存不存在则从数据库查询
     * </p>
     *
     * @param reqDto 请求参数，包含配置key
     * @return ResponseDTO<SysConfigCacheDO> 配置信息响应
     * @throws BlinkException 当查询发生异常时抛出
     */
    @Override
    public ResponseDTO<SysConfigCacheDO> getOneConfig(RequestDTO<QueryOneSysConfigReq> reqDto) {
        try {
            QueryOneSysConfigReq req = reqDto.getBody();
            SysConfigVO configVO = sysConfigService.getOneConfigFromDataBase(req);

            // 如果配置不存在，返回空响应
            if (Objects.isNull(configVO)) {
                BlinkException.throwBusinessException(CONFIG_NOT_EXIST);
            }

            // 使用 BeanUtil 进行对象属性拷贝
            SysConfigCacheDO cacheDO = BeanUtil.copyProperties(configVO, SysConfigCacheDO.class);

            return ResponseDTO.newSuccessInstance(cacheDO);
        } catch (BlinkException e) {
            log.warn("获取系统配置失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取系统配置异常", e);
            throw new BlinkException(e.getMessage());
        }
    }



    /**
     * 获取错误提示信息
     * <p>
     * 根据错误码和语言获取对应的错误提示信息
     * </p>
     *
     * @param reqDto 请求参数，包含错误码和语言
     * @return ResponseDTO<QueryErrMsgRsp> 错误信息响应
     * @throws BlinkException 当查询发生异常时抛出
     */
    @Override
    public ResponseDTO<QueryErrMsgRsp> getErrorMsgInfo(RequestDTO<QueryErrMsgReq> reqDto) {
        try {
            QueryErrMsgReq req = reqDto.getBody();
            QueryErrMsgRsp rsp = sysErrorMsgService.getErrorMsg(req);

            if (Objects.isNull(rsp)) {
                BlinkException.throwBusinessException(ERR_MSG_NOT_EXIST);
            }

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            log.warn("获取错误消息失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取错误消息异常", e);
            throw new BlinkException(e.getMessage());
        }
    }

    /**
     * 根据用户ID获取用户权限标识
     * <p>
     * 查询指定用户的所有权限标识列表
     * </p>
     *
     * @param reqDto 请求参数，包含用户ID
     * @return ResponseDTO<QueryUserPermissionRsp> 用户权限响应
     * @throws BlinkException 当查询发生异常时抛出
     */
    @Override
    public ResponseDTO<QueryUserPermissionRsp> getUserPermissionsByUerId(RequestDTO<QueryUserPermissionReq> reqDto) {
        try {
            QueryUserPermissionReq req = reqDto.getBody();
            QueryUserPermissionRsp rsp = sysPermissionService.getPermissions(req);

            if (Objects.isNull(rsp)) {
                BlinkException.throwBusinessException(USER_PERMISSION_NOT_EXIST);
            }

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            log.warn("获取用户权限失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取用户权限异常", e);
            throw new BlinkException(e.getMessage());
        }
    }

    /**
     * 根据请求路径获取用户权限标识
     * <p>
     * 查询指定路径所需的所有权限标识列表
     * </p>
     *
     * @param reqDto 请求参数，包含请求路径
     * @return ResponseDTO<QueryUserPermissionRsp> 权限信息响应
     * @throws BlinkException 当查询发生异常时抛出
     */
    @Override
    public ResponseDTO<QueryUserPermissionRsp> getUserPermissionsByPath(RequestDTO<QueryUserPermissionReq> reqDto) {
        try {
            QueryUserPermissionReq req = reqDto.getBody();
            QueryUserPermissionRsp rsp = sysPermissionService.getPermissions(req);

            if (Objects.isNull(rsp)) {
                BlinkException.throwBusinessException(PATH_PERMISSION_NOT_EXIST);
            }

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            log.warn("获取路径权限失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取路径权限异常", e);
            throw new BlinkException(e.getMessage());
        }
    }

    /**
     * 获取所有接口权限
     * <p>
     * 查询系统中所有API接口的权限配置信息
     * </p>
     *
     * @param reqDto 请求参数
     * @return ResponseDTO<GetAllApiPermissionsRsp> 所有接口权限响应
     * @throws BlinkException 当查询发生异常时抛出
     */
    @Override
    public ResponseDTO<GetAllApiPermissionsRsp> getAllApiPermissions(RequestDTO<GetAllApiPermissionsReq> reqDto) {
        try {
            GetAllApiPermissionsReq req = reqDto.getBody();
            GetAllApiPermissionsRsp rsp = sysPermissionService.getAllApiPermission(req);

            if (Objects.isNull(rsp)) {
                BlinkException.throwBusinessException(API_PERMISSION_NOT_EXIST);
            }

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            log.warn("获取所有接口权限失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取所有接口权限异常", e);
            throw new BlinkException(e.getMessage());
        }
    }

    // ==================== 异步方法实现 ====================

    @Override
    public CompletableFuture<ResponseDTO<SysConfigCacheDO>> getOneConfigAsync(RequestDTO<QueryOneSysConfigReq> reqDto) {
        if (ioThreadPool != null) {
            return CompletableFuture.supplyAsync(() -> getOneConfig(reqDto), ioThreadPool);
        }
        return CompletableFuture.supplyAsync(() -> getOneConfig(reqDto));
    }

    @Override
    public CompletableFuture<ResponseDTO<QueryErrMsgRsp>> getErrorMsgInfoAsync(RequestDTO<QueryErrMsgReq> reqDto) {
        if (ioThreadPool != null) {
            return CompletableFuture.supplyAsync(() -> getErrorMsgInfo(reqDto), ioThreadPool);
        }
        return CompletableFuture.supplyAsync(() -> getErrorMsgInfo(reqDto));
    }

    @Override
    public CompletableFuture<ResponseDTO<QueryUserPermissionRsp>> getUserPermissionsByUerIdAsync(RequestDTO<QueryUserPermissionReq> reqDto) {
        if (ioThreadPool != null) {
            return CompletableFuture.supplyAsync(() -> getUserPermissionsByUerId(reqDto), ioThreadPool);
        }
        return CompletableFuture.supplyAsync(() -> getUserPermissionsByUerId(reqDto));
    }

    @Override
    public CompletableFuture<ResponseDTO<QueryUserPermissionRsp>> getUserPermissionsByPathAsync(RequestDTO<QueryUserPermissionReq> reqDto) {
        if (ioThreadPool != null) {
            return CompletableFuture.supplyAsync(() -> getUserPermissionsByPath(reqDto), ioThreadPool);
        }
        return CompletableFuture.supplyAsync(() -> getUserPermissionsByPath(reqDto));
    }

    @Override
    public CompletableFuture<ResponseDTO<GetAllApiPermissionsRsp>> getAllApiPermissionsAsync(RequestDTO<GetAllApiPermissionsReq> reqDto) {
        if (ioThreadPool != null) {
            return CompletableFuture.supplyAsync(() -> getAllApiPermissions(reqDto), ioThreadPool);
        }
        return CompletableFuture.supplyAsync(() -> getAllApiPermissions(reqDto));
    }

    // ==================== 渠道关联用户选择 ====================

    /**
     * 查询简化用户列表（用于弹窗选择）
     *
     * @param reqDto 请求参数
     * @return 用户列表
     */
    @Override
    public ResponseDTO<QuerySimpleUserRsp> getSimpleUserList(RequestDTO<QuerySimpleUserReq> reqDto) {
        try {
            QuerySimpleUserReq req = reqDto.getBody();
            QuerySimpleUserRsp rsp = new QuerySimpleUserRsp();
            PageUtils.queryPage(req, () -> sysUserMapper.selectSimpleUserList(req), rsp);
            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            log.warn("[BaseDubbo] 查询简化用户列表失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[BaseDubbo] 查询简化用户列表异常", e);
            throw new BlinkException(e.getMessage(), e, SIMPLE_USER_LIST_QUERY_FAILED);
        }
    }

    /**
     * 查询用户权限详情（角色、接口权限、数据过滤权限）
     *
     * @param reqDto 请求参数
     * @return 权限详情
     */
    @Override
    public ResponseDTO<UserPermissionDetailRsp> getUserPermissionDetail(RequestDTO<UserIdReq> reqDto) {
        try {
            UserIdReq req = reqDto.getBody();
            Integer userId = req.getUserId();

            // 查询用户是否存在
            SysUserDO user = sysUserMapper.selectById(userId);
            if (Objects.isNull(user)) {
                BlinkException.throwBusinessException(USER_NOT_EXIST);
            }

            UserPermissionDetailRsp rsp = new UserPermissionDetailRsp();

            // 超级管理员拥有所有权限
            if (SUPER_ADMIN_YES.equals(user.getSuperFlag())) {
                // 查询所有角色
                List<SysRoleDO> allRoles = sysRoleMapper.selectList(
                        new LambdaQueryWrapper<SysRoleDO>().eq(SysRoleDO::getStatus, SWITCH_OPEN));
                rsp.setRoles(BeanUtil.copyToList(allRoles, SysRoleVO.class));

                // 查询所有接口权限
                List<SysPermissionDO> allPermissions = sysPermissionMapper.selectList(
                        new LambdaQueryWrapper<SysPermissionDO>()
                                .eq(SysPermissionDO::getAcType, PERMISSION_API_TYPE));
                rsp.setPermissions(BeanUtil.copyToList(allPermissions, SysPermissionVO.class));

                // 查询所有数据过滤规则
                List<SysDataFilterDO> allDataFilters = sysDataFilterMapper.selectList(
                        new LambdaQueryWrapper<SysDataFilterDO>().eq(SysDataFilterDO::getStatus, SWITCH_OPEN));
                rsp.setDataFilters(BeanUtil.copyToList(allDataFilters, DataFilterVO.class));

                return ResponseDTO.newSuccessInstance(rsp);
            }

            // 查询用户关联的角色
            List<SysUserRoleRelaDO> userRoleRelas = sysUserRoleRelaMapper.selectList(
                    new LambdaQueryWrapper<SysUserRoleRelaDO>().eq(SysUserRoleRelaDO::getUserId, userId));

            if (CollUtil.isEmpty(userRoleRelas)) {
                rsp.setRoles(new ArrayList<>());
                rsp.setPermissions(new ArrayList<>());
                rsp.setDataFilters(new ArrayList<>());
                return ResponseDTO.newSuccessInstance(rsp);
            }

            List<Integer> roleIds = userRoleRelas.stream()
                    .map(SysUserRoleRelaDO::getRoleId)
                    .collect(Collectors.toList());

            // 查询角色信息
            List<SysRoleDO> roles = sysRoleMapper.selectList(
                    new LambdaQueryWrapper<SysRoleDO>()
                            .in(SysRoleDO::getRoleId, roleIds)
                            .eq(SysRoleDO::getStatus, SWITCH_OPEN));
            rsp.setRoles(BeanUtil.copyToList(roles, SysRoleVO.class));

            // 查询角色关联的权限ID
            List<SysRolePermRelaDO> permRelas = sysRolePermRelaMapper.selectList(
                    new LambdaQueryWrapper<SysRolePermRelaDO>().in(SysRolePermRelaDO::getRoleId, roleIds));

            if (CollUtil.isNotEmpty(permRelas)) {
                Set<Integer> permIds = permRelas.stream()
                        .map(SysRolePermRelaDO::getAcId)
                        .collect(Collectors.toSet());

                // 查询接口权限（acType=1）
                List<SysPermissionDO> permissions = sysPermissionMapper.selectList(
                        new LambdaQueryWrapper<SysPermissionDO>()
                                .in(SysPermissionDO::getAcId, permIds)
                                .eq(SysPermissionDO::getAcType, PERMISSION_API_TYPE));
                rsp.setPermissions(BeanUtil.copyToList(permissions, SysPermissionVO.class));
            } else {
                rsp.setPermissions(new ArrayList<>());
            }

            // 查询数据过滤权限
            List<DataFilterVO> dataFilters = sysDataFilterMapper.selectDataFiltersByRoleIds(roleIds);
            rsp.setDataFilters(CollUtil.isEmpty(dataFilters) ? new ArrayList<>() : dataFilters);

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            log.warn("[BaseDubbo] 查询用户权限详情失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[BaseDubbo] 查询用户权限详情异常", e);
            throw new BlinkException(e.getMessage(), e, USER_PERMISSION_DETAIL_QUERY_FAILED);
        }
    }

    @Override
    public CompletableFuture<ResponseDTO<QuerySimpleUserRsp>> getSimpleUserListAsync(RequestDTO<QuerySimpleUserReq> reqDto) {
        if (ioThreadPool != null) {
            return CompletableFuture.supplyAsync(() -> getSimpleUserList(reqDto), ioThreadPool);
        }
        return CompletableFuture.supplyAsync(() -> getSimpleUserList(reqDto));
    }

    @Override
    public CompletableFuture<ResponseDTO<UserPermissionDetailRsp>> getUserPermissionDetailAsync(RequestDTO<UserIdReq> reqDto) {
        if (ioThreadPool != null) {
            return CompletableFuture.supplyAsync(() -> getUserPermissionDetail(reqDto), ioThreadPool);
        }
        return CompletableFuture.supplyAsync(() -> getUserPermissionDetail(reqDto));
    }

}