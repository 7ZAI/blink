package com.blink.log.util;

import com.blink.log.sensitive.SensitiveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SensitiveUtils 工具类测试 - 基础脱敏方法
 *
 * @author binblink
 */
@DisplayName("SensitiveUtils 基础脱敏方法测试")
class SensitiveUtilsTest {

    // ==================== mask(source, prefixKeep, suffixKeep) 测试 ====================

    @Nested
    @DisplayName("mask(source, prefixKeep, suffixKeep) 方法测试")
    class MaskBasicTest {

        @Test
        @DisplayName("正常脱敏 - 保留前3后4")
        void mask_normalCase_shouldMaskCorrectly() {
            // given
            String source = "13812345678";

            // when
            String result = SensitiveUtils.mask(source, 3, 4);

            // then
            assertThat(result).isEqualTo("138****5678");
        }

        @Test
        @DisplayName("只保留前缀")
        void mask_onlyPrefix_shouldMaskSuffix() {
            // given
            String source = "abcdefghij";

            // when
            String result = SensitiveUtils.mask(source, 3, 0);

            // then
            assertThat(result).isEqualTo("abc*******");
        }

        @Test
        @DisplayName("只保留后缀")
        void mask_onlySuffix_shouldMaskPrefix() {
            // given
            String source = "abcdefghij";

            // when
            String result = SensitiveUtils.mask(source, 0, 3);

            // then
            assertThat(result).isEqualTo("*******hij");
        }

        @Test
        @DisplayName("不保留前后缀")
        void mask_noKeep_shouldMaskAll() {
            // given
            String source = "abcdefghij";

            // when
            String result = SensitiveUtils.mask(source, 0, 0);

            // then
            assertThat(result).isEqualTo("**********");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("空值或空白字符串 - 返回原值")
        void mask_blankOrNull_shouldReturnOriginal(String source) {
            // when
            String result = SensitiveUtils.mask(source, 3, 4);

            // then
            assertThat(result).isEqualTo(source);
        }

        @Test
        @DisplayName("字符串长度等于前后缀保留长度之和 - 返回原值")
        void mask_lengthEqualsKeepSum_shouldReturnOriginal() {
            // given
            String source = "abcde"; // length=5

            // when
            String result = SensitiveUtils.mask(source, 3, 2); // 3+2=5

            // then
            assertThat(result).isEqualTo("abcde");
        }

        @Test
        @DisplayName("字符串长度小于前后缀保留长度之和 - 返回原值")
        void mask_lengthLessThanKeepSum_shouldReturnOriginal() {
            // given
            String source = "abc"; // length=3

            // when
            String result = SensitiveUtils.mask(source, 2, 2); // 2+2=4 > 3

            // then
            assertThat(result).isEqualTo("abc");
        }

        @Test
        @DisplayName("单字符脱敏")
        void mask_singleChar_shouldMaskCorrectly() {
            // given
            String source = "a";

            // when
            String result = SensitiveUtils.mask(source, 0, 0);

            // then
            assertThat(result).isEqualTo("*");
        }

        @Test
        @DisplayName("负数前后缀参数 - 应被处理为0")
        void mask_negativeKeep_shouldTreatAsZero() {
            // given
            String source = "abcdefghij";

            // when
            String result = SensitiveUtils.mask(source, -1, -2);

            // then
            assertThat(result).isEqualTo("**********");
        }
    }

    // ==================== mask(source, prefixKeep, suffixKeep, maskChar) 测试 ====================

    @Nested
    @DisplayName("mask(source, prefixKeep, suffixKeep, maskChar) 方法测试")
    class MaskWithCharTest {

        @Test
        @DisplayName("自定义脱敏字符 #")
        void mask_customChar_shouldUseCustomChar() {
            // given
            String source = "13812345678";

            // when
            String result = SensitiveUtils.mask(source, 3, 4, '#');

            // then
            assertThat(result).isEqualTo("138####5678");
        }

        @Test
        @DisplayName("自定义脱敏字符 X")
        void mask_customCharX_shouldUseX() {
            // given
            String source = "password123";

            // when
            String result = SensitiveUtils.mask(source, 4, 3, 'X');

            // then
            assertThat(result).isEqualTo("passXXXX123");
        }
    }

