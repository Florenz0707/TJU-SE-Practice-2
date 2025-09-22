package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.Business;
import cn.edu.tju.elm.model.Food;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.elm.model.Order;
import cn.edu.tju.elm.model.OrderDetailet;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.FoodService;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.service.OrderDetailetService;
import cn.edu.tju.elm.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/foods")
@Tag(name = "管理商品")
public class FoodController {

    @Autowired
    private UserService userService;

    @Autowired
    private FoodService foodService;

    @Autowired
    private BusinessService businessService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderDetailetService orderDetailetService;

    @GetMapping("/{id}")
    @Operation(summary = "返回查询到的一条商品记录", method = "GET")
    public HttpResult<Food> getFoodById(@PathVariable Long id) {
        Food food = foodService.getFoodById(id);
        if (food == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");
        return HttpResult.success(food);
    }

    @GetMapping("")
    public HttpResult<List<Food>> getAllFoods(
            @RequestParam(name = "business", required = false) Long businessId,
            @RequestParam(name = "order", required = false) Long orderId) {
        if ((businessId == null && orderId == null) || (businessId != null && orderId != null)) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "HAVE TO PROVIDE ONE AND ONLY ONE ARG");
        }

        if (businessId != null) {
            Business business = businessService.getBusinessById(businessId);
            if (business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
            return HttpResult.success(foodService.getFoodsByBusinessId(businessId));
        }

        Order order = orderService.getOrderById(orderId);
        if (order == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
        List<OrderDetailet> orderDetailetList = orderDetailetService.getOrderDetailetByOrderId(orderId);
        List<Food> foodList = new ArrayList<>(orderDetailetList.size());
        for (OrderDetailet orderDetailet : orderDetailetList) {
            foodList.add(orderDetailet.getFood());
        }
        return HttpResult.success(foodList);
    }

    @PostMapping("")
    public HttpResult<Food> addFood(@RequestBody Food food) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND);
        User me = meOptional.get();

        if (food.getFoodName() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodName CANT BE NULL");
        if (food.getFoodPrice() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodPrice CANT BE NULL");
        if (food.getBusiness() == null || food.getBusiness().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.Id CANT BE NULL");

        boolean isAdmin = false;
        boolean isBusiness = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) isAdmin = true;
            if (authority.getName().equals("BUSINESS")) isBusiness = true;
        }

        Business business = businessService.getBusinessById(food.getBusiness().getId());
        if (business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
        User user = business.getBusinessOwner();

        if (isAdmin || (isBusiness && me.equals(user))) {
            food.setBusiness(business);

            LocalDateTime now = LocalDateTime.now();
            food.setCreateTime(now);
            food.setUpdateTime(now);
            food.setCreator(me.getId());
            food.setUpdater(me.getId());
            food.setDeleted(false);
            foodService.addFood(food);
            return HttpResult.success(food);
        }

        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新商品信息（创建新版本）", method = "PUT")
    public HttpResult<Food> updateFood(@PathVariable Long id, @RequestBody Food food) {
        // 获取当前登录用户信息
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND);
        User me = meOptional.get();

        Food existingFood = foodService.getFoodById(id);
        if (existingFood == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");

        boolean isAdmin = false;
        boolean isBusiness = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) isAdmin = true;
            if (authority.getName().equals("BUSINESS")) isBusiness = true;
        }

        // 获取原商品的商家信息和商家所有者
        Business business = existingFood.getBusiness();
        User businessOwner = business.getBusinessOwner();

        // 只有管理员或该商家的所有者才能更新商品
        if (isAdmin || (isBusiness && me.equals(businessOwner))) {
            //先软删除
            existingFood.setDeleted(true);
            existingFood.setUpdateTime(LocalDateTime.now());
            existingFood.setUpdater(me.getId());
            foodService.addFood(existingFood); // 使用addFood来更新现有记录

            //基于请求体中的food数据创建新版本的食品
            Food newFood = new Food();
            newFood.setId(null);
            newFood.setBusiness(existingFood.getBusiness());
            // 使用请求体中的数据更新商品信息，如果为空则使用原数据
            newFood.setFoodName(food.getFoodName() != null ? food.getFoodName() : existingFood.getFoodName());
            newFood.setFoodPrice(food.getFoodPrice() != null ? food.getFoodPrice() : existingFood.getFoodPrice());
            newFood.setFoodExplain(food.getFoodExplain() != null ? food.getFoodExplain() : existingFood.getFoodExplain());
            newFood.setFoodImg(food.getFoodImg() != null ? food.getFoodImg() : existingFood.getFoodImg());
            newFood.setRemarks(food.getRemarks() != null ? food.getRemarks() : existingFood.getRemarks());

            // 设置审计信息
            LocalDateTime now = LocalDateTime.now();
            newFood.setCreateTime(now);
            newFood.setUpdateTime(now);
            newFood.setCreator(me.getId());
            newFood.setUpdater(me.getId());
            newFood.setDeleted(false);

            Food savedNewFood = foodService.addFood(newFood);

            return HttpResult.success(savedNewFood);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "No permission to update this food");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品（软删除）", method = "DELETE")
    public HttpResult<Food> deleteFood(@PathVariable Long id) {
        // 获取当前登录用户信息
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND);
        User me = meOptional.get();

        // 根据路径变量中的ID查询要删除的商品
        Food food = foodService.getFoodById(id);
        if (food == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");

        // 检查用户权限：是否为管理员或商家
        boolean isAdmin = false;
        boolean isBusiness = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) isAdmin = true;
            if (authority.getName().equals("BUSINESS")) isBusiness = true;
        }

        // 获取商品的商家信息和商家所有者
        Business business = food.getBusiness();
        User businessOwner = business.getBusinessOwner();

        if (isAdmin || (isBusiness && me.equals(businessOwner))) {
            food.setDeleted(true);
            food.setUpdateTime(LocalDateTime.now());
            food.setUpdater(me.getId());
            Food deletedFood = foodService.addFood(food);

            return HttpResult.success(deletedFood);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "No permission to delete this food");
    }
}