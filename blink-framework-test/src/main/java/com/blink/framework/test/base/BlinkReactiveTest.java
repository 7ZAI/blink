package com.blink.framework.test.base;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 响应式测试基类
 * 用于测试 WebFlux 响应式应用（如 blink-gateway-reactive）
 *
 * 使用方式：
 * <pre>
 * @IntegrationTest
 * class GatewayHandlerTest extends BlinkReactiveTest {
 *
 *     @Test
 *     void should_return_response_when_route_exists() {
 *         webTestClient
 *             .post()
 *             .uri("/api/route/getRouteList")
 *             .bodyValue(requestDTO)
 *             .exchange()
 *             .expectStatus().isOk()
 *             .expectBody(ResponseDTO.class)
 *             .value(response -> assertThat(response.getMsgCode()).isEqualTo("BLINK0000"));
 *     }
 * }
 * </pre>
 *
 * @author binblink
 * @since 2026-04-16
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class BlinkReactiveTest {

    /**
     * WebTestClient - 响应式 HTTP 测试客户端
     * 自动配置，无需手动创建
     */
    @Autowired
    protected WebTestClient webTestClient;

    /**
     * ApplicationContext - 用于获取 Bean 或验证配置
     */
    @Autowired
    protected ApplicationContext applicationContext;

    /**
     * 创建 GET 请求测试
     *
     * @param uri 请求路径
     * @return WebTestClient.RequestHeadersSpec
     */
    protected WebTestClient.RequestHeadersSpec<?> get(String uri) {
        return webTestClient.get().uri(uri);
    }

    /**
     * 创建 POST 请求测试
     *
     * @param uri 请求路径
     * @return WebTestClient.RequestBodySpec
     */
    protected WebTestClient.RequestBodySpec post(String uri) {
        return webTestClient.post().uri(uri);
    }

    /**
     * 创建 PUT 请求测试
     *
     * @param uri 请求路径
     * @return WebTestClient.RequestBodySpec
     */
    protected WebTestClient.RequestBodySpec put(String uri) {
        return webTestClient.put().uri(uri);
    }

    /**
     * 创建 DELETE 请求测试
     *
     * @param uri 请求路径
     * @return WebTestClient.RequestHeadersSpec
     */
    protected WebTestClient.RequestHeadersSpec<?> delete(String uri) {
        return webTestClient.delete().uri(uri);
    }

    /**
     * 验证响应状态为 200 OK
     *
     * @param responseSpec 响应规格
     * @return WebTestClient.ResponseSpec
     */
    protected WebTestClient.ResponseSpec expectOk(WebTestClient.ResponseSpec responseSpec) {
        return responseSpec.expectStatus().isOk();
    }

    /**
     * 验证响应状态为 4xx
     *
     * @param responseSpec 响应规格
     * @return WebTestClient.ResponseSpec
     */
    protected WebTestClient.ResponseSpec expect4xx(WebTestClient.ResponseSpec responseSpec) {
        return responseSpec.expectStatus().is4xxClientError();
    }

    /**
     * 验证响应状态为 5xx
     *
     * @param responseSpec 响应规格
     * @return WebTestClient.ResponseSpec
     */
    protected WebTestClient.ResponseSpec expect5xx(WebTestClient.ResponseSpec responseSpec) {
        return responseSpec.expectStatus().is5xxServerError();
    }
}