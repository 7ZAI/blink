package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.dto.req.SaveUserPreferenceReq;
import com.blink.base.dto.vo.UserPreferenceVO;
import com.blink.base.entity.SysUserPreferenceDO;
import com.blink.base.mapper.SysUserPreferenceMapper;
import com.blink.base.service.SysUserPreferenceService;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.exception.BlinkException;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 用户偏好设置 服务实现类
 * </p>
 *
 * @author binblink
 */
@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
public class SysUserPreferenceServiceImpl implements SysUserPreferenceService {

    @Resource
    private SysUserPreferenceMapper sysUserPreferenceMapper;

    @Override
    public void saveOrUpdatePreference(Integer userId, SaveUserPreferenceReq req) throws BlinkException {
        // 查询是否已存在
        LambdaQueryWrapper<SysUserPreferenceDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPreferenceDO::getUserId, userId);
        SysUserPreferenceDO exist = sysUserPreferenceMapper.selectOne(wrapper);

        String loginName = BlinkRequestContextHolder.getLoginName();

        if (exist == null) {
            // 新增
            SysUserPreferenceDO preference = new SysUserPreferenceDO();
            preference.setUserId(userId);
            preference.setTheme(req.getTheme());
            preference.setLanguage(req.getLanguage());
            preference.setSidebarCollapsed(req.getSidebarCollapsed());
            preference.setFontSize(req.getFontSize());
            sysUserPreferenceMapper.insert(preference);
        } else {
            // 更新
            exist.setTheme(req.getTheme());
            exist.setLanguage(req.getLanguage());
            exist.setSidebarCollapsed(req.getSidebarCollapsed());
            exist.setFontSize(req.getFontSize());
            sysUserPreferenceMapper.updateById(exist);
        }
    }

    @Override
    public UserPreferenceVO getPreferenceByUserId(Integer userId) throws BlinkException {
        LambdaQueryWrapper<SysUserPreferenceDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPreferenceDO::getUserId, userId);
        SysUserPreferenceDO preference = sysUserPreferenceMapper.selectOne(wrapper);

        if (preference == null) {
            // 返回默认设置
            UserPreferenceVO vo = new UserPreferenceVO();
            vo.setUserId(userId);
            vo.setTheme("light");
            vo.setLanguage("zh_cn");
            vo.setSidebarCollapsed(false);
            vo.setFontSize(14);
            return vo;
        }

        return BeanUtil.copyProperties(preference, UserPreferenceVO.class);
    }

}
