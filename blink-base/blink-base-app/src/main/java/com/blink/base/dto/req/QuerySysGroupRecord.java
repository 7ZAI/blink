package com.blink.base.dto.req;

import com.blink.framework.common.record.PageRecord;

import java.io.Serializable;
import java.time.LocalDateTime;

public record QuerySysGroupRecord(

        /**
         * 分组id
         */
        Integer groupId,


        /**
         * 组编号
         */
        String groupNo,


        /**
         * 组名称
         */
        String groupName,


        /**
         * 父组id
         */
        Integer groupParentId,


        /**
         * 组领导
         */
        String groupLeader,


        /**
         * 组地址
         */
        String groupAddress,


        /**
         * 组电话
         */
        String phone,


        /**
         * 创建时间
         */
        LocalDateTime createTime,


        PageRecord pageRecord
) implements Serializable {
}
