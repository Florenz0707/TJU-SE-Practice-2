package cn.edu.tju.catalog.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import cn.edu.tju.catalog.model.bo.Business;
import cn.edu.tju.catalog.model.bo.Food;
import cn.edu.tju.catalog.model.vo.BusinessSnapshotVO;
import cn.edu.tju.catalog.model.vo.FoodSnapshotVO;
import cn.edu.tju.catalog.service.CatalogInternalService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogInnerControllerTest {

  @Mock private CatalogInternalService catalogInternalService;

  @InjectMocks private CatalogInnerController catalogInnerController;

  @Test
  void getBusinessById_shouldReturnSuccess_whenFound() {
    Business business = new Business();
    business.setId(1L);
    business.setDeliveryPrice(new BigDecimal("3"));
    when(catalogInternalService.getBusinessSnapshotById(1L))
        .thenReturn(new BusinessSnapshotVO(business));

    var result = catalogInnerController.getBusinessById(1L);

    assertTrue(result.getSuccess());
    assertEquals(1L, result.getData().getId());
  }

  @Test
  void getBusinessById_shouldReturnFailure_whenMissing() {
    when(catalogInternalService.getBusinessSnapshotById(99L)).thenReturn(null);

    var result = catalogInnerController.getBusinessById(99L);

    assertFalse(result.getSuccess());
  }

  @Test
  void getFoodById_shouldReturnSuccess_whenFound() {
    Business business = new Business();
    business.setId(2L);
    Food food = new Food();
    food.setId(8L);
    food.setBusiness(business);
    food.setStock(9);
    when(catalogInternalService.getFoodSnapshotById(8L)).thenReturn(new FoodSnapshotVO(food));

    var result = catalogInnerController.getFoodById(8L);

    assertTrue(result.getSuccess());
    assertEquals(2L, result.getData().getBusinessId());
    assertEquals(9, result.getData().getStock());
  }
}
