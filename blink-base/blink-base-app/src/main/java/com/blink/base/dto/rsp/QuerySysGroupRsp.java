package com.blink.base.dto.rsp;

import com.blink.base.entity.SysGroupDO;
import com.blink.framework.common.data.PageDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * QuerySysGroupRspDTO 新增组请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-04
 */
@Getter
@Setter
@ToString
public class QuerySysGroupRsp extends PageDTO<SysGroupDO> implements Serializable {

  private static final long serialVersionUID = 1L;


    /**
     * 分组id
     */
    private Integer groupId;


    /**
     * 组编号
     */
    private String groupNo;


    /**
     * 组名称
     */
    private String groupName;


    /**
     * 组英文名称
     */
    private String groupEnName;


    /**
     * 父组id
     */
    private Integer groupParentId;


    /**
     * 层级
     */
    private Integer groupLevel;


    /**
     * 是否叶子节点 0否 1是
     */
    private Byte isLeaf;


    /**
     * 组领导
     */
    private String groupLeader;


    /**
     * 组地址
     */
    private String groupAddress;


    /**
     * 组电话
     */
    private String phone;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;


    /**
     * 创建者
     */
    private String createBy;


    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


    /**
     * 更新人
     */
    private String updateBy;





}
