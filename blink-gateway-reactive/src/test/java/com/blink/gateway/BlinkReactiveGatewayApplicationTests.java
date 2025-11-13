package com.blink.gateway;

import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.service.BaseAppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

import java.util.concurrent.*;

@SpringBootTest(classes={BlinkReactiveGatewayApplication.class})
class BlinkReactiveGatewayApplicationTests {

    @Autowired
    private ReactiveRedisClient redisClient;

    @Autowired
    private BaseAppService baseAppService;



    @Test
    void contextLoads() {
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
    void testThread() throws InterruptedException, ExecutionException {
//        IncrSeqCache incrSeqCache =  new IncrSeqCache(0L,10000L,99900L);
//
//        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(16,32,
//                10,TimeUnit.SECONDS,new LinkedBlockingQueue<>(20000));
//        Set<Long> set = new HashSet<>();
//        List<Future<Long>> futures = new ArrayList<>();
//        for (int i = 0; i < 10000; i++) {
//           futures.add(poolExecutor.submit(() -> incrSeqCache.nextValue()));
//        }
//
//        for(Future future : futures){
//            Long v = (Long) future.get();
//            set.add(v.longValue());
//            System.out.println(Thread.currentThread().getName() + "---currentValue----"+ v.longValue());
//        }
////        poolExecutor.shutdown();
////        poolExecutor.awaitTermination(20, TimeUnit.SECONDS);
//
////        Thread.sleep(10000);
////        if(poolExecutor.isTerminated()){
//            System.out.println(set.size());
////        }

    }
}
