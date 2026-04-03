package com.blink.datasource.code;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.fill.Column;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.util.*;

/**
 * 代码生成器
 * 提供mybatis-plus 默认生成 和 自定义模板生成
 *
 * @author binblink
 */
public class CodeGenerator {

    /**
     * 使用mybatis plus 默认模板生成代码
     *
     * @param url       数据库连接 url
     * @param username  用户名
     * @param password  密码
     * @param outputDir 生成目录
     */
    public static void generate(String url, String username, String password, String outputDir) {

        FastAutoGenerator.create(url, username, password)

                // 全局配置
                .globalConfig((scanner, builder) -> builder.author(scanner.apply("请输入作者名称？"))
                        // 指定输出目录
                        .outputDir(outputDir))
                // 包配置
                .packageConfig((scanner, builder) -> builder.parent(scanner.apply("请输入包名？")))

                // 策略配置
                .strategyConfig((scanner, builder) -> builder
                                //表名
                                .addInclude(getTables(scanner.apply("请输入表名，多个英文逗号分隔 所有输入 all")))

                                //过滤表前缀
                                .addTablePrefix(getTablePrefix(scanner.apply("请输入要过滤的表前缀，多个英文逗号分隔 都不过滤输入none字符串")))
                                // controller 策略
                                .controllerBuilder().enableRestStyle().enableHyphenStyle()
                                //service 策略
                                .serviceBuilder().convertServiceFileName(entityName -> entityName + ConstVal.SERVICE)
                                //实体类策略
                                .entityBuilder().enableTableFieldAnnotation().enableLombok().addTableFills(
                                        new Column("create_time", FieldFill.INSERT),
                                        new Column("update_time", FieldFill.INSERT_UPDATE),
                                        new Column("update_by", FieldFill.INSERT_UPDATE),
                                        new Column("create_by", FieldFill.INSERT)
                                ).convertFileName(entityName -> entityName + "DO")
//                        .logicDeleteColumnName()
                                .mapperBuilder().mapperAnnotation(Mapper.class)
                                .build()
                )
                /*
                    模板引擎配置，默认 Velocity 可选模板引擎 Beetl 或 Freemarker
                   .templateEngine(new BeetlTemplateEngine())
                   .templateEngine(new FreemarkerTemplateEngine())
                 */
                .execute();
    }

    /**
     * 根据自定义模板生成
     * 模板分为 传统 DTO 和 record DTO两种
     *
     * @param url      数据库连接
     * @param username 数据库用户名
     * @param password 数据库密码
     */
    public static void generateByCustomTemplate(String url, String username, String password) {
        // 数据源配置
        DataSourceConfig dataSourceConfig = new DataSourceConfig.Builder(url, username, password)
                .build();
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入作者名称！");
        String author = scanner.nextLine();
        System.out.println("请输入应用包名（已有前缀com.blink）！");
        String appName = scanner.nextLine();
        System.out.println("请输入表名，多个英文逗号分隔 生成所有表则输入all ！");
        String tableName = scanner.nextLine();
        System.out.println("请输入要过滤的表前缀，多个英文逗号分隔 都不过滤输入none字符串！");
        String tablePrefix = scanner.nextLine();

//        System.out.println("请选择模板类型输入1或者2（1.DTO 2.record）");
//        String templateType = scanner.nextLine();

        scanner.close();
        // 全局配置
        String projectPath = System.getProperty("user.dir");
        GlobalConfig globalConfig = new GlobalConfig.Builder().outputDir(projectPath)
                .author(author)
                .disableOpenDir()
                .build();
        // 包配置
        PackageConfig packageConfig = new PackageConfig.Builder()
                .parent("com.blink." + appName)
                .build();


        // 策略配置
        StrategyConfig strategyConfig = new StrategyConfig.Builder()
                //表名
                .addInclude(getTables(tableName))
                //过滤表前缀
                .addTablePrefix(getTablePrefix(tablePrefix))

                // controller 策略
                .controllerBuilder()
                .template("/codeTemplate/controller.java.vm")
                .enableHyphenStyle()
                .enableRestStyle()
                .build()
                //service 策略
                .serviceBuilder()
                // 禁用 Service 接口生成
                .disableService()
                .convertServiceFileName(entityName -> entityName + ConstVal.SERVICE)
                .serviceTemplate("/codeTemplate/service.java.vm")
                .serviceImplTemplate("/codeTemplate/serviceImpl.java.vm")
                .build()
                .mapperBuilder()
                .enableFileOverride()
                .enableBaseColumnList()
                .enableBaseResultMap()
                .enableFileOverride()
                .mapperAnnotation(Mapper.class)
                .mapperTemplate("/codeTemplate/mapper.java.vm")
                .mapperXmlTemplate("/codeTemplate/mapper.xml.vm")
                .build()
                //实体类策略
                .entityBuilder()
                .enableTableFieldAnnotation()
                .enableLombok()
                .addTableFills(
                        new Column("create_time", FieldFill.INSERT),
                        new Column("update_time", FieldFill.INSERT_UPDATE),
                        new Column("update_by", FieldFill.INSERT_UPDATE),
                        new Column("create_by", FieldFill.INSERT)
                )
                .convertFileName(entityName -> entityName + "DO")
                .enableFileOverride()
                .build();

        // 代码生成器
        new AutoGenerator(dataSourceConfig)
                // 全局配置
                .global(globalConfig)
                // 包配置
                .packageInfo(packageConfig)
                // 策略配置
                .strategy(strategyConfig)
                //自定义模板配置
                .injection(getInjectionConfig(projectPath, packageConfig.getParent()))
                // 执行
                .execute(new BlinkTemplateEngine());
    }

