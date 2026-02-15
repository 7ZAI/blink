package com.blink.base.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.base.dto.req.QueryErrMsgReq;
import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.entity.SysMsgInfoDO;
import com.blink.framework.core.mapper.SysMsgInfoMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * <p>
 *   错误码查询 API
 *   仅为内部调用 不对外公开
 *   internal 为内部调用api专用前缀
 * </p>
 *
 * @author binblink
 *
 * @module blink
 */
@RestController
@RequestMapping("/internal/error/msg")
public class SysErrorMsgController {

    @Autowired
    private SysMsgInfoMapper sysMsgInfoMapper;


    /**
     * 根据缓存key值查询查询条件查询单个渠道信息
     *
     * @param requestDTO
     * @return String
     * @throws BlinkException
     */
    @PostMapping("/getMsg")
    public ResponseDTO<QueryErrMsgRsp> getMsg(@RequestBody @Validated RequestDTO<QueryErrMsgReq> requestDTO) throws BlinkException {

        var rsp = new ResponseDTO<QueryErrMsgRsp>();
        QueryErrMsgRsp rspDTO = new QueryErrMsgRsp();

        SysMsgInfoDO result = sysMsgInfoMapper.selectOne(new LambdaQueryWrapper<SysMsgInfoDO>()
                .eq(SysMsgInfoDO::getMsgCode, requestDTO.getBody().getCode())
                .eq(SysMsgInfoDO::getMsgLang, requestDTO.getBody().getLocal()));

        if(Objects.isNull(result)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.ERR_MSG_NOT_EXIST);
        }

        BeanUtils.copyProperties(result, rspDTO);
        rsp.setBody(rspDTO);
        return rsp;
    }
}
