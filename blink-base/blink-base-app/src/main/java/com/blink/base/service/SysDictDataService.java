package com.blink.base.service;

import com.blink.base.dto.req.AddSysDictDataReq;
import com.blink.base.dto.req.DeleteSysDictDataReq;
import com.blink.base.dto.req.QuerySysDictDataReq;
import com.blink.base.dto.req.UpdateSysDictDataReq;
import com.blink.base.dto.rsp.DictDataMapRsp;
import com.blink.base.dto.rsp.QuerySysDictDataRsp;
import com.blink.base.dto.vo.SysDictDataVO;
import com.blink.framework.common.exception.BlinkException;

import java.util.List;

/**
 * <p>
 * 字典数据表 服务类
 * </p>
 *
 * @author blink
 * @since 2026-03-07
 */
public interface SysDictDataService {

    /**
     * 保存字典数据
     *
     * @param saveParam 保存参数
     * @return 保存后的字典数据 VO
     * @throws BlinkException 业务异常
     */
    SysDictDataVO saveSysDictData(AddSysDictDataReq saveParam) throws BlinkException;

    /**
     * 删除字典数据
     *
     * @param deleteParam 删除参数
     * @throws BlinkException 业务异常
     */
    void deleteSysDictData(DeleteSysDictDataReq deleteParam) throws BlinkException;

    /**
     * 更新字典数据
     *
     * @param updateParam 更新参数
     * @return 更新后的字典数据 VO
     * @throws BlinkException 业务异常
     */
    SysDictDataVO modifySysDictData(UpdateSysDictDataReq updateParam) throws BlinkException;

    /**
     * 查询字典数据列表
     *
     * @param queryParam 查询参数
     * @return 字典数据列表
     * @throws BlinkException 业务异常
     */
    QuerySysDictDataRsp getSysDictDataList(QuerySysDictDataReq queryParam) throws BlinkException;

    /**
     * 根据字典类型编码获取字典数据列表
     *
     * @param dictType 字典类型编码
     * @return 字典数据列表
     * @throws BlinkException 业务异常
     */
    List<SysDictDataVO> getDictDataByType(String dictType) throws BlinkException;

    /**
     * 批量根据字典类型编码获取字典数据
     *
     * @param dictTypes 字典类型编码列表
     * @return 字典数据Map
     * @throws BlinkException 业务异常
     */
    DictDataMapRsp getDictDataByTypes(List<String> dictTypes) throws BlinkException;
}
