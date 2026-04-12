package com.blink.gateway.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.blink.gateway.base.constants.BaseErrCodeConstant;
import com.blink.gateway.base.constants.CommonConstants;
import com.blink.gateway.base.constants.RedisKeyConstants;
import com.blink.gateway.base.dto.req.KickoutUserReq;
import com.blink.gateway.base.dto.req.QueryOnlineUserReq;
import com.blink.gateway.base.dto.rsp.OnlineUserRsp;
import com.blink.gateway.base.dto.vo.OnlineUserVO;
import com.blink.gateway.base.service.OnlineUserService;
import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.Cursor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 在线用户服务实现类
 */
@Service
@Slf4j
public class OnlineUserServiceImpl implements OnlineUserService {

    @Resource
    private RedisClient redisClient;

    /**
     * 获取在线用户列表
     * @param queryParam 查询参数
     * @return 在线用户响应
     * @throws BlinkException 业务异常
     */
    @Override
    public OnlineUserRsp getOnlineUserList(QueryOnlineUserReq queryParam) throws BlinkException {
        OnlineUserRsp onlineUserRsp = new OnlineUserRsp();
        
        List<OnlineUserVO> onlineUsers = new ArrayList<>();
        
        // 使用scan方法扫描所有用户token键
        // count=100 表示每次迭代返回约100个元素，不是限制总数
        // cursor.hasNext() 会持续迭代直到所有匹配的键都被扫描完
        // 注意：需要过滤掉 user:token:old: 前缀的 key，只扫描正常登录用户的 token
        String pattern = RedisKeyConstants.USER_TOKEN + "*";
        String oldTokenPrefix = RedisKeyConstants.USER_TOKEN_OLD;
        Cursor<String> cursor = redisClient.scan(pattern, CommonConstants.REDIS_SCAN_BATCH_SIZE);
        
        try {
            while (cursor.hasNext()) {
                String key = cursor.next();
                // 跳过旧 token key（用于顶替登录提示）
                if (key.startsWith(oldTokenPrefix)) {
                    continue;
                }
                Object value = redisClient.get(key);
                if (value != null) {
                    String valueStr = value instanceof String ? (String) value : JacksonUtil.toJson(value);
                    UserInfoRedisDO userInfo = JacksonUtil.fromJson(valueStr, UserInfoRedisDO.class);
                    if (userInfo != null) {
                        OnlineUserVO vo = new OnlineUserVO();
                        BeanUtil.copyProperties(userInfo, vo);
                        vo.setLoginTime(userInfo.getLoginDateTime());
                        onlineUsers.add(vo);
                    }
                }
            }
        } finally {
            cursor.close();
        }
        
        onlineUserRsp.setRows(onlineUsers);
        onlineUserRsp.setTotal(onlineUsers.size());
        
        return onlineUserRsp;
    }

    /**
     * 强制用户下线
     * @param kickoutUserReq 强制下线请求
     * @throws BlinkException 业务异常
     */
    @Override
    public void kickoutUser(KickoutUserReq kickoutUserReq) throws BlinkException {
        String token = kickoutUserReq.getToken();

        Object userInfoObj = redisClient.get(RedisKeyConstants.USER_TOKEN + token);
        if (userInfoObj == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.TOKEN_EXPIRED);
        }

        UserInfoRedisDO userInfo = JacksonUtil.convert(userInfoObj, UserInfoRedisDO.class);
        Integer userId = userInfo.getUserId();

        // 删除 token
        redisClient.delete(RedisKeyConstants.USER_TOKEN + token);
        // 从 ZSet 移除
        redisClient.zRemove(RedisKeyConstants.USER_TOKENS + userId, token);
        // 设置旧 token 标记（用于提示）
        redisClient.setEx(RedisKeyConstants.USER_TOKEN_OLD + token, userId, CommonConstants.OLD_TOKEN_EXPIRE_SECONDS);

        log.info("[OnlineUser] 强制用户下线 | userId: {}, token: {}", userId, token);
    }

    /**
     * 根据用户ID列表查询在线用户的token
     *
     * @param userIdList 用户ID列表
     * @return 在线用户的token列表
     */
    @Override
    public List<String> getOnlineUserTokensByUserIds(List<Integer> userIdList) {
        List<String> onlineTokens = new ArrayList<>();

        String pattern = RedisKeyConstants.USER_TOKEN + "*";
        String oldTokenPrefix = RedisKeyConstants.USER_TOKEN_OLD;
        Cursor<String> cursor = redisClient.scan(pattern, CommonConstants.REDIS_SCAN_BATCH_SIZE);

        try {
            while (cursor.hasNext()) {
                String key = cursor.next();
                // 跳过旧 token key
                if (key.startsWith(oldTokenPrefix)) {
                    continue;
                }
                Object value = redisClient.get(key);
                if (value != null) {
                    String valueStr = value instanceof String ? (String) value : JacksonUtil.toJson(value);
                    UserInfoRedisDO userInfo = JacksonUtil.fromJson(valueStr, UserInfoRedisDO.class);
                    if (userInfo != null && userInfo.getUserId() != null && userIdList.contains(userInfo.getUserId())) {
                        onlineTokens.add(userInfo.getToken());
                    }
                }
            }
        } finally {
            cursor.close();
        }

        return onlineTokens;
    }

    /**
     * 根据用户ID列表强制下线
     *
     * @param userIdList 用户ID列表
     */
    @Override
    public void kickoutUsersByUserIds(List<Integer> userIdList) {
        if (CollUtil.isEmpty(userIdList)) {
            return;
        }

        for (Integer userId : userIdList) {
            // 获取该用户的所有 token
            Set<Object> tokens = redisClient.zRange(RedisKeyConstants.USER_TOKENS + userId, 0, -1);
            if (CollUtil.isNotEmpty(tokens)) {
                for (Object tokenObj : tokens) {
                    String token = String.valueOf(tokenObj);
                    redisClient.delete(RedisKeyConstants.USER_TOKEN + token);
                    log.info("[OnlineUser] 强制用户下线 | userId: {}, token: {}", userId, token);
                }
            }
            // 删除整个 ZSet
            redisClient.delete(RedisKeyConstants.USER_TOKENS + userId);
        }
    }
}
