package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/httpRest")
@Tag(name = "统一结果测试")
public class HttpRestController {

    @Operation(summary = "通用返回成功（没有返回结果）", method = "GET")
    @GetMapping("/success")
    public HttpResult success(){
        return HttpResult.success();
    }

    @Operation(summary = "返回成功（有返回结果）", method = "GET")
    @GetMapping("/successWithData")
    public HttpResult successWithData(){
        return HttpResult.success("风尘博客");
    }

    @Operation(summary = "通用返回失败", method = "GET")
    @GetMapping("/failure")
    public HttpResult failure(){
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND);
    }

}
