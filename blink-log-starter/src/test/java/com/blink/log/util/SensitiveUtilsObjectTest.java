package com.blink.log.util;

import com.blink.log.annotation.SensitiveField;
import com.blink.log.sensitive.SensitiveType;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SensitiveUtils 工具类测试 - 对象脱敏方法
 *
 * <p>修复后：toSensitiveString(Object obj) 方法会先转 Map 再进行敏感字段脱敏
 *
 * @author binblink
 */
@DisplayName("SensitiveUtils 对象脱敏方法测试")
class SensitiveUtilsObjectTest {

    // ==================== toSensitiveString() 测试 ====================

    @Nested
    @DisplayName("toSensitiveString() 方法测试")
    class ToSensitiveStringTest {

        @Test
        @DisplayName("null对象 - 返回null字符串")
        void toSensitiveString_null_shouldReturnNullString() {
            // when
            String result = SensitiveUtils.toSensitiveString(null);

            // then
            assertThat(result).isEqualTo("null");
        }

        @Test
        @DisplayName("简单字符串对象 - 直接序列化（无法转Map）")
        void toSensitiveString_simpleString_shouldReturnJson() {
            // given
            String str = "hello";

            // when - String无法转为Map，直接序列化
            String result = SensitiveUtils.toSensitiveString(str);

            // then
            assertThat(result).isEqualTo("\"hello\"");
        }

        @Test
        @DisplayName("数字对象 - 直接序列化（无法转Map）")
        void toSensitiveString_number_shouldReturnJson() {
            // given
            Integer num = 123;

            // when - Integer无法转为Map，直接序列化
            String result = SensitiveUtils.toSensitiveString(num);

            // then
            assertThat(result).isEqualTo("123");
        }

        @Test
        @DisplayName("简单POJO对象 - 自动脱敏敏感字段")
        void toSensitiveString_simplePojo_shouldReturnJson() {
            // given
            SimpleUser user = new SimpleUser();
            user.setName("张三");
            user.setAge(25);

            // when
            String result = SensitiveUtils.toSensitiveString(user);

            // then - 无敏感字段，正常输出
            assertThat(result).contains("\"name\":\"张三\"");
            assertThat(result).contains("\"age\":25");
        }

        @Test
        @DisplayName("包含敏感字段的POJO - 自动脱敏")
        void toSensitiveString_withSensitiveField_shouldAutoMask() {
            // given
            UserWithPassword user = new UserWithPassword();
            user.setUsername("admin");
            user.setPassword("secret123");
            user.setPhone("13812345678");

            // when - toSensitiveString 会自动脱敏敏感字段
            String result = SensitiveUtils.toSensitiveString(user);

            // then - 敏感字段被脱敏
            assertThat(result).contains("\"password\":\"******\"");
            assertThat(result).contains("\"username\":\"admin\"");
            assertThat(result).contains("\"phone\":\"138****5678\""); // 手机号脱敏
        }

        @Test
        @DisplayName("List集合 - 直接序列化（无法转Map）")
        void toSensitiveString_list_shouldReturnJsonArray() {
            // given
            List<String> list = Arrays.asList("a", "b", "c");

            // when - List无法转为Map，直接序列化
            String result = SensitiveUtils.toSensitiveString(list);

            // then
            assertThat(result).isEqualTo("[\"a\",\"b\",\"c\"]");
        }

        @Test
        @DisplayName("Map对象 - 自动脱敏敏感字段")
        void toSensitiveString_map_shouldMaskSensitiveFields() {
            // given
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "张三");
            map.put("age", 25);
            map.put("password", "secret123");
            map.put("phone", "13812345678");

            // when
            String result = SensitiveUtils.toSensitiveString(map);

            // then - 敏感字段被脱敏
            assertThat(result).contains("\"name\":\"张三\"");
            assertThat(result).contains("\"age\":25");
            assertThat(result).contains("\"password\":\"******\"");
            assertThat(result).contains("\"phone\":\"138****5678\"");
        }

