package com.mri.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一接口响应")
public record ApiResult<T>(
        @Schema(description = "业务是否成功") boolean success,
        @Schema(description = "业务状态码") String code,
        @Schema(description = "提示信息") String message,
        @Schema(description = "响应数据") T data
) {
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, "0", "success", data);
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<>(true, "0", "success", null);
    }

    public static ApiResult<Void> fail(String code, String message) {
        return new ApiResult<>(false, code, message, null);
    }
}
