package com.blink.framework.common.data;

import java.util.List;
/**
 * PageDTO
 * 用于DTO继承
 **/
public class PageDTO<T> extends Page {

    /**
     * 当前页对应的数据中的记录
     */
    private List<T> rows;

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    @Override
    public String toString() {
        return "PageDTO{" +
                "pageNum=" + getPageNum() +
                ", pageSize=" + getPageSize() +
                ", total=" + getTotal() +
                ", pages=" + getPages() +
                ", orderBy='" + getOrderBy() + '\'' +
                ", rows=" + rows +
                '}';
    }

    /**
     * 静态工厂方法，创建分页对象
     *
     * @param rows     数据列表
     * @param total    总记录数
     * @param pageNum  当前页
     * @param pageSize 每页大小
     * @param <T>      数据类型
     * @return PageDTO对象
     */
    public static <T> PageDTO<T> of(List<T> rows, int total, Integer pageNum, Integer pageSize) {
        PageDTO<T> pageDTO = new PageDTO<>();
        pageDTO.setRows(rows);
        pageDTO.setTotal(total);
        pageDTO.setPageNum(pageNum != null ? pageNum : 1);
        pageDTO.setPageSize(pageSize != null ? pageSize : 10);
        // 计算总页数
        int size = pageSize != null ? pageSize : 10;
        int pages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        pageDTO.setPages(pages);
        return pageDTO;
    }
}
