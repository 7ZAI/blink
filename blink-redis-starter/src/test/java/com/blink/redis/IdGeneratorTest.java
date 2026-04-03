package com.blink.redis;


import cn.hutool.core.lang.Assert;
import com.blink.framework.redis.id.IdGenerator;
import com.blink.framework.redis.id.ReactiveIdGenerator;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 测试id生成器
 *  web /webflux 环境切换需要手动注释代码和依赖库
 */
@SpringBootTest(classes = TestApplicationConfig.class)
@ActiveProfiles("test")
public class IdGeneratorTest {

    private static final int SIZE = 1000000;
    private static final boolean VERBOSE = true;

//    @Resource
    private IdGenerator idGenerator;

    @Resource
    private ReactiveIdGenerator reactiveIdGenerator;

    @Test
    public void testConcurrentGenerateIdUniqueness() throws InterruptedException {
        int totalRequests = 10000; // 测试生成 1w 个 ID
        int parallelism = 16;      // 模拟 50 个并发线程

        // 使用 ConcurrentHashMap 的 KeySet 来实现线程安全的去重集合
        Set<Long> idSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
        CountDownLatch latch = new CountDownLatch(1);

        System.out.println("开始并发生成 ID 测试...");

        Flux.range(0, totalRequests)
                .parallel(parallelism)             // 开启并行执行
                .runOn(Schedulers.parallel())     // 在并行调度器上运行
                .flatMap(i -> reactiveIdGenerator.generateId("test"))
                .doOnNext(id -> {
                    if (!idSet.add(id)) {
                        System.err.println("发现重复 ID: " + id);
                    }
                })
                .sequential()                      // 转回顺序流以便统计
                .doFinally(signalType -> latch.countDown())
                .subscribe();

        latch.await(); // 等待所有异步任务完成

        System.out.println("期望数量: " + totalRequests);
        System.out.println("实际去重后数量: " + idSet.size());

        // 断言：去重后的集合大小应等于总请求数
        Assertions.assertEquals(totalRequests, idSet.size(), "ID 生成存在重复！");
    }

    /**
     * 串行
     * 
     * @throws IOException
     */
    @Test
    public void testSerialGenerate() throws IOException {
        // Generate UID serially
        long start = System.currentTimeMillis();
        Set<Long> uidSet = new HashSet<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            doGenerate(uidSet, i);
        }

        // Check UIDs are all unique
        checkUniqueID(uidSet);
        long end = System.currentTimeMillis();

