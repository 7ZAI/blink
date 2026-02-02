
# blink微服务开发框架

该项目目的提供一套可以快速开发微服务的框架，并作为个人工作经验技术的总结和提炼。

目标：向企业级开发框架靠拢

## 技术栈

JDK17、Spring Cloud Alibaba、Spring Boot 3、Mysql 8.0、Spring MVC、Mybatis-Plus、Redis、RabbitMQ

具体版本详情查看[build.gradle](build.gradle)

## 模块封装

采用企业级工程按功能维度模块化拆分策略，将各核心能力封装为独立可发布的依赖库（如 Spring Boot Starter）：
实现模块间依赖隔离与功能可插拔，降低模块耦合性；
Starter 内置自动化配置能力，外部引用时可通过自定义配置灵活覆盖依赖库默认参数；
后续功能迭代或重大调整可通过发布新版本依赖包支撑持续交付，最终使工程架构达成高内聚、低耦合的管理目标

目前项目具有以下模块

### 模块列表
| 模块名称                                                               | 功能               |
|--------------------------------------------------------------------|------------------|
| [blink-datasource-starter](blink-datasource-starter/README.md)                                         | mysql数据库支持       | 
| [blink-framework-common](blink-framework-common/README.md)                                           | 通用类封装            |
| [blink-framework-mq](blink-framework-mq/README.md)                 | rabbitmq支持       |
| [blink-framework-openfeign](blink-framework-openfeign/README.md)   | rpc调用封装          |
| [blink-framework-validation](blink-framework-validation/README.md) | 数据校验支持           |
| [blink-redis-starter](blink-redis-starter/README.md)               | redis客户端支持       |
| [blink-web-starter](blink-web-starter/README.md)                   | webApp 通用非业务功能支持 |

各个模块的具体功能详情，请查看各个模块的README文档


## 快速构建

构建服务应用，可以参考[blink-base](blink-base)

1、 环境准备
    需要Idea、gradle >=8.8、mysql >=8.0、redis >7.0、nacos 2.3、nexus（可选）
    下载项目后，可以在[build.gradle](build.gradle)修改私库配置，gradle执行publish任务打包发布各模块到私库。如果没有私库，可以配置为本地仓库 打成jar包后，手动执行gradle或maven的安装命令 安装到本地库即可

2、 添加依赖

使用idea创建新工程，或者下载本项目后在根目录上添加新模块
编写gradle构建脚本 引入依赖

参考base-app或者gateway的yml, 配置redis、nacos、mysql


```groovy
    implementation 'com.blink:blink-web-spring-boot-starter:1.0.0-SNAPSHOT'
    implementation 'com.blink:blink-redis-spring-boot-starter:1.0.0-SNAPSHOT'
    implementation 'com.blink:blink-dataSource-spring-boot-starter:1.0.0-SNAPSHOT'
    
    implementation 'com.blink:blink-framework-common:1.0.0-SNAPSHOT'
    implementation 'com.blink:blink-framework-validation:1.0.0-SNAPSHOT'
```


3、生成模板代码

  使用类[CodeGenerator](blink-datasource-starter/src/main/java/com/blink/datasource/code/CodeGenerator.java)生成代码 

  
```java
  public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/blink?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8";
        String username = "root";
        String password = "123456";
        
        var codeGenerator = new CodeGenerator();
        codeGenerator.generateByCustomTemplate(url, username, password);

    }
```
运行后按输入数据表名可根据数据表信息自动生成controller、Service、ServiceImpl、Mapper、mapper.xml、DO实体
还生成了curdDTO

生成成功控制台显示

```text
请输入作者名称！
blink
请输入应用包名（已有前缀com.blink）！

请输入表名，多个英文逗号分隔 生成所有表则输入all ！
sys_user
请输入要过滤的表前缀，多个英文逗号分隔 都不过滤输入none字符串！
none
请选择模板类型输入1或者2（1.DTO 2.record）
1
18:21:57.113 [main] DEBUG com.baomidou.mybatisplus.generator.AutoGenerator -- ==========================文件生成完成！！！==========================
```
默认是生成地址是在项目根目录中，随后将生成的代码拖入项目对应目录即可



4、添加启动类
 运行启动类 到此一个微服务应用构建完毕，可以进行业务开发了

```java
/**
 * 启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
public class BlinkBaseAppApplication {

    public static void main(String[] args) {

       SpringApplication.run(BlinkBaseAppApplication.class, args);

    }

}
```

## 应用

目前blink框架具有两个服务应用：blink-base和blink-gateway-reactive

### blink-base 

blink-base RBAC后台管理服务。具有用户、角色、权限、菜单、组等curd接口，是企业中必不可少的应用。
除了RBAC外 还会提供系统参数管理、外接渠道管理、数据字典等功能

[blink-base](blink-base/blink-base-app/README.md)

### blink-gateway-reactive 

blink-gateway-reactive 是基于spring cloud gateway实现的响应式非阻塞网关。
目前具有路由转发、动态路由、认证管理、权限校验、渠道接入、报文加密解密、签名验证等功能

[blink-gateway-reactive](blink-gateway-reactive/README.md)



#### blink项目处于持续开发的状态，后续持续更新 