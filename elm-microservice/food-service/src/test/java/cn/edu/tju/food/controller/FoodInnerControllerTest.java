package cn.edu.tju.food.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.edu.tju.food.model.bo.Food;
import cn.edu.tju.food.model.vo.FoodSnapshotVO;
import cn.edu.tju.food.service.FoodInternalService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FoodInnerControllerTest {

  @Mock private FoodInternalService foodInternalService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new FoodInnerController(foodInternalService)).build();
  }

  @Test
  void getFoodsByBusinessIdReturnsSnapshots() throws Exception {
    when(foodInternalService.getFoodSnapshotsByBusinessId(9L))
        .thenReturn(List.of(snapshot(1L, 9L, "宫保鸡丁"), snapshot(2L, 9L, "鱼香肉丝")));

    mockMvc
        .perform(get("/api/inner/food").param("businessId", "9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].foodName").value("宫保鸡丁"))
        .andExpect(jsonPath("$.data[1].foodName").value("鱼香肉丝"));
  }

  @Test
  void getFoodByIdReturnsNotFoundWhenMissing() throws Exception {
    when(foodInternalService.getFoodSnapshotById(5L)).thenReturn(null);

    mockMvc
        .perform(get("/api/inner/food/5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Food NOT FOUND"));
  }

  @Test
  void reserveStockRejectsEmptyItems() throws Exception {
    mockMvc
        .perform(
            post("/api/inner/food/stock/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\":\"r1\",\"orderId\":\"o1\",\"items\":[]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Items CANT BE EMPTY"));

    verify(foodInternalService, never()).reserveStock(eq("r1"), eq("o1"), anyList());
  }

  @Test
  void reserveStockReturnsSuccessWhenServiceAcceptsRequest() throws Exception {
    when(foodInternalService.reserveStock(eq("reserve-1"), eq("order-1"), anyList())).thenReturn(true);

    mockMvc
        .perform(
            post("/api/inner/food/stock/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "requestId": "reserve-1",
                      "orderId": "order-1",
                      "items": [{"foodId": 1, "quantity": 2}]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(true));
  }

  @Test
  void reserveStockReturnsServerErrorWhenServiceRejectsRequest() throws Exception {
    when(foodInternalService.reserveStock(eq("reserve-2"), eq("order-2"), anyList())).thenReturn(false);

    mockMvc
        .perform(
            post("/api/inner/food/stock/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "requestId": "reserve-2",
                      "orderId": "order-2",
                      "items": [{"foodId": 1, "quantity": 2}]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GENERAL_ERROR"))
        .andExpect(jsonPath("$.message").value("Failed to reserve stock"));
  }

  @Test
  void releaseStockRejectsEmptyItems() throws Exception {
    mockMvc
        .perform(
            post("/api/inner/food/stock/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\":\"r2\",\"orderId\":\"o2\",\"items\":[]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Items CANT BE EMPTY"));

    verify(foodInternalService, never()).releaseStock(eq("r2"), eq("o2"), anyList());
  }

  @Test
  void releaseStockReturnsServerErrorWhenServiceRejectsRequest() throws Exception {
    when(foodInternalService.releaseStock(eq("release-1"), eq("order-2"), anyList())).thenReturn(false);

    mockMvc
        .perform(
            post("/api/inner/food/stock/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "requestId": "release-1",
                      "orderId": "order-2",
                      "items": [{"foodId": 1, "quantity": 2}]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("GENERAL_ERROR"))
        .andExpect(jsonPath("$.message").value("Failed to release stock"));
  }

  @Test
  void releaseStockReturnsSuccessWhenServiceAcceptsRequest() throws Exception {
    when(foodInternalService.releaseStock(eq("release-2"), eq("order-3"), anyList())).thenReturn(true);

    mockMvc
        .perform(
            post("/api/inner/food/stock/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "requestId": "release-2",
                      "orderId": "order-3",
                      "items": [{"foodId": 1, "quantity": 2}]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(true));
  }

  private static FoodSnapshotVO snapshot(Long id, Long businessId, String foodName) {
    Food food = new Food();
    food.setId(id);
    food.setBusinessId(businessId);
    food.setFoodName(foodName);
    food.setFoodPrice(BigDecimal.valueOf(18));
    food.setStock(10);
    food.setDeleted(false);
    return new FoodSnapshotVO(food);
  }
}