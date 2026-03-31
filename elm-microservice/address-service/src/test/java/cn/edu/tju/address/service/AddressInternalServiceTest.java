package cn.edu.tju.address.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.address.model.bo.DeliveryAddress;
import cn.edu.tju.address.model.vo.DeliveryAddressSnapshotVO;
import cn.edu.tju.address.repository.DeliveryAddressRepository;
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
  void createAddressSavesAndReturnsSnapshot() {
    DeliveryAddress saved = createAddress(1L, 100L, "Alice", "TJU", false);
    when(deliveryAddressRepository.save(any(DeliveryAddress.class))).thenReturn(saved);

    DeliveryAddressSnapshotVO result =
        addressInternalService.createAddress(
            new AddressInternalService.CreateAddressCommand(100L, "Alice", 1, "18800000000", "TJU"));

    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getCustomerId()).isEqualTo(100L);
    assertThat(result.getContactName()).isEqualTo("Alice");
    assertThat(result.getAddress()).isEqualTo("TJU");
    verify(deliveryAddressRepository).save(any(DeliveryAddress.class));
  }

  @Test
  void createAddressThrowsWhenCustomerIdMissing() {
    assertThatThrownBy(
            () ->
                addressInternalService.createAddress(
                    new AddressInternalService.CreateAddressCommand(null, "Alice", 1, "18800000000", "TJU")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("customerId CANT BE NULL");
  }

  @Test
  void updateAddressUpdatesExistingAddress() {
    DeliveryAddress address = createAddress(2L, 101L, "Bob", "Old", false);
    when(deliveryAddressRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(address));
    when(deliveryAddressRepository.save(address)).thenReturn(address);

    DeliveryAddressSnapshotVO result =
        addressInternalService.updateAddress(
            2L, new AddressInternalService.UpdateAddressCommand(102L, "Bob2", 0, "17700000000", "New"));

    assertThat(result.getCustomerId()).isEqualTo(102L);
    assertThat(result.getContactName()).isEqualTo("Bob2");
    assertThat(result.getAddress()).isEqualTo("New");
    verify(deliveryAddressRepository).save(address);
  }

  @Test
  void getAddressesByCustomerIdReturnsSnapshots() {
    when(deliveryAddressRepository.findAllByCustomerIdAndDeletedFalse(103L))
        .thenReturn(List.of(createAddress(3L, 103L, "C1", "A1", false), createAddress(4L, 103L, "C2", "A2", false)));

    List<DeliveryAddressSnapshotVO> result = addressInternalService.getAddressesByCustomerId(103L);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getCustomerId()).isEqualTo(103L);
    assertThat(result.get(1).getCustomerId()).isEqualTo(103L);
  }

  @Test
  void deleteAddressMarksEntityDeleted() {
    DeliveryAddress address = createAddress(5L, 104L, "D", "Addr", false);
    when(deliveryAddressRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(address));
    when(deliveryAddressRepository.save(address)).thenReturn(address);

    boolean deleted = addressInternalService.deleteAddress(5L);

    assertThat(deleted).isTrue();
    assertThat(address.getDeleted()).isTrue();
    verify(deliveryAddressRepository).save(address);
  }

  private DeliveryAddress createAddress(
      Long id, Long customerId, String contactName, String addressValue, boolean deleted) {
    DeliveryAddress address = new DeliveryAddress();
    address.setId(id);
    address.setCustomerId(customerId);
    address.setContactName(contactName);
    address.setAddress(addressValue);
    address.setDeleted(deleted);
    return address;
  }
}