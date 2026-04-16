package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.SysJobLogRsp;
import com.blink.base.dto.rsp.SysJobRsp;
import com.blink.base.service.SysJobLogService;
import com.blink.base.service.SysJobService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务管理 Controller
 *
 * @author binblink
 */
@Slf4j
@RestController
@RequestMapping("/job")
@RequiredArgsConstructor
public class SysJobController {

    private final SysJobService sysJobService;
    private final SysJobLogService sysJobLogService;

    @PostMapping("/getJobList")
    public ResponseDTO<SysJobRsp> getJobList(@RequestBody RequestDTO<QuerySysJobReq> reqDto) {
        return ResponseDTO.newSuccessInstance(sysJobService.getJobList(reqDto.getBody()));
    }

    @PostMapping("/addJob")
    public ResponseDTO<EmptyBody> addJob(@RequestBody @Validated RequestDTO<AddSysJobReq> reqDto) {
        sysJobService.addJob(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    @PostMapping("/updateJob")
    public ResponseDTO<EmptyBody> updateJob(@RequestBody @Validated RequestDTO<UpdateSysJobReq> reqDto) {
        sysJobService.updateJob(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    @PostMapping("/deleteJob")
    public ResponseDTO<EmptyBody> deleteJob(@RequestBody @Validated RequestDTO<DeleteSysJobReq> reqDto) {
        sysJobService.deleteJob(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    @PostMapping("/pauseJob")
    public ResponseDTO<EmptyBody> pauseJob(@RequestBody @Validated RequestDTO<JobIdReq> reqDto) {
        sysJobService.pauseJob(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    @PostMapping("/resumeJob")
    public ResponseDTO<EmptyBody> resumeJob(@RequestBody @Validated RequestDTO<JobIdReq> reqDto) {
        sysJobService.resumeJob(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    @PostMapping("/triggerJob")
    public ResponseDTO<EmptyBody> triggerJob(@RequestBody @Validated RequestDTO<JobIdReq> reqDto) {
        sysJobService.triggerJob(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    @PostMapping("/getLogList")
    public ResponseDTO<SysJobLogRsp> getLogList(@RequestBody RequestDTO<QuerySysJobLogReq> reqDto) {
        return ResponseDTO.newSuccessInstance(sysJobLogService.getLogList(reqDto.getBody()));
    }
}
