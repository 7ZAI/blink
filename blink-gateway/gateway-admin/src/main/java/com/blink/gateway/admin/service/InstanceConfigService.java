package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetInstanceConfigReq;
import com.blink.gateway.admin.dto.req.SaveInstanceConfigContentReq;
import com.blink.gateway.admin.dto.req.UpdateInstanceConfigReq;
import com.blink.gateway.admin.dto.vo.InstanceConfigFileVO;

/**
 * 实例配置文件服务接口
 *
 * @author binblink
 * @since 2026-04-26
 */
public interface InstanceConfigService {

    /**
     * 获取实例配置文件
     *
     * @param req 请求参数
     * @return 配置文件信息
     */
    ResponseDTO<InstanceConfigFileVO> getInstanceConfig(GetInstanceConfigReq req);

    /**
     * 更新实例动态路由配置
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> updateInstanceConfig(UpdateInstanceConfigReq req);

    /**
     * 保存实例配置文件内容
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> saveInstanceConfigContent(SaveInstanceConfigContentReq req);
}
