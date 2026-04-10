package com.blink.base.config;

import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * 验证码配置类
 * <p>
 * 缓存类型说明：
 * - local: 本地缓存，仅适用于单实例部署
 * - redis: Redis缓存，适用于微服务多实例部署
 * </p>
 * <p>
 * 验证码类型说明：
 * - clickWord: 点选文字验证码，需要中文字体支持
 * - blockPuzzle: 滑块拼图验证码，无需字体支持
 * </p>
 *
 * @author binblink
 */
@Slf4j
@Configuration
public class CaptchaConfig {

    /**
     * 验证码类型: clickWord(点选文字) / blockPuzzle(滑块拼图)
     */
    @Value("${blink.captcha.type:blockPuzzle}")
    private String captchaType;

    /**
     * 配置验证码服务
     * 使用 Redis 缓存方式存储验证码，支持微服务多实例环境
     */
    @Bean
    public CaptchaService captchaService() {
        Properties config = new Properties();

        // ==================== 缓存配置 ====================
        // 验证码缓存方式：local(本地缓存)/redis(Redis缓存)
        // 微服务多实例环境必须使用 redis，否则会出现验证失败问题
        config.setProperty("captcha.cache.type", "redis");

        // ==================== 验证码基本配置 ====================
        // 验证码类型：从配置文件读取，支持 clickWord/blockPuzzle
        config.setProperty("captcha.type", captchaType);
        // 验证码有效期（秒）
        config.setProperty("captcha.ttl", "120");

        // ==================== 图片配置 ====================
        // 验证码图片宽度
        config.setProperty("captcha.image.width", "310");
        // 验证码图片高度
        config.setProperty("captcha.image.height", "155");

        // ==================== 自定义图片路径配置 ====================
        // 滑块拼图背景图片路径（支持classpath:或file:前缀）
        // 目录结构：images/jigsaw/original/ 存放背景原图
        config.setProperty("captcha.jigsaw", "classpath:images/jigsaw");

        // 点选文字背景图片路径
        // 目录结构：images/pic-click/ 存放点选背景图
        config.setProperty("captcha.pic-click", "classpath:images/pic-click");

        // ==================== 滑块验证码配置 ====================
        // 滑块验证码干扰选项：0-无干扰，1-有干扰
        config.setProperty("captcha.blockPuzzle.interference", "1");
        // 滑块验证码偏移量容错（像素）
        config.setProperty("captcha.slip.offset", "5");

        // ==================== 点选验证码配置 ====================
        // 点选验证码文字数量
        config.setProperty("captcha.clickWord.wordCount", "4");

        // ==================== 安全配置 ====================
        // 关闭AES加密，前端直接传递明文坐标
        config.setProperty("captcha.aes.status", "false");

        // ==================== 字体配置 ====================
        // 点选文字验证码字体配置
        // captcha.font.type 应设置为字体文件名（库从 classpath:/fonts/ 加载）
        // 可选：WenQuanZhengHei.ttf（库自带）、simsun.ttc（宋体）
        if ("clickWord".equals(captchaType)) {
            config.setProperty("captcha.font.type", "simsun.ttc");
            log.info("验证码配置 - 类型: {}, 字体: simsun.ttc", captchaType);
        } else {
            log.info("验证码配置 - 类型: {}", captchaType);
        }

        return CaptchaServiceFactory.getInstance(config);
    }
}