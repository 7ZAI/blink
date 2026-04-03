package com.blink.gateway.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blink.gateway.base.dto.req.AddSysDictTypeReq;
import com.blink.gateway.base.dto.req.DeleteSysDictTypeReq;
import com.blink.gateway.base.dto.req.QuerySysDictTypeReq;
import com.blink.gateway.base.dto.req.UpdateSysDictTypeReq;
import com.blink.gateway.base.dto.rsp.QuerySysDictTypeRsp;
import com.blink.gateway.base.dto.vo.SysDictTypeVO;
import com.blink.gateway.base.entity.SysDictTypeDO;
import com.blink.gateway.base.mapper.SysDictTypeMapper;
import com.blink.gateway.base.service.SysDictTypeService;
import com.blink.datasource.utils.PageUtils;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.blink.gateway.base.constants.BaseErrCodeConstant.DICT_TYPE_NOT_EXIST;
import static com.blink.gateway.base.constants.BaseErrCodeConstant.DICT_TYPE_REPEAT;

/**
 * 字典类型表 服务实现类
 *
 * @author blink
 * @since 2025-03-07
 */
@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class SysDictTypeServiceImpl implements SysDictTypeService {

    @Resource
    private SysDictTypeMapper sysDictTypeMapper;

    /**
     * 保存字典类型
     *
     * @param saveParam 保存参数
     * @return 保存后的字典类型 VO
     * @throws BlinkException 业务异常
     */
    @Override
    public SysDictTypeVO saveSysDictType(AddSysDictTypeReq saveParam) throws BlinkException {
        // 获取locale，优先使用传入值，否则从上下文获取
        String locale = StrUtil.isNotBlank(saveParam.getLocale())
                ? saveParam.getLocale()
                : BlinkRequestContextHolder.getContext().getLanguage();
        if (StrUtil.isBlank(locale)) {
            locale = "zh_cn";
        }

        // 检查同一语言下字典类型编码是否重复
        SysDictTypeDO existType = sysDictTypeMapper.selectOne(
                new LambdaQueryWrapper<SysDictTypeDO>()
                        .eq(SysDictTypeDO::getDictType, saveParam.getDictType())
                        .eq(SysDictTypeDO::getLocale, locale)
        );

        if (Objects.nonNull(existType)) {
            BlinkException.throwBusinessException(DICT_TYPE_REPEAT);
        }

        // 转换并保存
        SysDictTypeDO sysDictTypeDO = new SysDictTypeDO();
        BeanUtil.copyProperties(saveParam, sysDictTypeDO);
        sysDictTypeDO.setLocale(locale);
        sysDictTypeMapper.insert(sysDictTypeDO);

        log.info("[SysDictType] 新增字典类型成功 | dictId: {}, dictType: {}, dictName: {}, locale: {}",
                sysDictTypeDO.getDictId(), sysDictTypeDO.getDictType(), sysDictTypeDO.getDictName(), locale);

        // 返回保存后的 VO
        SysDictTypeVO vo = new SysDictTypeVO();
        BeanUtil.copyProperties(sysDictTypeDO, vo);
        return vo;
    }

    /**
     * 删除字典类型
     *
     * @param deleteParam 删除参数
     * @throws BlinkException 业务异常
     */
    @Override
    public void deleteSysDictType(DeleteSysDictTypeReq deleteParam) throws BlinkException {
        if (Boolean.TRUE.equals(deleteParam.getBatchDelete())) {
            // 批量删除
            sysDictTypeMapper.deleteByIds(deleteParam.getIdList());
            log.info("[SysDictType] 批量删除字典类型成功 | dictIds: {}", deleteParam.getIdList());
        } else {
            // 单个删除
            sysDictTypeMapper.deleteById(deleteParam.getDeleteId());
            log.info("[SysDictType] 删除字典类型成功 | dictId: {}", deleteParam.getDeleteId());
        }
    }

    /**
     * 更新字典类型
     *
     * @param updateParam 更新参数
     * @return 更新后的字典类型 VO
     * @throws BlinkException 业务异常
     */
    @Override
    public SysDictTypeVO modifySysDictType(UpdateSysDictTypeReq updateParam) throws BlinkException {
        // 检查字典类型是否存在
        SysDictTypeDO oldOne = sysDictTypeMapper.selectById(updateParam.getDictId());
        if (Objects.isNull(oldOne)) {
            BlinkException.throwBusinessException(DICT_TYPE_NOT_EXIST);
        }

        // 获取locale，优先使用传入值，否则使用原值
        String locale = StrUtil.isNotBlank(updateParam.getLocale())
                ? updateParam.getLocale()
                : oldOne.getLocale();

        // 如果修改了字典类型编码，检查同一语言下是否重复
        if (StrUtil.isNotBlank(updateParam.getDictType())
                && !updateParam.getDictType().equals(oldOne.getDictType())) {
            SysDictTypeDO existType = sysDictTypeMapper.selectOne(
                    new LambdaQueryWrapper<SysDictTypeDO>()
                            .eq(SysDictTypeDO::getDictType, updateParam.getDictType())
                            .eq(SysDictTypeDO::getLocale, locale)
            );
            if (Objects.nonNull(existType)) {
                BlinkException.throwBusinessException(DICT_TYPE_REPEAT);
            }
        }

        // 转换并更新
        SysDictTypeDO sysDictTypeDO = new SysDictTypeDO();
        BeanUtil.copyProperties(updateParam, sysDictTypeDO);
        sysDictTypeMapper.updateById(sysDictTypeDO);

        log.info("[SysDictType] 更新字典类型成功 | dictId: {}, dictType: {}, dictName: {}",
                sysDictTypeDO.getDictId(), sysDictTypeDO.getDictType(), sysDictTypeDO.getDictName());

        // 返回更新后的 VO
        SysDictTypeVO vo = new SysDictTypeVO();
        BeanUtil.copyProperties(sysDictTypeDO, vo);
        return vo;
    }

    /**
     * 查询字典类型列表
     *
     * @param queryParam 查询参数
     * @return 分页结果
     * @throws BlinkException 业务异常
     */
    @Override
    public QuerySysDictTypeRsp getSysDictTypeList(QuerySysDictTypeReq queryParam) throws BlinkException {
        QuerySysDictTypeRsp pageRsp = new QuerySysDictTypeRsp();
        QuerySysDictTypeRsp result = PageUtils.queryPage(
                queryParam,
                () -> sysDictTypeMapper.findSysDictTypeList(queryParam),
                pageRsp
        );
        return result;
    }

    /**
     * 根据字典类型编码查询字典类型
     *
     * @param dictType 字典类型编码
     * @return 字典类型VO
     * @throws BlinkException 业务异常
     */
    @Override
    public SysDictTypeVO getSysDictTypeByType(String dictType) throws BlinkException {
        SysDictTypeDO dictTypeDO = sysDictTypeMapper.selectOne(
                new LambdaQueryWrapper<SysDictTypeDO>()
                        .eq(SysDictTypeDO::getDictType, dictType)
        );

        if (Objects.isNull(dictTypeDO)) {
            return null;
        }

        SysDictTypeVO vo = new SysDictTypeVO();
        BeanUtil.copyProperties(dictTypeDO, vo);
        return vo;
    }

    /**
     * 根据字典主键id查询字典类型
     *
     * @param dictId 字典主键id
     * @return 字典类型VO
     * @throws BlinkException 业务异常
     */
    @Override
    public SysDictTypeVO getSysDictTypeById(Integer dictId) throws BlinkException {
        SysDictTypeDO dictTypeDO = sysDictTypeMapper.selectById(dictId);

        if (Objects.isNull(dictTypeDO)) {
            return null;
        }

        SysDictTypeVO vo = new SysDictTypeVO();
        BeanUtil.copyProperties(dictTypeDO, vo);
        return vo;
    }
}
