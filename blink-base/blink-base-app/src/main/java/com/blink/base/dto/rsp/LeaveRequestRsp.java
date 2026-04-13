package com.blink.base.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import com.blink.base.dto.vo.LeaveRequestVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 请假申请列表响应
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeaveRequestRsp extends PageDTO<LeaveRequestVO> {

}
