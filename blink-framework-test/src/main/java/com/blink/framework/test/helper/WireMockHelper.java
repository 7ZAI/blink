package com.blink.framework.test.helper;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock HTTP Mock 辅助工具
 * 用于集成测试中 Mock 外部 HTTP API
 *
 * 使用方式：
 * <pre>
 * // 在测试类中注册 WireMockExtension
 * @RegisterExtension
 * static WireMockExtension wireMock = WireMockExtension.newInstance()
 *     .options(WireMockConfiguration.wireMockConfig().dynamicPort())
 *     .build();
 *
 * // 使用 WireMockHelper 快速配置 Mock
 * WireMockHelper.mockGet(wireMock, "/api/users/1", "{\"id\":1,\"name\":\"test\"}");
 * </pre>
 *
 * @author binblink
 * @since 2026-04-16
 */
public class WireMockHelper {

    /**
     * Mock GET 请求返回 JSON 响应
     *
     * @param wireMock WireMock 扩展实例
     * @param path     请求路径
     * @param body     JSON 响应体
     * @return StubMapping
     */
    public static StubMapping mockGet(WireMockExtension wireMock, String path, String body) {
        return wireMock.stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                        .withStatus(200)));
    }

    /**
     * Mock GET 请求返回 JSON 响应（带状态码）
     *
     * @param wireMock   WireMock 扩展实例
     * @param path       请求路径
     * @param body       JSON 响应体
     * @param statusCode HTTP 状态码
     * @return StubMapping
     */
    public static StubMapping mockGet(WireMockExtension wireMock, String path, String body, int statusCode) {
        return wireMock.stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                        .withStatus(statusCode)));
    }

    /**
     * Mock POST 请求返回 JSON 响应
     *
     * @param wireMock WireMock 扩展实例
     * @param path     请求路径
     * @param body     JSON 响应体
     * @return StubMapping
     */
    public static StubMapping mockPost(WireMockExtension wireMock, String path, String body) {
        return wireMock.stubFor(post(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                        .withStatus(200)));
    }

    /**
     * Mock POST 请求（验证请求体，返回指定响应）
     *
     * @param wireMock     WireMock 扩展实例
     * @param path         请求路径
     * @param requestBody  期望的请求体（JSON）
     * @param responseBody 响应体（JSON）
     * @return StubMapping
     */
    public static StubMapping mockPost(WireMockExtension wireMock, String path,
                                        String requestBody, String responseBody) {
        return wireMock.stubFor(post(urlPathEqualTo(path))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                        .withStatus(200)));
    }

    /**
     * Mock PUT 请求返回 JSON 响应
     *
     * @param wireMock WireMock 扩展实例
     * @param path     请求路径
     * @param body     JSON 响应体
     * @return StubMapping
     */
    public static StubMapping mockPut(WireMockExtension wireMock, String path, String body) {
        return wireMock.stubFor(put(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                        .withStatus(200)));
    }

    /**
     * Mock DELETE 请求
     *
     * @param wireMock WireMock 扩展实例
     * @param path     请求路径
     * @return StubMapping
     */
    public static StubMapping mockDelete(WireMockExtension wireMock, String path) {
        return wireMock.stubFor(delete(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(204)));
    }

    /**
     * Mock 延迟响应（用于测试超时场景）
     *
     * @param wireMock    WireMock 扩展实例
     * @param path        请求路径
     * @param body        JSON 响应体
     * @param delayMillis 延迟毫秒数
     * @return StubMapping
     */
    public static StubMapping mockGetWithDelay(WireMockExtension wireMock, String path,
                                                String body, int delayMillis) {
        return wireMock.stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                        .withStatus(200)
                        .withFixedDelay(delayMillis)));
    }

    /**
     * Mock 错误响应（500）
     *
     * @param wireMock WireMock 扩展实例
     * @param path     请求路径
     * @return StubMapping
     */
    public static StubMapping mockError(WireMockExtension wireMock, String path) {
        return wireMock.stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("{\"error\":\"Internal Server Error\"}")));
    }

    /**
     * 验证请求被调用
     *
     * @param wireMock WireMock 扩展实例
     * @param path     请求路径
     * @param method   HTTP 方法（GET/POST/PUT/DELETE）
     */
    public static void verifyCalled(WireMockExtension wireMock, String path, String method) {
        switch (method.toUpperCase()) {
            case "GET":
                wireMock.verify(getRequestedFor(urlPathEqualTo(path)));
                break;
            case "POST":
                wireMock.verify(postRequestedFor(urlPathEqualTo(path)));
                break;
            case "PUT":
                wireMock.verify(putRequestedFor(urlPathEqualTo(path)));
                break;
            case "DELETE":
                wireMock.verify(deleteRequestedFor(urlPathEqualTo(path)));
                break;
            default:
                throw new IllegalArgumentException("Unknown HTTP method: " + method);
        }
    }

    /**
     * 验证请求未被调用
     *
     * @param wireMock WireMock 扩展实例
     * @param path     请求路径
     */
    public static void verifyNotCalled(WireMockExtension wireMock, String path) {
        wireMock.verify(0, getRequestedFor(urlPathEqualTo(path)));
    }

    /**
     * 验证请求被调用指定次数
     *
     * @param wireMock WireMock 扩展实例
     * @param path     请求路径
     * @param times    期望调用次数
     */
    public static void verifyCalledTimes(WireMockExtension wireMock, String path, int times) {
        wireMock.verify(times, getRequestedFor(urlPathEqualTo(path)));
    }

    /**
     * 清除所有 Mock
     *
     * @param wireMock WireMock 扩展实例
     */
    public static void resetAll(WireMockExtension wireMock) {
        wireMock.resetAll();
    }
}