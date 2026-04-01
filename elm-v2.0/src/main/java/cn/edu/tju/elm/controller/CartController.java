package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Cart;
import cn.edu.tju.elm.model.BO.Food;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.FoodService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.InternalOrderClient;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "管理购物车", description = "提供对购物车商品的增删改查功能")
public class CartController {
  private final UserService userService;
  private final InternalOrderClient internalOrderClient;
  private final FoodService foodService;
  private final BusinessService businessService;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public CartController(
      UserService userService,
      InternalOrderClient internalOrderClient,
      FoodService foodService,
      BusinessService businessService,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.userService = userService;
    this.internalOrderClient = internalOrderClient;
    this.foodService = foodService;
    this.businessService = businessService;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  @PostMapping("/carts")
  @Operation(summary = "添加购物车商品", description = "将商品添加到购物车")
  public HttpResult<Cart> addCartItem(
      @Parameter(description = "购物车项信息", required = true) @RequestBody Cart cart) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (cart == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Cart CANT BE NULL");
    if (cart.getFood() == null || cart.getFood().getId() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food.Id CANT BE NULL");
    if (cart.getQuantity() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Quantity CANT BE NULL");

    Food food = foodService.getFoodById(cart.getFood().getId());
    if (food == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");
    if (food.getDeleted() != null && food.getDeleted())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "商品已下架");
    Business business = food.getBusiness();
    if (business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");

    InternalOrderClient.CartSnapshot created =
        internalOrderClient.createCart(
            new InternalOrderClient.CreateCartCommand(
                food.getId(), me.getId(), business.getId(), cart.getQuantity()));
    if (created == null) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to create cart");
    }
    Cart createdCart = toCart(created);
    compatibilityEnricher.enrichCart(createdCart);
    return HttpResult.success(createdCart);
  }

  @GetMapping("/carts")
  @Operation(summary = "获取我的购物车", description = "获取当前用户的所有购物车商品")
  public HttpResult<List<Cart>> getCarts() {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();
    List<Cart> carts =
        internalOrderClient.getCartsByCustomerId(me.getId()).stream().map(this::toCart).toList();
    compatibilityEnricher.enrichCarts(carts);
    return HttpResult.success(carts);
  }

  @PatchMapping("/carts/{id}")
  @Operation(summary = "更新购物车商品数量", description = "修改购物车中商品的数量")
  public HttpResult<Cart> updateCartItem(
      @Parameter(description = "购物车项ID", required = true) @PathVariable("id") Long id,
      @Parameter(description = "新的购物车信息", required = true) @RequestBody Cart newCart) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (newCart == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Cart CANT BE NULL");

    if (newCart.getQuantity() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Cart.Quantity CANT BE NULL");

    InternalOrderClient.CartSnapshot existing = internalOrderClient.getCartById(id);
    Cart cart = existing == null ? null : toCart(existing);
    if (cart == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Cart NOT FOUND");
    Long ownerId = cart.getCustomerId();

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    if (isAdmin || me.getId().equals(ownerId)) {
      InternalOrderClient.CartSnapshot updated =
          internalOrderClient.updateCartQuantity(id, newCart.getQuantity());
      if (updated == null) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to update cart");
      }
      Cart updatedCart = toCart(updated);
      compatibilityEnricher.enrichCart(updatedCart);
      return HttpResult.success(updatedCart);
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @DeleteMapping("/carts/{id}")
  @Operation(summary = "删除购物车商品", description = "从购物车中移除指定商品")
  public HttpResult<String> deleteCartItem(
      @Parameter(description = "购物车项ID", required = true) @PathVariable("id") Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Authority NOT FOUND");
    User me = meOptional.get();

    InternalOrderClient.CartSnapshot existing = internalOrderClient.getCartById(id);
    Cart cart = existing == null ? null : toCart(existing);
    if (cart == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Cart NOT FOUND");

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    if (isAdmin || me.getId().equals(cart.getCustomerId())) {
      boolean deleted = internalOrderClient.deleteCart(id);
      if (!deleted) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to delete cart");
      }
      return HttpResult.success("Delete cart successfully.");
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  private Cart toCart(InternalOrderClient.CartSnapshot snapshot) {
    Cart cart = new Cart();
    cart.setId(snapshot.id());
    cart.setCustomerId(snapshot.customerId());
    cart.setQuantity(snapshot.quantity());

    Food food = foodService.getFoodById(snapshot.foodId());
    if (food == null) {
      food = new Food();
      food.setId(snapshot.foodId());
    }
    cart.setFood(food);

    Business business = businessService.getBusinessById(snapshot.businessId());
    if (business == null) {
      business = new Business();
      business.setId(snapshot.businessId());
    }
    cart.setBusiness(business);
    return cart;
  }
}
