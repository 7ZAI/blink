package com.blink.base.dto.rsp;


import com.blink.base.dto.vo.SysUserVO;
import com.blink.framework.common.data.PageDTO;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户列表数据
 * @author binblink
 */
public class SysUserRsp extends PageDTO<SysUserVO> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


}
