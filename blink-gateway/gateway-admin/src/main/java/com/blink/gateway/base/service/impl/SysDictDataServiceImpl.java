package com.blink.gateway.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.gateway.base.dto.req.AddSysDictDataReq;
import com.blink.gateway.base.dto.req.DeleteSysDictDataReq;
import com.blink.gateway.base.dto.req.QuerySysDictDataReq;
import com.blink.gateway.base.dto.req.UpdateSysDictDataReq;
import com.blink.gateway.base.dto.rsp.DictDataMapRsp;
import com.blink.gateway.base.dto.rsp.QuerySysDictDataRsp;
import com.blink.gateway.base.dto.vo.SysDictDataVO;
import com.blink.gateway.base.entity.SysDictDataDO;
import com.blink.gateway.base.mapper.SysDictDataMapper;
import com.blink.gateway.base.service.SysDictDataService;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.blink.gateway.base.constants.BaseErrCodeConstant.*;

/**
 * 字典数据表 服务实现类
 *
 * @author blink
 * @since 2026-03-07
 */
@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class SysDictDataServiceImpl implements SysDictDataService {

    @Resource
    private SysDictDataMapper sysDictDataMapper;

    /**
     * 保存字典数据
     *
     * @param saveParam 保存参数
     * @return 保存后的字典数据 VO
     * @throws BlinkException 业务异常
     */
    @Override
    public SysDictDataVO saveSysDictData(AddSysDictDataReq saveParam) throws BlinkException {
        var sysDictDataDO = new SysDictDataDO();
        BeanUtil.copyProperties(saveParam, sysDictDataDO);

        // 获取locale，优先使用传入值，否则从上下文获取
        String locale = StrUtil.isNotBlank(saveParam.getLocale())
                ? saveParam.getLocale()
                : BlinkRequestContextHolder.getContext().getLanguage();
        if (StrUtil.isBlank(locale)) {
            locale = "zh_cn";
        }
        sysDictDataDO.setLocale(locale);

        // 检查同一字典类型+语言下标签是否重复
        SysDictDataDO existLabel = sysDictDataMapper.selectOne(
                new LambdaQueryWrapper<SysDictDataDO>()
                        .eq(SysDictDataDO::getDictType, sysDictDataDO.getDictType())
                        .eq(SysDictDataDO::getDictLabel, sysDictDataDO.getDictLabel())
                        .eq(SysDictDataDO::getLocale, locale)
        );

        if (Objects.nonNull(existLabel)) {
            BlinkException.throwBusinessException(DICT_LABEL_REPEAT);
        }

        // 检查同一字典类型+语言下值是否重复
        SysDictDataDO existValue = sysDictDataMapper.selectOne(
                new LambdaQueryWrapper<SysDictDataDO>()
                        .eq(SysDictDataDO::getDictType, sysDictDataDO.getDictType())
                        .eq(SysDictDataDO::getDictValue, sysDictDataDO.getDictValue())
                        .eq(SysDictDataDO::getLocale, locale)
        );

        if (Objects.nonNull(existValue)) {
            BlinkException.throwBusinessException(DICT_VALUE_REPEAT);
        }

        sysDictDataMapper.insert(sysDictDataDO);

        log.info("[SysDictData] 新增字典数据成功 | dictCode: {}, dictType: {}, dictLabel: {}, dictValue: {}, locale: {}",
                sysDictDataDO.getDictCode(), sysDictDataDO.getDictType(), sysDictDataDO.getDictLabel(),
                sysDictDataDO.getDictValue(), locale);

        // 返回保存后的 VO
        SysDictDataVO vo = new SysDictDataVO();
        BeanUtil.copyProperties(sysDictDataDO, vo);
        return vo;
    }

    /**
     * 删除字典数据
     *
     * @param deleteParam 删除参数
     * @throws BlinkException 业务异常
     */
    @Override
    public void deleteSysDictData(DeleteSysDictDataReq deleteParam) throws BlinkException {
        if (Boolean.TRUE.equals(deleteParam.getBatchDelete())) {
            sysDictDataMapper.deleteByIds(deleteParam.getIdList());
            log.info("[SysDictData] 批量删除字典数据成功 | dictCodes: {}", deleteParam.getIdList());
        } else {
            sysDictDataMapper.deleteById(deleteParam.getDeleteId());
            log.info("[SysDictData] 删除字典数据成功 | dictCode: {}", deleteParam.getDeleteId());
        }
    }

    /**
     * 更新字典数据
     *
     * @param updateParam 更新参数
     * @return 更新后的字典数据 VO
     * @throws BlinkException 业务异常
     */
    @Override
    public SysDictDataVO modifySysDictData(UpdateSysDictDataReq updateParam) throws BlinkException {
        SysDictDataDO oldOne = sysDictDataMapper.selectById(updateParam.getDictCode());

        // 字典数据不存在
        if (Objects.isNull(oldOne)) {
            BlinkException.throwBusinessException(DICT_DATA_NOT_EXIST);
        }

        // 获取locale，优先使用传入值，否则使用原值
        String locale = StrUtil.isNotBlank(updateParam.getLocale())
                ? updateParam.getLocale()
                : oldOne.getLocale();

        // 检查同一字典类型+语言下标签是否重复（排除自身）
        if (StrUtil.isNotBlank(updateParam.getDictLabel())) {
            SysDictDataDO existLabel = sysDictDataMapper.selectOne(
                    new LambdaQueryWrapper<SysDictDataDO>()
                            .eq(SysDictDataDO::getDictType, oldOne.getDictType())
                            .eq(SysDictDataDO::getDictLabel, updateParam.getDictLabel())
                            .eq(SysDictDataDO::getLocale, locale)
                            .ne(SysDictDataDO::getDictCode, updateParam.getDictCode())
            );

            if (Objects.nonNull(existLabel)) {
                BlinkException.throwBusinessException(DICT_LABEL_REPEAT);
            }
        }

        // 检查同一字典类型+语言下值是否重复（排除自身）
        if (StrUtil.isNotBlank(updateParam.getDictValue())) {
            SysDictDataDO existValue = sysDictDataMapper.selectOne(
                    new LambdaQueryWrapper<SysDictDataDO>()
                            .eq(SysDictDataDO::getDictType, oldOne.getDictType())
                            .eq(SysDictDataDO::getDictValue, updateParam.getDictValue())
                            .eq(SysDictDataDO::getLocale, locale)
                            .ne(SysDictDataDO::getDictCode, updateParam.getDictCode())
            );

            if (Objects.nonNull(existValue)) {
                BlinkException.throwBusinessException(DICT_VALUE_REPEAT);
            }
        }

        var sysDictDataDO = new SysDictDataDO();
        BeanUtil.copyProperties(updateParam, sysDictDataDO);

        sysDictDataMapper.updateById(sysDictDataDO);

        log.info("[SysDictData] 更新字典数据成功 | dictCode: {}, dictType: {}, dictLabel: {}, dictValue: {}",
                sysDictDataDO.getDictCode(), sysDictDataDO.getDictType(), sysDictDataDO.getDictLabel(),
                sysDictDataDO.getDictValue());

        // 返回更新后的 VO
        SysDictDataVO vo = new SysDictDataVO();
        BeanUtil.copyProperties(sysDictDataDO, vo);
        return vo;
    }

    /**
     * 查询字典数据列表
     *
     * @param queryParam 查询参数
     * @return 字典数据列表
     * @throws BlinkException 业务异常
     */
    @Override
    public QuerySysDictDataRsp getSysDictDataList(QuerySysDictDataReq queryParam) throws BlinkException {
        var pageRsp = new QuerySysDictDataRsp();
        QuerySysDictDataRsp result = PageUtils.queryPage(queryParam, () -> sysDictDataMapper.findSysDictDataList(queryParam), pageRsp);
        return result;
    }

    /**
     * 根据字典类型编码获取字典数据列表
     *
     * @param dictType 字典类型编码
     * @return 字典数据列表
     * @throws BlinkException 业务异常
     */
    @Override
    public List<SysDictDataVO> getDictDataByType(String dictType) throws BlinkException {
        // 从上下文获取当前语言
        String locale = BlinkRequestContextHolder.getContext().getLanguage();
        if (StrUtil.isBlank(locale)) {
            locale = "zh_cn";
        }

        List<SysDictDataDO> dictDataList = sysDictDataMapper.selectDictDataByTypeAndLocale(dictType, locale);

        List<SysDictDataVO> voList = new ArrayList<>();
        for (SysDictDataDO dictDataDO : dictDataList) {
            SysDictDataVO vo = new SysDictDataVO();
            BeanUtil.copyProperties(dictDataDO, vo);
            voList.add(vo);
        }

        return voList;
    }

    /**
     * 批量根据字典类型编码获取字典数据
     *
     * @param dictTypes 字典类型编码列表
     * @return 字典数据Map
     * @throws BlinkException 业务异常
     */
    @Override
    public DictDataMapRsp getDictDataByTypes(List<String> dictTypes) throws BlinkException {
        // 从上下文获取当前语言
        String locale = BlinkRequestContextHolder.getContext().getLanguage();
        if (StrUtil.isBlank(locale)) {
            locale = "zh_cn";
        }

        // 查询所有字典数据
        List<SysDictDataDO> dictDataList = sysDictDataMapper.selectDictDataByTypesAndLocale(dictTypes, locale);

        // 按dictType分组
        Map<String, List<DictDataMapRsp.DictDataItem>> dictDataMap = dictDataList.stream()
            .collect(Collectors.groupingBy(
                SysDictDataDO::getDictType,
                Collectors.mapping(this::convertToDictDataItem, Collectors.toList())
            ));

        DictDataMapRsp rsp = new DictDataMapRsp();
        rsp.setDictDataMap(dictDataMap);

        log.info("[SysDictData] 批量获取字典数据成功 | dictTypes: {}, locale: {}", dictTypes, locale);
        return rsp;
    }

    /**
     * 将DO转换为DictDataItem
     *
     * @param dictDataDO 字典数据DO
     * @return DictDataItem
     */
    private DictDataMapRsp.DictDataItem convertToDictDataItem(SysDictDataDO dictDataDO) {
        DictDataMapRsp.DictDataItem item = new DictDataMapRsp.DictDataItem();
        item.setDictValue(dictDataDO.getDictValue());
        item.setDictLabel(dictDataDO.getDictLabel());
        item.setListClass(dictDataDO.getListClass());
        item.setIsDefault(dictDataDO.getIsDefault());
        return item;
    }
}
