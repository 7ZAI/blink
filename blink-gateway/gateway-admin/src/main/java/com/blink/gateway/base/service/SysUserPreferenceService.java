package com.blink.gateway.base.service;

import com.blink.gateway.base.dto.req.SaveUserPreferenceReq;
import com.blink.gateway.base.dto.vo.UserPreferenceVO;
import com.blink.framework.common.exception.BlinkException;

/**
 * <p>
 * 用户偏好设置 服务类
 * </p>
 *
 * @author binblink
 */
public interface SysUserPreferenceService {

    /**
     * 保存或更新用户偏好设置
     *
     * @param userId 用户ID
     * @param req 偏好设置请求
     * @throws BlinkException
     */
    void saveOrUpdatePreference(Integer userId, SaveUserPreferenceReq req) throws BlinkException;

    /**
     * 根据用户ID查询偏好设置
     *
     * @param userId 用户ID
     * @return 偏好设置
     * @throws BlinkException
     */
    UserPreferenceVO getPreferenceByUserId(Integer userId) throws BlinkException;

}
