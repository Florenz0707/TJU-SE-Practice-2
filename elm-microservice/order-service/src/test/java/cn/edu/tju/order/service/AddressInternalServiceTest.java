package cn.edu.tju.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import cn.edu.tju.order.model.bo.DeliveryAddress;
import cn.edu.tju.order.repository.DeliveryAddressRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressInternalServiceTest {
  @Mock private DeliveryAddressRepository deliveryAddressRepository;

  @InjectMocks private AddressInternalService addressInternalService;

  @Test
  void createAddress_shouldReturnCreatedSnapshot() {
    DeliveryAddress saved = new DeliveryAddress();
    saved.setId(10L);
    saved.setCustomerId(9L);
    saved.setAddress("addr");
    when(deliveryAddressRepository.save(org.mockito.ArgumentMatchers.any(DeliveryAddress.class)))
        .thenReturn(saved);

    var result =
        addressInternalService.createAddress(
            new AddressInternalService.CreateAddressCommand(9L, "n", 1, "188", "addr"));

    assertEquals(10L, result.getId());
    assertEquals(9L, result.getCustomerId());
  }

  @Test
  void getAddressById_shouldReturnNullWhenMissing() {
    when(deliveryAddressRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.empty());
    var result = addressInternalService.getAddressById(100L);
    assertEquals(null, result);
  }

  @Test
  void getAddressesByCustomerId_shouldReturnList() {
    DeliveryAddress address = new DeliveryAddress();
    address.setId(1L);
    address.setCustomerId(9L);
    when(deliveryAddressRepository.findAllByCustomerIdAndDeletedFalse(9L))
        .thenReturn(List.of(address));

    var result = addressInternalService.getAddressesByCustomerId(9L);

    assertEquals(1, result.size());
    assertEquals(1L, result.getFirst().getId());
  }

  @Test
  void updateAddress_shouldThrowWhenMissing() {
    when(deliveryAddressRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            addressInternalService.updateAddress(
                1L, new AddressInternalService.UpdateAddressCommand(9L, "n", 1, "188", "addr")));
  }

  @Test
  void deleteAddress_shouldReturnTrueWhenSuccess() {
    DeliveryAddress address = new DeliveryAddress();
    address.setId(1L);
    address.setDeleted(false);
    when(deliveryAddressRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(address));
    when(deliveryAddressRepository.save(address)).thenReturn(address);

    boolean result = addressInternalService.deleteAddress(1L);

    assertTrue(result);
    assertNotNull(address.getUpdateTime());
    assertTrue(Boolean.TRUE.equals(address.getDeleted()));
  }
}