    /**
     * 默认配置文件  datasource-dev.yml
     * 根据配置的数据库源 按默认模板生成
     */
    public static void generate() {
        loadYmltoGeneral("datasource-dev.yml", System.getProperty("user.dir"));
    }

    /**
     * 指定profile 生成
     *
     * @param acProfile 配置文件名
     * @param path      指定生成路径 可以为空
     */
    public static void generate(String acProfile, String path) {

        if (Objects.isNull(path) || StrUtil.isBlank(path.trim())) {
            path = System.getProperty("user.dir");
        }

        loadYmltoGeneral("datasource-" + acProfile + ".yml", path);
    }

    private static void loadYmltoGeneral(String res, String path) {

        YamlPropertiesFactoryBean yamlProFb = new YamlPropertiesFactoryBean();
        yamlProFb.setResources(new ClassPathResource(res));
        Properties properties = yamlProFb.getObject();
        assert properties != null;
        String url = properties.getProperty("generator.url");
        String username = properties.getProperty("generator.username");
        String password = properties.getProperty("generator.password");

        generate(url, username, password, path);
    }


    // 处理 all 情况
    protected static List<String> getTables(String tables) {
        return "all".equals(tables) ? Collections.emptyList() : Arrays.asList(tables.split(","));
    }

    // 处理 none 情况
    private static List<String> getTablePrefix(String apply) {
        List<String> list = new ArrayList<>();
        list.add(apply);
        return "none".equals(apply.trim()) ? Collections.emptyList() : list;
    }

    /**
     * 自定义DTO 生成模板
     *
     * @return InjectionConfig
     */
    private static InjectionConfig getInjectionConfig(String projectPath,String basePackage) {

        List<CustomFile> customFiles = getDtoCustomFile(projectPath,basePackage);

        //自定义模板文件上下文属性配置 用于生成正确类名
        return new InjectionConfig.Builder().beforeOutputFile((tableInfo, stringMap) -> {
                    String entityName = tableInfo.getEntityName().replaceAll("DO", "");

                    Object object = stringMap.get("package");
                    Map<String, String> packageInfo = new HashMap<>();
                    if (object instanceof Map<?, ?> map) {
                        // 类型安全的转换，确保 key 和 value 都是 String 类型
                        map.forEach((key, value) -> {
                            if (key instanceof String k && value instanceof String v) {
                                packageInfo.put(k, v);
                            }
                        });
                    }
                    String packageName = packageInfo.get("Parent");
                    stringMap.put("reqPackage", packageName + ".dto.req");
                    stringMap.put("rspPackage", packageName + ".dto.rsp");

                    stringMap.put("addReqDTOName", "Add" + entityName + "Req");
                    stringMap.put("deleteReqDTOName", "Delete" + entityName + "Req");
                    stringMap.put("queryReqDTOName", "Query" + entityName + "Req");
                    stringMap.put("updateReqDTOName", "Update" + entityName + "Req");

                    stringMap.put("queryRspDTOName", "Query" + entityName + "Rsp");

                })
                .customFile(customFiles)
                .build();
    }

