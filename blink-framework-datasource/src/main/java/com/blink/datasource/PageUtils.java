package com.blink.datasource;

import cn.hutool.core.bean.BeanUtil;
import com.blink.framework.common.data.ExecuteFunction;
import com.blink.framework.common.data.PageDTO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;


/**
 * 根据 pageHelper的使用封装 分页工具类
 * ExecuteFunction 为了隐藏 ISelect的引用 让使用者不必依赖pageHelper包
 */
public class PageUtils {

    /**
     * 执行分页查询 直接返回最终
     * @param page 请求参数
     * @param selectSql ExecuteFunction 传入执行语句 如 mapper.selectXXX();
     * @param r 响应实际业务对象
     * @return 响应实体对象
     * @param <T> sql 返回的实体类型
     * @param <R> 返回类型
     * @param <P> 分页参数类型
     */
    public static  <T,R extends PageDTO<T>,P extends PageDTO<T>> R queryPage(P page, ExecuteFunction selectSql, R r){

        PageHelper.orderBy(page.getOrderBy());
        PageInfo<T> pageInfo  =  PageHelper.startPage(page.getPageNum(), page.getPageSize(),!(page.getTotal()==-1))
                .doSelectPageInfo(selectSql::execute);
        //分页结果设置进入真正的返回值中
        BeanUtil.copyProperties(pageInfo,r);
        //设置值进入row
        r.setRows(pageInfo.getList());

        return r;
    }




}
