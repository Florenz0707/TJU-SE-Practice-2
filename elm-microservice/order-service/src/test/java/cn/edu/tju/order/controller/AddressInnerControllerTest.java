package cn.edu.tju.order.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import cn.edu.tju.order.model.bo.DeliveryAddress;
import cn.edu.tju.order.model.vo.DeliveryAddressSnapshotVO;
import cn.edu.tju.order.service.AddressInternalService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressInnerControllerTest {
  @Mock private AddressInternalService addressInternalService;

  @InjectMocks private AddressInnerController addressInnerController;

  @Test
  void createAddress_shouldFailWhenCustomerMissing() {
    AddressInnerController.AddressUpsertRequest request =
        new AddressInnerController.AddressUpsertRequest();
    var result = addressInnerController.createAddress(request);
    assertFalse(result.getSuccess());
  }

  @Test
  void getAddressById_shouldReturnData() {
    DeliveryAddress address = new DeliveryAddress();
    address.setId(1L);
    when(addressInternalService.getAddressById(1L))
        .thenReturn(new DeliveryAddressSnapshotVO(address));
    var result = addressInnerController.getAddressById(1L);
    assertTrue(result.getSuccess());
  }

  @Test
  void getAddressesByCustomerId_shouldReturnList() {
    when(addressInternalService.getAddressesByCustomerId(9L)).thenReturn(List.of());
    var result = addressInnerController.getAddressesByCustomerId(9L);
    assertTrue(result.getSuccess());
  }

  @Test
  void updateAddress_shouldFailWhenBodyMissing() {
    var result = addressInnerController.updateAddress(1L, null);
    assertFalse(result.getSuccess());
  }
}
