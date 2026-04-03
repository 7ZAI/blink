package com.blink.gateway.base.controller;

import com.blink.gateway.base.dto.req.*;
import com.blink.gateway.base.dto.rsp.ConfigGroupRsp;
import com.blink.gateway.base.dto.rsp.QuerySysConfigRsp;
import com.blink.gateway.base.dto.vo.SysConfigVO;
import com.blink.gateway.base.service.SysConfigService;
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
 * 参数配置设置
 * 一般直接由管理员在数据库添加 删除
 * 不提供管理接口，页面只提供设置值或修改值功能
 * 只提供内部查询
 * @author blink
 * @since 2025-09-05
 */
@RestController
@RequestMapping("/sysConfig")
public class SysConfigController {

    @Resource
    private SysConfigService sysConfigService;


//    /**
//     *
//     * 新增参数配置表
//     *
//     * @param reqDto
//     * @return {@link ResponseDTO<EmptyBody>}
//     * @throws Throwable
//     */
//    @PostMapping("/saveSysConfig")
//    public ResponseDTO<EmptyBody> saveSysConfig(@RequestBody @Validated RequestDTO<AddSysConfigReq> reqDto) throws BlinkException {
//        sysConfigService.saveSysConfig(reqDto.getBody());
//        return ResponseDTO.newSuccessInstance();
//    }

    /**
     * 删除参数配置表
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/deleteSysConfig")
    public ResponseDTO<EmptyBody> deleteSysConfig(@RequestBody @Validated RequestDTO<DeleteSysConfigReq> reqDto) throws BlinkException {
        sysConfigService.deleteSysConfig(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新参数配置表
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/modifySysConfig")
    public ResponseDTO<EmptyBody> modifySysConfig(@RequestBody @Validated RequestDTO<UpdateSysConfigReq> reqDto) throws BlinkException {
        sysConfigService.modifySysConfig(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 根据查询条件查询参数配置表列表
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/getSysConfigList")
    public ResponseDTO<QuerySysConfigRsp> getSysConfigList(@RequestBody @Validated RequestDTO<QuerySysConfigReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysConfigService.getSysConfigList(reqDto.getBody()));
    }

    /**
     * 根据查询条件查询数据库获取单个参数配置
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/getConfigFromDB")
    public ResponseDTO<SysConfigVO> getOneConfigFromDataBase(@RequestBody @Validated RequestDTO<QueryOneSysConfigReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysConfigService.getOneConfigFromDataBase(reqDto.getBody()));
    }

    /**
     * 根据查询条件查询
     * 缓存或者数据库获取单个参数配置
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/getConfigFromCache")
    public ResponseDTO<SysConfigVO> getOneConfigFromCacheOrDataBase(@RequestBody @Validated RequestDTO<QueryOneSysConfigReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysConfigService.getOneConfigFromCacheOrDataBase(reqDto.getBody()));
    }

    /**
     * 根据分组键名查询配置
     *
     * @param reqDto
     * @return {@link ResponseDTO<ConfigGroupRsp>}
     * @throws BlinkException
     */
    @PostMapping("/getConfigsByGroupKey")
    public ResponseDTO<ConfigGroupRsp> getConfigsByGroupKey(@RequestBody @Validated RequestDTO<QueryConfigByGroupKeyReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysConfigService.getConfigsByGroupKey(reqDto.getBody().getGroupKey()));
    }

    /**
     * 批量更新配置值
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws BlinkException
     */
    @PostMapping("/batchUpdateConfigs")
    public ResponseDTO<EmptyBody> batchUpdateConfigs(@RequestBody @Validated RequestDTO<BatchUpdateSysConfigReq> reqDto) throws BlinkException {
        sysConfigService.batchUpdateConfigs(reqDto.getBody().getConfigs());
        return ResponseDTO.newSuccessInstance();
    }

}
