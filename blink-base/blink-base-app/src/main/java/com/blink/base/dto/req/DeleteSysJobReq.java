package com.blink.base.dto.req;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 删除任务请求
 *
 * @author binblink
 */
@Data
public class DeleteSysJobReq {

    @NotEmpty(message = "任务ID列表不能为空")
    private List<Long> jobIds;
}