        System.out.println("串行生成"+SIZE+"个序号花费时间："+(end - start)+"ms");
    }

    /**
     * 并行
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    public void testParallelGenerate() throws InterruptedException, IOException {
        long start = System.currentTimeMillis();
        Set<Long> uidSet = new ConcurrentSkipListSet<>();
        AtomicInteger control = new AtomicInteger(-1);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(16,
                32,
                6,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(60000),
                new NamedThreadFactory("线程池线程---"),
                new ThreadPoolExecutor.DiscardPolicy());

        while(control.get() < SIZE) {
                executor.submit(()->{
                    //检测是否满了
                    int myPosition = control.updateAndGet(old -> (old == SIZE ? SIZE : old + 1));
                    if (myPosition == SIZE) {
                        return;
                    }
                    long uid = idGenerator.generateId("test");
                    boolean existed = !uidSet.add(uid);
                    if (existed) {
                        System.out.println("Found duplicate UID " + uid);
                        throw new RuntimeException("出现重复id");
                    }
//                    System.out.println(Thread.currentThread().getName() + "  No."+ myPosition +" seq >>> " + uid);
            });
        }

        // 关闭 不再接收新的任务
        executor.shutdown();
        try {
            // 2. 阻塞等待，直到：
            //    a) 所有任务执行完
            //    b) 超过指定的超时时间 (例如 1 小时)
            //    c) 当前线程被中断
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                // 如果超时了，任务还没跑完，可以选择强制关闭或记录日志
                System.err.println("警告：线程池在超时时间内未完全关闭，部分任务可能仍在运行。");
                executor.shutdownNow();
            } else {
                System.out.println("所有任务执行完毕，线程池已正常关闭。");
            }
        } catch (InterruptedException e) {
            // 3. 如果当前线程在等待时被中断，立即尝试强制关闭
            executor.shutdownNow();
            Thread.currentThread().interrupt(); // 保持中断状态
        }

        long end = System.currentTimeMillis();
        System.out.println("并行生成"+uidSet.size()+"个序号花费时间："+(end - start)+"ms");
    }

    /**
     * CAS 优化 读写锁 细粒度下
     *  10000id 10线程 共10万  0.59 秒
     * 20 线程 50000 id 共 100万  0.91 秒
     *
     * 仅仅读写锁 粗粒度控制并发
     *  10000id 10线程 共10万  0.59 秒耗时: 0.70 秒
     *  20 线程 50000 id 共 100万  耗时: 2.87 秒
     * @throws InterruptedException
     */
    @Test
    public void multiThreadTest() throws InterruptedException {
        // 配置测试参数
        int threadCount = 16;       // 并发线程数
        int idsPerThread = 100000;     // 每个线程获取的ID数量
        int totalExpected = threadCount * idsPerThread;

        // 用于存储结果，验证是否重复
        // 使用 ConcurrentHashMap 的 KeySet 模拟线程安全的 Set
//        Set<Long> idSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Set<Long> idSet =  new ConcurrentSkipListSet<>();

        // 倒计时锁：用于模拟瞬时并发
        CountDownLatch startGate = new CountDownLatch(1);
        // 倒计时锁：用于等待所有线程执行结束
        CountDownLatch endGate = new CountDownLatch(threadCount);

        // 错误计数
        AtomicLong errorCounter = new AtomicLong(0);

        // 创建任务
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startGate.await(); // 所有线程在此处阻塞等待鸣枪

                    for (int j = 0; j < idsPerThread; j++) {
                        Long id = idGenerator.generateId("test");
//                        Long id =  reactiveIdGenerator.generateId("test").block(Duration.ofSeconds(10));
                        if (id != null) {
//                            System.out.println("seq id: ---------------------" + id);
                            if(!idSet.add(id)){
                                errorCounter.incrementAndGet();
                                System.err.println("重复的 seq id: ---------------------" + id);
                            }
                        } else {
                            System.out.println("------有空值-----------------------");
                            errorCounter.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    System.out.println("------有异常-----------------------");
                    errorCounter.incrementAndGet();
                    throw new RuntimeException(e);
                } finally {
                    endGate.countDown();
                }
            }).start();
        }

        // --- 开始测试 ---
        System.out.println(">>> 测试开始：并发线程数 " + threadCount + ", 计划生成 ID 总数 " + totalExpected);
        long startTime = System.nanoTime();

        startGate.countDown(); // 鸣枪！所有线程同时开始
        endGate.await();       // 等待所有线程跑完

        long endTime = System.nanoTime();
        // --- 测试结束 ---

        double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
        int actualSize = idSet.size();

        // --- 结果分析 ---
        System.out.println("---------------------------------------");
        System.out.println("耗时: " + String.format("%.2f", durationSeconds) + " 秒");
        System.out.println("吞吐量 (TPS): " + (int)(actualSize / durationSeconds));
        System.out.println("预期 ID 数量: " + totalExpected);
        System.out.println("实际去重后 ID 数量: " + actualSize);
        System.out.println("异常请求数: " + errorCounter.get());

        if (actualSize == totalExpected && errorCounter.get() == 0) {
            System.out.println("验证结果: SUCCESS (无重复，无丢失)");
        } else {
            System.err.println("验证结果: FAILED (存在重复或丢失！)");
            System.err.println("丢失/重复差值: " + (totalExpected - actualSize));
        }
    }


    @Test
    public void multiThreadRequestId() throws InterruptedException {
        // 配置测试参数
        int threadCount = 16;       // 并发线程数
        int idsPerThread = 100000;     // 每个线程获取的ID数量
        int totalExpected = threadCount * idsPerThread;

        // 用于存储结果，验证是否重复
        // 使用 ConcurrentHashMap 的 KeySet 模拟线程安全的 Set
//        Set<Long> idSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Set<String> idSet =  new ConcurrentSkipListSet<>();

        // 倒计时锁：用于模拟瞬时并发
        CountDownLatch startGate = new CountDownLatch(1);
        // 倒计时锁：用于等待所有线程执行结束
        CountDownLatch endGate = new CountDownLatch(threadCount);

        // 错误计数
        AtomicLong errorCounter = new AtomicLong(0);

        // 创建任务
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startGate.await(); // 所有线程在此处阻塞等待鸣枪

                    for (int j = 0; j < idsPerThread; j++) {
                        String id = idGenerator.generateRequestId();
                        if (id != null) {
//                            System.out.println("seq id: ---------------------" + id);
                            if(!idSet.add(id)){
                                errorCounter.incrementAndGet();
                                System.err.println("重复的 seq id: ---------------------" + id);
                            }
                        } else {
                            System.out.println("------有空值-----------------------");
                            errorCounter.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    System.out.println("------有异常-----------------------");
                    errorCounter.incrementAndGet();
                    throw new RuntimeException(e);
                } finally {
                    endGate.countDown();
                }
            }).start();
        }

        // --- 开始测试 ---
        System.out.println(">>> 测试开始：并发线程数 " + threadCount + ", 计划生成 ID 总数 " + totalExpected);
        long startTime = System.nanoTime();

        startGate.countDown(); // 鸣枪！所有线程同时开始
        endGate.await();       // 等待所有线程跑完

        long endTime = System.nanoTime();
        // --- 测试结束 ---

        double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
        int actualSize = idSet.size();

        // --- 结果分析 ---
        System.out.println("---------------------------------------");
        System.out.println("耗时: " + String.format("%.2f", durationSeconds) + " 秒");
        System.out.println("吞吐量 (TPS): " + (int)(actualSize / durationSeconds));
        System.out.println("预期 ID 数量: " + totalExpected);
        System.out.println("实际去重后 ID 数量: " + actualSize);
        System.out.println("异常请求数: " + errorCounter.get());

        if (actualSize == totalExpected && errorCounter.get() == 0) {
            System.out.println("验证结果: SUCCESS (无重复，无丢失)");
        } else {
            System.err.println("验证结果: FAILED (存在重复或丢失！)");
            System.err.println("丢失/重复差值: " + (totalExpected - actualSize));
        }
    }


    /**
     * Do generating
     */
    private void doGenerate(Set<Long> uidSet, int index) {
        long uid = idGenerator.generateId("test");
        boolean existed = !uidSet.add(uid);
        if (existed) {
            System.out.println("Found duplicate UID " + uid);
        }

        // Check UID is positive, and can be parsed
        Assert.isTrue(uid > 0L);
        Assert.isTrue(StringUtils.isNotBlank(String.valueOf(uid)));

        if (VERBOSE) {
            System.out.println(Thread.currentThread().getName() + " No." + index + "-----------------" + String.valueOf(uid));
        }
    }

    /**
     * Check UIDs are all unique
     */
    private void checkUniqueID(Set<Long> uidSet) throws IOException {
        System.out.println(uidSet.size());
        Assert.equals(uidSet.size(), SIZE);
    }



}
