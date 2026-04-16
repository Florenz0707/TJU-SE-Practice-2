package cn.edu.tju.product.controller;

import cn.edu.tju.product.model.Food;
import cn.edu.tju.product.repository.FoodRepository;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.product.service.MerchantBusinessService;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping
public class FoodController {

    private final FoodRepository foodRepository;
    private final MerchantBusinessService merchantBusinessService;

    public FoodController(FoodRepository foodRepository, MerchantBusinessService merchantBusinessService) {
        this.foodRepository = foodRepository;
        this.merchantBusinessService = merchantBusinessService;
    }

    // Modern API endpoints
    @GetMapping("/api/foods")
    public HttpResult<List<Food>> getFoods(
            @RequestParam(value = "business", required = false) Long businessId,
            @RequestParam(value = "order", required = false) Long orderId) {
        // 微服务拆分后：food 不再直接根据 order 查询（order-service 负责订单明细）。
        // 为兼容前端 contract，这里接受 order 参数但不作为过滤条件，仅避免 400。
        // 读权限策略（按最新业务期望）：
        // - 顾客主页/店铺详情：所有人（含登录的顾客/商家）都可以按 business 查询该店铺菜品。
        // - 如需“商家仅查看/管理自己店铺菜品”，请使用下方 /api/foods/my-management 接口。

    // TODO(权限语义收敛): food 表目前没有 deleted/onSale 等字段（与 merchant-service 的软删除不同）。
    // 因此这里无法做“未删除/在售”过滤，只能按 businessId 查询或返回全量。
    List<Food> foods =
        (businessId != null) ? foodRepository.findByBusinessId(businessId) : foodRepository.findAll();
        return HttpResult.success(foods);
    }

    /**
     * 商家菜单管理专用：仅返回“当前登录商家自己的店铺”的菜品。
     * 前端可用于“商家管理页”，避免误把顾客浏览接口当作管理接口。
     */
    @GetMapping("/api/foods/my-management")
    public HttpResult<List<Food>> getMyManagementFoods() {
        var actorOpt = merchantBusinessService.getActor();
        if (actorOpt.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
        }
        var actor = actorOpt.get();
        if (actor.isAdmin()) {
            return HttpResult.success(foodRepository.findAll());
        }

        // 商家：先用 ownerId 找到自己的店铺，再用店铺 id 查菜品
        // 约束：每个商家只有一个活跃店铺（merchant-service 已做 hard guard）
        Long businessId = merchantBusinessService.getMyBusinessId().orElse(null);
        if (businessId == null) {
            // 按前端体验：没有店铺就返回空（而不是 404）
            return HttpResult.success(List.of());
        }
        return HttpResult.success(foodRepository.findByBusinessId(businessId));
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
        var actorOpt = merchantBusinessService.getActor();
        if (actorOpt.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
        }
        var actor = actorOpt.get();

        if (food == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food CANT BE NULL");
        if (food.getFoodName() == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodName CANT BE NULL");
        if (food.getFoodPrice() == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "FoodPrice CANT BE NULL");
        if (food.getBusinessId() == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessId CANT BE NULL");

        if (!actor.isAdmin()) {
            Long ownerId = merchantBusinessService.getBusinessOwnerId(food.getBusinessId()).orElse(null);
            if (ownerId == null) {
                return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
            }
            if (!actor.userId().equals(ownerId)) {
                return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
            }
        }
        return HttpResult.success(foodRepository.save(food));
    }

    @PutMapping("/api/foods/{id}")
    @Transactional
    public HttpResult<Food> updateFood(@PathVariable("id") Long id, @RequestBody Food patch) {
        var actorOpt = merchantBusinessService.getActor();
        if (actorOpt.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
        }
        var actor = actorOpt.get();

        Optional<Food> existingOpt = foodRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");
        }
        if (patch == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food CANT BE NULL");
        }
        Food existing = existingOpt.get();

        if (!actor.isAdmin()) {
            Long businessId = existing.getBusinessId();
            Long ownerId = merchantBusinessService.getBusinessOwnerId(businessId).orElse(null);
            if (ownerId == null) {
                return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
            }
            if (!actor.userId().equals(ownerId)) {
                return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
            }
        }

        // 这里按“全量更新”语义实现，但仍做 null 保护。
        if (patch.getFoodName() != null) existing.setFoodName(patch.getFoodName());
        if (patch.getFoodExplain() != null) existing.setFoodExplain(patch.getFoodExplain());
        if (patch.getFoodImg() != null) existing.setFoodImg(patch.getFoodImg());
        if (patch.getFoodPrice() != null) existing.setFoodPrice(patch.getFoodPrice());
        // 安全考虑：不允许通过 update/patch 把菜品“挪到别家店铺”
        if (patch.getRemarks() != null) existing.setRemarks(patch.getRemarks());

        return HttpResult.success(foodRepository.save(existing));
    }

    @DeleteMapping("/api/foods/{id}")
    @Transactional
    public HttpResult<String> deleteFood(@PathVariable("id") Long id) {
        var actorOpt = merchantBusinessService.getActor();
        if (actorOpt.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
        }
        var actor = actorOpt.get();

        Optional<Food> existingOpt = foodRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Food NOT FOUND");
        }

        Food existing = existingOpt.get();
        if (!actor.isAdmin()) {
            Long ownerId = merchantBusinessService.getBusinessOwnerId(existing.getBusinessId()).orElse(null);
            if (ownerId == null) {
                return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
            }
            if (!actor.userId().equals(ownerId)) {
                return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
            }
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
