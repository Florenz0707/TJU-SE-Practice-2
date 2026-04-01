package cn.edu.tju.catalog.controller;

import cn.edu.tju.catalog.model.vo.BusinessSnapshotVO;
import cn.edu.tju.catalog.model.vo.FoodSnapshotVO;
import cn.edu.tju.catalog.service.CatalogInternalService;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner/catalog")
@Tag(name = "目录内部接口", description = "订单服务等内部调用的商家/菜品查询接口")
public class CatalogInnerController {
  private final CatalogInternalService catalogInternalService;

  public CatalogInnerController(CatalogInternalService catalogInternalService) {
    this.catalogInternalService = catalogInternalService;
  }

  @GetMapping("/business/{businessId}")
  @Operation(summary = "按ID查询商家", description = "查询商家运营快照，供订单侧校验")
  public HttpResult<BusinessSnapshotVO> getBusinessById(
      @Parameter(description = "商家ID", required = true) @PathVariable("businessId")
          Long businessId) {
    if (businessId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessId CANT BE NULL");
    }
    BusinessSnapshotVO business = catalogInternalService.getBusinessSnapshotById(businessId);
    if (business == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
    }
    return HttpResult.success(business);
  }

  @GetMapping("/food/{foodId}")
  @Operation(summary = "按ID查询菜品", description = "查询菜品快照，供订单侧校验")
  public HttpResult<FoodSnapshotVO> getFoodById(
      @Parameter(description = "菜品ID", required = true) @PathVariable("foodId") Long foodId) {
    if (foodId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodId CANT BE NULL");
    }
    FoodSnapshotVO food = catalogInternalService.getFoodSnapshotById(foodId);
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
        catalogInternalService.reserveStock(
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
        catalogInternalService.releaseStock(
            request.getRequestId(), request.getOrderId(), toCommands(request.getItems()));
    if (!success) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to release stock");
    }
    return HttpResult.success(true);
  }

  private List<CatalogInternalService.StockItemCommand> toCommands(List<StockItemRequest> items) {
    return items.stream()
        .map(
            item ->
                new CatalogInternalService.StockItemCommand(item.getFoodId(), item.getQuantity()))
        .collect(Collectors.toList());
  }

  public static class StockOperateRequest {
    private String requestId;
    private String orderId;
    private List<StockItemRequest> items;

    public String getRequestId() {
      return requestId;
    }

    public void setRequestId(String requestId) {
      this.requestId = requestId;
    }

    public String getOrderId() {
      return orderId;
    }

    public void setOrderId(String orderId) {
      this.orderId = orderId;
    }

    public List<StockItemRequest> getItems() {
      return items;
    }

    public void setItems(List<StockItemRequest> items) {
      this.items = items;
    }
  }

  public static class StockItemRequest {
    private Long foodId;
    private Integer quantity;

    public Long getFoodId() {
      return foodId;
    }

    public void setFoodId(Long foodId) {
      this.foodId = foodId;
    }

    public Integer getQuantity() {
      return quantity;
    }

    public void setQuantity(Integer quantity) {
      this.quantity = quantity;
    }
  }
}
