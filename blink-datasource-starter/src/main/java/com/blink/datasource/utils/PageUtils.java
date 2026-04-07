package com.blink.datasource.utils;

import cn.hutool.core.bean.BeanUtil;
import com.blink.datasource.function.ExecuteFunction;
import com.blink.datasource.function.ListQueryFunction;
import com.blink.datasource.function.OrderFieldConverter;
import com.blink.framework.common.data.PageDTO;
import com.blink.framework.common.data.Page;
import com.blink.framework.common.record.PageRecord;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.function.Supplier;


/**
 * 根据 pageHelper的使用封装 分页工具类
 * ExecuteFunction 为了隐藏 ISelect的引用 让外部应用不必显式依赖pageHelper包
 *
 * @author binblink
 */
public class PageUtils {

    /**
     * 执行分页查询
     *
     * @param page 请求参数
     * @param selectSql ExecuteFunction 传入执行语句 如 mapper.selectXXX();
     * @param r 响应实际业务对象
     * @return 响应实体对象
     * @param <P> 分页参数类型
     * @param <T> sql 返回的实体类型
     * @param <R> 返回类型
     */
    public static  <P extends Page,T,R extends PageDTO<T>> R queryPage(P page, ExecuteFunction selectSql, R r){

        // 设置排序
        if (page.getOrderBy() != null && !page.getOrderBy().isEmpty()) {
            PageHelper.orderBy(page.getOrderBy());
        }

        PageInfo<T> pageInfo  =  PageHelper.startPage(page.getPageNum(), page.getPageSize(),!(page.getTotal()==-1))
                .doSelectPageInfo(selectSql::execute);

        //分页结果设置进入真正的返回值中
        BeanUtil.copyProperties(pageInfo,r);
        //设置值进入row
        r.setRows(pageInfo.getList());

        return r;
    }

    /**
     * 执行分页查询（record类型版）
     *
     * @param queryParam 查询参数（record）
     * @param selectSql sql执行函数
     * @return Record 类型
     * @param <T> sql返回的类型
     */
    public static <T> PageRecord<T> queryPage(PageRecord<?> queryParam,ExecuteFunction selectSql){

        // 设置排序
        if (queryParam.orderBy() != null && !queryParam.orderBy().isEmpty()) {
            PageHelper.orderBy(queryParam.orderBy());
        }


        PageInfo<T> pageInfo  =  PageHelper.startPage(queryParam.pageNum(), queryParam.pageSize(),!(queryParam.total()==-1))
                .doSelectPageInfo(selectSql::execute);

        return new PageRecord<>(pageInfo.getPageNum(),pageInfo.getPageSize(),
                (int) pageInfo.getTotal(),pageInfo.getPages(), queryParam.orderBy(),pageInfo.getList());
    }

    /**
     * 执行自定义分页查询（支持自定义 count 和 list 查询）
     * 适用于复杂查询场景，如多表关联、子查询等
     *
     * @param page 分页参数
     * @param countQuery count 查询函数，返回总记录数
     * @param listQuery 列表查询函数，返回分页数据
     * @param r 响应对象
     * @return 响应实体对象
     * @param <T> sql 返回的实体类型
     * @param <R> 返回类型
     * @param <P> 分页参数类型
     */
    public static <T, R extends PageDTO<T>, P extends Page> R queryPageCustom(
            P page,
            Supplier<Long> countQuery,
            ListQueryFunction<T> listQuery,
            R r) {

        // 设置排序
        if (page.getOrderBy() != null && !page.getOrderBy().isEmpty()) {
            PageHelper.orderBy(page.getOrderBy());
        }

        // 执行 count 查询
        long total = countQuery.get();

        // 计算总页数
        int pageSize = page.getPageSize();
        int pages = (int) ((total + pageSize - 1) / pageSize);

        // 如果没有数据，直接返回空结果
        if (total == 0) {
            r.setPageNum(page.getPageNum());
            r.setPageSize(pageSize);
            r.setTotal(0);
            r.setPages(0);
            r.setRows(List.of());
            return r;
        }

        // 计算 offset
        int offset = (page.getPageNum() - 1) * pageSize;

        // 执行分页查询
        PageHelper.offsetPage(offset, pageSize, false);

        List<T> list = listQuery.execute();

        // 设置分页结果
        r.setPageNum(page.getPageNum());
        r.setPageSize(pageSize);
        r.setTotal((int) total);
        r.setPages(pages);
        r.setRows(list);

        return r;
    }

