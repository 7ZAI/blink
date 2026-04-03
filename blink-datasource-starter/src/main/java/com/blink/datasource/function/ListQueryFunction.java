package com.blink.datasource.function;

import java.util.List;

/**
 * 返回列表的查询函数
 * 用于自定义分页查询场景
 *
 * @param <T> 返回的列表元素类型
 * @author binblink
 */
@FunctionalInterface
public interface ListQueryFunction<T> {

    /**
     * 执行查询并返回列表
     *
     * @return 查询结果列表
     */
    List<T> execute();
}