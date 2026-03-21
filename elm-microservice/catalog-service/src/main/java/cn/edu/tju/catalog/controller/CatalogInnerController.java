package cn.edu.tju.catalog.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner/catalog")
@Tag(name = "目录内部接口", description = "订单服务等内部调用的商家/菜品查询接口")
public class CatalogInnerController {

  @GetMapping("/business/{businessId}")
  @Operation(summary = "按ID查询商家", description = "阶段4骨架接口，后续补齐字段与实现")
  public HttpResult<Map<String, Object>> getBusinessById(
      @Parameter(description = "商家ID", required = true) @PathVariable("businessId")
          Long businessId) {
    if (businessId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessId CANT BE NULL");
    }
    return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "NOT_IMPLEMENTED");
  }

  @GetMapping("/food/{foodId}")
  @Operation(summary = "按ID查询菜品", description = "阶段4骨架接口，后续补齐字段与实现")
  public HttpResult<Map<String, Object>> getFoodById(
      @Parameter(description = "菜品ID", required = true) @PathVariable("foodId") Long foodId) {
    if (foodId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodId CANT BE NULL");
    }
    return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "NOT_IMPLEMENTED");
  }
}
