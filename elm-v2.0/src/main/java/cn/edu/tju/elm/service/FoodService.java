package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BO.Food;
import cn.edu.tju.elm.repository.FoodRepository;
import cn.edu.tju.elm.utils.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FoodService {
    private final FoodRepository foodRepository;

    public FoodService(FoodRepository foodRepository) {
        this.foodRepository = foodRepository;
    }

    public void addFood(Food food) {
        foodRepository.save(food);
    }

    public List<Food> getFoodsByBusinessId(Long businessId) {
        return EntityUtils.filterEntityList(foodRepository.findAllByBusinessId(businessId));
    }

    public Food getFoodById(Long id) {
        Optional<Food> foodOptional = foodRepository.findById(id);
        return foodOptional.map(EntityUtils::filterEntity).orElse(null);
    }

    public void updateFood(Food food) {
        foodRepository.save(food);
    }
}
