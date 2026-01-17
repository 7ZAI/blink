-- blink.blink_channel definition

CREATE TABLE `blink_channel`
(
    `channel_id`           varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '渠道ID',
    `channel_name`         varchar(32)                                                    DEFAULT NULL COMMENT '渠道名',
    `app_key`              varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  DEFAULT NULL COMMENT '应用key值',
    `app_secret`           varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  DEFAULT NULL COMMENT '应用秘钥',
    `rela_user_id`         varchar(64)                                                    DEFAULT NULL COMMENT '关联用户',
    `access_token`         varchar(64)                                                    DEFAULT NULL COMMENT '认证token',
    `system_publickey`     varchar(2048) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '系统公钥',
    `system_privatekey`    varchar(2048) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '系统私钥',
    `channel_publickey`    varchar(2048) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '渠道公钥',
    `channel_privatekey`   varchar(2048) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '渠道私钥',
    `enable`               tinyint                                                        DEFAULT '0' COMMENT '渠道开关 0 开启 1关闭',
    `encryption_switch`    tinyint                                                        DEFAULT '1' COMMENT '加密开关 0 开启 1关闭',
    `token_timeout_switch` tinyint                                                        DEFAULT '1' COMMENT '认证token过期开关 0 开启 1关闭',
    `authority_switch`     tinyint                                                        DEFAULT '0' COMMENT '权限校验开关 0 开启 1关闭',
    `remark`               varchar(255)                                                   DEFAULT '' COMMENT '备注',
    `create_by`            varchar(30)                                                    DEFAULT NULL COMMENT '创建者',
    `update_by`            varchar(30)                                                    DEFAULT NULL COMMENT '更新者',
    `create_time`          timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `update_time`          timestamp NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`channel_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='对接渠道';


-- blink.mq_msg_rece definition

CREATE TABLE `mq_msg_rece`
(
    `msg_id`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息id',
    `receive_id`     varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接收者标识',
    `buss_id`        varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci          DEFAULT NULL COMMENT '业务id',
    `req_id`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci          DEFAULT NULL COMMENT '请求id',
    `receive_sts`    int                                                          NOT NULL DEFAULT '0' COMMENT '消息接收状态 ‘0’ 未消费 1 消费成功 2 消费失败',
    `mq_type`        varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL DEFAULT 'N' COMMENT '消息类型 N 普通 B 业务 ',
    `mq_mode`        varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL DEFAULT 'S' COMMENT '工作模式 S 单消费  M 多消费 ',
    `mq_context`     json                                                         NOT NULL COMMENT '消息内容',
    `send_sys`       varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci          DEFAULT NULL COMMENT '发送者',
    `receive_sys`    varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci          DEFAULT NULL COMMENT '接收者',
    `receive_time`   timestamp NULL DEFAULT NULL COMMENT '接收时间',
    `consumer_times` int                                                          NOT NULL DEFAULT '0' COMMENT '消费次数',
    `fail_times`     int                                                          NOT NULL DEFAULT '0' COMMENT '失败次数',
    `remark`         varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci          DEFAULT NULL COMMENT '备注',
    `create_time`    timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `update_time`    timestamp NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`msg_id`, `receive_id`),
    UNIQUE KEY `msg_id` (`msg_id`,`receive_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='消息消费记录表';


-- blink.mq_msg_send definition

CREATE TABLE `mq_msg_send`
(
    `msg_id`           varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息id',
    `buss_id`          varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci          DEFAULT NULL COMMENT '业务id',
    `req_id`           varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci          DEFAULT NULL COMMENT '请求id',
    `send_sts`         int                                                          NOT NULL DEFAULT '0' COMMENT '消息发送状态 ‘0’未发送 1 发送成功 2 发送失败',
    `mq_type`          varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL DEFAULT 'N' COMMENT '消息类型 N 普通 B 业务 ',
    `mq_mode`          varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL DEFAULT 'S' COMMENT '工作模式 S 单消费  M 多消费 ',
    `mq_context`       json                                                         NOT NULL COMMENT '消息内容',
    `mq_context_class` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息类',
    `mq_exchange`      varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息交换机',
    `mq_routing_key`   varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息路由key',
    `send_sys`         varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci          DEFAULT NULL COMMENT '发送者',
    `send_time`        timestamp NULL DEFAULT NULL COMMENT '初始发送时间',
    `last_send_time`   timestamp NULL DEFAULT NULL COMMENT '最新发送时间',
    `enable_retry`     int                                                          NOT NULL DEFAULT '0' COMMENT '是否允许重发 0 开启 1关闭 ',
    `retry_times`      int                                                          NOT NULL DEFAULT '0' COMMENT '发送次数',
    `fail_times`       int                                                          NOT NULL DEFAULT '0' COMMENT '失败次数',
    `remark`           varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci          DEFAULT NULL COMMENT '备注',
    `create_time`      timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `update_time`      timestamp NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`msg_id`),
    UNIQUE KEY `msg_id` (`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='消息发送记录表';


-- blink.redis_mq definition

CREATE TABLE `redis_mq`
(
    `msg_id`        varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL COMMENT '消息id',
    `msg_status`    varchar(1)                                                    NOT NULL DEFAULT '0' COMMENT '未读 0 已读 1 已消费 2',
    `stream_id`     varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci           DEFAULT NULL COMMENT 'stream_id',
    `topic`         varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL COMMENT 'StreamKey',
    `msg_type`      varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL DEFAULT 'NORMAL' COMMENT '消息类型 NORMAL ',
    `payload`       json                                                          NOT NULL COMMENT '消息内容',
    `payload_class` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'payload类的全限定名',
    `sender`        varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci           DEFAULT NULL COMMENT '发送者',
    `receiver`      varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci           DEFAULT NULL COMMENT '接收者',
    `version`       varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci           DEFAULT NULL COMMENT '版本号',
    `retry_times`   int                                                           NOT NULL DEFAULT '0' COMMENT '发送次数',
    `fail_times`    int                                                           NOT NULL DEFAULT '0' COMMENT '失败次数',
    `extra`         json                                                                   DEFAULT NULL COMMENT '备注',
    `create_time`   timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `update_time`   timestamp NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`msg_id`),
    UNIQUE KEY `msg_id` (`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='redis stream消息发送记录表';


-- blink.seq_no definition

CREATE TABLE `seq_no`
(
    `seq_id`         int          NOT NULL AUTO_INCREMENT COMMENT '顺序号ID',
    `seq_name`       varchar(255) NOT NULL COMMENT '顺序号名称',
    `current_number` bigint       NOT NULL DEFAULT '1' COMMENT '当前值',
    `seq_incr`       int          NOT NULL DEFAULT '1' COMMENT '增量',
    `start_number`   bigint       NOT NULL DEFAULT '1' COMMENT '起始值',
    `max_number`     bigint       NOT NULL DEFAULT '999999' COMMENT '最大值',
    `warn_number`    int          NOT NULL DEFAULT '99999' COMMENT '预警值',
    PRIMARY KEY (`seq_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='顺序号表';


-- blink.sys_config definition

CREATE TABLE `sys_config`
(
    `id`           int          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key`   varchar(100) NOT NULL COMMENT '参数键名',
    `config_name`  varchar(100) NOT NULL COMMENT '参数名称',
    `config_value` text         NOT NULL COMMENT '参数值',
    `config_type`  tinyint      NOT NULL DEFAULT '0' COMMENT '参数类型：0-字符串 1-数字 2-布尔 3-JSON 4-数组',
    `group_id`     int          NOT NULL DEFAULT '0' COMMENT '参数分组ID',
    `description`  varchar(500)          DEFAULT NULL COMMENT '参数描述',
    `readonly`     tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否只读：0-可修改 1-只读',
    `status`       tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-禁用  0-启用',
    `create_by`    varchar(50)  NOT NULL COMMENT '创建者',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    varchar(50)           DEFAULT NULL COMMENT '更新者',
    `update_time`  datetime              DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`       varchar(500)          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置表';


-- blink.sys_config_group definition

CREATE TABLE `sys_config_group`
(
    `id`          int         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_key`   varchar(50) NOT NULL COMMENT '分组键名',
    `group_name`  varchar(50) NOT NULL COMMENT '分组名称',
    `parent_id`   int         NOT NULL DEFAULT '0' COMMENT '父分组ID',
    `order_num`   int         NOT NULL DEFAULT '0' COMMENT '显示顺序',
    `status`      tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态：1-禁用 0-启用',
    `create_by`   varchar(50) NOT NULL COMMENT '创建者',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(50)          DEFAULT NULL COMMENT '更新者',
    `update_time` datetime             DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      varchar(500)         DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数分组表';


-- blink.sys_data_dict definition

CREATE TABLE `sys_data_dict`
(
    `dict_id`          int         NOT NULL AUTO_INCREMENT COMMENT '数据字典id',
    `dict_name`        varchar(64) NOT NULL COMMENT '数据字典名称',
    `dict_description` varchar(200)                                                  DEFAULT NULL COMMENT '数据字典描述',
    `data_type`        varchar(10) NOT NULL                                          DEFAULT 'S' COMMENT '数据字典类型 C char N number D decimal S string T time',
    `max_length`       int         NOT NULL                                          DEFAULT '10' COMMENT '数据字典最大长度',
    `data_pattern`     varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据正则表达式',
    `data_precision`   int                                                           DEFAULT NULL COMMENT '数据精度（针对小数）',
    PRIMARY KEY (`dict_id`) USING BTREE,
    UNIQUE KEY `dict_name` (`dict_name`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='数据字典表';


-- blink.sys_data_filter definition

CREATE TABLE `sys_data_filter`
(
    `data_ac_id`               int     NOT NULL AUTO_INCREMENT COMMENT '数据过滤id',
    `ac_name`                  int              DEFAULT NULL COMMENT '所属权限名称',
    `ac_id`                    int              DEFAULT NULL COMMENT '所属权限id',
    `ac_url`                   int              DEFAULT NULL COMMENT '所属权限url',
    `data_filter_name`         varchar(30)      DEFAULT NULL COMMENT '数据过滤名称',
    `data_filter_name_en_name` varchar(30)      DEFAULT NULL COMMENT '数据过滤英文名称',
    `fliter_type`              tinyint          DEFAULT NULL COMMENT '数据过滤类型 0 字段过滤 1条件过滤 3日期过滤',
    `fliter_expression`        varchar(500)     DEFAULT NULL COMMENT '过滤表达式',
    `status`                   tinyint NOT NULL DEFAULT '0' COMMENT '状态 0启动 1禁用',
    `create_by`                varchar(30)      DEFAULT NULL COMMENT '创建者',
    `create_time`              timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `update_by`                varchar(30)      DEFAULT NULL COMMENT '更新者',
    `update_time`              timestamp NULL DEFAULT NULL COMMENT '更新时间',
    `remark`                   varchar(500)     DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`data_ac_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='数据范围权限类型';


-- blink.sys_group definition

CREATE TABLE `sys_group`
(
    `group_id`        int     NOT NULL AUTO_INCREMENT COMMENT '分组id',
    `group_no`        varchar(8)       DEFAULT NULL COMMENT '组编号',
    `group_name`      varchar(64)      DEFAULT NULL COMMENT '组名称',
    `group_en_name`   varchar(64)      DEFAULT NULL COMMENT '组英文名称',
    `group_parent_id` int              DEFAULT NULL COMMENT '父组id',
    `group_level`     int     NOT NULL COMMENT '层级',
    `isLeaf`          tinyint NOT NULL DEFAULT '1' COMMENT '是否叶子节点 0否 1是',
    `group_leader`    varchar(30)      DEFAULT NULL COMMENT '组领导',
    `group_address`   varchar(64)      DEFAULT NULL COMMENT '组地址',
    `phone`           varchar(64)      DEFAULT NULL COMMENT '组电话',
    `create_time`     timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(30)      DEFAULT NULL COMMENT '创建者',
    `update_time`     timestamp NULL DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(30)      DEFAULT NULL COMMENT '更新人',
    `delFlag`         tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`group_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='组';


-- blink.sys_menu definition

CREATE TABLE `sys_menu`
(
    `menu_id`        int     NOT NULL AUTO_INCREMENT COMMENT '菜单id',
    `menu_name`      varchar(30)      DEFAULT NULL COMMENT '菜单名称',
    `menu_en_name`   varchar(30)      DEFAULT NULL COMMENT '菜单英文名称',
    `type`           tinyint          DEFAULT NULL COMMENT '菜单类型',
    `icon`           varchar(255)     DEFAULT NULL COMMENT '菜单图标',
    `url`            varchar(255)     DEFAULT NULL COMMENT '菜单地址',
    `order_number`   int              DEFAULT NULL COMMENT '排序序号',
    `status`         tinyint NOT NULL DEFAULT '0' COMMENT '状态 0显示 1隐藏',
    `parent_id`      int              DEFAULT NULL COMMENT '父菜单id',
    `menu_level`     int              DEFAULT NULL COMMENT '菜单层级',
    `component_path` varchar(255)     DEFAULT NULL COMMENT '组件路径',
    `hasChildren`    tinyint(1) DEFAULT '0' COMMENT '是否有子菜单（按钮不算）',
    `create_by`      varchar(30)      DEFAULT NULL COMMENT '创建者',
    `create_time`    timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `update_by`      varchar(30)      DEFAULT NULL COMMENT '更新者',
    `update_time`    timestamp NULL DEFAULT NULL COMMENT '更新时间',
    `delFlag`        tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='系统菜单';


-- blink.sys_msg_info definition

CREATE TABLE `sys_msg_info`
(
    `msg_id`   int         NOT NULL AUTO_INCREMENT COMMENT '数据字典id',
    `msg_code` varchar(16) NOT NULL COMMENT '消息代码',
    `msg_info` varchar(200)         DEFAULT NULL COMMENT '消息描述',
    `msg_type` varchar(8)           DEFAULT NULL COMMENT '消息类型 错误E 警告W 成功S',
    `msg_lang` varchar(10) NOT NULL DEFAULT 'zh_cn' COMMENT '消息语言',
    PRIMARY KEY (`msg_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='消息码信息表';


-- blink.sys_permission definition

CREATE TABLE `sys_permission`
(
    `ac_id`          int     NOT NULL AUTO_INCREMENT COMMENT '权限id',
    `ac_name`        varchar(30)      DEFAULT NULL COMMENT '权限名称',
    `ac_en_name`     varchar(30)      DEFAULT NULL COMMENT '权限英文名称',
    `ac_identity`    varchar(30)      DEFAULT NULL COMMENT '权限标识',
    `ac_type`        tinyint          DEFAULT NULL COMMENT '权限类型 0 菜单权限 1数据权限 2功能权限 3接口权限',
    `icon`           varchar(255)     DEFAULT NULL COMMENT '权限图标',
    `url`            varchar(255)     DEFAULT NULL COMMENT '权限地址',
    `status`         tinyint NOT NULL DEFAULT '0' COMMENT '状态 0启动 1禁用 2隐藏',
    `parent_id`      int              DEFAULT NULL COMMENT '父权限id',
    `data_filter_id` int              DEFAULT NULL COMMENT '数据过滤器id',
    `create_by`      varchar(30)      DEFAULT NULL COMMENT '创建者',
    `create_time`    timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `update_by`      varchar(30)      DEFAULT NULL COMMENT '更新者',
    `update_time`    timestamp NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`ac_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='权限菜单';


-- blink.sys_role definition

CREATE TABLE `sys_role`
(
    `role_id`      int     NOT NULL AUTO_INCREMENT COMMENT '角色id',
    `role_name`    varchar(64)      DEFAULT NULL COMMENT '角色名称',
    `role_en_name` varchar(64)      DEFAULT NULL COMMENT '角色英文名称',
    `status`       tinyint NOT NULL DEFAULT '0' COMMENT '角色状态',
    `role_code`    varchar(60)      DEFAULT '' COMMENT '角色代码',
    `role_type`    tinyint          DEFAULT '0' COMMENT '角色类型',
    `create_by`    varchar(30)      DEFAULT NULL COMMENT '创建者',
    `update_by`    varchar(30)      DEFAULT NULL COMMENT '更新者',
    `create_time`  timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `update_time`  timestamp NULL DEFAULT NULL COMMENT '更新时间',
    `delFlag`      tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`role_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='系统角色';


-- blink.sys_role_perm_rela definition

CREATE TABLE `sys_role_perm_rela`
(
    `role_id` int NOT NULL COMMENT '角色id',
    `ac_id`   int NOT NULL COMMENT '权限id',
    PRIMARY KEY (`role_id`, `ac_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='角色权限关系表';


-- blink.sys_user definition

CREATE TABLE `sys_user`
(
    `user_id`         int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `login_name`      varchar(30)  DEFAULT NULL COMMENT '登录名',
    `password`        varchar(64)  DEFAULT NULL COMMENT '密码',
    `username`        varchar(30)  DEFAULT NULL COMMENT '昵称',
    `avatar`          varchar(255) DEFAULT NULL COMMENT '头像',
    `sex`             tinyint      DEFAULT '3' COMMENT '性别 1男 2女 3不确定',
    `phone`           varchar(20)  DEFAULT NULL COMMENT '电话',
    `email`           varchar(64)  DEFAULT NULL COMMENT '邮箱',
    `last_login_time` timestamp NULL DEFAULT NULL COMMENT '上次登录时间',
    `locked`          tinyint      DEFAULT '0' COMMENT '锁定状态 0 未锁定 1 管理员锁定 2 输错密码锁定',
    `salt`            varchar(64)  DEFAULT NULL COMMENT '加密盐值',
    `psw_retry`       tinyint      DEFAULT '0' COMMENT '密码重试次数',
    `superFlag`       tinyint      DEFAULT '0' COMMENT '超级管理员标志 0否 1是',
    `remark`          varchar(500) DEFAULT '' COMMENT '备注',
    `create_by`       varchar(30)  DEFAULT NULL COMMENT '创建者',
    `update_by`       varchar(30)  DEFAULT NULL COMMENT '更新者',
    `create_time`     timestamp NULL DEFAULT NULL COMMENT '创建时间',
    `update_time`     timestamp NULL DEFAULT NULL COMMENT '更新时间',
    `lock_time`       timestamp NULL DEFAULT NULL COMMENT '锁定时间',
    `delFlag`         tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='系统用户';


-- blink.sys_user_group_rela definition

CREATE TABLE `sys_user_group_rela`
(
    `user_id`  int NOT NULL COMMENT '用户id',
    `group_id` int NOT NULL COMMENT '组id',
    PRIMARY KEY (`user_id`, `group_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='用户组关系表 多对多';


-- blink.sys_user_role_rela definition

CREATE TABLE `sys_user_role_rela`
(
    `user_id` int NOT NULL COMMENT '用户id',
    `role_id` int NOT NULL COMMENT '角色id',
    PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='用户角色关系表 多对多';