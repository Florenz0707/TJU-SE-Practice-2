package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.Food;
import cn.edu.tju.elm.repository.FoodRepository;
import cn.edu.tju.elm.utils.Utils;
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

    public Food addFood(Food food) {
        return foodRepository.save(food);
    }

    public List<Food> getFoodsByBusinessId(Long businessId) {
        return Utils.removeDeleted(foodRepository.findAllByBusinessId(businessId));
    }

    public Food getFoodById(Long id) {
        Optional<Food> foodOptional = foodRepository.findById(id);
        return foodOptional.orElse(null);
    }
}