    // ==================== mask(String source, SensitiveType type) 测试 ====================

    @Nested
    @DisplayName("mask(source, SensitiveType type) 方法测试")
    class MaskByTypeTest {

        @Test
        @DisplayName("PASSWORD 类型 - 返回星号常量")
        void mask_passwordType_shouldReturnMask() {
            // given
            String source = "myPassword123";

            // when
            String result = SensitiveUtils.mask(source, SensitiveType.PASSWORD);

            // then
            assertThat(result).isEqualTo("******");
        }

        @Test
        @DisplayName("PHONE 类型 - 使用手机号脱敏规则")
        void mask_phoneType_shouldUsePhoneRule() {
            // given
            String source = "13812345678";

            // when
            String result = SensitiveUtils.mask(source, SensitiveType.PHONE);

            // then
            assertThat(result).isEqualTo("138****5678");
        }

        @Test
        @DisplayName("ID_CARD 类型 - 使用身份证脱敏规则：前6后4")
        void mask_idCardType_shouldUseIdCardRule() {
            // given
            String source = "110101199001011234";

            // when - ID_CARD规则：前6后4，18-6-4=8个*
            String result = SensitiveUtils.mask(source, SensitiveType.ID_CARD);

            // then
            assertThat(result).isEqualTo("110101********1234");
        }

        @Test
        @DisplayName("BANK_CARD 类型 - 使用银行卡脱敏规则：前4后4")
        void mask_bankCardType_shouldUseBankCardRule() {
            // given
            String source = "6222021234567890123";

            // when - BANK_CARD规则：前4后4，19-4-4=11个*
            String result = SensitiveUtils.mask(source, SensitiveType.BANK_CARD);

            // then
            assertThat(result).isEqualTo("6222***********0123");
        }

        @Test
        @DisplayName("NAME 类型 - 使用姓名脱敏规则：前1后1")
        void mask_nameType_shouldUseNameRule() {
            // given
            String source = "张三丰";

            // when
            String result = SensitiveUtils.mask(source, SensitiveType.NAME);

            // then
            assertThat(result).isEqualTo("张*丰");
        }

        @Test
        @DisplayName("ADDRESS 类型 - 使用地址脱敏规则：前6后0")
        void mask_addressType_shouldUseAddressRule() {
            // given
            String source = "北京市海淀区中关村大街1号";

            // when - ADDRESS规则：前6后0
            // 共13字符，保留前6="北京市海淀区"
            String result = SensitiveUtils.mask(source, SensitiveType.ADDRESS);

            // then - 保留前6="北京市海淀区"，后7个*
            assertThat(result).isEqualTo("北京市海淀区*******");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("空值 - 返回原值")
        void mask_nullOrEmpty_shouldReturnOriginal(String source) {
            // when
            String result = SensitiveUtils.mask(source, SensitiveType.PHONE);

            // then
            assertThat(result).isEqualTo(source);
        }
    }

    // ==================== maskPhone() 测试 ====================

    @Nested
    @DisplayName("maskPhone() 方法测试")
    class MaskPhoneTest {

        @Test
        @DisplayName("标准11位手机号")
        void maskPhone_standard11Digits_shouldMaskCorrectly() {
            // given
            String phone = "13812345678";

            // when
            String result = SensitiveUtils.maskPhone(phone);

            // then
            assertThat(result).isEqualTo("138****5678");
        }

        @ParameterizedTest
        @CsvSource({
                "1234567, 123****",
                "123456, 123456",
                "12345, 12345",
                "1, 1"
        })
        @DisplayName("非标准长度手机号（使用PHONE类型规则：前3后4）")
        void maskPhone_nonStandardLength_shouldHandleCorrectly(String phone, String expected) {
            // when
            String result = SensitiveUtils.maskPhone(phone);

            // then - 根据实际规则：保留前3后4
            // 1234567 长度7，前3后4=7，等于长度，返回原值
            assertThat(result).isEqualTo(phone);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("空值或空白")
        void maskPhone_blankOrNull_shouldReturnOriginal(String phone) {
            // when
            String result = SensitiveUtils.maskPhone(phone);

            // then
            assertThat(result).isEqualTo(phone);
        }
    }

