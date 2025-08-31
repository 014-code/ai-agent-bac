package com.mashang.bac.web.utils;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 响应结果工具类
 */
@Data
@NoArgsConstructor//无参
@AllArgsConstructor//全参
@Component//其它类注解
@Schema(name = "RowsTUtil")
public class RowsTUtil<T> {

    @Schema(description = "响应状态码")
    private Integer code;//响应状态码
    @Schema(description = "响应消息")
    private String msg;//响应消息
    @Schema(description = "响应内容")
    private List<T> rows;//响应内容
    @Schema(description = "总记录数")
    private Long total;//总记录数


    //响应为成功
    public RowsTUtil<T> success(String str, Long cont, List<T> object) {
        return (new RowsTUtil<T>(0, str, object, cont));

    }

    //响应为失败
    public RowsTUtil<T> error(String str, Long cont, List<T> object) {
        return (new RowsTUtil<T>(500, str, object, cont));

    }
}
