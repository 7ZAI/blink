package com.blink.base.dto.rsp;

import com.blink.base.dto.vo.SysGroupVO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 查询组列表响应对象（树形数据不分页）
 *
 * @author binblink
 * @since 2024-01-04
 */
@Getter
@Setter
@ToString
public class QuerySysGroupRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 组列表
     */
    private List<SysGroupVO> list;

}