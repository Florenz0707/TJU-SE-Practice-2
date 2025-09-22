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
    public HttpResult<Food> updateFood(@PathVariable Long id, @RequestBody Food food) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Food oldFood = foodService.getFoodById(id);
        if (oldFood == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");

        if (food.getFoodName() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodName CANT BE NULL");
        if (food.getFoodPrice() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodPrice CANT BE NULL");
        if (food.getBusiness() == null || food.getBusiness().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.Id CANT BE NULL");

        Business business = businessService.getBusinessById(food.getBusiness().getId());
        if (business == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");

        boolean isAdmin = false;
        boolean isBusiness = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) isAdmin = true;
            if (authority.getName().equals("BUSINESS")) isBusiness = true;
        }

        if (isAdmin || (isBusiness && me.equals(business.getBusinessOwner()))) {
            food.setId(oldFood.getId());
            food.setBusiness(business);

            LocalDateTime now = LocalDateTime.now();
            food.setCreator(oldFood.getCreator());
            food.setUpdater(me.getId());
            food.setCreateTime(oldFood.getCreateTime());
            food.setUpdateTime(now);
            food.setDeleted(false);
            foodService.updateFood(food);
            return HttpResult.success(food);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @DeleteMapping("/{id}")
    public HttpResult<Food> deleteFood(@PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Food food = foodService.getFoodById(id);
        if (food == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");

        boolean isAdmin = false;
        boolean isBusiness = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) isAdmin = true;
            if (authority.getName().equals("BUSINESS")) isBusiness = true;
        }

        if (isAdmin || (isBusiness && me.equals(food.getBusiness().getBusinessOwner()))) {
            LocalDateTime now = LocalDateTime.now();
            food.setUpdateTime(now);
            food.setUpdater(me.getId());
            food.setDeleted(true);
            foodService.updateFood(food);
            return HttpResult.success(food);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
}
