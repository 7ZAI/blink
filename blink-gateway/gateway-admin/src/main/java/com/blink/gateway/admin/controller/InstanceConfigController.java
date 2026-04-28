package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetInstanceConfigReq;
import com.blink.gateway.admin.dto.req.SaveInstanceConfigContentReq;
import com.blink.gateway.admin.dto.req.UpdateInstanceConfigReq;
import com.blink.gateway.admin.dto.vo.InstanceConfigFileVO;
import com.blink.gateway.admin.service.InstanceConfigService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 实例配置文件管理控制器
 * 提供网关实例配置文件的查看、修改功能
 *
 * @author binblink
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/instanceConfig")
public class InstanceConfigController {

    @Resource
    private InstanceConfigService instanceConfigService;

    /**
     * 获取实例配置文件
     *
     * @param reqDto 请求参数
     * @return 配置文件信息
     */
    @PostMapping("/get")
    public ResponseDTO<InstanceConfigFileVO> getInstanceConfig(
        @RequestBody @Validated RequestDTO<GetInstanceConfigReq> reqDto) {
        return instanceConfigService.getInstanceConfig(reqDto.getBody());
    }

    /**
     * 更新实例动态路由配置
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/update")
    public ResponseDTO<EmptyBody> updateInstanceConfig(
        @RequestBody @Validated RequestDTO<UpdateInstanceConfigReq> reqDto) {
        return instanceConfigService.updateInstanceConfig(reqDto.getBody());
    }

    /**
     * 保存实例配置文件内容
     * 直接保存 YAML 内容，适用于高级编辑场景
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/saveContent")
    public ResponseDTO<EmptyBody> saveInstanceConfigContent(
        @RequestBody @Validated RequestDTO<SaveInstanceConfigContentReq> reqDto) {
        return instanceConfigService.saveInstanceConfigContent(reqDto.getBody());
    }
}
