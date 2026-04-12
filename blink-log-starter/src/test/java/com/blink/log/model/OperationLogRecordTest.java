package com.blink.log.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OperationLogRecord 模型类测试
 *
 * @author binblink
 */
@DisplayName("OperationLogRecord 模型类测试")
class OperationLogRecordTest {

    @Test
    @DisplayName("所有字段 getter/setter")
    void allFields_shouldWorkCorrectly() {
        // given
        OperationLogRecord record = new OperationLogRecord();
        LocalDateTime now = LocalDateTime.now();

        // when
        record.setLogType("OPERATION");
        record.setDescription("测试操作");
        record.setUserId(1);
        record.setLoginName("admin");
        record.setRequestUrl("/api/test");
        record.setRequestMethod("POST");
        record.setRequestParams("{\"key\":\"value\"}");
        record.setResponseData("{\"result\":\"success\"}");
        record.setExecuteStatus(0);
        record.setErrorMsg(null);
        record.setExecuteTimeMs(100);
        record.setIpAddress("192.168.1.1");
        record.setUserAgent("Mozilla/5.0");
        record.setOperationTime(now);

        // then
        assertThat(record.getLogType()).isEqualTo("OPERATION");
        assertThat(record.getDescription()).isEqualTo("测试操作");
        assertThat(record.getUserId()).isEqualTo(1);
        assertThat(record.getLoginName()).isEqualTo("admin");
        assertThat(record.getRequestUrl()).isEqualTo("/api/test");
        assertThat(record.getRequestMethod()).isEqualTo("POST");
        assertThat(record.getRequestParams()).isEqualTo("{\"key\":\"value\"}");
        assertThat(record.getResponseData()).isEqualTo("{\"result\":\"success\"}");
        assertThat(record.getExecuteStatus()).isEqualTo(0);
        assertThat(record.getErrorMsg()).isNull();
        assertThat(record.getExecuteTimeMs()).isEqualTo(100);
        assertThat(record.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(record.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(record.getOperationTime()).isEqualTo(now);
    }

    @Test
    @DisplayName("addExtraField - 添加扩展字段")
    void addExtraField_shouldAddField() {
        // given
        OperationLogRecord record = new OperationLogRecord();

        // when
        record.addExtraField("traceId", "abc123");
        record.addExtraField("moduleId", "MOD001");

        // then
        Map<String, Object> extraFields = record.getExtraFields();
        assertThat(extraFields).isNotNull();
        assertThat(extraFields).hasSize(2);
        assertThat(extraFields.get("traceId")).isEqualTo("abc123");
        assertThat(extraFields.get("moduleId")).isEqualTo("MOD001");
    }

    @Test
    @DisplayName("getExtraField - 获取扩展字段")
    void getExtraField_shouldReturnCorrectValue() {
        // given
        OperationLogRecord record = new OperationLogRecord();
        record.addExtraField("key1", "value1");

        // when
        Object value = record.getExtraField("key1");
        Object notExist = record.getExtraField("notExist");

        // then
        assertThat(value).isEqualTo("value1");
        assertThat(notExist).isNull();
    }

    @Test
    @DisplayName("getExtraField - 无扩展字段时返回null")
    void getExtraField_noExtraFields_shouldReturnNull() {
        // given
        OperationLogRecord record = new OperationLogRecord();

        // when
        Object value = record.getExtraField("anyKey");

        // then
        assertThat(value).isNull();
    }

    @Test
    @DisplayName("扩展字段为不同类型")
    void extraField_differentTypes_shouldWorkCorrectly() {
        // given
        OperationLogRecord record = new OperationLogRecord();

        // when
        record.addExtraField("stringField", "string");
        record.addExtraField("intField", 123);
        record.addExtraField("boolField", true);
        record.addExtraField("objectField", new Object());

        // then
        assertThat(record.getExtraField("stringField")).isEqualTo("string");
        assertThat(record.getExtraField("intField")).isEqualTo(123);
        assertThat(record.getExtraField("boolField")).isEqualTo(true);
        assertThat(record.getExtraField("objectField")).isNotNull();
    }

    @Test
    @DisplayName("执行状态枚举值")
    void executeStatus_shouldBeCorrect() {
        // given
        OperationLogRecord successRecord = new OperationLogRecord();
        OperationLogRecord failRecord = new OperationLogRecord();

        // when
        successRecord.setExecuteStatus(0);
        failRecord.setExecuteStatus(1);

        // then
        assertThat(successRecord.getExecuteStatus()).isEqualTo(0); // 成功
        assertThat(failRecord.getExecuteStatus()).isEqualTo(1);    // 失败
    }
}