    /**
     * 执行自定义分页查询（record类型版）
     * 支持自定义 count 和 list 查询
     *
     * @param queryParam 查询参数
     * @param countQuery count 查询函数
     * @param listQuery 列表查询函数
     * @return 分页结果
     * @param <T> 列表元素类型
     */
    public static <T> PageRecord<T> queryPageCustom(
            PageRecord<?> queryParam,
            Supplier<Long> countQuery,
            ListQueryFunction<T> listQuery) {

        // 设置排序
        if (queryParam.orderBy() != null && !queryParam.orderBy().isEmpty()) {
            PageHelper.orderBy(queryParam.orderBy());
        }

        // 执行 count 查询
        long total = countQuery.get();

        // 计算总页数
        int pageSize = queryParam.pageSize();
        int pages = (int) ((total + pageSize - 1) / pageSize);

        // 如果没有数据，直接返回空结果
        if (total == 0) {
            return new PageRecord<>(queryParam.pageNum(), pageSize, 0, 0, queryParam.orderBy(), List.of());
        }

        // 计算 offset
        int offset = (queryParam.pageNum() - 1) * pageSize;

        // 执行分页查询
        PageHelper.offsetPage(offset, pageSize, false);

        List<T> list = listQuery.execute();

        return new PageRecord<>(queryParam.pageNum(), pageSize, (int) total, pages, queryParam.orderBy(), list);
    }

    /**
     * 转换前端传递的排序字段为数据库字段
     * 因为前端传递的字段名往往和数据库的字段不匹配，需要转换
     *
     * <p>排序格式支持：
     * <ul>
     *   <li>单字段排序：{@code fieldName asc} 或 {@code fieldName desc}</li>
     *   <li>多字段排序：{@code field1 asc, field2 desc}</li>
     * </ul>
     *
     * <p>使用示例：
     * <pre>
     * // 使用驼峰转下划线转换器
     * String dbOrderBy = PageUtils.transformOrderBy("userName asc, createTime desc",
     *     field -> CaseUtil.toUnderlineCase(field));
     * // 结果: "user_name asc, create_time desc"
     *
     * // 使用自定义映射转换器
     * Map<String, String> mapping = Map.of("userName", "u.name", "createTime", "u.create_time");
     * String dbOrderBy = PageUtils.transformOrderBy("userName desc", mapping::get);
     * // 结果: "u.name desc"
     *
     * // 不需要转换时返回原值
     * String dbOrderBy = PageUtils.transformOrderBy("user_name asc", field -> field);
     * // 结果: "user_name asc"
     * </pre>
     *
     * @param page 前端传递的原始查询dto
     * @param converter 字段名转换器，接收前端字段名返回数据库字段名
     * @return 转换后的数据库排序字符串，如果输入为空则返回空字符串
     */
    public static <P extends Page> P transformOrderBy(P page, OrderFieldConverter converter) {

        String originOrderBy = page.getOrderBy();
        // 设置排序
        if (originOrderBy == null || originOrderBy.isEmpty()) {
            return page;
        }

        // 分割多个排序字段（格式：field1 asc, field2 desc）
        String[] orderParts = originOrderBy.split(",");

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < orderParts.length; i++) {
            String orderPart = orderParts[i].trim();
            if (orderPart.isEmpty()) {
                continue;
            }

            // 分割字段名和排序方向
            String[] fieldAndDirection = orderPart.split("\\s+");
            String frontendField = fieldAndDirection[0];

            // 使用转换器转换字段名
            String dbField = converter.convert(frontendField);

            // 如果转换器返回 null，保留原字段名
            if (dbField == null || dbField.trim().isEmpty()) {
                dbField = frontendField;
            }

            // 拼接转换后的排序片段
            if (!result.isEmpty()) {
                result.append(", ");
            }
            result.append(dbField);

            // 添加排序方向（asc 或 desc）
            if (fieldAndDirection.length > 1) {
                String direction = fieldAndDirection[1].toLowerCase();
                if ("asc".equals(direction) || "desc".equals(direction)) {
                    result.append(" ").append(direction);
                }
            }
        }
        //设置转换后的值
        page.setOrderBy(result.toString());
        return page;
    }

}
