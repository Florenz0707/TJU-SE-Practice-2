package cn.edu.tju.catalog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.catalog.model.bo.Business;
import cn.edu.tju.catalog.model.bo.Food;
import cn.edu.tju.catalog.model.bo.StockRequestLog;
import cn.edu.tju.catalog.repository.BusinessRepository;
import cn.edu.tju.catalog.repository.FoodRepository;
import cn.edu.tju.catalog.repository.StockRequestLogRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogInternalServiceTest {

  @Mock private BusinessRepository businessRepository;
  @Mock private FoodRepository foodRepository;
  @Mock private StockRequestLogRepository stockRequestLogRepository;

  @InjectMocks private CatalogInternalService catalogInternalService;

  @Test
  void getBusinessSnapshotById_shouldReturnSnapshot_whenExists() {
    Business business = new Business();
    business.setId(11L);
    business.setStartPrice(new BigDecimal("15"));
    when(businessRepository.findById(11L)).thenReturn(Optional.of(business));

    var result = catalogInternalService.getBusinessSnapshotById(11L);

    assertNotNull(result);
    assertEquals(11L, result.getId());
    assertEquals(0, result.getStartPrice().compareTo(new BigDecimal("15")));
  }

  @Test
  void getFoodSnapshotById_shouldReturnNull_whenMissing() {
    when(foodRepository.findById(22L)).thenReturn(Optional.empty());

    var result = catalogInternalService.getFoodSnapshotById(22L);

    assertNull(result);
  }

  @Test
  void getFoodSnapshotById_shouldContainBusinessId_whenExists() {
    Business business = new Business();
    business.setId(100L);
    Food food = new Food();
    food.setId(22L);
    food.setBusiness(business);
    food.setFoodPrice(new BigDecimal("12.50"));
    food.setStock(7);
    when(foodRepository.findById(22L)).thenReturn(Optional.of(food));

    var result = catalogInternalService.getFoodSnapshotById(22L);

    assertNotNull(result);
    assertEquals(22L, result.getId());
    assertEquals(100L, result.getBusinessId());
    assertEquals(7, result.getStock());
  }

  @Test
  void reserveStock_shouldDeductAndLog_whenStockEnough() {
    Food food = new Food();
    food.setId(7L);
    food.setStock(10);
    when(stockRequestLogRepository.findByRequestId("req-1")).thenReturn(Optional.empty());
    when(foodRepository.findById(7L)).thenReturn(Optional.of(food));

    boolean success =
        catalogInternalService.reserveStock(
            "req-1", "ORDER_1", List.of(new CatalogInternalService.StockItemCommand(7L, 3)));

    assertTrue(success);
    assertEquals(7, food.getStock());
    verify(foodRepository).save(food);
    verify(stockRequestLogRepository).save(org.mockito.ArgumentMatchers.any(StockRequestLog.class));
  }

  @Test
  void reserveStock_shouldReturnIdempotentSuccess_whenRequestAlreadyProcessed() {
    StockRequestLog log = new StockRequestLog();
    log.setRequestId("req-idempotent");
    log.setSuccess(true);
    when(stockRequestLogRepository.findByRequestId("req-idempotent")).thenReturn(Optional.of(log));

    boolean success =
        catalogInternalService.reserveStock(
            "req-idempotent",
            "ORDER_2",
            List.of(new CatalogInternalService.StockItemCommand(8L, 1)));

    assertTrue(success);
    verify(foodRepository, never()).findById(8L);
  }
}
