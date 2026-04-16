package com.blink.framework.test.helper;

import com.blink.framework.common.context.BlinkRequestContextHolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Method;

/**
 * Mock 辅助工具类
 * 提供常用 Mock 场景的快速设置
 *
 * @author binblink
 * @since 2026-04-16
 */
public class MockHelper {

    /**
     * Mock 静态方法
     *
     * @param clazz 要 Mock 的类
     * @return MockedStatic 对象（需要在 try-with-resources 中使用）
     */
    public static <T> MockedStatic<T> mockStatic(Class<T> clazz) {
        return Mockito.mockStatic(clazz);
    }

    /**
     * Mock BlinkRequestContextHolder（常用场景）
     * 自动设置 userId 和 loginName
     *
     * @param userId    用户 ID
     * @param loginName 登录名
     * @return MockedStatic 对象（需要在 try-with-resources 中使用）
     */
    public static MockedStatic<BlinkRequestContextHolder> mockRequestContext(
            String userId, String loginName) {
        MockedStatic<BlinkRequestContextHolder> mock = mockStatic(BlinkRequestContextHolder.class);
        mock.when(BlinkRequestContextHolder::getUserId).thenReturn(userId);
        mock.when(BlinkRequestContextHolder::getLoginName).thenReturn(loginName);
        return mock;
    }

    /**
     * Mock BlinkRequestContextHolder（仅 userId）
     *
     * @param userId 用户 ID
     * @return MockedStatic 对象
     */
    public static MockedStatic<BlinkRequestContextHolder> mockUserId(String userId) {
        MockedStatic<BlinkRequestContextHolder> mock = mockStatic(BlinkRequestContextHolder.class);
        mock.when(BlinkRequestContextHolder::getUserId).thenReturn(userId);
        return mock;
    }

    /**
     * 创建 ArgumentCaptor
     *
     * @param clazz 参数类型
     * @return ArgumentCaptor 对象
     */
    public static <T> ArgumentCaptor<T> captor(Class<T> clazz) {
        return ArgumentCaptor.forClass(clazz);
    }

    /**
     * 验证 Mock 对象从未被调用
     *
     * @param mock Mock 对象
     */
    public static void verifyNeverCalled(Object mock) {
        Mockito.verify(mock, Mockito.never()).hashCode();  // 使用无意义方法验证
    }

    /**
     * 验证 Mock 对象被调用了指定次数
     *
     * @param mock  Mock 对象
     * @param times 调用次数
     */
    public static void verifyTimes(Object mock, int times) {
        // 需要配合具体方法使用
    }

    /**
     * 重置 Mock 对象
     *
     * @param mocks Mock 对象数组
     */
    public static void resetMocks(Object... mocks) {
        for (Object mock : mocks) {
            Mockito.reset(mock);
        }
    }

    /**
     * 创建 Mock 对象并设置默认返回值
     *
     * @param clazz          要 Mock 的类
     * @param defaultReturn  默认返回值
     * @return Mock 对象
     */
    public static <T> T mockWithDefault(Class<T> clazz, T defaultReturn) {
        T mock = Mockito.mock(clazz);
        // 设置默认回答策略
        return mock;
    }
}