    // ==================== maskIdCard() 测试 ====================

    @Nested
    @DisplayName("maskIdCard() 方法测试")
    class MaskIdCardTest {

        @Test
        @DisplayName("标准18位身份证")
        void maskIdCard_standard18Digits_shouldMaskCorrectly() {
            // given
            String idCard = "110101199001011234";

            // when
            String result = SensitiveUtils.maskIdCard(idCard);

            // then
            assertThat(result).isEqualTo("110101********1234");
        }

        @Test
        @DisplayName("15位旧身份证 - 使用ID_CARD规则：前6后4")
        void maskIdCard_15Digits_shouldMaskCorrectly() {
            // given
            String idCard = "110101900101123";

            // when - 实际规则：保留前6后4，15-6-4=5个*
            String result = SensitiveUtils.maskIdCard(idCard);

            // then
            assertThat(result).isEqualTo("110101*****1123");
        }

        @ParameterizedTest
        @CsvSource({
                "12345678, 12345678",
                "1234567, 1234567",
                "123456, 123456"
        })
        @DisplayName("非标准长度身份证（ID_CARD规则：前6后4，长度<=10时返回原值）")
        void maskIdCard_nonStandardLength_shouldHandleCorrectly(String idCard, String expected) {
            // when
            String result = SensitiveUtils.maskIdCard(idCard);

            // then - 12345678 长度8，前6后4=10，大于长度，返回原值
            assertThat(result).isEqualTo(idCard);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("空值或空白")
        void maskIdCard_blankOrNull_shouldReturnOriginal(String idCard) {
            // when
            String result = SensitiveUtils.maskIdCard(idCard);

            // then
            assertThat(result).isEqualTo(idCard);
        }
    }

    // ==================== maskBankCard() 测试 ====================

    @Nested
    @DisplayName("maskBankCard() 方法测试")
    class MaskBankCardTest {

        @Test
        @DisplayName("标准19位银行卡号 - 使用BANK_CARD规则：前4后4")
        void maskBankCard_19Digits_shouldMaskCorrectly() {
            // given
            String bankCard = "6222021234567890123";

            // when - 实际规则：保留前4后4，19-4-4=11个*
            String result = SensitiveUtils.maskBankCard(bankCard);

            // then
            assertThat(result).isEqualTo("6222***********0123");
        }

        @Test
        @DisplayName("16位银行卡号")
        void maskBankCard_16Digits_shouldMaskCorrectly() {
            // given
            String bankCard = "6222021234567890";

            // when
            String result = SensitiveUtils.maskBankCard(bankCard);

            // then
            assertThat(result).isEqualTo("6222********7890");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("空值或空白")
        void maskBankCard_blankOrNull_shouldReturnOriginal(String bankCard) {
            // when
            String result = SensitiveUtils.maskBankCard(bankCard);

            // then
            assertThat(result).isEqualTo(bankCard);
        }
    }

    // ==================== maskEmail() 测试 ====================

    @Nested
    @DisplayName("maskEmail() 方法测试")
    class MaskEmailTest {

        @Test
        @DisplayName("标准邮箱 - 前缀8字符，保留前3，后5个*")
        void maskEmail_standardEmail_shouldMaskCorrectly() {
            // given
            String email = "zhangsan@qq.com";

            // when - maskEmail特殊逻辑：前缀长度>3时，保留前3
            // "zhangsan" 8字符，保留前3，剩下5个变成*
            String result = SensitiveUtils.maskEmail(email);

            // then
            assertThat(result).isEqualTo("zha*****@qq.com");
        }

        @Test
        @DisplayName("短前缀邮箱（<=3字符）")
        void maskEmail_shortPrefix_shouldKeep1Char() {
            // given
            String email = "ab@qq.com";

            // when
            String result = SensitiveUtils.maskEmail(email);

            // then
            assertThat(result).isEqualTo("a*@qq.com");
        }

