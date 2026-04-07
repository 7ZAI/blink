
package com.blink.gateway.base.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * QuerySysConfigHistoryReqDTO 查询列表参数历史表请求参数对象
 * </p>
 *
 * @author blink
 * @since 2025-09-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class QuerySysConfigHistoryReq extends Page {


    /**
     * 主键ID
     */
    private Long id;


    /**
     * 参数ID
     */
    private Long configId;


    /**
     * 旧值
     */
    private String oldValue;


    /**
     * 新值
     */
    private String newValue;


    /**
     * 变更类型：1-新增 2-修改 3-删除
     */
    private Byte changeType;


    /**
     * 操作者
     */
    private String operateBy;


    /**
     * 操作时间
     */
    private LocalDateTime operateTime;


    /**
     * 备注
     */
    private String remark;


}
