package com.blink.framework.common.mq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * MQ相关类单元测试
 * <p>
 * 测试覆盖：
 * 1. BlinkMessage 接口
 * 2. BlinkProducer 接口
 * 3. BlinkConsumer 接口
 * 4. 具体实现示例
 *
 * @author binblink
 */
@DisplayName("MQ消息队列相关测试")
class MQTests {

    // ==================== BlinkMessage 测试 ====================

    @Nested
    @DisplayName("BlinkMessage 接口测试")
    class BlinkMessageTests {

        @Test
        @DisplayName("应该能够创建BlinkMessage的实现")
        void shouldCreateBlinkMessageImplementation() {
            // given
            TestMessage message = new TestMessage("test-id", "test content");

            // when & then
            assertThat(message).isInstanceOf(BlinkMessage.class);
            assertThat(message.getId()).isEqualTo("test-id");
            assertThat(message.getContent()).isEqualTo("test content");
        }

        @Test
        @DisplayName("BlinkMessage应该可以作为方法参数传递")
        void shouldPassBlinkMessageAsParameter() {
            // given
            TestMessage message = new TestMessage("id-001", "payload");

            // when
            boolean isBlinkMessage = isMessageValid(message);

            // then
            assertThat(isBlinkMessage).isTrue();
        }

        private boolean isMessageValid(BlinkMessage message) {
            return message instanceof TestMessage;
        }
    }

    // ==================== BlinkProducer 测试 ====================

    @Nested
    @DisplayName("BlinkProducer 接口测试")
    class BlinkProducerTests {

        @Test
        @DisplayName("应该能够调用sendMessage方法")
        void shouldCallSendMessage() {
            // given
            TestProducer producer = new TestProducer();

            // when
            String result = producer.sendMessage(new TestMessage("id-001", "test"));

            // then
            assertThat(result).isEqualTo("sent: id-001");
        }

        @Test
        @DisplayName("发送消息应该返回正确的结果")
        void shouldReturnCorrectResult() {
            // given
            TestProducer producer = new TestProducer();
            TestMessage message = new TestMessage("msg-123", "hello world");

            // when
            String result = producer.sendMessage(message);

            // then
            assertThat(result).contains("msg-123");
        }
    }

    // ==================== BlinkConsumer 测试 ====================

    @Nested
    @DisplayName("BlinkConsumer 接口测试")
    class BlinkConsumerTests {

        @Test
        @DisplayName("应该能够调用receiveMessage方法")
        void shouldCallReceiveMessage() {
            // given
            TestConsumer consumer = new TestConsumer();
            TestMessage message = new TestMessage("id-001", "test content");

            // when
            String result = consumer.receiveMessage(message);

            // then
            assertThat(result).isEqualTo("processed: test content");
        }

        @Test
        @DisplayName("消费消息应该返回处理结果")
        void shouldReturnProcessingResult() {
            // given
            TestConsumer consumer = new TestConsumer();
            TestMessage message = new TestMessage("msg-456", "important data");

            // when
            String result = consumer.receiveMessage(message);

            // then
            assertThat(result).contains("processed");
            assertThat(result).contains("important data");
        }
    }

    // ==================== 完整流程测试 ====================

    @Nested
    @DisplayName("完整消息流程测试")
    class MessageFlowTests {

        @Test
        @DisplayName("应该完成发送-接收流程")
        void shouldCompleteSendReceiveFlow() {
            // given
            TestProducer producer = new TestProducer();
            TestConsumer consumer = new TestConsumer();
            TestMessage message = new TestMessage("flow-001", "flow test message");

            // when
            String sendResult = producer.sendMessage(message);
            String receiveResult = consumer.receiveMessage(message);

            // then
            assertThat(sendResult).contains("flow-001");
            assertThat(receiveResult).contains("flow test message");
        }
    }

    // ==================== 测试辅助类 ====================

    /**
     * 测试用消息实现
     */
    static class TestMessage implements BlinkMessage {
        private final String id;
        private final String content;

        public TestMessage(String id, String content) {
            this.id = id;
            this.content = content;
        }

        public String getId() {
            return id;
        }

        public String getContent() {
            return content;
        }
    }

    /**
     * 测试用生产者实现
     */
    static class TestProducer implements BlinkProducer<TestMessage, String> {
        @Override
        public String sendMessage(TestMessage message) {
            return "sent: " + message.getId();
        }
    }

    /**
     * 测试用消费者实现
     */
    static class TestConsumer implements BlinkConsumer<TestMessage, String> {
        @Override
        public String receiveMessage(TestMessage message) {
            return "processed: " + message.getContent();
        }
    }
}
