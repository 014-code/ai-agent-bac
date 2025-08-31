package com.mashang.bac.web.utils;
import com.mashang.bac.web.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 响应结果工具类
 */
@Data
@NoArgsConstructor//无参
@AllArgsConstructor//全参
@Component//其它类注解
@Schema(name = "ResultTUtil")
public class ResultTUtil<T> {

    @Schema(description = "响应消息")
    private String msg;//响应消息
    @Schema(description = "响应状态码")
    private Integer code;//响应状态码
    @Schema(description = "响应内容")
    private T data;//响应内容

    //响应为成功
    public ResultTUtil<T> success(String str) {
        return (new ResultTUtil<T>(str, 0, null));
    }

    //响应为成功的重载方法
    public ResultTUtil<T> success(String str, T object) {
        return (new ResultTUtil(str, 0, object));
    }

    //响应为失败
    public ResultTUtil<T> error(String str) {
        return (new ResultTUtil(str, 500, null));
    }

    public ResultTUtil<T> error(String str, T obj) {
        return (new ResultTUtil(str, 500, obj));
    }

    //响应为失败
    public ResultTUtil<T> errorR(String str) {
        return (new ResultTUtil(str, 500, null));
    }

    //响应为失败-抛出自定义状态码
    public ResultTUtil<T> errorCode(ErrorCode errorCode, String str) {
        return (new ResultTUtil(str, errorCode.getCode(), null));
    }

    //用于增删改的响应方法-其中b为增删改结果的布尔值
    public ResultTUtil<T> to(String msg, boolean b) {
        return b ? new ResultTUtil<T>().success(msg) : new ResultTUtil<T>().error(msg);
    }

    //用于增删改的响应方法-其中b为增删改结果的布尔值
    public ResultTUtil<T> to(String msg, boolean b, Object obj) {
        return b ? new ResultTUtil<T>().success(msg, (T) obj) : new ResultTUtil<T>().error(msg, (T) obj);
    }


}
