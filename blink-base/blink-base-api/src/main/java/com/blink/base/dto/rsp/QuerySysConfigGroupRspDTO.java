
package com.blink.base.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * QuerySysConfigGroupRspDTO 新增参数分组表请求参数对象
 * </p>
 *
 * @author blink
 * @since 2025-10-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class QuerySysConfigGroupRspDTO extends PageDTO implements Serializable {

  private static final long serialVersionUID = 1L;


    /**
     * 主键ID
     */
    private Integer id;


    /**
     * 分组键名
     */
    private String groupKey;


    /**
     * 分组名称
     */
    private String groupName;


    /**
     * 父分组ID
     */
    private Integer parentId;


    /**
     * 显示顺序
     */
    private Integer orderNum;


    /**
     * 状态：0-禁用 1-启用
     */
    private Boolean status;


    /**
     * 创建者
     */
    private String createBy;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;


    /**
     * 更新者
     */
    private String updateBy;


    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


    /**
     * 备注
     */
    private String remark;


}
