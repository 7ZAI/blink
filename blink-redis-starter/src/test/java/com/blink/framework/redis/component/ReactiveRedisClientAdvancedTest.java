package com.blink.framework.redis.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReactiveRedisClient 高级操作单元测试
 * 包含List、Set、ZSet、Stream、Lua脚本、管道操作
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReactiveRedisClientAdvancedTest {

    @Mock
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @Mock
    private ReactiveRedisTemplate<String, Object> streamTemplate;

    @Mock
    private ReactiveListOperations<String, Object> listOperations;

    @Mock
    private ReactiveSetOperations<String, Object> setOperations;

    @Mock
    private ReactiveZSetOperations<String, Object> zSetOperations;

    @Mock
    private ReactiveStreamOperations<String, Object, Object> streamOperations;

    @Mock
    private ReactiveStreamOperations<String, Object, Object> streamTemplateOperations;

    private ReactiveRedisClient reactiveRedisClient;

    @BeforeEach
    void setUp() {
        when(reactiveRedisTemplate.opsForList()).thenReturn(listOperations);
        when(reactiveRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(reactiveRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(reactiveRedisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamTemplate.opsForStream()).thenReturn(streamTemplateOperations);

        reactiveRedisClient = new ReactiveRedisClient(reactiveRedisTemplate, streamTemplate);
    }

    // ==================== List 操作测试 ====================

    @Nested
    @DisplayName("List 操作测试")
    class ListOperationTests {

        @Test
        @DisplayName("07-01: 左侧推入")
        void testLPush() {
            // Given
            String key = "list-key";
            Object[] values = {"value1", "value2"};
            when(listOperations.leftPushAll(key, values)).thenReturn(Mono.just(2L));

            // When
            Mono<Long> result = reactiveRedisClient.lPush(key, values);

            // Then
            StepVerifier.create(result)
                    .expectNext(2L)
                    .verifyComplete();
            verify(listOperations).leftPushAll(key, values);
        }

        @Test
        @DisplayName("07-02: 右侧推入")
        void testRPush() {
            // Given
            String key = "list-key";
            Object[] values = {"value1", "value2"};
            when(listOperations.rightPushAll(key, values)).thenReturn(Mono.just(2L));

            // When
            Mono<Long> result = reactiveRedisClient.rPush(key, values);

            // Then
            StepVerifier.create(result)
                    .expectNext(2L)
                    .verifyComplete();
            verify(listOperations).rightPushAll(key, values);
        }

        @Test
        @DisplayName("07-03: 左侧弹出")
        void testLPop() {
            // Given
            String key = "list-key";
            Object expectedValue = "value1";
            when(listOperations.leftPop(key)).thenReturn(Mono.just(expectedValue));

            // When
            Mono<Object> result = reactiveRedisClient.lPop(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(expectedValue)
                    .verifyComplete();
            verify(listOperations).leftPop(key);
        }

        @Test
        @DisplayName("07-04: 右侧弹出")
        void testRPop() {
            // Given
            String key = "list-key";
            Object expectedValue = "value1";
            when(listOperations.rightPop(key)).thenReturn(Mono.just(expectedValue));

            // When
            Mono<Object> result = reactiveRedisClient.rPop(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(expectedValue)
                    .verifyComplete();
            verify(listOperations).rightPop(key);
        }

        @Test
        @DisplayName("07-05: 获取范围元素")
        void testLRange() {
            // Given
            String key = "list-key";
            long start = 0;
            long end = -1;
            when(listOperations.range(key, start, end)).thenReturn(Flux.just("value1", "value2"));

            // When
            Flux<Object> result = reactiveRedisClient.lRange(key, start, end);

            // Then
            StepVerifier.create(result)
                    .expectNext("value1", "value2")
                    .verifyComplete();
            verify(listOperations).range(key, start, end);
        }

        @Test
        @DisplayName("07-06: 获取列表长度")
        void testLLen() {
            // Given
            String key = "list-key";
            when(listOperations.size(key)).thenReturn(Mono.just(5L));

            // When
            Mono<Long> result = reactiveRedisClient.lLen(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(5L)
                    .verifyComplete();
            verify(listOperations).size(key);
        }

        @Test
        @DisplayName("07-07: 按索引获取元素")
        void testLIndex() {
            // Given
            String key = "list-key";
            long index = 0;
            Object expectedValue = "value1";
            when(listOperations.index(key, index)).thenReturn(Mono.just(expectedValue));

            // When
            Mono<Object> result = reactiveRedisClient.lIndex(key, index);

            // Then
            StepVerifier.create(result)
                    .expectNext(expectedValue)
                    .verifyComplete();
            verify(listOperations).index(key, index);
        }

        @Test
        @DisplayName("07-08: 设置指定索引元素")
        void testLSet() {
            // Given
            String key = "list-key";
            long index = 0;
            Object value = "new-value";
            when(listOperations.set(key, index, value)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.lSet(key, index, value);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(listOperations).set(key, index, value);
        }
    }

    // ==================== Set 操作测试 ====================

    @Nested
    @DisplayName("Set 操作测试")
    class SetOperationTests {

        @Test
        @DisplayName("07-09: 添加集合成员")
        void testSAdd() {
            // Given
            String key = "set-key";
            Object[] values = {"member1", "member2"};
            when(setOperations.add(key, values)).thenReturn(Mono.just(2L));

            // When
            Mono<Long> result = reactiveRedisClient.sAdd(key, values);

            // Then
            StepVerifier.create(result)
                    .expectNext(2L)
                    .verifyComplete();
            verify(setOperations).add(key, values);
        }

        @Test
        @DisplayName("07-10: 获取所有成员")
        void testSMembers() {
            // Given
            String key = "set-key";
            when(setOperations.members(key)).thenReturn(Flux.just("member1", "member2"));

            // When
            Flux<Object> result = reactiveRedisClient.sMembers(key);

            // Then
            StepVerifier.create(result)
                    .expectNext("member1", "member2")
                    .verifyComplete();
            verify(setOperations).members(key);
        }

        @Test
        @DisplayName("07-11: 成员存在判断")
        void testSIsMember() {
            // Given
            String key = "set-key";
            Object value = "member1";
            when(setOperations.isMember(key, value)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.sIsMember(key, value);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(setOperations).isMember(key, value);
        }

        @Test
        @DisplayName("07-12: 获取集合大小")
        void testSSize() {
            // Given
            String key = "set-key";
            when(setOperations.size(key)).thenReturn(Mono.just(3L));

            // When
            Mono<Long> result = reactiveRedisClient.sSize(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(3L)
                    .verifyComplete();
            verify(setOperations).size(key);
        }

        @Test
        @DisplayName("07-13: 移除成员")
        void testSRemove() {
            // Given
            String key = "set-key";
            Object[] values = {"member1"};
            when(setOperations.remove(key, values)).thenReturn(Mono.just(1L));

            // When
            Mono<Long> result = reactiveRedisClient.sRemove(key, values);

            // Then
            StepVerifier.create(result)
                    .expectNext(1L)
                    .verifyComplete();
            verify(setOperations).remove(key, values);
        }

        @Test
        @DisplayName("07-14: 随机弹出成员")
        void testSPop() {
            // Given
            String key = "set-key";
            Object expectedValue = "member1";
            when(setOperations.pop(key)).thenReturn(Mono.just(expectedValue));

            // When
            Mono<Object> result = reactiveRedisClient.sPop(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(expectedValue)
                    .verifyComplete();
            verify(setOperations).pop(key);
        }
    }

    // ==================== ZSet 操作测试 ====================

    @Nested
    @DisplayName("ZSet 操作测试")
    class ZSetOperationTests {

        @Test
        @DisplayName("07-15: 添加有序集合成员")
        void testZAdd() {
            // Given
            String key = "zset-key";
            Object value = "member1";
            double score = 10.0;
            when(zSetOperations.add(key, value, score)).thenReturn(Mono.just(true));

            // When
            Mono<Boolean> result = reactiveRedisClient.zAdd(key, value, score);

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
            verify(zSetOperations).add(key, value, score);
        }

        @Test
        @DisplayName("07-16: 批量添加")
        @SuppressWarnings("unchecked")
        void testZAddBatch() {
            // Given
            String key = "zset-key";
            Map<Object, Double> valueScores = new HashMap<>();
            valueScores.put("member1", 10.0);
            valueScores.put("member2", 20.0);

            when(zSetOperations.addAll(eq(key), anyList())).thenReturn(Mono.just(2L));

            // When
            Mono<Long> result = reactiveRedisClient.zAdd(key, valueScores);

            // Then
            StepVerifier.create(result)
                    .expectNext(2L)
                    .verifyComplete();
            verify(zSetOperations).addAll(eq(key), anyList());
        }

        @Test
        @DisplayName("07-17: 按索引范围获取")
        @SuppressWarnings("unchecked")
        void testZRange() {
            // Given
            String key = "zset-key";
            when(zSetOperations.range(eq(key), any(Range.class))).thenReturn(Flux.just("member1", "member2"));

            // When
            Flux<Object> result = reactiveRedisClient.zRange(key, 0, -1);

            // Then
            StepVerifier.create(result)
                    .expectNext("member1", "member2")
                    .verifyComplete();
            verify(zSetOperations).range(eq(key), any(Range.class));
        }

        @Test
        @DisplayName("07-18: 按分数范围获取")
        @SuppressWarnings("unchecked")
        void testZRangeByScore() {
            // Given
            String key = "zset-key";
            when(zSetOperations.rangeByScore(eq(key), any(Range.class))).thenReturn(Flux.just("member1"));

            // When
            Flux<Object> result = reactiveRedisClient.zRangeByScore(key, 0.0, 100.0);

            // Then
            StepVerifier.create(result)
                    .expectNext("member1")
                    .verifyComplete();
            verify(zSetOperations).rangeByScore(eq(key), any(Range.class));
        }

        @Test
        @DisplayName("07-19: 获取成员分数")
        void testZScore() {
            // Given
            String key = "zset-key";
            Object value = "member1";
            when(zSetOperations.score(key, value)).thenReturn(Mono.just(10.0));

            // When
            Mono<Double> result = reactiveRedisClient.zScore(key, value);

            // Then
            StepVerifier.create(result)
                    .expectNext(10.0)
                    .verifyComplete();
            verify(zSetOperations).score(key, value);
        }

        @Test
        @DisplayName("07-20: 获取成员数量")
        void testZSize() {
            // Given
            String key = "zset-key";
            when(zSetOperations.size(key)).thenReturn(Mono.just(5L));

            // When
            Mono<Long> result = reactiveRedisClient.zSize(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(5L)
                    .verifyComplete();
            verify(zSetOperations).size(key);
        }

        @Test
        @DisplayName("07-21: 获取成员排名")
        void testZRank() {
            // Given
            String key = "zset-key";
            Object value = "member1";
            when(zSetOperations.rank(key, value)).thenReturn(Mono.just(0L));

            // When
            Mono<Long> result = reactiveRedisClient.zRank(key, value);

            // Then
            StepVerifier.create(result)
                    .expectNext(0L)
                    .verifyComplete();
            verify(zSetOperations).rank(key, value);
        }

        @Test
        @DisplayName("07-22: 移除成员")
        void testZRemove() {
            // Given
            String key = "zset-key";
            Object[] values = {"member1"};
            when(zSetOperations.remove(key, values)).thenReturn(Mono.just(1L));

            // When
            Mono<Long> result = reactiveRedisClient.zRemove(key, values);

            // Then
            StepVerifier.create(result)
                    .expectNext(1L)
                    .verifyComplete();
            verify(zSetOperations).remove(key, values);
        }
    }

    // ==================== Stream 操作测试 ====================

    @Nested
    @DisplayName("Stream 操作测试")
    class StreamOperationTests {

        @Test
        @DisplayName("07-23: 发送单字段消息")
        @SuppressWarnings("unchecked")
        void testXAdd_SingleField() {
            // Given
            String key = "stream-key";
            String field = "field1";
            Object value = "value1";
            RecordId recordId = RecordId.of("1234567890123-0");

            when(streamOperations.add(any(MapRecord.class))).thenReturn(Mono.just(recordId));

            // When
            Mono<String> result = reactiveRedisClient.xAdd(key, field, value);

            // Then
            StepVerifier.create(result)
                    .expectNext("1234567890123-0")
                    .verifyComplete();
            verify(streamOperations).add(any(MapRecord.class));
        }

        @Test
        @DisplayName("07-24: 发送多字段消息")
        @SuppressWarnings("unchecked")
        void testXAdd_MultiField() {
            // Given
            String key = "stream-key";
            Map<String, Object> body = new HashMap<>();
            body.put("field1", "value1");
            body.put("field2", "value2");
            RecordId recordId = RecordId.of("1234567890123-1");

            when(streamOperations.add(any(MapRecord.class))).thenReturn(Mono.just(recordId));

            // When
            Mono<String> result = reactiveRedisClient.xAdd(key, body);

            // Then
            StepVerifier.create(result)
                    .expectNext("1234567890123-1")
                    .verifyComplete();
            verify(streamOperations).add(any(MapRecord.class));
        }

        @Test
        @DisplayName("07-25: 获取Stream长度")
        void testXLen() {
            // Given
            String key = "stream-key";
            when(streamOperations.size(key)).thenReturn(Mono.just(100L));

            // When
            Mono<Long> result = reactiveRedisClient.xLen(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(100L)
                    .verifyComplete();
            verify(streamOperations).size(key);
        }

        @Test
        @DisplayName("07-26: 删除消息")
        void testXDel() {
            // Given
            String key = "stream-key";
            String[] messageIds = {"1234567890123-0"};
            when(streamOperations.delete(key, messageIds)).thenReturn(Mono.just(1L));

            // When
            Mono<Long> result = reactiveRedisClient.xDel(key, messageIds);

            // Then
            StepVerifier.create(result)
                    .expectNext(1L)
                    .verifyComplete();
            verify(streamOperations).delete(key, messageIds);
        }

        @Test
        @DisplayName("07-27: 范围查询")
        @SuppressWarnings("unchecked")
        void testXRange() {
            // Given
            String key = "stream-key";
            when(streamOperations.range(eq(key), any(Range.class))).thenReturn(Flux.empty());

            // When
            Flux<MapRecord<String, Object, Object>> result = reactiveRedisClient.xRange(key, "-", "+");

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
            verify(streamOperations).range(eq(key), any(Range.class));
        }

        @Test
        @DisplayName("07-28: 带数量限制的范围查询")
        @SuppressWarnings("unchecked")
        void testXRangeWithCount() {
            // Given
            String key = "stream-key";
            int count = 10;
            when(streamOperations.range(eq(key), any(Range.class), any(Limit.class))).thenReturn(Flux.empty());

            // When
            Flux<MapRecord<String, Object, Object>> result = reactiveRedisClient.xRange(key, "-", "+", count);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
            verify(streamOperations).range(eq(key), any(Range.class), any(Limit.class));
        }

        @Test
        @DisplayName("07-29: 反向范围查询")
        @SuppressWarnings("unchecked")
        void testXRevRange() {
            // Given
            String key = "stream-key";
            when(streamOperations.reverseRange(eq(key), any(Range.class))).thenReturn(Flux.empty());

            // When
            Flux<MapRecord<String, Object, Object>> result = reactiveRedisClient.xRevRange(key, "-", "+");

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
            verify(streamOperations).reverseRange(eq(key), any(Range.class));
        }

        @Test
        @DisplayName("07-30: 创建消费者组")
        void testXGroupCreate() {
            // Given
            String key = "stream-key";
            String groupName = "test-group";
            String startId = "0";
            when(streamOperations.createGroup(eq(key), any(ReadOffset.class), eq(groupName)))
                    .thenReturn(Mono.just("OK"));

            // When
            Mono<String> result = reactiveRedisClient.xGroupCreate(key, groupName, startId);

            // Then
            StepVerifier.create(result)
                    .expectNext("OK")
                    .verifyComplete();
            verify(streamOperations).createGroup(eq(key), any(ReadOffset.class), eq(groupName));
        }

        @Test
        @DisplayName("07-31: 删除消费者组")
        void testXGroupDestroy() {
            // Given
            String key = "stream-key";
            String groupName = "test-group";
            when(streamOperations.destroyGroup(key, groupName)).thenReturn(Mono.just("OK"));

            // When
            Mono<String> result = reactiveRedisClient.xGroupDestroy(key, groupName);

            // Then
            StepVerifier.create(result)
                    .expectNext("OK")
                    .verifyComplete();
            verify(streamOperations).destroyGroup(key, groupName);
        }

        @Test
        @DisplayName("07-32: 从消费者组读取")
        @SuppressWarnings("unchecked")
        void testXReadGroup() {
            // Given
            Consumer consumer = Consumer.from("test-group", "test-consumer");
            StreamOffset<String> streamOffset = StreamOffset.create("stream-key", ReadOffset.from(">"));

            when(streamOperations.read(eq(consumer), any(StreamOffset.class))).thenReturn(Flux.empty());

            // When
            Flux<MapRecord<String, Object, Object>> result = reactiveRedisClient.xReadGroup(consumer, streamOffset);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
            verify(streamOperations).read(eq(consumer), any(StreamOffset.class));
        }

        @Test
        @DisplayName("07-33: 确认消息")
        void testXAck() {
            // Given
            String key = "stream-key";
            String groupName = "test-group";
            String[] messageIds = {"1234567890123-0"};
            when(streamOperations.acknowledge(key, groupName, messageIds)).thenReturn(Mono.just(1L));

            // When
            Mono<Long> result = reactiveRedisClient.xAck(key, groupName, messageIds);

            // Then
            StepVerifier.create(result)
                    .expectNext(1L)
                    .verifyComplete();
            verify(streamOperations).acknowledge(key, groupName, messageIds);
        }

        @Test
        @DisplayName("07-34: 查看待处理消息")
        void testXPending() {
            // Given
            String key = "stream-key";
            String groupName = "test-group";
            PendingMessagesSummary summary = mock(PendingMessagesSummary.class);
            when(streamOperations.pending(key, groupName)).thenReturn(Mono.just(summary));

            // When
            Mono<PendingMessagesSummary> result = reactiveRedisClient.xPending(key, groupName);

            // Then
            StepVerifier.create(result)
                    .expectNext(summary)
                    .verifyComplete();
            verify(streamOperations).pending(key, groupName);
        }

        @Test
        @DisplayName("07-35: 修剪Stream")
        void testXTrim() {
            // Given
            String key = "stream-key";
            long maxLength = 1000;
            when(streamTemplateOperations.trim(key, maxLength)).thenReturn(Mono.just(100L));

            // When
            Mono<Long> result = reactiveRedisClient.xTrim(key, maxLength);

            // Then
            StepVerifier.create(result)
                    .expectNext(100L)
                    .verifyComplete();
            verify(streamTemplateOperations).trim(key, maxLength);
        }

        @Test
        @DisplayName("07-36: 获取Stream信息")
        void testXInfo() {
            // Given
            String key = "stream-key";
            StreamInfo.XInfoStream xInfoStream = mock(StreamInfo.XInfoStream.class);
            when(streamTemplateOperations.info(key)).thenReturn(Mono.just(xInfoStream));

            // When
            Mono<StreamInfo.XInfoStream> result = reactiveRedisClient.xInfo(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(xInfoStream)
                    .verifyComplete();
            verify(streamTemplateOperations).info(key);
        }

        @Test
        @DisplayName("07-37: 获取消费者组信息")
        void testXInfoGroups() {
            // Given
            String key = "stream-key";
            StreamInfo.XInfoGroup xInfoGroup = mock(StreamInfo.XInfoGroup.class);
            when(streamTemplateOperations.groups(key)).thenReturn(Flux.just(xInfoGroup));

            // When
            Flux<StreamInfo.XInfoGroup> result = reactiveRedisClient.xInfoGroups(key);

            // Then
            StepVerifier.create(result)
                    .expectNext(xInfoGroup)
                    .verifyComplete();
            verify(streamTemplateOperations).groups(key);
        }

        @Test
        @DisplayName("07-38: 获取消费者信息")
        void testXInfoConsumers() {
            // Given
            String key = "stream-key";
            String groupName = "test-group";
            StreamInfo.XInfoConsumer xInfoConsumer = mock(StreamInfo.XInfoConsumer.class);
            when(streamTemplateOperations.consumers(key, groupName)).thenReturn(Flux.just(xInfoConsumer));

            // When
            Flux<StreamInfo.XInfoConsumer> result = reactiveRedisClient.xInfoConsumers(key, groupName);

            // Then
            StepVerifier.create(result)
                    .expectNext(xInfoConsumer)
                    .verifyComplete();
            verify(streamTemplateOperations).consumers(key, groupName);
        }

        @Test
        @DisplayName("07-39: 转移消息所有权")
        @SuppressWarnings("unchecked")
        void testXClaim() {
            // Given
            String key = "stream-key";
            String groupName = "test-group";
            Consumer consumer = Consumer.from(groupName, "test-consumer");
            long minIdleTime = 60000;
            String[] messageIds = {"1234567890123-0"};

            when(streamTemplateOperations.claim(eq(key), eq(groupName), eq(groupName), any(Duration.class), any(RecordId[].class)))
                    .thenReturn(Flux.empty());

            // When
            Flux<MapRecord<String, Object, Object>> result = reactiveRedisClient.xClaim(key, groupName, consumer, minIdleTime, messageIds);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
            verify(streamTemplateOperations).claim(eq(key), eq(groupName), eq(groupName), any(Duration.class), any(RecordId[].class));
        }

        @Test
        @DisplayName("07-40: 批量添加消息")
        @SuppressWarnings("unchecked")
        void testXAddBatch() {
            // Given
            String key = "stream-key";
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> msg1 = new HashMap<>();
            msg1.put("field1", "value1");
            messages.add(msg1);

            RecordId recordId = RecordId.of("1234567890123-0");
            when(streamOperations.add(any(MapRecord.class))).thenReturn(Mono.just(recordId));

            // When
            Flux<String> result = reactiveRedisClient.xAddBatch(key, messages);

            // Then
            StepVerifier.create(result)
                    .expectNext("1234567890123-0")
                    .verifyComplete();
        }

        @Test
        @DisplayName("07-41: 读取并确认消息")
        @SuppressWarnings("unchecked")
        void testXReadGroupAndAck() {
            // Given
            String key = "stream-key";
            String groupName = "test-group";
            Consumer consumer = Consumer.from(groupName, "test-consumer");
            StreamOffset<String> streamOffset = StreamOffset.create(key, ReadOffset.from(">"));

            // Create a mock record
            MapRecord<String, Object, Object> mockRecord = mock(MapRecord.class);
            when(mockRecord.getId()).thenReturn(RecordId.of("1234567890123-0"));

            when(streamOperations.read(eq(consumer), any(StreamOffset.class))).thenReturn(Flux.just(mockRecord));
            when(streamOperations.acknowledge(eq(key), eq(groupName), anyString())).thenReturn(Mono.just(1L));

            // When
            Flux<MapRecord<String, Object, Object>> result = reactiveRedisClient.xReadGroupAndAck(consumer, streamOffset, groupName);

            // Then
            StepVerifier.create(result)
                    .expectNext(mockRecord)
                    .verifyComplete();
        }
    }

    // ==================== 发布订阅 & Lua & 管道测试 ====================

    @Nested
    @DisplayName("发布订阅 & Lua & 管道测试")
    class PubSubLuaPipelineTests {

        @Test
        @DisplayName("07-42: 发布消息")
        void testPublish() {
            // Given
            String channel = "test-channel";
            Object message = "test-message";
            when(reactiveRedisTemplate.convertAndSend(channel, message)).thenReturn(Mono.just(2L));

            // When
            Mono<Long> result = reactiveRedisClient.publish(channel, message);

            // Then
            StepVerifier.create(result)
                    .expectNext(2L)
                    .verifyComplete();
            verify(reactiveRedisTemplate).convertAndSend(channel, message);
        }

        @Test
        @DisplayName("07-43: 执行Lua脚本字符串")
        @SuppressWarnings("unchecked")
        void testExecute_WithString() {
            // Given
            String scriptStr = "return redis.call('get', KEYS[1])";
            List<String> keys = Arrays.asList("key1");
            Class<String> clazz = String.class;
            RedisSerializer<String> serializer = mock(RedisSerializer.class);
            List<String> vals = Collections.emptyList();

            when(reactiveRedisTemplate.execute(any(RedisScript.class), eq(keys), eq(vals), any(), any()))
                    .thenReturn(Flux.just("value1"));

            // When
            Flux<String> result = reactiveRedisClient.execute(scriptStr, keys, clazz, serializer, vals);

            // Then
            StepVerifier.create(result)
                    .expectNext("value1")
                    .verifyComplete();
        }

        @Test
        @DisplayName("07-44: 执行RedisScript")
        @SuppressWarnings("unchecked")
        void testExecute_WithRedisScript() {
            // Given
            RedisScript<String> script = mock(RedisScript.class);
            List<String> keys = Arrays.asList("key1");
            List<Object> args = Collections.emptyList();

            when(reactiveRedisTemplate.execute(script, keys, args)).thenReturn(Flux.just("result"));

            // When
            Flux<String> result = reactiveRedisClient.execute(script, keys, args);

            // Then
            StepVerifier.create(result)
                    .expectNext("result")
                    .verifyComplete();
            verify(reactiveRedisTemplate).execute(script, keys, args);
        }

        @Test
        @DisplayName("07-45: 执行Lua返回单个结果")
        @SuppressWarnings("unchecked")
        void testExecuteForMono() {
            // Given
            RedisScript<String> script = mock(RedisScript.class);
            List<String> keys = Arrays.asList("key1");
            List<Object> args = Collections.emptyList();

            when(reactiveRedisTemplate.execute(script, keys, args)).thenReturn(Flux.just("result"));

            // When
            Mono<String> result = reactiveRedisClient.executeForMono(script, keys, args);

            // Then
            StepVerifier.create(result)
                    .expectNext("result")
                    .verifyComplete();
            verify(reactiveRedisTemplate).execute(script, keys, args);
        }

        @Test
        @DisplayName("07-46: 管道执行")
        @SuppressWarnings("unchecked")
        void testExecutePipelined() {
            // Given
            ReactiveRedisCallback<String> callback = connection -> Mono.just("result");
            when(reactiveRedisTemplate.execute(any(ReactiveRedisCallback.class))).thenReturn(Flux.just("result"));

            // When
            Flux<String> result = reactiveRedisClient.executePipelined(callback);

            // Then
            StepVerifier.create(result)
                    .expectNext("result")
                    .verifyComplete();
            verify(reactiveRedisTemplate).execute(any(ReactiveRedisCallback.class));
        }
    }
}
