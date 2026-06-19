package com.mri.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页响应")
public record PageResult<T>(
        @Schema(description = "当前页码") long page,
        @Schema(description = "每页条数") long size,
        @Schema(description = "总记录数") long total,
        @Schema(description = "列表数据") List<T> records
) {
    public static <T> PageResult<T> of(long page, long size, long total, List<T> records) {
        return new PageResult<>(page, size, total, records);
    }
}
