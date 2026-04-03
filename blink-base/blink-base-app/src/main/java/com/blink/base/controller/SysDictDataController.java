package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.DictDataMapRsp;
import com.blink.base.dto.rsp.QuerySysDictDataRsp;
import com.blink.base.dto.vo.SysDictDataVO;
import com.blink.base.service.SysDictDataService;
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

import java.util.List;

/**
 * 字典数据表 管理API
 *
 * @author blink
 * @module blink
 * @since 2026-03-07
 */
@RestController
@RequestMapping("/sysDictData")
public class SysDictDataController {

    @Resource
    private SysDictDataService sysDictDataService;

    /**
     * 新增字典数据
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<SysDictDataVO>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/saveSysDictData")
    public ResponseDTO<SysDictDataVO> saveSysDictData(@RequestBody @Validated RequestDTO<AddSysDictDataReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysDictDataService.saveSysDictData(reqDto.getBody()));
    }

    /**
     * 删除字典数据
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/deleteSysDictData")
    public ResponseDTO<EmptyBody> deleteSysDictData(@RequestBody @Validated RequestDTO<DeleteSysDictDataReq> reqDto) throws BlinkException {
        sysDictDataService.deleteSysDictData(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新字典数据
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<SysDictDataVO>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/modifySysDictData")
    public ResponseDTO<SysDictDataVO> modifySysDictData(@RequestBody @Validated RequestDTO<UpdateSysDictDataReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysDictDataService.modifySysDictData(reqDto.getBody()));
    }

    /**
     * 根据查询条件查询字典数据列表
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<QuerySysDictDataRsp>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/getSysDictDataList")
    public ResponseDTO<QuerySysDictDataRsp> getSysDictDataList(@RequestBody @Validated RequestDTO<QuerySysDictDataReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysDictDataService.getSysDictDataList(reqDto.getBody()));
    }

    /**
     * 根据字典类型编码获取字典数据列表
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<List<SysDictDataVO>>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/getDictDataByType")
    public ResponseDTO<List<SysDictDataVO>> getDictDataByType(@RequestBody @Validated RequestDTO<GetDictDataByTypeReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysDictDataService.getDictDataByType(reqDto.getBody().getDictType()));
    }

    /**
     * 批量根据字典类型编码获取字典数据
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<DictDataMapRsp>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/getDictDataByTypes")
    public ResponseDTO<DictDataMapRsp> getDictDataByTypes(@RequestBody @Validated RequestDTO<GetDictDataByTypesReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysDictDataService.getDictDataByTypes(reqDto.getBody().getDictTypes()));
    }
}
