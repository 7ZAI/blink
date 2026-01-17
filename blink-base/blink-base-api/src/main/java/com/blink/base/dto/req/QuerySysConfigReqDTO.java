
package com.blink.base.dto.req;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * QuerySysConfigReqDTO 查询列表参数配置表请求参数对象
 * </p>
 *
 * @author blink
 * @since 2025-09-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class QuerySysConfigReqDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 主键ID
     */
    private Integer id;


    /**
     * 参数键名
     */
    private String configKey;


    /**
     * 参数名称
     */
    private String configName;


    /**
     * 参数值
     */
    private String configValue;


    /**
     * 参数类型：0-字符串 1-数字 2-布尔 3-JSON 4-数组
     */
    private Byte configType;


    /**
     * 参数分组ID
     */
    private List<Integer> groupIds;

    /**
     * 是否查询该分组下所有子分组参数 如果为false 仅仅查询等于groupId的参数
     */
    private Boolean findAllChild = false;


    /**
     * 是否只读：0-可修改 1-只读
     */
    private Boolean readonly;


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


}
