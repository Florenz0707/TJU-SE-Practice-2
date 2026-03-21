package cn.edu.tju.catalog.service;

import cn.edu.tju.catalog.model.bo.Food;
import cn.edu.tju.catalog.model.bo.StockRequestLog;
import cn.edu.tju.catalog.model.vo.BusinessSnapshotVO;
import cn.edu.tju.catalog.model.vo.FoodSnapshotVO;
import cn.edu.tju.catalog.repository.BusinessRepository;
import cn.edu.tju.catalog.repository.FoodRepository;
import cn.edu.tju.catalog.repository.StockRequestLogRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogInternalService {
  private final BusinessRepository businessRepository;
  private final FoodRepository foodRepository;
  private final StockRequestLogRepository stockRequestLogRepository;

  public CatalogInternalService(
      BusinessRepository businessRepository,
      FoodRepository foodRepository,
      StockRequestLogRepository stockRequestLogRepository) {
    this.businessRepository = businessRepository;
    this.foodRepository = foodRepository;
    this.stockRequestLogRepository = stockRequestLogRepository;
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

  @Transactional
  public boolean reserveStock(String requestId, String orderId, List<StockItemCommand> items) {
    return adjustStock(requestId, orderId, items, true);
  }

  @Transactional
  public boolean releaseStock(String requestId, String orderId, List<StockItemCommand> items) {
    return adjustStock(requestId, orderId, items, false);
  }

  private boolean adjustStock(
      String requestId, String orderId, List<StockItemCommand> items, boolean reserve) {
    if (!isValidRequest(requestId, items)) {
      return false;
    }
    StockRequestLog existing = stockRequestLogRepository.findByRequestId(requestId).orElse(null);
    if (existing != null) {
      return Boolean.TRUE.equals(existing.getSuccess());
    }

    Map<Long, Food> foodMap = new HashMap<>();
    for (StockItemCommand item : items) {
      Food food = foodRepository.findById(item.foodId()).orElse(null);
      if (food == null || Boolean.TRUE.equals(food.getDeleted()) || item.quantity() == null) {
        return false;
      }
      if (item.quantity() <= 0) {
        return false;
      }
      if (reserve && (food.getStock() == null || food.getStock() < item.quantity())) {
        return false;
      }
      foodMap.put(item.foodId(), food);
    }

    for (StockItemCommand item : items) {
      Food food = foodMap.get(item.foodId());
      int currentStock = food.getStock() == null ? 0 : food.getStock();
      int nextStock = reserve ? currentStock - item.quantity() : currentStock + item.quantity();
      food.setStock(nextStock);
      foodRepository.save(food);
    }
    saveSuccessRequest(requestId, reserve ? "reserve" : "release", orderId);
    return true;
  }

  private boolean isValidRequest(String requestId, List<StockItemCommand> items) {
    return requestId != null
        && !requestId.isEmpty()
        && items != null
        && !items.isEmpty()
        && items.stream().noneMatch(item -> item == null || item.foodId() == null);
  }

  private void saveSuccessRequest(String requestId, String action, String orderId) {
    StockRequestLog log = new StockRequestLog();
    log.setRequestId(requestId);
    log.setAction(action);
    log.setOrderId(orderId);
    log.setSuccess(true);
    stockRequestLogRepository.save(log);
  }

  public record StockItemCommand(Long foodId, Integer quantity) {}
}
