package com.example.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 记录列表 */
    private T records;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int page;

    /** 每页大小 */
    private int size;

    public static <T> PageResult<T> of(T records, long total, int page, int size) {
        return new PageResult<>(records, total, page, size);
    }
}
