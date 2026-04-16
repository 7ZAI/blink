package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.SysJobRsp;
import com.blink.base.dto.rsp.SysJobVO;
import com.blink.base.entity.SysJobDO;
import com.blink.base.mapper.SysJobMapper;
import com.blink.base.service.SysJobService;
import com.blink.framework.common.exception.BlinkException;
import com.blink.datasource.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时任务服务实现
 *
 * @author binblink
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysJobServiceImpl implements SysJobService {

    private static final String ERR_JOB_NOT_EXIST = "JOB0001";

    private final SysJobMapper sysJobMapper;

    @Override
    public SysJobRsp getJobList(QuerySysJobReq req) {
        LambdaQueryWrapper<SysJobDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ObjectUtil.isNotNull(req.getJobName()), SysJobDO::getJobName, req.getJobName())
                .eq(ObjectUtil.isNotNull(req.getJobGroup()), SysJobDO::getJobGroup, req.getJobGroup())
                .eq(ObjectUtil.isNotNull(req.getJobStatus()), SysJobDO::getJobStatus, req.getJobStatus())
                .orderByDesc(SysJobDO::getCreateTime);

        SysJobRsp rsp = new SysJobRsp();
        PageUtils.queryPage(req, () -> sysJobMapper.selectList(wrapper), rsp);

        // 转换 DO 到 VO
        if (CollUtil.isNotEmpty(rsp.getRows())) {
            List<SysJobVO> voList = rsp.getRows().stream()
                    .map(d -> BeanUtil.copyProperties(d, SysJobVO.class))
                    .collect(Collectors.toList());
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
            throw new BlinkException("任务名称已存在", "JOB0002");
        }

        SysJobDO jobDO = BeanUtil.copyProperties(req, SysJobDO.class);
        jobDO.setJobStatus((byte) 1);
        jobDO.setCreateTime(LocalDateTime.now());
        jobDO.setUpdateTime(LocalDateTime.now());
        sysJobMapper.insert(jobDO);

        log.info("[SysJobService] 新增任务成功 | jobId: {}, jobName: {}", jobDO.getJobId(), jobDO.getJobName());
    }

    @Override
    public void updateJob(UpdateSysJobReq req) {
        SysJobDO existing = sysJobMapper.selectById(req.getJobId());
        if (existing == null) {
            BlinkException.throwBusinessException(ERR_JOB_NOT_EXIST);
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
        sysJobMapper.deleteBatchIds(req.getJobIds());
        log.info("[SysJobService] 删除任务成功 | jobIds: {}", req.getJobIds());
    }

    @Override
    public void pauseJob(JobIdReq req) {
        SysJobDO jobDO = sysJobMapper.selectById(req.getJobId());
        if (jobDO == null) {
            BlinkException.throwBusinessException(ERR_JOB_NOT_EXIST);
        }

        jobDO.setJobStatus((byte) 0);
        jobDO.setUpdateTime(LocalDateTime.now());
        sysJobMapper.updateById(jobDO);

        log.info("[SysJobService] 暂停任务成功 | jobId: {}", req.getJobId());
    }

    @Override
    public void resumeJob(JobIdReq req) {
        SysJobDO jobDO = sysJobMapper.selectById(req.getJobId());
        if (jobDO == null) {
            BlinkException.throwBusinessException(ERR_JOB_NOT_EXIST);
        }

        jobDO.setJobStatus((byte) 1);
        jobDO.setUpdateTime(LocalDateTime.now());
        sysJobMapper.updateById(jobDO);

        log.info("[SysJobService] 恢复任务成功 | jobId: {}", req.getJobId());
    }

    @Override
    public void triggerJob(JobIdReq req) {
        SysJobDO jobDO = sysJobMapper.selectById(req.getJobId());
        if (jobDO == null) {
            BlinkException.throwBusinessException(ERR_JOB_NOT_EXIST);
        }

        // TODO: 调用调度器立即执行
        log.info("[SysJobService] 手动触发任务 | jobId: {}, jobName: {}", req.getJobId(), jobDO.getJobName());
    }
}
