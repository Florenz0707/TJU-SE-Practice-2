package cn.edu.tju.food.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.food.model.vo.FoodSnapshotVO;
import cn.edu.tju.food.service.FoodInternalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner/food")
@Tag(name = "食品内部接口", description = "订单和聚合层内部调用的菜品和库存接口")
public class FoodInnerController {
  private final FoodInternalService foodInternalService;

  public FoodInnerController(FoodInternalService foodInternalService) {
    this.foodInternalService = foodInternalService;
  }

  @GetMapping("")
  @Operation(summary = "按商家查询菜品列表", description = "供聚合层回退读取菜品列表")
  public HttpResult<List<FoodSnapshotVO>> getFoodsByBusinessId(
      @Parameter(description = "商家ID", required = true) @RequestParam("businessId") Long businessId) {
    if (businessId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessId CANT BE NULL");
    }
    return HttpResult.success(foodInternalService.getFoodSnapshotsByBusinessId(businessId));
  }

  @GetMapping("/{foodId}")
  @Operation(summary = "按ID查询菜品", description = "查询菜品快照")
  public HttpResult<FoodSnapshotVO> getFoodById(
      @Parameter(description = "菜品ID", required = true) @PathVariable("foodId") Long foodId) {
    if (foodId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodId CANT BE NULL");
    }
    FoodSnapshotVO food = foodInternalService.getFoodSnapshotById(foodId);
    if (food == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");
    }
    return HttpResult.success(food);
  }

  @PostMapping("/stock/reserve")
  @Operation(summary = "批量扣减库存", description = "按requestId幂等扣减库存")
  public HttpResult<Boolean> reserveStock(@RequestBody StockOperateRequest request) {
    if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Items CANT BE EMPTY");
    }
    boolean success =
        foodInternalService.reserveStock(
            request.getRequestId(), request.getOrderId(), toCommands(request.getItems()));
    if (!success) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to reserve stock");
    }
    return HttpResult.success(true);
  }

  @PostMapping("/stock/release")
  @Operation(summary = "批量回补库存", description = "按requestId幂等回补库存")
  public HttpResult<Boolean> releaseStock(@RequestBody StockOperateRequest request) {
    if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Items CANT BE EMPTY");
    }
    boolean success =
        foodInternalService.releaseStock(
            request.getRequestId(), request.getOrderId(), toCommands(request.getItems()));
    if (!success) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to release stock");
    }
    return HttpResult.success(true);
  }

  private List<FoodInternalService.StockItemCommand> toCommands(List<StockItemRequest> items) {
    return items.stream()
        .map(item -> new FoodInternalService.StockItemCommand(item.getFoodId(), item.getQuantity()))
        .collect(Collectors.toList());
  }

  public static class StockOperateRequest {
    private String requestId;
    private String orderId;
    private List<StockItemRequest> items;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public List<StockItemRequest> getItems() { return items; }
    public void setItems(List<StockItemRequest> items) { this.items = items; }
  }

  public static class StockItemRequest {
    private Long foodId;
    private Integer quantity;

    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
  }
}
