package com.blink.base.dto.rsp;

import com.blink.base.dto.vo.SysUserVO;
import com.blink.base.entity.SysUserDO;
import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 用户列表数据
 */
@Getter
@Setter
@ToString
public class SysUserRspDTO<T> extends PageDTO<T> implements Serializable {

    private static final long serialVersionUID = 1L;


}
