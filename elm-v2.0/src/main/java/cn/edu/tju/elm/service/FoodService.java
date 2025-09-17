package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.Food;
import cn.edu.tju.elm.repository.FoodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        List<Food> foodList = foodRepository.findAllByBusinessId(businessId);
        for (int i = 0; i < foodList.size(); ++i) {
            if (foodList.get(i).getDeleted()) foodList.remove(i--);
        }
        return foodList;
    }
}
