package com.blink.base.mapper;

import com.blink.base.entity.SysDictDataDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.base.dto.req.QuerySysDictDataReq;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 字典数据表 Mapper 接口
 * </p>
 *
 * @author blink
 * @since 2026-03-07
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictDataDO> {

    /**
     * 查询字典数据列表
     *
     * @param reqDTO 查询请求参数
     * @return 字典数据列表
     */
    List<SysDictDataDO> findSysDictDataList(QuerySysDictDataReq reqDTO);

    /**
     * 根据字典类型编码查询字典数据列表
     *
     * @param dictType 字典类型编码
     * @return 字典数据列表
     */
    List<SysDictDataDO> selectDictDataByType(String dictType);

    /**
     * 根据字典类型编码和语言查询字典数据列表
     *
     * @param dictType 字典类型编码
     * @param locale 语言标识
     * @return 字典数据列表
     */
    List<SysDictDataDO> selectDictDataByTypeAndLocale(String dictType, String locale);

    /**
     * 根据字典类型编码列表和语言批量查询字典数据
     *
     * @param dictTypes 字典类型编码列表
     * @param locale 语言标识
     * @return 字典数据列表
     */
    List<SysDictDataDO> selectDictDataByTypesAndLocale(
        @Param("dictTypes") List<String> dictTypes,
        @Param("locale") String locale
    );
}
