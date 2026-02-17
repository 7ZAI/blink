package com.blink.gateway.config;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.blink.gateway.constant.GatewayConstant.*;


/**
 * @Author binblink
 * @Date 2025/9/6
 */
@Configuration
public class GatewayLocalCacheConfig {


    private volatile ThreadPoolTaskExecutor executor;
    /**
     * 在响应式环境中 使用Caffeine .buildAsync() 异步加载作为本地缓存 它异步的地方在 缓存过期或者为null 是异步取缓存 而不是get put 异步
     *  AsyncCache 的真正价值体现在多个请求同时访问未缓存的 key 时：只有一个会发起异步加载请求 其他线程等待
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager(@Value("${blink.gateway.localCacheEnable:false}") boolean localCacheEnable) {

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 根据配置决定是否注册缓存
        if (localCacheEnable) {

            //数据变更较少，但一旦变更，要求缓存尽快失效（最终一致性窗口小）。同时，数据可能被频繁读取。
            AsyncCache<Object, Object> consistentCache = Caffeine.newBuilder()
                    .initialCapacity(100)
                    // 最大条目数
                    .maximumSize(1000)
                    // 写入后短过期：10分钟，保证脏数据窗口不超过10分钟
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    // 【关键】为异步加载指定独立的线程池，避免阻塞 EventLoop
                    .executor(getExecutor())
                    // 开启统计信息
                    .recordStats()
                    .buildAsync();


            //适用 值长期固定不变的数据 数据永驻内存
            AsyncCache<Object, Object> staticDataCache = Caffeine.newBuilder()
                    .initialCapacity(100)
                    // 最大条目数
                    .maximumSize(1000)
                    //访问后自动续期：1天无访问才淘汰，使常用配置长期保留
                    .expireAfterAccess(24, TimeUnit.HOURS)
                    //写入后7天后过期
                    .expireAfterWrite(7, TimeUnit.DAYS)
                    // 【关键】为异步加载指定独立的线程池，避免阻塞 EventLoop
                    .executor(getExecutor())
                    // 开启统计信息
                    .recordStats()
                    .buildAsync();


            //适用 值经常变动的数据
            AsyncCache<Object, Object> frequentlyChangedCache = Caffeine.newBuilder()
                    .initialCapacity(100)
                    // 最大条目数
                    .maximumSize(1000)
                    // 写入后硬过期：5分钟，保证数据不会太旧
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    // 访问后自动续期：2分钟无访问则淘汰，避免冷数据占用内存
                    .expireAfterAccess(2, TimeUnit.MINUTES)
                    // 异步刷新：1分钟后如果被访问，后台刷新数据，保持新鲜
//                    .refreshAfterWrite(1, TimeUnit.MINUTES)
                    // 【关键】为异步加载指定独立的线程池，避免阻塞 EventLoop
                    .executor(getExecutor())
                    // 开启统计信息
                    .recordStats()
                    .buildAsync();


            cacheManager.registerCustomCache(CONSISTENT_CACHE, consistentCache);
            cacheManager.registerCustomCache(STATICDATA_CACHE, staticDataCache);
            cacheManager.registerCustomCache(FREQUENTLY_CHANGED_CACHE, frequentlyChangedCache);

        }

        return cacheManager;
    }


    /**
     * 单例获取 同一个线程池
     *
     */
    private Executor getExecutor() {
        if (executor == null) {
            synchronized (this) {
                if (executor == null) {
                    ThreadPoolTaskExecutor newTemp = getThreadPoolTaskExecutor();
                    // 通常在容器中会自动调用，显式调用也可
                    newTemp.initialize();
                    executor = newTemp;
                }
                return executor;
            }
        }
        return executor;
    }

    /**
     * 使用spring的线程池
     */
    private static ThreadPoolTaskExecutor getThreadPoolTaskExecutor() {

        ThreadPoolTaskExecutor newTemp = new ThreadPoolTaskExecutor();
        newTemp.setCorePoolSize(4);
        //只有在核心线程满且队列满时才会创建新线程到最大线程
        newTemp.setMaxPoolSize(8);
        //任务队列上限
        newTemp.setQueueCapacity(1000);
        //空闲线程存活时间
        newTemp.setKeepAliveSeconds(60);
        newTemp.setThreadNamePrefix("caffeine-executor");
        //拒绝策略 由调用线程执行
        newTemp.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关闭
        newTemp.setWaitForTasksToCompleteOnShutdown(true);
        //如果有任务未执行完成 等待30秒后关闭线程池
        newTemp.setAwaitTerminationSeconds(30);
        return newTemp;
    }


}
