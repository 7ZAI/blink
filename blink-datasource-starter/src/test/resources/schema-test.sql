-- =====================================================
-- 集成测试数据库初始化脚本
-- 使用 H2 内存数据库 (MySQL 兼容模式)
-- =====================================================

-- 测试用户表
CREATE TABLE IF NOT EXISTS test_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_name VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    create_by VARCHAR(50),
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

-- 测试部门表
CREATE TABLE IF NOT EXISTS test_dept (
    dept_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

-- 测试用户部门关联表
CREATE TABLE IF NOT EXISTS test_user_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL
);

-- 测试其他表 (无注解实体)
CREATE TABLE IF NOT EXISTS test_other (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100)
);

-- =====================================================
-- 初始化测试数据
-- =====================================================

-- 插入部门数据
INSERT INTO test_dept (dept_id, dept_name, parent_id, create_time, update_time) VALUES
(1, '总部', NULL, NOW(), NOW()),
(2, '研发部', 1, NOW(), NOW()),
(3, '产品部', 1, NOW(), NOW()),
(4, '测试组', 2, NOW(), NOW()),
(5, '开发组', 2, NOW(), NOW());

-- 插入用户数据
INSERT INTO test_user (user_id, user_name, dept_id, create_by, create_time, update_time) VALUES
(1, 'admin', 1, 'system', NOW(), NOW()),
(2, 'zhangsan', 4, 'admin', NOW(), NOW()),
(3, 'lisi', 5, 'admin', NOW(), NOW()),
(4, 'wangwu', 3, 'admin', NOW(), NOW()),
(5, 'zhaoliu', 2, 'admin', NOW(), NOW());

-- 插入用户部门关联数据
INSERT INTO test_user_dept (id, user_id, dept_id) VALUES
(1, 1, 1),
(2, 2, 4),
(3, 3, 5),
(4, 4, 3),
(5, 5, 2);

-- 插入其他数据
INSERT INTO test_other (id, name) VALUES
(1, 'other1'),
(2, 'other2'),
(3, 'other3');
