package cn.edu.tju.food.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.food.client.BusinessLookupClient;
import cn.edu.tju.food.model.bo.Food;
import cn.edu.tju.food.model.bo.StockRequestLog;
import cn.edu.tju.food.model.vo.FoodSnapshotVO;
import cn.edu.tju.food.repository.FoodRepository;
import cn.edu.tju.food.repository.StockRequestLogRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FoodInternalServiceTest {

  @Mock private FoodRepository foodRepository;
  @Mock private StockRequestLogRepository stockRequestLogRepository;
  @Mock private BusinessLookupClient businessLookupClient;

  @InjectMocks private FoodInternalService foodInternalService;

  @Test
  void getFoodSnapshotsByBusinessIdReturnsEmptyWhenBusinessDoesNotExist() {
    when(businessLookupClient.exists(88L)).thenReturn(false);

    List<FoodSnapshotVO> result = foodInternalService.getFoodSnapshotsByBusinessId(88L);

    assertThat(result).isEmpty();
    verify(foodRepository, never()).findAllByBusinessId(any());
  }

  @Test
  void reserveStockReducesStockAndPersistsLog() {
    Food food = createFood(1L, 9L, 10, false);

    when(stockRequestLogRepository.findByRequestId("reserve-1")).thenReturn(Optional.empty());
    when(foodRepository.findById(1L)).thenReturn(Optional.of(food));
    when(businessLookupClient.exists(9L)).thenReturn(true);

    boolean result =
        foodInternalService.reserveStock(
            "reserve-1",
            "order-1",
            List.of(new FoodInternalService.StockItemCommand(1L, 3)));

    assertThat(result).isTrue();
    assertThat(food.getStock()).isEqualTo(7);
    verify(foodRepository).save(food);
    verify(stockRequestLogRepository).save(any(StockRequestLog.class));
  }

  @Test
  void reserveStockReturnsFalseWhenStockIsInsufficient() {
    Food food = createFood(2L, 9L, 2, false);

    when(stockRequestLogRepository.findByRequestId("reserve-2")).thenReturn(Optional.empty());
    when(foodRepository.findById(2L)).thenReturn(Optional.of(food));
    when(businessLookupClient.exists(9L)).thenReturn(true);

    boolean result =
        foodInternalService.reserveStock(
            "reserve-2",
            "order-2",
            List.of(new FoodInternalService.StockItemCommand(2L, 5)));

    assertThat(result).isFalse();
    assertThat(food.getStock()).isEqualTo(2);
    verify(foodRepository, never()).save(any(Food.class));
    verify(stockRequestLogRepository, never()).save(any(StockRequestLog.class));
  }

  @Test
  void reserveStockReturnsExistingSuccessForDuplicateRequestId() {
    StockRequestLog existing = new StockRequestLog();
    existing.setRequestId("reserve-dup");
    existing.setSuccess(true);

    when(stockRequestLogRepository.findByRequestId("reserve-dup")).thenReturn(Optional.of(existing));

    boolean result =
        foodInternalService.reserveStock(
            "reserve-dup",
            "order-3",
            List.of(new FoodInternalService.StockItemCommand(3L, 1)));

    assertThat(result).isTrue();
    verify(foodRepository, never()).findById(any());
  }

  @Test
  void releaseStockIncreasesStock() {
    Food food = createFood(4L, 10L, 6, false);

    when(stockRequestLogRepository.findByRequestId("release-1")).thenReturn(Optional.empty());
    when(foodRepository.findById(4L)).thenReturn(Optional.of(food));
    when(businessLookupClient.exists(10L)).thenReturn(true);

    boolean result =
        foodInternalService.releaseStock(
            "release-1",
            "order-4",
            List.of(new FoodInternalService.StockItemCommand(4L, 2)));

    assertThat(result).isTrue();
    assertThat(food.getStock()).isEqualTo(8);
    verify(foodRepository).save(food);
    verify(stockRequestLogRepository).save(any(StockRequestLog.class));
  }

  private Food createFood(Long foodId, Long businessId, Integer stock, boolean deleted) {
    Food food = new Food();
    food.setId(foodId);
    food.setBusinessId(businessId);
    food.setStock(stock);
    food.setDeleted(deleted);
    food.setFoodName("food-" + foodId);
    return food;
  }
}