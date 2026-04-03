package cn.edu.tju.product.controller;

import cn.edu.tju.product.model.Food;
import cn.edu.tju.product.repository.FoodRepository;
import cn.edu.tju.core.model.HttpResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping
public class FoodController {

    private final FoodRepository foodRepository;

    public FoodController(FoodRepository foodRepository) {
        this.foodRepository = foodRepository;
    }

    // Modern API endpoints
    @GetMapping("/api/foods")
    public HttpResult<List<Food>> getFoods(@RequestParam(value = "business", required = false) Long businessId) {
        List<Food> foods;
        if (businessId != null) {
            foods = foodRepository.findByBusinessId(businessId);
        } else {
            foods = foodRepository.findAll();
        }
        return HttpResult.success(foods);
    }

    @GetMapping("/api/foods/{id}")
    public HttpResult<Food> getFoodById(@PathVariable("id") Long id) {
        Food food = foodRepository.findById(id).orElse(null);
        if (food != null) {
            return HttpResult.success(food);
        }
        return HttpResult.failure(cn.edu.tju.core.model.ResultCodeEnum.NOT_FOUND, "Food object not found");
    }

    @PostMapping("/api/foods")
    @Transactional
    public HttpResult<Food> addFood(@RequestBody Food food) {
        // Assume frontend sends "business": {"id": xxx} which is mapped by default Jackson if we add setters?
        // Wait, if it sends food.business.id we should extract it and set businessId
        return HttpResult.success(foodRepository.save(food));
    }

    // Monolith compatibility endpoint
    @RequestMapping("/FoodController/listFoodByBusinessId")
    public List<Food> listFoodByBusinessId(@ModelAttribute Food food) {
        if (food.getBusinessId() != null) {
            return foodRepository.findByBusinessId(food.getBusinessId());
        }
        return foodRepository.findAll();
    }
}
