package cn.edu.tju.business.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.edu.tju.business.model.bo.Business;
import cn.edu.tju.business.model.vo.BusinessSnapshotVO;
import cn.edu.tju.business.service.BusinessInternalService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BusinessInnerControllerTest {

  @Mock private BusinessInternalService businessInternalService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new BusinessInnerController(businessInternalService)).build();
  }

  @Test
  void getBusinessesReturnsSnapshotList() throws Exception {
    when(businessInternalService.getBusinessSnapshots())
        .thenReturn(List.of(snapshot(1L, "北洋食堂"), snapshot(2L, "学一食堂")));

    mockMvc
        .perform(get("/api/inner/business"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].businessName").value("北洋食堂"))
        .andExpect(jsonPath("$.data[1].businessName").value("学一食堂"));
  }

  @Test
  void getBusinessByIdReturnsSnapshot() throws Exception {
    when(businessInternalService.getBusinessSnapshotById(1L)).thenReturn(snapshot(1L, "北洋食堂"));

    mockMvc
        .perform(get("/api/inner/business/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.businessName").value("北洋食堂"));
  }

  @Test
  void getBusinessByIdReturnsNotFoundWhenMissing() throws Exception {
    when(businessInternalService.getBusinessSnapshotById(99L)).thenReturn(null);

    mockMvc
        .perform(get("/api/inner/business/99"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Business NOT FOUND"));
  }

  private static BusinessSnapshotVO snapshot(Long id, String businessName) {
    Business business = new Business();
    business.setId(id);
    business.setBusinessName(businessName);
    business.setBusinessAddress("天津大学");
    business.setStartPrice(BigDecimal.valueOf(20));
    business.setDeliveryPrice(BigDecimal.valueOf(4));
    business.setDeleted(false);
    return new BusinessSnapshotVO(business);
  }
}