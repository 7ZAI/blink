package com.blink.framework.common.data;


import java.io.Serializable;
import java.util.List;

/**
 * PageDTO
 * 用于DTO继承
 **/
public class PageDTO<T> implements Serializable {


    /**
     * 是否执行导出操作
     */
    /**
     * 页码，默认是第一页
     */
    private int pageNum = 1;
    /**
     * 每页显示的记录数，默认是10
     */
    private int pageSize = 10;
    /**
     * 总记录数
     * 总记录数，设置为“-1”表示不查询总数
     */
    private int total;

    /**
     * 总页数
     */
    private int pages = 1;

    /**
     * 排序的字段(数据库字段) 以及 升降序方式
     * asc、 desc  默认为升序排序
     * order by 多个字段时，用逗号分隔每一个字段，如果字段不指明排序方式，默认是增序。
     * 注意：
     * 1、属性名按照生成的DO，驼峰
     * 2、排序 asc desc 必须小写
     */
    private String orderBy;

    /**
     * 当前页对应的数据中的记录
     */
    private List<T> rows;

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    @Override
    public String toString() {
        return "PageDTO{" +
                "pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                ", total=" + total +
                ", pages=" + pages +
                ", orderBy='" + orderBy + '\'' +
                ", rows=" + rows +
                '}';
    }
}
