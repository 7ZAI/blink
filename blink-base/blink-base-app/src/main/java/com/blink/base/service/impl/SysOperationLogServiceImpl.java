package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.dto.req.QueryOperationLogReq;
import com.blink.base.dto.rsp.OperationLogDetailRsp;
import com.blink.base.dto.rsp.OperationLogRsp;
import com.blink.base.dto.vo.OperationLogVO;
import com.blink.base.entity.SysOperationLogDO;
import com.blink.framework.core.data.CoreConstant;
import com.blink.log.constant.LogType;
import com.blink.base.mapper.SysOperationLogMapper;
import com.blink.base.service.SysOperationLogService;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 操作日志服务实现类
 *
 * @author binblink
 */
@Slf4j
@Service
public class SysOperationLogServiceImpl implements SysOperationLogService {

    /**
     * 单次归档批量处理数量
     */
    private static final int ARCHIVE_BATCH_SIZE = 1000;

    @Resource
    private SysOperationLogMapper sysOperationLogMapper;

    /**
     * 查询操作日志列表
     *
     * @param req 查询参数
     * @return 日志列表
     */
    @Override
    public OperationLogRsp getOperationLogList(QueryOperationLogReq req) throws BlinkException {
        OperationLogRsp rsp = new OperationLogRsp();

        // 构建查询条件
        LambdaQueryWrapper<SysOperationLogDO> wrapper = new LambdaQueryWrapper<>();
        // 登录名模糊查询
        wrapper.like(StrUtil.isNotBlank(req.getLoginName()), SysOperationLogDO::getLoginName, req.getLoginName());
        // 日志类型
        wrapper.eq(StrUtil.isNotBlank(req.getLogType()),SysOperationLogDO::getLogType, req.getLogType());
        // 执行状态
        wrapper.eq(Objects.nonNull(req.getExecuteStatus()),SysOperationLogDO::getExecuteStatus, req.getExecuteStatus());
        // 时间范围
        wrapper.ge(Objects.nonNull(req.getStartTime()),SysOperationLogDO::getOperationTime, req.getStartTime());
        wrapper.le(Objects.nonNull(req.getEndTime()),SysOperationLogDO::getOperationTime, req.getEndTime());

        // 关键词搜索（描述或URL）
        if (StrUtil.isNotBlank(req.getKeyword())) {
            wrapper.and(w -> w.like(SysOperationLogDO::getDescription, req.getKeyword())
                    .or()
                    .like(SysOperationLogDO::getRequestUrl, req.getKeyword()));
        }

        // 排序：支持动态排序，默认按操作时间降序
        String orderBy = req.getOrderBy();
        if (StrUtil.isNotBlank(orderBy)) {
            // 解析排序参数，格式：字段名 asc/desc
            String[] parts = orderBy.split(" ");
            if (parts.length >= 1) {
                String field = parts[0];
                boolean isAsc = parts.length > 1 && "asc".equalsIgnoreCase(parts[1]);

                // 根据字段名进行排序
                switch (field) {
                    case "operationTime":
                        // 操作时间排序
                        if (isAsc) {
                            wrapper.orderByAsc(SysOperationLogDO::getOperationTime);
                        } else {
                            wrapper.orderByDesc(SysOperationLogDO::getOperationTime);
                        }
                        break;
                    case "executeTimeMs":
                        // 执行时长排序
                        if (isAsc) {
                            wrapper.orderByAsc(SysOperationLogDO::getExecuteTimeMs);
                        } else {
                            wrapper.orderByDesc(SysOperationLogDO::getExecuteTimeMs);
                        }
                        break;
                    default:
                        // 其他字段默认按操作时间降序
                        wrapper.orderByDesc(SysOperationLogDO::getOperationTime);
                        break;
                }
            }
        } else {
            // 默认按操作时间降序
            wrapper.orderByDesc(SysOperationLogDO::getOperationTime);
        }

        // 清空 orderBy，避免 PageUtils 再次应用排序（会导致驼峰字段名无法转换为下划线）
        req.setOrderBy(null);

        // 使用自定义分页查询，支持查询后转换VO
        return PageUtils.queryPageCustom(
                req,
                () -> sysOperationLogMapper.selectCount(wrapper),
                () -> {
                    List<SysOperationLogDO> list = sysOperationLogMapper.selectList(wrapper);
                    return list.stream().map(this::convertToVO).toList();
                },
                rsp
        );
    }

