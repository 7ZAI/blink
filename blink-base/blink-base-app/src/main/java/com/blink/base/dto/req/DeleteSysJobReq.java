package com.blink.base.dto.req;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 删除任务请求
 *
 * @author binblink
 */
@Data
public class DeleteSysJobReq {

    @NotNull(message = "任务ID不能为空")
    private List<Long> jobIds;
}
