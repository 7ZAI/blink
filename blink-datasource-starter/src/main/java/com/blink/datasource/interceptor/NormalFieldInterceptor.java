package com.blink.datasource.interceptor;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

/**
 * 自定义拦截器 给常用字段如 createTime、updateTime、delFlag等 在系统层面统一赋值
 * 这里由于使用了mybatis plus ,它会提供相应的逻辑删除配置 所以delFlag不用在这赋值了
 */
// Executor 把增删都算做update里
@Intercepts(@Signature(type = Executor.class,method = "update",args={ MappedStatement.class,Object.class }) )
public class NormalFieldInterceptor implements Interceptor {

    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_TIME = "updateTime";
    private static final String DELETE_FLAG = "delFlag";


    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        //sql 命令类型
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();

        //参数
        Object parameter = invocation.getArgs()[1];

        //获取入参的属性
        Field[] fields = parameter.getClass().getDeclaredFields();

        LocalDateTime now = LocalDateTime.now();

        for(Field f : fields){
            //插入语句
            if(SqlCommandType.INSERT.equals(sqlCommandType)){

                if(CREATE_TIME.equals(f.getName()) || UPDATE_TIME.equals(f.getName())){
                    f.setAccessible(true);
                    f.set(parameter,now);
                }
            }
            // 更新语句
            if(SqlCommandType.UPDATE.equals(sqlCommandType)){
                if( UPDATE_TIME.equals(f.getName())){
                    f.setAccessible(true);
                    f.set(parameter,now);
                }
            }

        }

        return invocation.proceed();
    }

}
