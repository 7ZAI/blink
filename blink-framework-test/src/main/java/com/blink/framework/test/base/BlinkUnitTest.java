package com.blink.framework.test.base;

import com.blink.framework.common.data.RequestDTO;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

/**
 * 单元测试基类
 * 提供通用 Mock 初始化和测试工具
 *
 * 使用方式：
 * <pre>
 * class MyServiceTest extends BlinkUnitTest {
 *     @Mock
 *     private MyMapper mapper;
 *
 *     @InjectMocks
 *     private MyServiceImpl service;
 *
 *     @Test
 *     void shouldReturnData_whenExists() {
 *         // 使用 createRequestDTO 快速构建请求
 *         RequestDTO<MyReq> req = createRequestDTO(new MyReq());
 *         // ...
 *     }
 * }
 * </pre>
 *
 * @author binblink
 * @since 2026-04-16
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class BlinkUnitTest {

    /**
     * 快速创建 Mock 对象
     *
     * @param clazz 要 Mock 的类
     * @return Mock 对象
     */
    protected <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz);
    }

    /**
     * 快速创建 Spy 对象（部分 Mock）
     *
     * @param object 要 Spy 的对象
     * @return Spy 对象
     */
    protected <T> T spy(T object) {
        return Mockito.spy(object);
    }

    /**
     * 创建测试 RequestDTO
     * 自动设置 requestId 和 traceId
     *
     * @param body 业务数据
     * @return RequestDTO 对象
     */
    protected <T> RequestDTO<T> createRequestDTO(T body) {
        RequestDTO<T> dto = new RequestDTO<>();
        dto.setBody(body);
        dto.setRequestId(UUID.randomUUID().toString());
        dto.setTraceId(UUID.randomUUID().toString());
        return dto;
    }

    /**
     * 创建空的 RequestDTO（用于不需要 body 的请求）
     *
     * @return RequestDTO 对象
     */
    protected RequestDTO<Void> createEmptyRequestDTO() {
        return createRequestDTO(null);
    }
}