        @Test
        @DisplayName("Map中不包含敏感字段 - 正常输出")
        void toSensitiveString_mapNoSensitiveFields_shouldReturnOriginal() {
            // given
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "张三");
            map.put("age", 25);

            // when
            String result = SensitiveUtils.toSensitiveString(map);

            // then
            assertThat(result).contains("\"name\":\"张三\"");
            assertThat(result).contains("\"age\":25");
        }

        @Test
        @DisplayName("嵌套Map对象 - 递归脱敏")
        void toSensitiveString_nestedMap_shouldHandleRecursively() {
            // given
            Map<String, Object> outer = new LinkedHashMap<>();
            Map<String, Object> inner = new LinkedHashMap<>();
            inner.put("password", "innerSecret");
            inner.put("name", "内部用户");
            outer.put("user", inner);

            // when
            String result = SensitiveUtils.toSensitiveString(outer);

            // then
            assertThat(result).contains("\"password\":\"******\"");
            assertThat(result).contains("\"name\":\"内部用户\"");
        }

        @Test
        @DisplayName("嵌套对象")
        void toSensitiveString_nestedObject_shouldHandleRecursively() {
            // given
            OrderDTO order = new OrderDTO();
            order.setOrderId("ORD001");
            SimpleUser user = new SimpleUser();
            user.setName("张三");
            user.setAge(30);
            order.setUser(user);

            // when
            String result = SensitiveUtils.toSensitiveString(order);

            // then
            assertThat(result).contains("\"orderId\":\"ORD001\"");
            assertThat(result).contains("\"name\":\"张三\"");
        }

        @Test
        @DisplayName("Boolean值 - 直接序列化（无法转Map）")
        void toSensitiveString_boolean_shouldReturnJson() {
            // when
            String result = SensitiveUtils.toSensitiveString(true);

            // then
            assertThat(result).isEqualTo("true");
        }

        @Test
        @DisplayName("空List - 直接序列化")
        void toSensitiveString_emptyList_shouldReturnEmptyArray() {
            // when
            String result = SensitiveUtils.toSensitiveString(Collections.emptyList());

            // then
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("空Map - 返回空对象")
        void toSensitiveString_emptyMap_shouldReturnEmptyObject() {
            // when
            String result = SensitiveUtils.toSensitiveString(Collections.emptyMap());

            // then
            assertThat(result).isEqualTo("{}");
        }

        @Test
        @DisplayName("Map包含邮箱字段 - 邮箱脱敏")
        void toSensitiveString_mapWithEmail_shouldMaskEmail() {
            // given
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("email", "zhangsan@qq.com");

            // when
            String result = SensitiveUtils.toSensitiveString(map);

            // then
            assertThat(result).contains("\"email\":\"zha*****@qq.com\"");
        }

        @Test
        @DisplayName("Map包含身份证字段 - 身份证脱敏")
        void toSensitiveString_mapWithIdCard_shouldMaskIdCard() {
            // given
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("idCard", "110101199001011234");

            // when
            String result = SensitiveUtils.toSensitiveString(map);

            // then - ID_CARD规则：前6后4
            assertThat(result).contains("\"idCard\":\"110101********1234\"");
        }
    }

    // ==================== mask() 边界条件测试 ====================

    @Nested
    @DisplayName("mask() 边界条件测试")
    class MaskEdgeCaseTest {

        @Test
        @DisplayName("prefixKeep为0 - 脱敏全部后缀")
        void mask_zeroPrefix_shouldMaskAllSuffix() {
            // given
            String source = "abcdefghij";

            // when
            String result = SensitiveUtils.mask(source, 0, 3);

            // then
            assertThat(result).isEqualTo("*******hij");
        }

        @Test
        @DisplayName("suffixKeep为0 - 脱敏全部前缀")
        void mask_zeroSuffix_shouldMaskAllPrefix() {
            // given
            String source = "abcdefghij";

            // when
            String result = SensitiveUtils.mask(source, 3, 0);

            // then
            assertThat(result).isEqualTo("abc*******");
        }

        @Test
        @DisplayName("长字符串脱敏")
        void mask_longString_shouldHandleCorrectly() {
            // given
            String source = "a".repeat(1000);

            // when
            String result = SensitiveUtils.mask(source, 10, 10);

            // then
            assertThat(result).startsWith("a".repeat(10));
            assertThat(result).endsWith("a".repeat(10));
            assertThat(result).hasSize(1000);
        }

        @Test
        @DisplayName("Unicode字符脱敏")
        void mask_unicodeString_shouldHandleCorrectly() {
            // given - 4个中文+5个英文=9个字符
            String source = "你好世界Hello";

            // when - 前2后2，脱敏 9-2-2=5个
            String result = SensitiveUtils.mask(source, 2, 2);

            // then
            assertThat(result).isEqualTo("你好*****lo");
        }
    }

    // ==================== 特殊类型脱敏测试 ====================

    @Nested
    @DisplayName("特殊类型脱敏测试")
    class SpecialTypeMaskTest {

        @Test
        @DisplayName("银行卡号脱敏 - 不同长度")
        void maskBankCard_variousLength_shouldHandleCorrectly() {
            // 13位银行卡 - BANK_CARD规则前4后4，脱敏13-8=5个
            assertThat(SensitiveUtils.maskBankCard("6222021234567"))
                    .isEqualTo("6222*****4567");

            // 14位银行卡 - 脱敏14-8=6个
            assertThat(SensitiveUtils.maskBankCard("62220212345678"))
                    .isEqualTo("6222******5678");
        }

        @Test
        @DisplayName("手机号脱敏 - 带+86前缀")
        void maskPhone_withCountryCode_shouldOnlyMaskNumber() {
            // given - 带国际区号的手机号（非标准格式）
            String phone = "+8613812345678"; // 14个字符

            // when - 使用PHONE类型规则（前3后4），脱敏14-7=7个
            String result = SensitiveUtils.maskPhone(phone);

            // then
            assertThat(result).isEqualTo("+86*******5678");
        }

        @Test
        @DisplayName("邮箱脱敏 - 数字前缀")
        void maskEmail_numericPrefix_shouldMaskCorrectly() {
            // given
            String email = "12345@qq.com";

            // when - 前缀"12345"长度5>3，保留前3
            String result = SensitiveUtils.maskEmail(email);

            // then
            assertThat(result).isEqualTo("123**@qq.com");
        }

        @Test
        @DisplayName("身份证脱敏 - 带X结尾")
        void maskIdCard_endingWithX_shouldMaskCorrectly() {
            // given
            String idCard = "11010119900101123X";

            // when - ID_CARD规则：前6后4
            String result = SensitiveUtils.maskIdCard(idCard);

            // then
            assertThat(result).isEqualTo("110101********123X");
        }
    }

    // ==================== 测试用POJO ====================

    @Data
    static class SimpleUser {
        private String name;
        private Integer age;
    }

    @Data
    static class UserWithPassword {
        private String username;
        private String password;
        private String phone;
    }

    @Data
    static class OrderDTO {
        private String orderId;
        private SimpleUser user;
    }

    @Data
    static class AnnotatedUser {
        private String name;

        @SensitiveField(type = SensitiveType.PHONE)
        private String phone;

        @SensitiveField(type = SensitiveType.ID_CARD)
        private String idCard;

        @SensitiveField(type = SensitiveType.EMAIL)
        private String email;

        @SensitiveField(type = SensitiveType.PASSWORD)
        private String password;

        @SensitiveField(type = SensitiveType.CUSTOM, prefixKeep = 2, suffixKeep = 2)
        private String customField;
    }
}
