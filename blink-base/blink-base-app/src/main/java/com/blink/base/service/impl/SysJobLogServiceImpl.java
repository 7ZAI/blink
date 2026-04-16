package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.dto.req.QuerySysJobLogReq;
import com.blink.base.dto.rsp.SysJobLogRsp;
import com.blink.base.dto.rsp.SysJobLogVO;
import com.blink.base.entity.SysJobLogDO;
import com.blink.base.mapper.SysJobLogMapper;
import com.blink.base.service.SysJobLogService;
import com.blink.datasource.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时任务日志服务实现
 *
 * @author binblink
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysJobLogServiceImpl implements SysJobLogService {

    private final SysJobLogMapper sysJobLogMapper;

    @Override
    public SysJobLogRsp getLogList(QuerySysJobLogReq req) {
        LambdaQueryWrapper<SysJobLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtil.isNotNull(req.getJobId()), SysJobLogDO::getJobId, req.getJobId())
                .like(ObjectUtil.isNotNull(req.getJobName()), SysJobLogDO::getJobName, req.getJobName())
                .eq(ObjectUtil.isNotNull(req.getStatus()), SysJobLogDO::getStatus, req.getStatus())
                .ge(ObjectUtil.isNotNull(req.getTriggerTimeStart()), SysJobLogDO::getTriggerTime, req.getTriggerTimeStart())
                .le(ObjectUtil.isNotNull(req.getTriggerTimeEnd()), SysJobLogDO::getTriggerTime, req.getTriggerTimeEnd())
                .orderByDesc(SysJobLogDO::getTriggerTime);

        SysJobLogRsp rsp = new SysJobLogRsp();
        PageUtils.queryPage(req, () -> sysJobLogMapper.selectList(wrapper), rsp);

        // 转换 DO 到 VO
        if (CollUtil.isNotEmpty(rsp.getRows())) {
            List<SysJobLogVO> voList = rsp.getRows().stream()
                    .map(d -> BeanUtil.copyProperties(d, SysJobLogVO.class))
                    .collect(Collectors.toList());
            rsp.setRows(voList);
        }

        return rsp;
    }

    @Override
    public Long saveLog(Long jobId, String jobName, String jobGroup) {
        SysJobLogDO logDO = new SysJobLogDO();
        logDO.setJobId(jobId);
        logDO.setJobName(jobName);
        logDO.setJobGroup(jobGroup);
        logDO.setTriggerTime(LocalDateTime.now());
        logDO.setStatus((byte) 0);
        logDO.setExecuteCount(0);
        logDO.setCreateTime(LocalDateTime.now());

        sysJobLogMapper.insert(logDO);
        return logDO.getLogId();
    }

    @Override
    public void updateLogResult(Long logId, com.blink.job.api.dto.JobExecutionResult result, long duration) {
        SysJobLogDO logDO = new SysJobLogDO();
        logDO.setLogId(logId);
        logDO.setFinishTime(LocalDateTime.now());
        logDO.setDuration(duration);
        logDO.setStatus(result.isSuccess() ? (byte) 1 : (byte) 2);
        logDO.setResultMessage(result.getMessage());
        logDO.setErrorMessage(result.getErrorMessage());

        sysJobLogMapper.updateById(logDO);
    }

    @Override
    public void cleanExpiredLogs(Integer retentionDays) {
        if (retentionDays == null || retentionDays <= 0) {
            return;
        }

        LocalDateTime expireTime = LocalDateTime.now().minusDays(retentionDays);
        LambdaQueryWrapper<SysJobLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SysJobLogDO::getCreateTime, expireTime);

        int deleted = sysJobLogMapper.delete(wrapper);
        log.info("[SysJobLogService] 清理过期日志完成 | retentionDays: {}, deleted: {}", retentionDays, deleted);
    }
}
