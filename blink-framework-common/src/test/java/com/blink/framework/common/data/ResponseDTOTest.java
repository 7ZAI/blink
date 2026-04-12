package com.blink.framework.common.data;

import com.blink.framework.common.constrant.ResponseMsgType;
import com.blink.framework.common.constrant.SysConstant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * ResponseDTO 单元测试
 * <p>
 * 测试覆盖：
 * 1. 成功响应工厂方法
 * 2. 失败响应工厂方法
 * 3. 泛型支持
 * 4. Getter/Setter
 *
 * @author binblink
 */
@DisplayName("ResponseDTO 通用响应数据传输对象测试")
class ResponseDTOTest {

    // ==================== 成功响应测试 ====================

    @Nested
    @DisplayName("成功响应测试")
    class SuccessResponseTests {

        @Test
        @DisplayName("newSuccessInstance应该创建成功响应（无数据）")
        void shouldCreateSuccessResponseWithoutData() {
            // when
            ResponseDTO<EmptyBody> response = ResponseDTO.newSuccessInstance();

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMsgCode()).isEqualTo(SysConstant.SUCCESS_CODE);
            assertThat(response.getMsgType()).isEqualTo(ResponseMsgType.SUCCESS.getType());
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("newSuccessInstance(T)应该创建带数据的成功响应")
        void shouldCreateSuccessResponseWithData() {
            // given
            String data = "测试数据";

            // when
            ResponseDTO<String> response = ResponseDTO.newSuccessInstance(data);

            // then
            assertThat(response.getMsgCode()).isEqualTo(SysConstant.SUCCESS_CODE);
            assertThat(response.getMsgType()).isEqualTo(ResponseMsgType.SUCCESS.getType());
            assertThat(response.getBody()).isEqualTo(data);
        }

        @Test
        @DisplayName("应该支持复杂对象作为响应体")
        void shouldSupportComplexObjectAsBody() {
            // given
            @lombok.Data
            @lombok.AllArgsConstructor
            class UserInfo {
                private Long id;
                private String name;
            }
            UserInfo user = new UserInfo(1L, "张三");

            // when
            ResponseDTO<UserInfo> response = ResponseDTO.newSuccessInstance(user);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(1L);
            assertThat(response.getBody().getName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("null数据应该能正确处理")
        void shouldHandleNullData() {
            // when
            ResponseDTO<String> response = ResponseDTO.newSuccessInstance(null);

            // then
            assertThat(response.getBody()).isNull();
            assertThat(response.getMsgCode()).isEqualTo(SysConstant.SUCCESS_CODE);
        }
    }

    // ==================== 失败响应测试 ====================

    @Nested
    @DisplayName("失败响应测试")
    class FailResponseTests {

        @Test
        @DisplayName("newFailInstance应该创建失败响应")
        void shouldCreateFailResponse() {
            // when
            ResponseDTO<EmptyBody> response = ResponseDTO.newFailInstance();

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMsgCode()).isEqualTo(SysConstant.FAIL_CODE);
            assertThat(response.getMsgType()).isEqualTo(ResponseMsgType.SYSTEM_ERR.getType());
            assertThat(response.getBody()).isNull();
        }
    }

    // ==================== Getter/Setter测试 ====================

    @Nested
    @DisplayName("Getter/Setter测试")
    class GetterSetterTests {

        @Test
        @DisplayName("应该正确设置和获取msgCode")
        void shouldSetAndGetMsgCode() {
            // given
            ResponseDTO<String> response = new ResponseDTO<>();

            // when
            response.setMsgCode("CUSTOM001");

            // then
            assertThat(response.getMsgCode()).isEqualTo("CUSTOM001");
        }

        @Test
        @DisplayName("应该正确设置和获取msgInfo")
        void shouldSetAndGetMsgInfo() {
            // given
            ResponseDTO<String> response = new ResponseDTO<>();

            // when
            response.setMsgInfo("操作成功");

            // then
            assertThat(response.getMsgInfo()).isEqualTo("操作成功");
        }

        @Test
        @DisplayName("应该正确设置和获取msgType")
        void shouldSetAndGetMsgType() {
            // given
            ResponseDTO<String> response = new ResponseDTO<>();

            // when
            response.setMsgType(ResponseMsgType.BUSINESS_ERR.getType());

            // then
            assertThat(response.getMsgType()).isEqualTo(ResponseMsgType.BUSINESS_ERR.getType());
        }

        @Test
        @DisplayName("应该正确设置和获取body")
        void shouldSetAndGetBody() {
            // given
            ResponseDTO<String> response = new ResponseDTO<>();

            // when
            response.setBody("响应数据");

            // then
            assertThat(response.getBody()).isEqualTo("响应数据");
        }
    }

    // ==================== toString测试 ====================

    @Nested
    @DisplayName("toString测试")
    class ToStringTests {

        @Test
        @DisplayName("toString应该包含所有属性")
        void shouldIncludeAllPropertiesInToString() {
            // given
            ResponseDTO<String> response = ResponseDTO.newSuccessInstance("测试数据");
            response.setMsgInfo("操作成功");

            // when
            String str = response.toString();

            // then
            assertThat(str).contains("msgCode='" + SysConstant.SUCCESS_CODE + "'");
            assertThat(str).contains("msgInfo='操作成功'");
            assertThat(str).contains("msgType='" + ResponseMsgType.SUCCESS.getType() + "'");
            assertThat(str).contains("body=测试数据");
        }
    }

    // ==================== 泛型类型测试 ====================

    @Nested
    @DisplayName("泛型类型测试")
    class GenericTypeTests {

