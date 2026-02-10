package com.blink.base.component;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 获取Nacos配置文件
 * @Author binblink
 * @Date 2026/2/6
 */
@Component
@Slf4j
public class NacosConfigComponent {

    @Resource
    private NacosConfigManager nacosConfigManager;

    /**
     * 获取config
     *
     * @param dataId  配置文件id
     * @param groupId 配置文件组别
     * @return 配置文件的字符串格式
     * @throws BlinkException
     */
    public String getConfig(String dataId, String groupId) throws BlinkException {

        String config = "";

        try {
            config = nacosConfigManager.getConfigService().getConfig(dataId, groupId, 5000);
            return config;
        } catch (NacosException e) {
            log.error("从Nacos获取配置文件失败！{} dataId:{}, groupId:{}", e.getMessage(), dataId, groupId, e);
            throw new BlinkException(e, e.getMessage());
        }
    }

    public void configPublisher(String dataId, String groupId, String configContent) throws BlinkException {

        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            // 发布配置
            boolean isPublishOk = configService.publishConfig(dataId, groupId, configContent);
            log.info("从Nacos推送更新配置文件！结果：{} dataId:{}, groupId:{} configContent:{},",isPublishOk, dataId, groupId,configContent.substring(0,500)+"......");
        } catch (Exception e) {
            log.error("从Nacos推送更新配置文件失败！{} dataId:{}, groupId:{} configContent:{}", e.getMessage(), dataId, groupId,configContent, e);
            throw new BlinkException(e, e.getMessage());
        }
    }
}
