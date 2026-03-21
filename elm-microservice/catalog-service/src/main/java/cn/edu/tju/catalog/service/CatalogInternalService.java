package cn.edu.tju.catalog.service;

import cn.edu.tju.catalog.model.vo.BusinessSnapshotVO;
import cn.edu.tju.catalog.model.vo.FoodSnapshotVO;
import cn.edu.tju.catalog.repository.BusinessRepository;
import cn.edu.tju.catalog.repository.FoodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogInternalService {
  private final BusinessRepository businessRepository;
  private final FoodRepository foodRepository;

  public CatalogInternalService(
      BusinessRepository businessRepository, FoodRepository foodRepository) {
    this.businessRepository = businessRepository;
    this.foodRepository = foodRepository;
  }

  @Transactional(readOnly = true)
  public BusinessSnapshotVO getBusinessSnapshotById(Long businessId) {
    if (businessId == null) {
      return null;
    }
    return businessRepository.findById(businessId).map(BusinessSnapshotVO::new).orElse(null);
  }

  @Transactional(readOnly = true)
  public FoodSnapshotVO getFoodSnapshotById(Long foodId) {
    if (foodId == null) {
      return null;
    }
    return foodRepository.findById(foodId).map(FoodSnapshotVO::new).orElse(null);
  }
}
