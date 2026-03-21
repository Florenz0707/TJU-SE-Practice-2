package cn.edu.tju.order.controller;

import cn.edu.tju.core.model.HttpResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner/order")
@Tag(name = "订单内部接口", description = "order-service 基础联通接口")
public class OrderInnerController {

  @GetMapping("/ping")
  @Operation(summary = "联通检查", description = "用于服务启动后快速验证内部路由可用性")
  public HttpResult<String> ping() {
    return HttpResult.success("pong");
  }
}
