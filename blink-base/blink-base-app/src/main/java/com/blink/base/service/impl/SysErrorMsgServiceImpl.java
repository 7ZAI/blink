package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.dto.req.QueryErrMsgReq;
import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.base.entity.SysMsgInfoDO;
import com.blink.base.mapper.SysMsgInfoMapper;
import com.blink.base.service.SysErrorMsgService;
import com.blink.framework.common.exception.BlinkException;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 * 错误消息服务实现类
 * </p>
 *
 * @author blink
 * @since 2025-09-05
 */
@Slf4j
@Service
public class SysErrorMsgServiceImpl implements SysErrorMsgService {

    @Resource
    private SysMsgInfoMapper sysMsgInfoMapper;

    /**
     * 根据错误码和语言查询单个错误码消息
     *
     * @param queryParam 查询参数，包含错误码和语言
     * @return QueryErrMsgRsp 错误消息响应
     * @throws BlinkException 当错误消息不存在时抛出业务异常
     */
    @Override
    public QueryErrMsgRsp getErrorMsg(QueryErrMsgReq queryParam) throws BlinkException {
        try {
            SysMsgInfoDO result = sysMsgInfoMapper.selectOne(new LambdaQueryWrapper<SysMsgInfoDO>()
                    .eq(SysMsgInfoDO::getMsgCode, queryParam.getCode())
                    .eq(SysMsgInfoDO::getMsgLang, queryParam.getLocal()));

            if (Objects.isNull(result)) {
                log.warn("错误消息不存在，错误码: {}, 语言: {}", queryParam.getCode(), queryParam.getLocal());
                BlinkException.throwBusinessException(BaseErrCodeConstant.ERR_MSG_NOT_EXIST);
            }

            // 使用 BeanUtil 进行对象属性拷贝
            QueryErrMsgRsp rspDTO = BeanUtil.copyProperties(result, QueryErrMsgRsp.class);
            
            log.debug("成功查询错误消息，错误码: {}, 语言: {}", queryParam.getCode(), queryParam.getLocal());
            return rspDTO;
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询错误消息异常，错误码: {}, 语言: {}", queryParam.getCode(), queryParam.getLocal(), e);
            throw new BlinkException(e.getMessage());
        }
    }
}
