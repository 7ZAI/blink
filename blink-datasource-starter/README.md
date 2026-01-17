### 本项目目标 作为数据库相关的基础设施包 提供给外部应用模块使用

核心目标
1. 封装针对mysql基本的数据库连接配置，并可根据外部配置修改
2. 指定使用Druid作为数据库连接池
3. 指定使用mybatis-plus 作为orm框架
4. 提供代码生成器

扩展目标（未来版本）
1. 多数据源或 动态数据源 扩展
2. 可选多类型数据库 扩展  
3. 自定义代码生成模板
4. 配置文件密码加密
5. 添加record作为DTO模板

模块化
```java
module com.blink.framework.datasource {

    requires org.mybatis;
    requires spring.core;
    requires cn.hutool.core;
    requires com.baomidou.mybatis.plus.annotation;
    requires com.baomidou.mybatis.plus.generator;
    requires org.mybatis.spring;
    requires pagehelper;
//    requires pagehelper_5.3.1;
    requires com.baomidou.mybatis.plus.core;
    requires druid;
    requires com.baomidou.mybatis.plus.extension;
    requires java.sql;
    requires com.sun.istack.runtime;
    requires com.blink.framework.common;
    requires spring.context;


}
```