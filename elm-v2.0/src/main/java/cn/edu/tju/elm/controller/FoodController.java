package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.repository.UserRepository;
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
    private UserRepository userRepository;

    @Autowired
    private BusinessService businessService;

    @GetMapping("/{id}")
    @Operation(summary = "返回查询到的一条商品记录", method = "GET")
    public HttpResult<Food> getFoodById(@PathVariable Long id) {
        return null;
    }

    @GetMapping("")
    public HttpResult<List<Food>> getAllFoods(@RequestParam(name = "business", required = false) Long businessId,
                                              @RequestParam(name = "order", required = false) Long orderId) {
        return null;
    }

    @PostMapping("")
    public HttpResult<Food> addFood(@RequestBody Food food) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND);
        User me = meOptional.get();
        boolean isAdmin = false;
        boolean isBusiness = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                isAdmin = true;
                break;
            }
            if (authority.getName().equals("BUSINESS")) {
                isBusiness = true;
                break;
            }
        }

        // TODO: 不跟据附带的businessOwner信息获得User信息，而直接用businessId查找business实体从而获取User
        Optional<User> userOptional = userRepository.findOneWithAuthoritiesByUsername(food.getBusiness().getBusinessOwner().getUsername());
        if (userOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND);
        User user = userOptional.get();
        if (isAdmin || (isBusiness && user.getUsername().equals(me.getUsername()))) {
            Business business = businessService.getBusinessById(food.getBusiness().getId());
            if (business == null) return HttpResult.failure(ResultCodeEnum.SERVER_ERROR);
            food.setBusiness(business);
            return foodService.addFood(food);
        }

        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR);
    }
}
