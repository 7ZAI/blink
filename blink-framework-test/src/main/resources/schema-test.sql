-- Blink 测试数据库初始化脚本
-- 用于 H2 内存数据库和 Testcontainers MySQL 初始化
-- 创建基础测试表结构

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    login_name VARCHAR(50) NOT NULL,
    username VARCHAR(100),
    password VARCHAR(200),
    email VARCHAR(100),
    phone VARCHAR(20),
    status TINYINT DEFAULT 1,
    locked TINYINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    create_by INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_login_name (login_name)
);

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50),
    description VARCHAR(200),
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_by INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id INT AUTO_INCREMENT PRIMARY KEY,
    menu_name VARCHAR(50) NOT NULL,
    menu_code VARCHAR(50),
    parent_id INT DEFAULT 0,
    menu_type TINYINT DEFAULT 1,
    path VARCHAR(200),
    component VARCHAR(200),
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_by INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role_rela (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id)
);

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu_rela (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_id INT NOT NULL,
    menu_id INT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_menu (role_id, menu_id)
);

-- 配置组表
CREATE TABLE IF NOT EXISTS sys_config_group (
    group_id INT AUTO_INCREMENT PRIMARY KEY,
    group_name VARCHAR(50) NOT NULL,
    group_code VARCHAR(50),
    description VARCHAR(200),
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_by INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 配置表
CREATE TABLE IF NOT EXISTS sys_config (
    config_id INT AUTO_INCREMENT PRIMARY KEY,
    group_id INT,
    config_name VARCHAR(50) NOT NULL,
    config_code VARCHAR(50),
    config_value VARCHAR(500),
    description VARCHAR(200),
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_by INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 字典类型表
CREATE TABLE IF NOT EXISTS sys_dict_type (
    dict_type_id INT AUTO_INCREMENT PRIMARY KEY,
    dict_type_name VARCHAR(50) NOT NULL,
    dict_type_code VARCHAR(50),
    description VARCHAR(200),
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_by INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 字典数据表
CREATE TABLE IF NOT EXISTS sys_dict_data (
    dict_data_id INT AUTO_INCREMENT PRIMARY KEY,
    dict_type_id INT,
    dict_label VARCHAR(50) NOT NULL,
    dict_value VARCHAR(100),
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_by INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by INT,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    module VARCHAR(50),
    operation VARCHAR(100),
    method VARCHAR(200),
    request_params TEXT,
    response_result TEXT,
    ip VARCHAR(50),
    user_id INT,
    login_name VARCHAR(50),
    status TINYINT DEFAULT 1,
    error_msg TEXT,
    execution_time INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 插入默认测试数据
-- 超级管理员用户（ID=1，密码：123456）
INSERT INTO sys_user (user_id, login_name, username, password, status, locked, deleted)
VALUES (1, 'admin', '超级管理员', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 1, 0, 0);

-- 超级管理员角色（ID=1）
INSERT INTO sys_role (role_id, role_name, role_code, description, status, deleted)
VALUES (1, '超级管理员', 'SUPER_ADMIN', '拥有所有权限', 1, 0);

-- 测试用户（ID=2）
INSERT INTO sys_user (user_id, login_name, username, password, status, locked, deleted)
VALUES (2, 'testuser', '测试用户', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 1, 0, 0);

-- 测试角色（ID=2）
INSERT INTO sys_role (role_id, role_name, role_code, description, status, deleted)
VALUES (2, '测试角色', 'TEST_ROLE', '测试专用角色', 1, 0);

-- 用户角色关联（测试用户关联测试角色）
INSERT INTO sys_user_role_rela (user_id, role_id) VALUES (2, 2);