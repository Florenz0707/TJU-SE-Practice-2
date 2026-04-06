package cn.edu.tju.product.controller;

import cn.edu.tju.product.model.Food;
import cn.edu.tju.product.repository.FoodRepository;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping
public class FoodController {

    private final FoodRepository foodRepository;

    public FoodController(FoodRepository foodRepository) {
        this.foodRepository = foodRepository;
    }

    // Modern API endpoints
    @GetMapping("/api/foods")
    public HttpResult<List<Food>> getFoods(
            @RequestParam(value = "business", required = false) Long businessId,
            @RequestParam(value = "order", required = false) Long orderId) {
        // 微服务拆分后：food 不再直接根据 order 查询（order-service 负责订单明细）。
        // 为兼容前端 contract，这里接受 order 参数但不作为过滤条件，仅避免 400。
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
        if (food == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food CANT BE NULL");
        if (food.getFoodName() == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodName CANT BE NULL");
        if (food.getFoodPrice() == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodPrice CANT BE NULL");
        if (food.getBusinessId() == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessId CANT BE NULL");
        return HttpResult.success(foodRepository.save(food));
    }

    @PutMapping("/api/foods/{id}")
    @Transactional
    public HttpResult<Food> updateFood(@PathVariable("id") Long id, @RequestBody Food patch) {
        Optional<Food> existingOpt = foodRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");
        }
        if (patch == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food CANT BE NULL");
        }
        Food existing = existingOpt.get();
        // 这里按“全量更新”语义实现，但仍做 null 保护。
        if (patch.getFoodName() != null) existing.setFoodName(patch.getFoodName());
        if (patch.getFoodExplain() != null) existing.setFoodExplain(patch.getFoodExplain());
        if (patch.getFoodImg() != null) existing.setFoodImg(patch.getFoodImg());
        if (patch.getFoodPrice() != null) existing.setFoodPrice(patch.getFoodPrice());
        if (patch.getBusinessId() != null) existing.setBusinessId(patch.getBusinessId());
        if (patch.getRemarks() != null) existing.setRemarks(patch.getRemarks());

        return HttpResult.success(foodRepository.save(existing));
    }

    @DeleteMapping("/api/foods/{id}")
    @Transactional
    public HttpResult<String> deleteFood(@PathVariable("id") Long id) {
        Optional<Food> existingOpt = foodRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");
        }
        foodRepository.deleteById(id);
        return HttpResult.success("Delete food successfully.");
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
