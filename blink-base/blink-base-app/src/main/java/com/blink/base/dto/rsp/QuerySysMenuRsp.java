package com.blink.base.dto.rsp;

import com.blink.base.dto.vo.SysMenuVO;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


/**
 * <p>
 * QuerySysMenuRspDTO 查询系统菜单列表响应参数对象
 * </p>
 * 菜单为树形结构展示，不需要分页
 *
 * @author binblink
 */
public class QuerySysMenuRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜单树形列表
     */
    private List<SysMenuVO> rows;

    /**
     * 总数
     */
    private Integer total;

    public List<SysMenuVO> getRows() {
        return rows;
    }

    public void setRows(List<SysMenuVO> rows) {
        this.rows = rows;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}