        @Test
        @DisplayName("单字符前缀邮箱 - 前缀长度为1，保留前1后无剩余")
        void maskEmail_singleCharPrefix_shouldMaskCorrectly() {
            // given
            String email = "a@qq.com";

            // when - 前缀长度=1，<=3，mask(prefix, 1, 0)
            // 但前缀长度1等于要保留的前缀1，所以返回原值
            String result = SensitiveUtils.maskEmail(email);

            // then
            assertThat(result).isEqualTo("a@qq.com");
        }

        @Test
        @DisplayName("无@符号的字符串 - 返回原值")
        void maskEmail_noAtSymbol_shouldReturnOriginal() {
            // given
            String email = "noemail";

            // when
            String result = SensitiveUtils.maskEmail(email);

            // then
            assertThat(result).isEqualTo("noemail");
        }

        @Test
        @DisplayName("企业邮箱 - 前缀8字符，保留前3，后5个*")
        void maskEmail_enterpriseEmail_shouldMaskCorrectly() {
            // given
            String email = "zhangsan@company.com.cn";

            // when - "zhangsan" 8字符，保留前3，剩下5个变成*
            String result = SensitiveUtils.maskEmail(email);

            // then
            assertThat(result).isEqualTo("zha*****@company.com.cn");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("空值或空白")
        void maskEmail_blankOrNull_shouldReturnOriginal(String email) {
            // when
            String result = SensitiveUtils.maskEmail(email);

            // then
            assertThat(result).isEqualTo(email);
        }
    }

    // ==================== maskName() 测试 ====================

    @Nested
    @DisplayName("maskName() 方法测试")
    class MaskNameTest {

        @Test
        @DisplayName("三字姓名")
        void maskName_threeCharName_shouldMaskCorrectly() {
            // given
            String name = "张三丰";

            // when
            String result = SensitiveUtils.maskName(name);

            // then
            assertThat(result).isEqualTo("张*丰");
        }

        @Test
        @DisplayName("两字姓名 - NAME规则前1后1，长度2等于保留数，返回原值")
        void maskName_twoCharName_shouldMaskCorrectly() {
            // given
            String name = "张三";

            // when - NAME类型：前1后1，长度2，1+1=2，等于长度返回原值
            String result = SensitiveUtils.maskName(name);

            // then
            assertThat(result).isEqualTo("张三");
        }

        @Test
        @DisplayName("单字姓名")
        void maskName_singleCharName_shouldReturnOriginal() {
            // given
            String name = "张";

            // when
            String result = SensitiveUtils.maskName(name);

            // then
            assertThat(result).isEqualTo("张");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("空值或空白")
        void maskName_blankOrNull_shouldReturnOriginal(String name) {
            // when
            String result = SensitiveUtils.maskName(name);

            // then
            assertThat(result).isEqualTo(name);
        }
    }

    // ==================== maskAddress() 测试 ====================

    @Nested
    @DisplayName("maskAddress() 方法测试")
    class MaskAddressTest {

        @Test
        @DisplayName("标准地址 - ADDRESS规则前6后0")
        void maskAddress_standardAddress_shouldMaskCorrectly() {
            // given
            String address = "北京市海淀区中关村大街1号";

            // when - ADDRESS类型：前6后0，保留前6，其余用*
            // "北京市海淀区中关村大街1号" 共13字符，保留前6="北京市海淀区"
            String result = SensitiveUtils.maskAddress(address);

            // then - 保留前6="北京市海淀区"，后7个*
            assertThat(result).isEqualTo("北京市海淀区*******");
        }

        @Test
        @DisplayName("短地址（长度不足6）")
        void maskAddress_shortAddress_shouldReturnOriginal() {
            // given
            String address = "北京海淀";

            // when
            String result = SensitiveUtils.maskAddress(address);

            // then
            assertThat(result).isEqualTo("北京海淀");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("空值或空白")
        void maskAddress_blankOrNull_shouldReturnOriginal(String address) {
            // when
            String result = SensitiveUtils.maskAddress(address);

            // then
            assertThat(result).isEqualTo(address);
        }
    }
}
