package cn.edu.tju.address.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.edu.tju.address.model.bo.DeliveryAddress;
import cn.edu.tju.address.model.vo.DeliveryAddressSnapshotVO;
import cn.edu.tju.address.service.AddressInternalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AddressInnerControllerTest {

  @Mock private AddressInternalService addressInternalService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new AddressInnerController(addressInternalService)).build();
  }

  @Test
  void createAddressRejectsMissingCustomerId() throws Exception {
    mockMvc
        .perform(
            post("/api/inner/address")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"address\":\"TJU\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value(40400))
        .andExpect(jsonPath("$.message").value("CustomerId CANT BE NULL"));
  }

  @Test
  void createAddressReturnsServerErrorWhenServiceThrows() throws Exception {
    when(addressInternalService.createAddress(any())).thenThrow(new IllegalStateException("create failed"));

    mockMvc
        .perform(
            post("/api/inner/address")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":1,\"address\":\"TJU\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value(50000))
        .andExpect(jsonPath("$.message").value("create failed"));
  }

  @Test
  void updateAddressReturnsSnapshot() throws Exception {
    when(addressInternalService.updateAddress(any(), any())).thenReturn(snapshot(7L, 3L, "新校区"));

    mockMvc
        .perform(
            put("/api/inner/address/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":3,\"address\":\"新校区\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(7))
        .andExpect(jsonPath("$.data.customerId").value(3))
        .andExpect(jsonPath("$.data.address").value("新校区"));
  }

  @Test
  void getAddressByIdReturnsNotFoundWhenMissing() throws Exception {
    when(addressInternalService.getAddressById(9L)).thenReturn(null);

    mockMvc
        .perform(get("/api/inner/address/9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Address NOT FOUND"));
  }

  private static DeliveryAddressSnapshotVO snapshot(Long id, Long customerId, String addressText) {
    DeliveryAddress address = new DeliveryAddress();
    address.setId(id);
    address.setCustomerId(customerId);
    address.setAddress(addressText);
    return new DeliveryAddressSnapshotVO(address);
  }
}