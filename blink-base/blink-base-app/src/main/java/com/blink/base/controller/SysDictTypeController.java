package com.blink.base.controller;

import com.blink.base.dto.req.AddSysDictTypeReq;
import com.blink.base.dto.req.DeleteSysDictTypeReq;
import com.blink.base.dto.req.QuerySysDictTypeReq;
import com.blink.base.dto.req.UpdateSysDictTypeReq;
import com.blink.base.dto.rsp.QuerySysDictTypeRsp;
import com.blink.base.dto.vo.SysDictTypeVO;
import com.blink.base.service.SysDictTypeService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 字典类型表 管理API
 *
 * @author blink
 * @module blink
 * @since 2025-03-07
 */
@RestController
@RequestMapping("/sysDictType")
public class SysDictTypeController {

    @Resource
    private SysDictTypeService sysDictTypeService;

    /**
     * 新增字典类型
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<SysDictTypeVO>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/saveSysDictType")
    public ResponseDTO<SysDictTypeVO> saveSysDictType(@RequestBody @Validated RequestDTO<AddSysDictTypeReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysDictTypeService.saveSysDictType(reqDto.getBody()));
    }

    /**
     * 删除字典类型
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/deleteSysDictType")
    public ResponseDTO<EmptyBody> deleteSysDictType(@RequestBody @Validated RequestDTO<DeleteSysDictTypeReq> reqDto) throws BlinkException {
        sysDictTypeService.deleteSysDictType(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新字典类型
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<SysDictTypeVO>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/modifySysDictType")
    public ResponseDTO<SysDictTypeVO> modifySysDictType(@RequestBody @Validated RequestDTO<UpdateSysDictTypeReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysDictTypeService.modifySysDictType(reqDto.getBody()));
    }

    /**
     * 根据查询条件查询字典类型列表
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<QuerySysDictTypeRsp>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/getSysDictTypeList")
    public ResponseDTO<QuerySysDictTypeRsp> getSysDictTypeList(@RequestBody @Validated RequestDTO<QuerySysDictTypeReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysDictTypeService.getSysDictTypeList(reqDto.getBody()));
    }

    /**
     * 根据字典类型编码查询字典类型
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<SysDictTypeVO>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/getSysDictTypeByType")
    public ResponseDTO<SysDictTypeVO> getSysDictTypeByType(@RequestBody @Validated RequestDTO<QuerySysDictTypeReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysDictTypeService.getSysDictTypeByType(reqDto.getBody().getDictType()));
    }

    /**
     * 根据字典主键id查询字典类型
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<SysDictTypeVO>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/getSysDictTypeById")
    public ResponseDTO<SysDictTypeVO> getSysDictTypeById(@RequestBody @Validated RequestDTO<QuerySysDictTypeReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysDictTypeService.getSysDictTypeById(reqDto.getBody().getDictId()));
    }
}
