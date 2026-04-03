package com.blink.base.service;

import com.blink.base.dto.req.AddSysDictTypeReq;
import com.blink.base.dto.req.DeleteSysDictTypeReq;
import com.blink.base.dto.req.QuerySysDictTypeReq;
import com.blink.base.dto.req.UpdateSysDictTypeReq;
import com.blink.base.dto.rsp.QuerySysDictTypeRsp;
import com.blink.base.dto.vo.SysDictTypeVO;
import com.blink.framework.common.exception.BlinkException;

/**
 * 字典类型表 服务类
 *
 * @author blink
 * @since 2025-03-07
 */
public interface SysDictTypeService {

    /**
     * 保存字典类型
     *
     * @param saveParam 保存参数
     * @return 保存后的字典类型 VO
     * @throws BlinkException 业务异常
     */
    SysDictTypeVO saveSysDictType(AddSysDictTypeReq saveParam) throws BlinkException;

    /**
     * 删除字典类型
     *
     * @param deleteParam 删除参数
     * @throws BlinkException 业务异常
     */
    void deleteSysDictType(DeleteSysDictTypeReq deleteParam) throws BlinkException;

    /**
     * 更新字典类型
     *
     * @param updateParam 更新参数
     * @return 更新后的字典类型 VO
     * @throws BlinkException 业务异常
     */
    SysDictTypeVO modifySysDictType(UpdateSysDictTypeReq updateParam) throws BlinkException;

    /**
     * 查询字典类型列表
     *
     * @param queryParam 查询参数
     * @return 分页结果
     * @throws BlinkException 业务异常
     */
    QuerySysDictTypeRsp getSysDictTypeList(QuerySysDictTypeReq queryParam) throws BlinkException;

    /**
     * 根据字典类型编码查询字典类型
     *
     * @param dictType 字典类型编码
     * @return 字典类型VO
     * @throws BlinkException 业务异常
     */
    SysDictTypeVO getSysDictTypeByType(String dictType) throws BlinkException;

    /**
     * 根据字典主键id查询字典类型
     *
     * @param dictId 字典主键id
     * @return 字典类型VO
     * @throws BlinkException 业务异常
     */
    SysDictTypeVO getSysDictTypeById(Integer dictId) throws BlinkException;
}
