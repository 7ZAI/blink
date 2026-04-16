-- 清理 gateway_instance 表中的重复数据
-- 保留每个 instance_id 最新的一条记录（id 最大）
-- @author binblink
-- @since 2026-04-15

SET NAMES utf8mb4;

-- 查看重复数据数量（仅用于日志）
SELECT COUNT(*) AS duplicate_count
FROM gateway_instance t1
INNER JOIN gateway_instance t2
WHERE t1.instance_id = t2.instance_id
  AND t1.id < t2.id;

-- 删除重复数据，保留每个 instance_id 最新的一条（id 最大）
DELETE t1 FROM gateway_instance t1
INNER JOIN gateway_instance t2
WHERE t1.instance_id = t2.instance_id
  AND t1.id < t2.id;

SELECT '实例重复数据清理完成' AS message;
