package cn.edu.tju.elm.controller;

import cn.edu.tju.elm.model.Food;
import cn.edu.tju.core.model.HttpResult;
//import cn.edu.tju.elb.service.BusinessService;
//import cn.edu.tju.elb.service.FoodService;
import cn.edu.tju.core.security.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@Tag(name = "管理商品")
public class FoodController {
    @Autowired
    private UserService userService;

//    @Autowired
//    private FoodService foodService;
//
//    @Autowired
//    private BusinessService businessService;

    @GetMapping("/{id}")
    @Operation(summary = "返回查询到的一条商品记录", method = "GET")
    public HttpResult<Food> getFoodById(@PathVariable Long id){
        return null;
    }

    @GetMapping("")
    public HttpResult<List<Food>> getAllFoods(@RequestParam(name = "business", required = false) Long businessId,
                                              @RequestParam(name = "order", required = false) Long orderId){
        return null;
    }

    @PostMapping("")
    public HttpResult<Food> addFood(@RequestBody Food food){
        return null;
    }
}
