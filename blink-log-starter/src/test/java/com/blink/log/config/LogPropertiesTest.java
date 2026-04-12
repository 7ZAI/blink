package com.blink.log.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogProperties 配置类测试
 *
 * @author binblink
 */
@DisplayName("LogProperties 配置类测试")
class LogPropertiesTest {

    @Test
    @DisplayName("默认值验证")
    void defaultValues_shouldBeCorrect() {
        // when
        LogProperties properties = new LogProperties();

        // then - 验证嵌套对象初始化
        assertThat(properties.getRecord()).isNotNull();
        assertThat(properties.getConsole()).isNotNull();

        // 验证 LogRecord 默认值
        LogProperties.LogRecord record = properties.getRecord();
        assertThat(record.isEnabled()).isTrue();
        assertThat(record.isSaveRequest()).isTrue();
        assertThat(record.isSaveResponse()).isTrue();
        assertThat(record.getMaxRequestLength()).isEqualTo(4000);
        assertThat(record.getMaxResponseLength()).isEqualTo(4000);
        assertThat(record.getMaxErrorMsgLength()).isEqualTo(500);
        assertThat(record.getMaxUserAgentLength()).isEqualTo(500);

        // 验证 LogConsole 默认值
        LogProperties.LogConsole console = properties.getConsole();
        assertThat(console.isEnableControllerLog()).isTrue();
        assertThat(console.getUpperLimit()).isEqualTo(1000);
        assertThat(console.isAutoSkip()).isFalse();
        assertThat(console.isEnableSensitive()).isFalse();
    }

    @Test
    @DisplayName("setter/getter 测试")
    void setterGetter_shouldWorkCorrectly() {
        // given
        LogProperties properties = new LogProperties();

        // when - LogRecord
        LogProperties.LogRecord record = properties.getRecord();
        record.setEnabled(false);
        record.setSaveRequest(false);
        record.setSaveResponse(false);
        record.setMaxRequestLength(2000);
        record.setMaxResponseLength(2000);
        record.setMaxErrorMsgLength(300);
        record.setMaxUserAgentLength(300);

        // then
        assertThat(record.isEnabled()).isFalse();
        assertThat(record.isSaveRequest()).isFalse();
        assertThat(record.isSaveResponse()).isFalse();
        assertThat(record.getMaxRequestLength()).isEqualTo(2000);
        assertThat(record.getMaxResponseLength()).isEqualTo(2000);
        assertThat(record.getMaxErrorMsgLength()).isEqualTo(300);
        assertThat(record.getMaxUserAgentLength()).isEqualTo(300);

        // when - LogConsole
        LogProperties.LogConsole console = properties.getConsole();
        console.setEnableControllerLog(false);
        console.setUpperLimit(500);
        console.setAutoSkip(true);
        console.setEnableSensitive(true);

        // then
        assertThat(console.isEnableControllerLog()).isFalse();
        assertThat(console.getUpperLimit()).isEqualTo(500);
        assertThat(console.isAutoSkip()).isTrue();
        assertThat(console.isEnableSensitive()).isTrue();
    }

    @Test
    @DisplayName("嵌套属性独立设置")
    void nestedProperties_shouldBeIndependent() {
        // given
        LogProperties properties1 = new LogProperties();
        LogProperties properties2 = new LogProperties();

        // when
        properties1.getRecord().setEnabled(false);
        properties2.getRecord().setEnabled(true);

        // then - 两个实例的配置独立
        assertThat(properties1.getRecord().isEnabled()).isFalse();
        assertThat(properties2.getRecord().isEnabled()).isTrue();
    }
}
