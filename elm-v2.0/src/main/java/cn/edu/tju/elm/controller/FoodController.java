package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.Business;
import cn.edu.tju.elm.model.Food;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.FoodService;
import cn.edu.tju.core.security.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
        // TODO: getByOrderId

        return null;
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
            if (food.equals(foodService.addFood(food))) {
                return HttpResult.success(food);
            }
        }

        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR);
    }
}
