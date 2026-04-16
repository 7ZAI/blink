package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.constants.JobStatusConstant;
import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.SysJobRsp;
import com.blink.base.dto.rsp.SysJobVO;
import com.blink.base.entity.SysJobDO;
import com.blink.base.mapper.SysJobMapper;
import com.blink.base.service.SysJobService;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.exception.BlinkException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 定时任务服务实现
 *
 * @author binblink
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysJobServiceImpl implements SysJobService {

    private final SysJobMapper sysJobMapper;

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public SysJobRsp getJobList(QuerySysJobReq req) {
        LambdaQueryWrapper<SysJobDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ObjectUtil.isNotNull(req.getJobName()), SysJobDO::getJobName, req.getJobName())
                .eq(ObjectUtil.isNotNull(req.getJobGroup()), SysJobDO::getJobGroup, req.getJobGroup())
                .eq(ObjectUtil.isNotNull(req.getJobStatus()), SysJobDO::getJobStatus, req.getJobStatus())
                .orderByDesc(SysJobDO::getCreateTime);

        SysJobRsp rsp = new SysJobRsp();
        PageUtils.queryPage(req, () -> sysJobMapper.selectList(wrapper), rsp);

        // 转换 DO 到 VO（由于泛型擦除，PageUtils设置的是List<SysJobDO>）
        List rawList = rsp.getRows();
        if (CollUtil.isNotEmpty(rawList)) {
            List<SysJobVO> voList = new ArrayList<>();
            for (Object obj : rawList) {
                SysJobDO jobDO = (SysJobDO) obj;
                voList.add(BeanUtil.copyProperties(jobDO, SysJobVO.class));
            }
            rsp.setRows(voList);
        }

        return rsp;
    }

    @Override
    public void addJob(AddSysJobReq req) {
        // 检查任务名称是否重复
        LambdaQueryWrapper<SysJobDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysJobDO::getJobName, req.getJobName())
                .eq(SysJobDO::getJobGroup, req.getJobGroup());
        if (sysJobMapper.selectCount(wrapper) > 0) {
            log.warn("[SysJobService] 任务名称已存在 | jobName: {}, jobGroup: {}", req.getJobName(), req.getJobGroup());
            BlinkException.throwBusinessException(BaseErrCodeConstant.JOB_NAME_EXISTS);
        }

        SysJobDO jobDO = BeanUtil.copyProperties(req, SysJobDO.class);
        jobDO.setJobStatus(JobStatusConstant.NORMAL);
        jobDO.setCreateTime(LocalDateTime.now());
        jobDO.setUpdateTime(LocalDateTime.now());
        sysJobMapper.insert(jobDO);

        log.info("[SysJobService] 新增任务成功 | jobId: {}, jobName: {}", jobDO.getJobId(), jobDO.getJobName());
    }

    @Override
    public void updateJob(UpdateSysJobReq req) {
        SysJobDO existing = sysJobMapper.selectById(req.getJobId());
        if (ObjectUtil.isNull(existing)) {
            log.warn("[SysJobService] 任务不存在 | jobId: {}", req.getJobId());
            BlinkException.throwBusinessException(BaseErrCodeConstant.JOB_NOT_EXIST);
        }

        SysJobDO jobDO = BeanUtil.copyProperties(req, SysJobDO.class);
        jobDO.setUpdateTime(LocalDateTime.now());
        sysJobMapper.updateById(jobDO);

        log.info("[SysJobService] 更新任务成功 | jobId: {}", req.getJobId());
    }

    @Override
    public void deleteJob(DeleteSysJobReq req) {
        if (CollUtil.isEmpty(req.getJobIds())) {
            return;
        }
        sysJobMapper.deleteByIds(req.getJobIds());
        log.info("[SysJobService] 删除任务成功 | jobIds: {}", req.getJobIds());
    }

    @Override
    public void pauseJob(JobIdReq req) {
        SysJobDO jobDO = sysJobMapper.selectById(req.getJobId());
        if (ObjectUtil.isNull(jobDO)) {
            log.warn("[SysJobService] 任务不存在 | jobId: {}", req.getJobId());
            BlinkException.throwBusinessException(BaseErrCodeConstant.JOB_NOT_EXIST);
        }

        jobDO.setJobStatus(JobStatusConstant.PAUSED);
        jobDO.setUpdateTime(LocalDateTime.now());
        sysJobMapper.updateById(jobDO);

        log.info("[SysJobService] 暂停任务成功 | jobId: {}", req.getJobId());
    }

    @Override
    public void resumeJob(JobIdReq req) {
        SysJobDO jobDO = sysJobMapper.selectById(req.getJobId());
        if (ObjectUtil.isNull(jobDO)) {
            log.warn("[SysJobService] 任务不存在 | jobId: {}", req.getJobId());
            BlinkException.throwBusinessException(BaseErrCodeConstant.JOB_NOT_EXIST);
        }

        jobDO.setJobStatus(JobStatusConstant.NORMAL);
        jobDO.setUpdateTime(LocalDateTime.now());
        sysJobMapper.updateById(jobDO);

        log.info("[SysJobService] 恢复任务成功 | jobId: {}", req.getJobId());
    }

    @Override
    public void triggerJob(JobIdReq req) {
        SysJobDO jobDO = sysJobMapper.selectById(req.getJobId());
        if (ObjectUtil.isNull(jobDO)) {
            log.warn("[SysJobService] 任务不存在 | jobId: {}", req.getJobId());
            BlinkException.throwBusinessException(BaseErrCodeConstant.JOB_NOT_EXIST);
        }

        // TODO: 调用调度器立即执行
        log.info("[SysJobService] 手动触发任务 | jobId: {}, jobName: {}", req.getJobId(), jobDO.getJobName());
    }
}