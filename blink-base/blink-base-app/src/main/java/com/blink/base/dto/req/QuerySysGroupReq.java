package com.blink.base.dto.req;

import com.blink.framework.common.data.PageDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * QuerySysGroupReqDTO 查询列表组请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-04
 */
@Getter
@Setter
@ToString
public class QuerySysGroupReq extends PageDTO implements Serializable {

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
     * 父组id
     */
    private Integer groupParentId;



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




}