    /**
     * 查询操作日志详情
     *
     * @param logId 日志ID
     * @return 日志详情
     */
    @Override
    public OperationLogDetailRsp getOperationLogDetail(Long logId) throws BlinkException {
        if (ObjectUtil.isEmpty(logId)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PARAMETER_NOT_NULL);
        }

        SysOperationLogDO logDO = sysOperationLogMapper.selectById(logId);
        if (ObjectUtil.isEmpty(logDO)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.OPERATION_LOG_NOT_EXIST);
        }

        return convertToDetailRsp(logDO);
    }

    /**
     * 异步保存操作日志
     *
     * @param logDO 日志实体
     */
    @Async(CoreConstant.IO_THREADPOOL)
    @Override
    public void asyncSaveLog(SysOperationLogDO logDO) {
        try {
            sysOperationLogMapper.insert(logDO);
        } catch (Exception e) {
            log.error("保存操作日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 归档日志
     * <p>
     * 将指定日期之前的日志归档到历史表
     *
     * @param beforeDate 归档截止日期（不包含该日期）
     * @return 归档数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int archiveLogs(LocalDate beforeDate) throws BlinkException {
        if (Objects.isNull(beforeDate)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PARAMETER_NOT_NULL);
        }

        // 转换为 LocalDateTime（当天的开始时间）
        LocalDateTime beforeTime = beforeDate.atStartOfDay();

        int totalArchived = 0;
        int batchCount;

        do {
            // 查询需要归档的日志ID
            List<Long> logIds = sysOperationLogMapper.selectArchiveLogIds(beforeTime, ARCHIVE_BATCH_SIZE);

            if (logIds.isEmpty()) {
                break;
            }

            batchCount = logIds.size();

            // 归档到历史表
            int archivedCount = sysOperationLogMapper.batchArchiveToHistory(logIds);

            if (archivedCount != batchCount) {
                log.warn("归档数量不匹配: 预期{}, 实际{}", batchCount, archivedCount);
            }

            // 删除已归档的日志
            int deletedCount = sysOperationLogMapper.batchDeleteByIds(logIds);

            if (deletedCount != batchCount) {
                log.warn("删除数量不匹配: 预期{}, 实际{}", batchCount, deletedCount);
            }

            totalArchived += archivedCount;

            log.info("已归档 {} 条操作日志", archivedCount);

        } while (batchCount >= ARCHIVE_BATCH_SIZE);

        log.info("操作日志归档完成，共归档 {} 条数据", totalArchived);
        return totalArchived;
    }

    /**
     * 转换为VO
     *
     * @param logDO 日志实体
     * @return VO对象
     */
    private OperationLogVO convertToVO(SysOperationLogDO logDO) {
        OperationLogVO vo = new OperationLogVO();
        BeanUtil.copyProperties(logDO, vo);

        // 设置日志类型描述
        LogType logType = LogType.getByCode(logDO.getLogType());
        vo.setLogTypeDesc(logType.getDescription());

        // 设置执行状态描述
        vo.setExecuteStatusDesc(logDO.getExecuteStatus() == 0 ? "成功" : "失败");

        return vo;
    }

    /**
     * 转换为详情响应DTO
     *
     * @param logDO 日志实体
     * @return 详情DTO
     */
    private OperationLogDetailRsp convertToDetailRsp(SysOperationLogDO logDO) {
        OperationLogDetailRsp rsp = new OperationLogDetailRsp();
        BeanUtil.copyProperties(logDO, rsp);

        // 设置日志类型描述
        LogType logType = LogType.getByCode(logDO.getLogType());
        rsp.setLogTypeDesc(logType.getDescription());

        // 设置执行状态描述
        rsp.setExecuteStatusDesc(logDO.getExecuteStatus() == 0 ? "成功" : "失败");

        return rsp;
    }

}