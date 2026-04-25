-- 将实例分组表重命名为路由分组表
-- @author binblink
-- @since 2026-04-18

SET NAMES utf8mb4;

-- 重命名表
RENAME TABLE `gateway_instance_group` TO `gateway_route_group`;

-- 更新表注释
ALTER TABLE `gateway_route_group` COMMENT '网关路由分组表';

SELECT '路由分组表重命名完成' AS message;
