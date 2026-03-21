package cn.edu.tju.catalog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import cn.edu.tju.catalog.model.bo.Business;
import cn.edu.tju.catalog.model.bo.Food;
import cn.edu.tju.catalog.repository.BusinessRepository;
import cn.edu.tju.catalog.repository.FoodRepository;
import java.math.BigDecimal;
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
}