    private static List<CustomFile> getDtoCustomFile(String projectPath,String basePackage) {


        List<CustomFile> customFiles = new ArrayList<>();
        String basePath = basePackage.replaceAll("\\.","\\\\");
        String  fullPath = projectPath+ File.separator+ basePath;
        System.out.println(fullPath);
        CustomFile addFile = new CustomFile.Builder()
                .packageName("dto.req")
                .templatePath("/codeTemplate/dto/AddReq.java.vm")
                .filePath(fullPath)
                .fileName("AddReq.java")
//                .formatNameFunction((tableInfo -> getFileName(tableInfo, "Add", "Req.java")))
//                .fileName("AddReq")
                .enableFileOverride()
                .build();

        CustomFile delFile = new CustomFile.Builder()
                .packageName("dto.req")
                .templatePath("/codeTemplate/dto/DeleteReq.java.vm")
                .filePath(fullPath)
//                .formatNameFunction((tableInfo -> getFileName(tableInfo, "Delete", "Req.java")))
                .fileName("DeleteReq.java")
                .enableFileOverride()
                .build();

        CustomFile updateFile = new CustomFile.Builder()
                .packageName("dto.req")
                .templatePath("/codeTemplate/dto/UpdateReq.java.vm")
                .filePath(fullPath)
//                .formatNameFunction((tableInfo -> getFileName(tableInfo, "Update", "Req.java")))
                .fileName("UpdateReq.java")
                .enableFileOverride()
                .build();

        CustomFile queryFile = new CustomFile.Builder()
                .packageName("dto.req")
                .templatePath("/codeTemplate/dto/QueryReq.java.vm")
                .filePath(fullPath)
//                .formatNameFunction((tableInfo -> getFileName(tableInfo, "Query", "Req.java")))
                .fileName("QueryReq.java")
                .enableFileOverride()
                .build();

        CustomFile queryRspFile = new CustomFile.Builder()
                .packageName("dto.rsp")
                .templatePath("/codeTemplate/dto/QueryRsp.java.vm")
                .filePath(fullPath.replaceAll("req","rsp"))
//                .formatNameFunction((tableInfo -> getFileName(tableInfo, "Query", "Rsp.java")))
                .fileName("QueryRsp.java")
                .enableFileOverride()
                .build();

        CustomFile testFile = new CustomFile.Builder()
                .packageName("test")
                .templatePath("/codeTemplate/test.java.vm")
                .filePath(projectPath+ File.separator + basePath)
                .fileName("test.java")
//                .formatNameFunction((tableInfo -> {
//                    return tableInfo.getEntityName().replaceAll("DO", "") + "ControllerTest.java";
//                }))
                .enableFileOverride()
                .build();

        customFiles.add(addFile);
        customFiles.add(delFile);
        customFiles.add(updateFile);
        customFiles.add(queryFile);
        customFiles.add(queryRspFile);
        customFiles.add(testFile);

        return customFiles;
    }

    private static String getFileName(TableInfo tableInfo, String prefix, String suffix) {
        String entityName = tableInfo.getEntityName();
        entityName = entityName.replaceAll("DO", "");
        return prefix + entityName + suffix;
    }

//    private static List<CustomFile> getRecordCustomFile() {
//
//
//        List<CustomFile> customFiles = new ArrayList<>();
//
//
//        CustomFile addFile = new CustomFile.Builder()
//                .enableFileOverride()
//                .packageName("dto.req")
//                .templatePath("/codeTemplate/record/AddReq.java.vm")
//                .build();
//
//        CustomFile delFile = new CustomFile.Builder()
//                .enableFileOverride()
//                .packageName("dto.req")
//                .templatePath("/codeTemplate/record/DeleteReq.java.vm")
//                .build();
//
//        CustomFile updateFile = new CustomFile.Builder()
//                .enableFileOverride()
//                .packageName("dto.req")
//                .templatePath("/codeTemplate/record/UpdateReq.java.vm")
//                .build();
//
//        CustomFile queryFile = new CustomFile.Builder()
//                .enableFileOverride()
//                .packageName("dto.req")
//                .templatePath("/codeTemplate/record/QueryReq.java.vm")
//                .build();
//
//        CustomFile queryRspFile = new CustomFile.Builder()
//                .enableFileOverride()
//                .packageName("dto.rsp")
//                .templatePath("/codeTemplate/record/QueryRsp.java.vm")
//                .build();
//
//        CustomFile testFile = new CustomFile.Builder()
//                .enableFileOverride()
//                .packageName("test")
//                .templatePath("/codeTemplate/test.java.vm")
//                .build();
//
//        customFiles.add(addFile);
//        customFiles.add(delFile);
//        customFiles.add(updateFile);
//        customFiles.add(queryFile);
//        customFiles.add(queryRspFile);
//        customFiles.add(testFile);
//
//        return customFiles;
//    }


    public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/blink?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8";
        String username = "root";
        String password = "123456";
        CodeGenerator.generateByCustomTemplate(url, username, password);

    }

}
