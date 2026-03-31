package cn.edu.tju.cart.controller;

import cn.edu.tju.cart.model.vo.CartSnapshotVO;
import cn.edu.tju.cart.service.CartInternalService;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner/cart")
@Tag(name = "购物车内部接口", description = "cart-service 购物车域内部接口")
public class CartInnerController {
  private final CartInternalService cartInternalService;

  public CartInnerController(CartInternalService cartInternalService) {
    this.cartInternalService = cartInternalService;
  }

  @PostMapping("")
  @Operation(summary = "创建购物车项", description = "新增购物车商品")
  public HttpResult<CartSnapshotVO> createCart(@RequestBody CreateCartRequest request) {
    if (request == null || request.getFoodId() == null || request.getCustomerId() == null || request.getBusinessId() == null || request.getQuantity() == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Cart fields CANT BE NULL");
    }
    try {
      CartSnapshotVO created = cartInternalService.createCart(new CartInternalService.CreateCartCommand(request.getFoodId(), request.getCustomerId(), request.getBusinessId(), request.getQuantity()));
      return HttpResult.success(created);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/{cartId}")
  @Operation(summary = "查询购物车项", description = "按ID查询购物车项")
  public HttpResult<CartSnapshotVO> getCartById(@Parameter(description = "购物车ID", required = true) @PathVariable("cartId") Long cartId) {
    CartSnapshotVO cart = cartInternalService.getCartById(cartId);
    if (cart == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Cart NOT FOUND");
    }
    return HttpResult.success(cart);
  }

  @GetMapping("/customer/{customerId}")
  @Operation(summary = "查询用户购物车", description = "按用户ID查询购物车")
  public HttpResult<List<CartSnapshotVO>> getCartsByCustomerId(@Parameter(description = "用户ID", required = true) @PathVariable("customerId") Long customerId) {
    return HttpResult.success(cartInternalService.getCartsByCustomerId(customerId));
  }

  @GetMapping("/business/{businessId}/customer/{customerId}")
  @Operation(summary = "按商家+用户查询购物车", description = "下单链路查询指定商家的购物车项")
  public HttpResult<List<CartSnapshotVO>> getCartsByBusinessAndCustomerId(@Parameter(description = "商家ID", required = true) @PathVariable("businessId") Long businessId, @Parameter(description = "用户ID", required = true) @PathVariable("customerId") Long customerId) {
    return HttpResult.success(cartInternalService.getCartsByBusinessAndCustomerId(businessId, customerId));
  }

  @PostMapping("/{cartId}/quantity")
  @Operation(summary = "更新购物车数量", description = "更新购物车项数量")
  public HttpResult<CartSnapshotVO> updateCartQuantity(@Parameter(description = "购物车ID", required = true) @PathVariable("cartId") Long cartId, @RequestBody UpdateCartQuantityRequest request) {
    if (request == null || request.getQuantity() == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Quantity CANT BE NULL");
    }
    try {
      return HttpResult.success(cartInternalService.updateCartQuantity(cartId, request.getQuantity()));
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @DeleteMapping("/{cartId}")
  @Operation(summary = "删除购物车项", description = "删除指定购物车项")
  public HttpResult<Boolean> deleteCart(@Parameter(description = "购物车ID", required = true) @PathVariable("cartId") Long cartId) {
    try {
      return HttpResult.success(cartInternalService.deleteCart(cartId));
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  public static class CreateCartRequest {
    private Long foodId;
    private Long customerId;
    private Long businessId;
    private Integer quantity;

    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
  }

  public static class UpdateCartQuantityRequest {
    private Integer quantity;

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
  }
}
