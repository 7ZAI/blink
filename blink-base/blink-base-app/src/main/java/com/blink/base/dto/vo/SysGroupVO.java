package com.blink.base.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author binblink
 */
@Data
public class SysGroupVO implements Serializable {

    @Serial
    private static final long serialVersionUID = -5913464688771684934L;

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
    private Integer isLeaf;

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

    /**
     * 子节点列表（用于树形结构）
     */
    private List<SysGroupVO> children;

}
