package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Food;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.model.BO.OrderDetailet;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.FoodService;
import cn.edu.tju.elm.service.OrderDetailetService;
import cn.edu.tju.elm.service.OrderService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.EntityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/foods")
@Tag(name = "管理商品")
public class FoodController {
    private final UserService userService;
    private final FoodService foodService;
    private final BusinessService businessService;
    private final OrderService orderService;
    private final OrderDetailetService orderDetailetService;

    public FoodController(UserService userService, FoodService foodService, BusinessService businessService, OrderService orderService, OrderDetailetService orderDetailetService) {
        this.userService = userService;
        this.foodService = foodService;
        this.businessService = businessService;
        this.orderService = orderService;
        this.orderDetailetService = orderDetailetService;
    }

    @GetMapping("/{id}")
    public HttpResult<Food> getFoodById(@PathVariable Long id) {
        Food food = foodService.getFoodById(id);
        if (food == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");
        return HttpResult.success(food);
    }

    @GetMapping("")
    public HttpResult<List<Food>> getAllFoods(
            @RequestParam(name = "business", required = false) Long businessId,
            @RequestParam(name = "order", required = false) Long orderId) {
        if ((businessId == null && orderId == null) || (businessId != null && orderId != null))
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "HAVE TO PROVIDE ONE AND ONLY ONE ARG");

        if (businessId != null) {
            Business business = businessService.getBusinessById(businessId);
            if (business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
            return HttpResult.success(foodService.getFoodsByBusinessId(businessId));
        } else {
            Order order = orderService.getOrderById(orderId);
            if (order == null)
                return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
            List<OrderDetailet> orderDetailetList = orderDetailetService.getOrderDetailetsByOrderId(orderId);
            List<Food> foodList = new ArrayList<>(orderDetailetList.size());
            for (OrderDetailet orderDetailet : orderDetailetList)
                foodList.add(orderDetailet.getFood());
            return HttpResult.success(foodList);
        }
    }

    @PostMapping("")
    public HttpResult<Food> addFood(@RequestBody Food food) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND);
        User me = meOptional.get();

        if (food == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food CANT BE NULL");
        if (food.getFoodName() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodName CANT BE NULL");
        if (food.getFoodPrice() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodPrice CANT BE NULL");
        if (food.getBusiness() == null || food.getBusiness().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.Id CANT BE NULL");

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");

        Business business = businessService.getBusinessById(food.getBusiness().getId());
        if (business == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
        User owner = business.getBusinessOwner();

        if (isAdmin || (isBusiness && me.equals(owner))) {
            EntityUtils.setNewEntity(food);
            food.setBusiness(business);
            foodService.addFood(food);
            return HttpResult.success(food);
        }

        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR);
    }

    @PutMapping("/{id}")
    public HttpResult<Food> substituteFood(
            @PathVariable Long id,
            @RequestBody Food food) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Food oldFood = foodService.getFoodById(id);
        if (oldFood == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");

        if (food == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food CANT BE NULL");
        if (food.getFoodName() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodName CANT BE NULL");
        if (food.getFoodPrice() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodPrice CANT BE NULL");
        if (food.getBusiness() == null || food.getBusiness().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.Id CANT BE NULL");

        Business business = businessService.getBusinessById(oldFood.getBusiness().getId());

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
        if (isAdmin || (isBusiness && me.equals(business.getBusinessOwner()))) {
            food.setId(null);
            EntityUtils.substituteEntity(oldFood, food);
            foodService.updateFood(oldFood);
            foodService.updateFood(food);
            return HttpResult.success(food);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @PatchMapping("/{id}")
    public HttpResult<Food> updateFood(
            @PathVariable Long id,
            @RequestBody Food newFood) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Food food = foodService.getFoodById(id);
        if (food == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");

        if (newFood == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food CANT BE NULL");

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");

        if (isAdmin || (isBusiness && me.equals(food.getBusiness().getBusinessOwner()))) {
            if (newFood.getFoodName() == null)
                newFood.setFoodName(food.getFoodName());
            if (newFood.getFoodPrice() == null)
                newFood.setFoodPrice(food.getFoodPrice());
            if (newFood.getFoodExplain() == null)
                newFood.setFoodExplain(food.getFoodExplain());
            if (newFood.getFoodImg() == null)
                newFood.setFoodImg(food.getFoodImg());
            if (newFood.getRemarks() == null)
                newFood.setRemarks(food.getRemarks());
            EntityUtils.substituteEntity(food, newFood);
            foodService.updateFood(food);
            foodService.updateFood(newFood);
            return HttpResult.success(newFood);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @DeleteMapping("/{id}")
    public HttpResult<String> deleteFood(@PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Food food = foodService.getFoodById(id);
        if (food == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");

        if (isAdmin || (isBusiness && me.equals(food.getBusiness().getBusinessOwner()))) {
            EntityUtils.deleteEntity(food);
            foodService.updateFood(food);
            return HttpResult.success("Delete food successfully.");
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
}