        @Test
        @DisplayName("应该支持String类型")
        void shouldSupportStringType() {
            // when
            ResponseDTO<String> response = ResponseDTO.newSuccessInstance("字符串数据");

            // then
            assertThat(response.getBody()).isEqualTo("字符串数据");
        }

        @Test
        @DisplayName("应该支持Integer类型")
        void shouldSupportIntegerType() {
            // when
            ResponseDTO<Integer> response = ResponseDTO.newSuccessInstance(123);

            // then
            assertThat(response.getBody()).isEqualTo(123);
        }

        @Test
        @DisplayName("应该支持List类型")
        void shouldSupportListType() {
            // given
            java.util.List<String> list = java.util.Arrays.asList("a", "b", "c");

            // when
            ResponseDTO<java.util.List<String>> response = ResponseDTO.newSuccessInstance(list);

            // then
            assertThat(response.getBody()).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("应该支持Map类型")
        void shouldSupportMapType() {
            // given
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("key", "value");

            // when
            ResponseDTO<java.util.Map<String, Object>> response = ResponseDTO.newSuccessInstance(map);

            // then
            assertThat(response.getBody()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("应该支持EmptyBody类型")
        void shouldSupportEmptyBodyType() {
            // when
            ResponseDTO<EmptyBody> response = ResponseDTO.newSuccessInstance();

            // then
            assertThat(response.getBody()).isNull();
        }
    }

    // ==================== 常量值验证测试 ====================

    @Nested
    @DisplayName("常量值验证测试")
    class ConstantValueTests {

        @Test
        @DisplayName("成功码应该是BLINK0000")
        void shouldHaveCorrectSuccessCode() {
            assertThat(SysConstant.SUCCESS_CODE).isEqualTo("BLINK0000");
        }

        @Test
        @DisplayName("失败码应该是BLINK0001")
        void shouldHaveCorrectFailCode() {
            assertThat(SysConstant.FAIL_CODE).isEqualTo("BLINK0001");
        }

        @Test
        @DisplayName("成功消息类型应该是S")
        void shouldHaveCorrectSuccessMsgType() {
            assertThat(ResponseMsgType.SUCCESS.getType()).isEqualTo("S");
        }

        @Test
        @DisplayName("系统错误消息类型应该是SYS_ERR")
        void shouldHaveCorrectSystemErrMsgType() {
            assertThat(ResponseMsgType.SYSTEM_ERR.getType()).isEqualTo("SYS_ERR");
        }
    }

    // ==================== 实际使用场景测试 ====================

    @Nested
    @DisplayName("实际使用场景测试")
    class UsageScenarioTests {

        @Test
        @DisplayName("Controller层成功返回场景")
        void shouldWorkInControllerSuccessScenario() {
            // given - 模拟Service返回数据
            @lombok.Data
            @lombok.AllArgsConstructor
            class UserVO {
                private Long id;
                private String name;
            }
            UserVO user = new UserVO(1L, "张三");

            // when - Controller返回成功响应
            ResponseDTO<UserVO> response = ResponseDTO.newSuccessInstance(user);

            // then
            assertThat(response.getMsgCode()).isEqualTo(SysConstant.SUCCESS_CODE);
            assertThat(response.getMsgType()).isEqualTo(ResponseMsgType.SUCCESS.getType());
            assertThat(response.getBody().getName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("Controller层失败返回场景")
        void shouldWorkInControllerFailScenario() {
            // when - Controller返回失败响应
            ResponseDTO<EmptyBody> response = ResponseDTO.newFailInstance();
            response.setMsgInfo("系统繁忙，请稍后重试");

            // then
            assertThat(response.getMsgCode()).isEqualTo(SysConstant.FAIL_CODE);
            assertThat(response.getMsgType()).isEqualTo(ResponseMsgType.SYSTEM_ERR.getType());
            assertThat(response.getMsgInfo()).isEqualTo("系统繁忙，请稍后重试");
        }

        @Test
        @DisplayName("业务异常响应场景")
        void shouldHandleBusinessErrorResponse() {
            // given
            ResponseDTO<EmptyBody> response = new ResponseDTO<>();
            response.setMsgCode("BUSS00001");
            response.setMsgType(ResponseMsgType.BUSINESS_ERR.getType());
            response.setMsgInfo("用户名已存在");

            // when & then
            assertThat(response.getMsgCode()).isEqualTo("BUSS00001");
            assertThat(response.getMsgInfo()).isEqualTo("用户名已存在");
        }

        @Test
        @DisplayName("参数校验失败响应场景")
        void shouldHandleValidationErrorResponse() {
            // given
            ResponseDTO<EmptyBody> response = new ResponseDTO<>();
            response.setMsgCode("PARAM00001");
            response.setMsgType(ResponseMsgType.BUSINESS_ERR.getType());
            response.setMsgInfo("参数不能为空");

            // when & then
            assertThat(response.getMsgCode()).isEqualTo("PARAM00001");
            assertThat(response.getMsgType()).isEqualTo(ResponseMsgType.BUSINESS_ERR.getType());
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("空字符串body应该能正确处理")
        void shouldHandleEmptyStringBody() {
            // when
            ResponseDTO<String> response = ResponseDTO.newSuccessInstance("");

            // then
            assertThat(response.getBody()).isEmpty();
        }

        @Test
        @DisplayName("超长消息信息应该能正确处理")
        void shouldHandleLongMsgInfo() {
            // given
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("这是一条很长的错误信息。");
            }
            String longMsg = sb.toString();

            // when
            ResponseDTO<String> response = new ResponseDTO<>();
            response.setMsgInfo(longMsg);

            // then
            assertThat(response.getMsgInfo()).isEqualTo(longMsg);
        }
    }
}
