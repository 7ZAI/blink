package com.blink.log.annotation;

import com.blink.log.constant.LogType;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>
 * 标注在 Controller 方法上，自动记录操作日志
 * <p>
 * 使用示例：
 * <pre>
 *     &#064;OperationLog(
 *         type = LogType.OPERATION,
 *         description = "新增系统用户"
 *     )
 *     &#064;PostMapping("/saveSysUser")
 *     public ResponseDTO&lt;EmptyBody&gt; saveSysUser(@RequestBody @Validated RequestDTO&lt;AddSysUserReq&gt; reqDto) {
 *         // ...
 *     }
 * </pre>
 *
 * @author binblink
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RecordLog {

    /**
     * 日志类型
     * <p>
     * LOGIN-登入日志, SYSTEM-系统日志, OPERATION-操作日志
     *
     * @return 日志类型枚举
     */
    LogType type() default LogType.OPERATION;

    /**
     * 操作描述
     *
     * @return 操作描述文本
     */
    String description() default "";

    /**
     * 是否保存请求参数
     *
     * @return true-保存 false-不保存
     */
    boolean saveRequest() default true;

    /**
     * 是否保存响应结果
     *
     * @return true-保存 false-不保存
     */
    boolean saveResponse() default true;
}