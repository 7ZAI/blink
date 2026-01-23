package com.blink.gateway;

import com.blink.base.dto.req.QueryBlinkChannelReqDTO;
import com.blink.base.dto.rsp.QueryBlinkChannelRspDTO;
import com.blink.base.dto.vo.ChannelVO;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.framework.redis.id.IdGenerator;
import com.blink.framework.redis.id.ReactiveIdGenerator;
import com.blink.gateway.config.prop.BlinkGatewayConfigProperties;
import com.blink.gateway.service.BaseAppService;
import com.blink.gateway.util.WebClientUtil;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
@TestPropertySource(locations = {"classpath:application-test.yml"})
class BlinkReactiveGatewayApplicationTests {

//    @Autowired
    private ReactiveRedisClient redisClient;

//    @Autowired
    private BaseAppService baseAppService;

//    @Autowired
    private ReactiveIdGenerator reactiveIdGenerator;



    @Resource
    private BlinkGatewayConfigProperties configProperties;


//    @BeforeAll
//    static void setup() {
//        System.setProperty("spring.profiles.active", "test");
//    }

    // 打印 Redis 核心配置，验证是否加载
//    @Value("${spring.redis.host:未加载}")
//    private String redisHost;
//    @Value("${spring.redis.password:未加载}")
//    private String redisPassword;
//    @Value("${spring.redis.username:default}")
//    private String redisUsername;
//
//    @Test
//    public void printRedisConfig() {
//        System.out.println("测试环境 Redis Host：" + redisHost);
//        System.out.println("测试环境 Redis Password：" + redisPassword);
//        System.out.println("测试环境 Redis Username：" + redisUsername);
//
//        // 核心判断：若 Password 是「未加载」，说明配置未绑定
//        if ("未加载".equals(redisPassword)) {
//            throw new RuntimeException("测试环境未加载 Redis 密码配置！");
//        }
//    }

    /**
     * ip过滤器测试 改变ip过滤器配置测试
     * ipv4 总所周知
     * ipv6 正确格式例子：2001:0db8:85a3:0000:0000:8a2e:0370:7334，2001:0db8:85a3::8a2e:0370:7334，::1，2001:0db8::，::ffff:192.168.1.1，fe80::1234:5678:9abc:def0%eth0
     *                  fc00::1，2001:0DB8:85A3::8A2E:0370:7334
     *      错误格式：2001:0db8:85a3::8a2e::7334，2001:0db8:85a3:8a2e:0370:7334 ，2001:0db8:85a3:0000:0000:8a2e:0370:7334:1234， 2001:0db8:85a3:g789:0000:8a2e:0370:7334，fe80:1234:5678:9abc:def0
     *              2001:0db8:85a3:000:0000:8a2e:0370:7334， 2001:0db8:85a3:00000:0000:8a2e:0370:7334，::ffff:192.168.1.256，2001:0db8:::8a2e:0370:7334，2001-0db8-85a3-0000-0000-8a2e-0370-7334
     */
    @Test
    void ipFilterTest(){
        System.out.println(configProperties.getIpFilter().toString());
        System.out.println("test");
    }


    @Test
    void test() {

        redisClient.get("user:token:0b6ebd78-d6fc-4022-8e76-1b39d94b4cbf").flatMap(v->{
            String id = (String) v;
            System.out.println(id);
            return Mono.just(id);
        }).subscribe();
    }

    @Test
    void test2() {

        var result =  baseAppService.getOneConfig("black_list_switch").flatMap(r-> {
            System.out.println(r.toString());
            return Mono.just(r);
        }).block();

        var result2 =  baseAppService.getChannelInfo("0ad42e0e8d7760da3d3d2f25862fb8f5d8a4867b").flatMap(r-> {
            System.out.println(r.toString());
            return Mono.just(r);
        }).block();

        System.out.println(result);
        System.out.println(result2);
    }




    @Test
    void idgeneratortest2(){
        Mono<String> tm = reactiveIdGenerator
                .generateId("test", 4).doOnNext(s->{

                    System.out.println( Thread.currentThread().getName()+"---------------id:+" + s);
                });

        Thread t1 = new Thread(()->{
            for(int i = 0 ;i<10;i++){
                tm.subscribe();
            }
        },"t1-thread");

        Thread t2 = new Thread(()->{
            for(int i = 0 ;i<10;i++){
                tm.subscribe();
            }
        },"t2-thread");

        t1.start();
        t2.start();
    }

    @Test
    void idgeneratortest() throws InterruptedException {


        // 测试配置
        int threadCount = 20;          // 线程数量
        int requestsPerThread = 10;    // 每个线程请求次数
        String prefix = "test";
        int digits = 4;

        ThreadPoolExecutor executor = createExecutor();



        // 创建CountDownLatch确保同时开始
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 提交任务到线程池
        System.out.println("📋 提交 " + threadCount + " 个线程任务，每个生成 " + requestsPerThread + " 个ID");
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i + 1;
            executor.submit(() -> {
                try {
                    // 等待所有线程准备就绪
                    startLatch.await();

                    List<CompletableFuture<String>> futures = new ArrayList<>();

                    // 每个线程生成多个ID
                    for (int j = 0; j < requestsPerThread; j++) {
                        final int requestId = j + 1;

                        // 创建CompletableFuture来包装Mono
                        CompletableFuture<String> future = reactiveIdGenerator
                                .generateId(prefix, digits)
                                .doOnSubscribe(s -> System.out.println(
                                        Thread.currentThread().getName() +
                                                " - 线程" + threadId + " 第" + requestId + "次请求"
                                ))
                                .toFuture();

                        // 添加回调处理结果
                        future.thenAccept(id -> {
//                            generatedIds.add(id);
                            System.out.println(
                                    Thread.currentThread().getName() +
                                            " - 线程" + threadId + " 生成: " + id
                            );
                        });

                        futures.add(future);
                    }

                    // 等待所有请求完成
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .get(5, TimeUnit.SECONDS);

                } catch (Exception e) {
                    System.err.println("线程" + threadId + " 执行失败: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 等待所有线程准备就绪，然后同时开始
        Thread.sleep(1000); // 给线程创建一点时间
        System.out.println("\n🎬 所有线程准备就绪，开始并发测试...");
        startLatch.countDown(); // 同时释放所有线程

        // 等待所有线程完成
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);

        // 记录结束时间
        long endTime = System.currentTimeMillis();

        // 关闭线程池
        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS);
    }

    private ThreadPoolExecutor createExecutor(){
        // 线程池配置常量
         final int CORE_POOL_SIZE = 20;        // 核心线程数
         final int MAX_POOL_SIZE = 20;        // 最大线程数
         final int QUEUE_CAPACITY = 100;      // 队列容量
         final long KEEP_ALIVE_TIME = 60L;    // 空闲线程存活时间（秒）

        // 1. 创建线程池（生产环境推荐使用 ThreadPoolExecutor 以便清晰配置）
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new NamedThreadFactory("RequestProcessor"),
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略：调用者线程执行
        );
        return executor;
    }
}
