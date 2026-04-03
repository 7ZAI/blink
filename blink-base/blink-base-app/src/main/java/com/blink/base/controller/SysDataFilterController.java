package com.blink.base.controller;

import com.blink.base.dto.req.AddDataFilterReq;
import com.blink.base.dto.req.DataFilterIdReq;
import com.blink.base.dto.req.GetEntityFieldsReq;
import com.blink.base.dto.req.GetMatchTypesReq;
import com.blink.base.dto.req.QueryDataFilterReq;
import com.blink.base.dto.req.UpdateDataFilterReq;
import com.blink.base.dto.rsp.EntityFieldsRsp;
import com.blink.base.dto.rsp.EntityListRsp;
import com.blink.base.dto.rsp.MatchTypesRsp;
import com.blink.base.dto.rsp.QueryDataFilterRsp;
import com.blink.base.dto.vo.DataFilterVO;
import com.blink.base.service.SysDataFilterService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据过滤规则控制器
 *
 * @author binblink
 */
@RestController
@RequestMapping("/sysDataFilter")
public class SysDataFilterController {

    @Resource
    private SysDataFilterService sysDataFilterService;

    /**
     * 查询数据过滤规则列表（分页）
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    @PostMapping("/queryDataFilterList")
    public ResponseDTO<QueryDataFilterRsp> queryDataFilterList(@RequestBody RequestDTO<QueryDataFilterReq> reqDto) {
        return sysDataFilterService.queryDataFilterList(reqDto);
    }

    /**
     * 新增数据过滤规则
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    @PostMapping("/addDataFilter")
    public ResponseDTO<EmptyBody> addDataFilter(@RequestBody @Validated RequestDTO<AddDataFilterReq> reqDto) {
        return sysDataFilterService.addDataFilter(reqDto);
    }

    /**
     * 更新数据过滤规则
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    @PostMapping("/updateDataFilter")
    public ResponseDTO<EmptyBody> updateDataFilter(@RequestBody @Validated RequestDTO<UpdateDataFilterReq> reqDto) {
        return sysDataFilterService.updateDataFilter(reqDto);
    }

    /**
     * 删除数据过滤规则
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    @PostMapping("/deleteDataFilter")
    public ResponseDTO<EmptyBody> deleteDataFilter(@RequestBody @Validated RequestDTO<DataFilterIdReq> reqDto) {
        return sysDataFilterService.deleteDataFilter(reqDto);
    }

    /**
     * 获取数据过滤规则详情
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    @PostMapping("/getDataFilterDetail")
    public ResponseDTO<DataFilterVO> getDataFilterDetail(@RequestBody @Validated RequestDTO<DataFilterIdReq> reqDto) {
        return sysDataFilterService.getDataFilterDetail(reqDto);
    }

    /**
     * 获取实体类字段列表
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    @PostMapping("/getEntityFields")
    public ResponseDTO<EntityFieldsRsp> getEntityFields(@RequestBody @Validated RequestDTO<GetEntityFieldsReq> reqDto) {
        EntityFieldsRsp rsp = sysDataFilterService.getEntityFields(reqDto.getBody().getEntityClass());
        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 获取已注册实体列表
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    @PostMapping("/getEntityList")
    public ResponseDTO<EntityListRsp> getEntityList(@RequestBody RequestDTO<EmptyBody> reqDto) {
        EntityListRsp rsp = sysDataFilterService.getEntityList();
        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 获取匹配类型选项
     * 根据过滤对象和关联关系返回可用的匹配类型
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    @PostMapping("/getMatchTypes")
    public ResponseDTO<MatchTypesRsp> getMatchTypes(@RequestBody @Validated RequestDTO<GetMatchTypesReq> reqDto) {
        GetMatchTypesReq req = reqDto.getBody();
        MatchTypesRsp rsp = sysDataFilterService.getMatchTypes(req.getTableName(), req.getRelationName());
        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 刷新数据权限缓存
     *
     * @param reqDto 请求DTO
     * @return 响应DTO
     */
    @PostMapping("/refreshCache")
    public ResponseDTO<EmptyBody> refreshCache(@RequestBody RequestDTO<EmptyBody> reqDto) {
        return sysDataFilterService.refreshCache();
    }
}