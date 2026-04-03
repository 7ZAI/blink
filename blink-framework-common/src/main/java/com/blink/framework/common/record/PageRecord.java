package com.blink.framework.common.record;

import java.io.Serializable;
import java.util.List;
import com.blink.framework.common.record.PageRecord;
/**
 * 分页数据封装 Record类型
 *
 * @param pageNum  当前页面
 * @param pageSize 每页显示数量 默认是10
 * @param total    总记录数
 * @param pages    总页数
 * @param orderBy  排序 aaa desc,bbb ase
 * @param rows     记录集合
 * @param <T>      记录类型
 */
public record PageRecord<T>(

        int pageNum,
        int pageSize,
        int total,
        int pages,
        String orderBy,
        List<T> rows
) implements Serializable {
    /**
     * 默认构造方法，提供默认值
     */
    public PageRecord {
        // 参数验证和默认值设置
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }
        if (pages < 1) {
            pages = 1;
        }
    }

    /**
     * 简化构造方法 - 只提供必要参数，其他使用默认值
     */
    public PageRecord(int pageNum, int pageSize, List<T> rows) {
        this(pageNum, pageSize, 0, 1, null, rows);
    }

    /**
     * 简化构造方法 - 只提供页码和页大小
     */
    public PageRecord(int pageNum, int pageSize) {
        this(pageNum, pageSize, 0, 1, null, null);
    }

    /**
     * 无参构造方法，使用所有默认值
     */
    public PageRecord() {
        this(1, 10, 0, 1, null, null);
    }

    /**
     * 是否执行导出操作 (如果需要的话可以添加)
     */
    // private boolean export;

    /**
     * 计算总页数的便捷方法
     */
    public int calculatePages() {
        if (total <= 0 || pageSize <= 0) {
            return 1;
        }
        return (total + pageSize - 1) / pageSize;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return pageNum > 1;
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return pageNum < pages;
    }
}
