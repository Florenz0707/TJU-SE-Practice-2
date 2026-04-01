package cn.edu.tju.food.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
    when(foodRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(food));
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
    when(foodRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(food));
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
  void reserveStockReturnsExistingFailureForDuplicateRequestId() {
    StockRequestLog existing = new StockRequestLog();
    existing.setRequestId("reserve-failed");
    existing.setSuccess(false);

    when(stockRequestLogRepository.findByRequestId("reserve-failed"))
        .thenReturn(Optional.of(existing));

    boolean result =
        foodInternalService.reserveStock(
            "reserve-failed",
            "order-failed",
            List.of(new FoodInternalService.StockItemCommand(3L, 1)));

    assertThat(result).isFalse();
    verify(foodRepository, never()).findById(any());
    verify(stockRequestLogRepository, never()).save(any(StockRequestLog.class));
  }

  @Test
  void reserveStockReturnsFalseWhenQuantityIsNotPositive() {
    Food food = createFood(3L, 9L, 10, false);

    when(stockRequestLogRepository.findByRequestId("reserve-zero")).thenReturn(Optional.empty());
    when(foodRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(food));
    when(businessLookupClient.exists(9L)).thenReturn(true);

    boolean result =
        foodInternalService.reserveStock(
            "reserve-zero",
            "order-zero",
            List.of(new FoodInternalService.StockItemCommand(3L, 0)));

    assertThat(result).isFalse();
    assertThat(food.getStock()).isEqualTo(10);
    verify(foodRepository, never()).save(any(Food.class));
    verify(stockRequestLogRepository, never()).save(any(StockRequestLog.class));
  }

  @Test
  void reserveStockReturnsFalseWhenBusinessLookupFails() {
    Food food = createFood(5L, 11L, 8, false);

    when(stockRequestLogRepository.findByRequestId("reserve-business-missing"))
        .thenReturn(Optional.empty());
    when(foodRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(food));
    when(businessLookupClient.exists(11L)).thenReturn(false);

    boolean result =
        foodInternalService.reserveStock(
            "reserve-business-missing",
            "order-business-missing",
            List.of(new FoodInternalService.StockItemCommand(5L, 2)));

    assertThat(result).isFalse();
    assertThat(food.getStock()).isEqualTo(8);
    verify(foodRepository, never()).save(any(Food.class));
    verify(stockRequestLogRepository, never()).save(any(StockRequestLog.class));
  }

  @Test
  void reserveStockUpdatesAllFoodsWhenBatchSucceeds() {
    Food firstFood = createFood(7L, 12L, 10, false);
    Food secondFood = createFood(8L, 12L, 6, false);

    when(stockRequestLogRepository.findByRequestId("reserve-batch-ok")).thenReturn(Optional.empty());
    when(foodRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(firstFood));
    when(foodRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(secondFood));
    when(businessLookupClient.exists(12L)).thenReturn(true);

    boolean result =
        foodInternalService.reserveStock(
            "reserve-batch-ok",
            "order-batch-ok",
            List.of(
                new FoodInternalService.StockItemCommand(7L, 3),
                new FoodInternalService.StockItemCommand(8L, 2)));

    assertThat(result).isTrue();
    assertThat(firstFood.getStock()).isEqualTo(7);
    assertThat(secondFood.getStock()).isEqualTo(4);
    verify(foodRepository).save(firstFood);
    verify(foodRepository).save(secondFood);
    verify(stockRequestLogRepository).save(any(StockRequestLog.class));
  }

  @Test
  void reserveStockDoesNotSaveAnyFoodWhenBatchValidationFails() {
    Food firstFood = createFood(9L, 13L, 10, false);
    Food secondFood = createFood(10L, 13L, 1, false);

    when(stockRequestLogRepository.findByRequestId("reserve-batch-fail")).thenReturn(Optional.empty());
    when(foodRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(firstFood));
    when(foodRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(secondFood));
    when(businessLookupClient.exists(13L)).thenReturn(true);

    boolean result =
        foodInternalService.reserveStock(
            "reserve-batch-fail",
            "order-batch-fail",
            List.of(
                new FoodInternalService.StockItemCommand(9L, 3),
                new FoodInternalService.StockItemCommand(10L, 2)));

    assertThat(result).isFalse();
    assertThat(firstFood.getStock()).isEqualTo(10);
    assertThat(secondFood.getStock()).isEqualTo(1);
    verify(foodRepository, never()).save(any(Food.class));
    verify(stockRequestLogRepository, never()).save(any(StockRequestLog.class));
  }

  @Test
  void releaseStockIncreasesStock() {
    Food food = createFood(4L, 10L, 6, false);

    when(stockRequestLogRepository.findByRequestId("release-1")).thenReturn(Optional.empty());
    when(foodRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(food));
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

  @Test
  void releaseStockReturnsExistingFailureForDuplicateRequestId() {
    StockRequestLog existing = new StockRequestLog();
    existing.setRequestId("release-failed");
    existing.setSuccess(false);

    when(stockRequestLogRepository.findByRequestId("release-failed"))
        .thenReturn(Optional.of(existing));

    boolean result =
        foodInternalService.releaseStock(
            "release-failed",
            "order-release-failed",
            List.of(new FoodInternalService.StockItemCommand(4L, 2)));

    assertThat(result).isFalse();
    verify(foodRepository, never()).findById(any());
    verify(stockRequestLogRepository, never()).save(any(StockRequestLog.class));
  }

  @Test
  void releaseStockReturnsExistingSuccessForDuplicateRequestId() {
    StockRequestLog existing = new StockRequestLog();
    existing.setRequestId("release-dup");
    existing.setSuccess(true);

    when(stockRequestLogRepository.findByRequestId("release-dup")).thenReturn(Optional.of(existing));

    boolean result =
        foodInternalService.releaseStock(
            "release-dup",
            "order-release-dup",
            List.of(new FoodInternalService.StockItemCommand(4L, 2)));

    assertThat(result).isTrue();
    verify(foodRepository, never()).findById(any());
    verify(stockRequestLogRepository, never()).save(any(StockRequestLog.class));
  }

  @Test
  void releaseStockReturnsFalseWhenFoodDeleted() {
    Food food = createFood(6L, 10L, 6, true);

    when(stockRequestLogRepository.findByRequestId("release-deleted")).thenReturn(Optional.empty());
    when(foodRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(food));

    boolean result =
        foodInternalService.releaseStock(
            "release-deleted",
            "order-release-deleted",
            List.of(new FoodInternalService.StockItemCommand(6L, 2)));

    assertThat(result).isFalse();
    assertThat(food.getStock()).isEqualTo(6);
    verify(foodRepository, never()).save(any(Food.class));
    verify(stockRequestLogRepository, never()).save(any(StockRequestLog.class));
  }

  @Test
  void releaseStockReturnsFalseWhenRequestIdMissing() {
    boolean result =
        foodInternalService.releaseStock(
            null,
            "order-release-invalid",
            List.of(new FoodInternalService.StockItemCommand(6L, 2)));

    assertThat(result).isFalse();
    verify(foodRepository, never()).findByIdForUpdate(any());
    verify(stockRequestLogRepository, never()).save(any(StockRequestLog.class));
  }

  @Test
  void releaseStockReturnsFalseWhenQuantityMissing() {
    Food food = createFood(11L, 10L, 6, false);

    when(stockRequestLogRepository.findByRequestId("release-quantity-null"))
        .thenReturn(Optional.empty());
    when(foodRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(food));

    boolean result =
        foodInternalService.releaseStock(
            "release-quantity-null",
            "order-release-quantity-null",
            List.of(new FoodInternalService.StockItemCommand(11L, null)));

    assertThat(result).isFalse();
    assertThat(food.getStock()).isEqualTo(6);
    verify(foodRepository, never()).save(any(Food.class));
    verify(stockRequestLogRepository, never()).save(any(StockRequestLog.class));
  }

  @Test
  void releaseStockReturnsFalseWhenBusinessLookupFails() {
    Food food = createFood(12L, 14L, 6, false);

    when(stockRequestLogRepository.findByRequestId("release-business-missing"))
        .thenReturn(Optional.empty());
    when(foodRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(food));
    when(businessLookupClient.exists(14L)).thenReturn(false);

    boolean result =
        foodInternalService.releaseStock(
            "release-business-missing",
            "order-release-business-missing",
            List.of(new FoodInternalService.StockItemCommand(12L, 2)));

    assertThat(result).isFalse();
    assertThat(food.getStock()).isEqualTo(6);
    verify(foodRepository, never()).save(any(Food.class));
    verify(stockRequestLogRepository, never()).save(any(StockRequestLog.class));
  }

  @Test
  void releaseStockDoesNotSaveAnyFoodWhenBatchValidationFails() {
    Food firstFood = createFood(13L, 15L, 6, false);
    Food secondFood = createFood(14L, 15L, 4, false);

    when(stockRequestLogRepository.findByRequestId("release-batch-fail"))
        .thenReturn(Optional.empty());
    when(foodRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(firstFood));
    when(foodRepository.findByIdForUpdate(14L)).thenReturn(Optional.of(secondFood));
    when(businessLookupClient.exists(15L)).thenReturn(true);

    boolean result =
        foodInternalService.releaseStock(
            "release-batch-fail",
            "order-release-batch-fail",
            List.of(
                new FoodInternalService.StockItemCommand(13L, 2),
                new FoodInternalService.StockItemCommand(14L, 0)));

    assertThat(result).isFalse();
    assertThat(firstFood.getStock()).isEqualTo(6);
    assertThat(secondFood.getStock()).isEqualTo(4);
    verify(foodRepository, never()).save(any(Food.class));
    verify(stockRequestLogRepository, never()).save(any(StockRequestLog.class));
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