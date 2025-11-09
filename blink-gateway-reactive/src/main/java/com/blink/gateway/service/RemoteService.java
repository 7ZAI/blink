package com.blink.gateway.service;

import reactor.core.publisher.Mono;

/**
 * 参数配置远程调用抽象接口
 * @author binblink
 */
@FunctionalInterface
public interface RemoteService<T> {

    /**
     * 函数式接口
     * @param key 参数key
     * @param clazz 对象类型
     * @return
     */
    Mono<T> call(String key, Class<T> clazz);
}
