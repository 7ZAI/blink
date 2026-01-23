# blink-datasource-starter

目标： 封装数据库相关类 依赖 并能作为数据库相关的基础设施包 提供给外部应用使用

## 自动化配置
 
关闭了mybatis 一二级缓存 配置了mapper扫描、pagehelper插件

[MybatisPlusConfiguration](src/main/java/com/blink/datasource/config/MybatisPlusConfiguration.java)

## 自动生成模板代码

 在 mybatis-plus 自动代码生成器基础上修改，通过自定义模板、自定义属性注入模板域、自定义文件名称生成规则，
 实现了controller、service、Impl、mapper、mapper.xml、entity、DTO的代码生成

调用 CodeGenerator.generateByCustomTemplate()
```java
  public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/blink?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8";
        String username = "root";
        String password = "123456";

        var codeGenerator = new CodeGenerator();
        codeGenerator.generateByCustomTemplate(url, username, password);

    }
```
ps.关于模式2 record类的自动创建 功能未完成 目前请选择1

关于record只是做尝试，使用下来发现record 如果字段较多则需要配建造者模式或者做拆分 在开发阶段需要变动字段的情况下，很麻烦 
总结是record不好用

[CodeGenerator](src/main/java/com/blink/datasource/code/CodeGenerator.java)

## 分页插件

使用了PageHelper来实现分页功能,并做了工具类封装

[PageUtils](src/main/java/com/blink/datasource/PageUtils.java)

QuerySysConfigRspDTO 是一个继承了[PageDTO](../blink-framework-common/src/main/java/com/blink/framework/common/data/PageDTO.java)的DTO 用来获取结果

函数接口 [ExecuteFunction](../blink-framework-common/src/main/java/com/blink/framework/common/data/ExecuteFunction.java) 是一个无参数无返回值的接口 是对执行sql的行为进行封装
里面一定要包含执行分页查询的程序

(后来发现ExecuteFunction可以用java原生Runnable实现 不用自己创建一个空参空返回的函数接口 但是现在已经没有改的必要了)

使用示例
```java
    var pageRsp = new QuerySysConfigRspDTO();
    QuerySysConfigRspDTO result = PageUtils.queryPage(queryParam, () -> sysConfigMapper.findSysConfigList(queryParam), pageRsp);

```

未来版本可能方向
1. 可选多类型数据库 扩展  
2. 自定义代码生成模板
3. 配置文件密码加密
4. 添加record作为DTO模板

模块化 搁置
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