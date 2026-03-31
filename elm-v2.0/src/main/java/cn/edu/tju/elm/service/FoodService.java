package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Food;
import cn.edu.tju.elm.repository.FoodRepository;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.InternalCatalogClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FoodService {
  private final FoodRepository foodRepository;
  private final InternalCatalogClient internalCatalogClient;

  public FoodService(FoodRepository foodRepository, InternalCatalogClient internalCatalogClient) {
    this.foodRepository = foodRepository;
    this.internalCatalogClient = internalCatalogClient;
  }

  public void addFood(Food food) {
    foodRepository.save(food);
  }

  public List<Food> getFoodsByBusinessId(Long businessId) {
    List<Food> foods = EntityUtils.filterEntityList(foodRepository.findAllByBusinessId(businessId));
    if (foods.isEmpty()) {
      InternalCatalogClient.BusinessSnapshot businessSnapshot =
          internalCatalogClient.getBusinessSnapshot(businessId);
      foods =
          new ArrayList<>(
              internalCatalogClient.getFoodSnapshotsByBusinessId(businessId).stream()
                  .filter(snapshot -> !Boolean.TRUE.equals(snapshot.deleted()))
                  .map(snapshot -> toFood(snapshot, businessSnapshot))
                  .toList());
    }
    return foods;
  }

  public Food getFoodById(Long id) {
    Optional<Food> foodOptional = foodRepository.findById(id);
    Food localFood = foodOptional.map(EntityUtils::filterEntity).orElse(null);
    if (localFood != null) {
      return localFood;
    }

    InternalCatalogClient.FoodSnapshot snapshot = internalCatalogClient.getFoodSnapshot(id);
    if (snapshot == null || Boolean.TRUE.equals(snapshot.deleted())) {
      return null;
    }

    InternalCatalogClient.BusinessSnapshot businessSnapshot =
        snapshot.businessId() == null
            ? null
            : internalCatalogClient.getBusinessSnapshot(snapshot.businessId());
    return toFood(snapshot, businessSnapshot);
  }

  private Food toFood(InternalCatalogClient.FoodSnapshot snapshot) {
    return toFood(snapshot, null);
  }

  private Food toFood(
      InternalCatalogClient.FoodSnapshot snapshot,
      InternalCatalogClient.BusinessSnapshot businessSnapshot) {
    Food food = new Food();
    food.setId(snapshot.foodId());
    food.setDeleted(snapshot.deleted());
    food.setFoodName(snapshot.foodName());
    food.setFoodExplain(snapshot.foodExplain());
    food.setFoodImg(snapshot.foodImg());
    food.setFoodPrice(snapshot.foodPrice());
    food.setStock(snapshot.stock());
    food.setRemarks(snapshot.remarks());
    if (snapshot.businessId() != null) {
      Business business = toBusiness(snapshot.businessId(), businessSnapshot);
      food.setBusiness(business);
    }
    return food;
  }

  private Business toBusiness(
      Long businessId, InternalCatalogClient.BusinessSnapshot businessSnapshot) {
    Business business = new Business();
    business.setId(businessId);
    if (businessSnapshot == null) {
      return business;
    }
    business.setDeleted(businessSnapshot.deleted());
    business.setBusinessName(businessSnapshot.businessName());
    business.setBusinessOwnerId(businessSnapshot.businessOwnerId());
    business.setBusinessAddress(businessSnapshot.businessAddress());
    business.setBusinessExplain(businessSnapshot.businessExplain());
    business.setBusinessImg(businessSnapshot.businessImg());
    business.setOrderTypeId(businessSnapshot.orderTypeId());
    business.setStartPrice(businessSnapshot.startPrice());
    business.setDeliveryPrice(businessSnapshot.deliveryPrice());
    business.setRemarks(businessSnapshot.remarks());
    business.setOpenTime(businessSnapshot.openTime());
    business.setCloseTime(businessSnapshot.closeTime());
    return business;
  }

  public void updateFood(Food food) {
    foodRepository.save(food);
  }
}
