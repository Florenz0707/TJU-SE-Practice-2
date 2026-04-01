package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.DeliveryAddress;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.InternalAddressClient;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

  @Mock private InternalAddressClient internalAddressClient;
  @Mock private UserService userService;
  @Mock private ResponseCompatibilityEnricher compatibilityEnricher;

  @InjectMocks private AddressController addressController;

  @Test
  void addDeliveryAddress_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = addressController.addDeliveryAddress(new DeliveryAddress());

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY NOT FOUND", result.getMessage());
    verify(internalAddressClient, never()).createAddress(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void addDeliveryAddress_shouldFailWhenInternalCreateReturnsNull() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    DeliveryAddress request = new DeliveryAddress();
    request.setContactName("Tom");
    request.setContactSex(1);
    request.setContactTel("123");
    request.setAddress("Road 1");
    when(internalAddressClient.createAddress(org.mockito.ArgumentMatchers.any())).thenReturn(null);

    var result = addressController.addDeliveryAddress(request);

    assertFalse(result.getSuccess());
    assertEquals("Failed to create address", result.getMessage());
  }

  @Test
  void getMyAddresses_shouldMapSnapshotsToAddresses() {
    User me = new User();
    me.setId(9L);
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalAddressClient.getAddressesByCustomerId(9L))
        .thenReturn(List.of(new InternalAddressClient.AddressSnapshot(1L, 9L, "Tom", 1, "123", "Road 1")));

    var result = addressController.getMyAddresses();

    assertTrue(result.getSuccess());
    assertEquals(1, result.getData().size());
    assertEquals("Tom", result.getData().getFirst().getContactName());
    verify(compatibilityEnricher).enrichAddresses(result.getData());
  }

  @Test
  void updateAddress_shouldRejectOtherUsersAddress() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalAddressClient.getAddressById(100L))
        .thenReturn(new InternalAddressClient.AddressSnapshot(100L, 10L, "Tom", 1, "123", "Road 1"));

    var result = addressController.updateAddress(100L, new DeliveryAddress());

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY LACKED", result.getMessage());
    verify(internalAddressClient, never()).updateAddress(org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void updateAddress_shouldAllowOwnerAndReturnUpdatedAddress() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalAddressClient.getAddressById(100L))
        .thenReturn(new InternalAddressClient.AddressSnapshot(100L, 9L, "Tom", 1, "123", "Road 1"));
    DeliveryAddress request = new DeliveryAddress();
    request.setContactName("Jerry");
    request.setContactSex(0);
    request.setContactTel("456");
    request.setAddress("Road 2");
    when(internalAddressClient.updateAddress(org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.any()))
        .thenReturn(new InternalAddressClient.AddressSnapshot(100L, 9L, "Jerry", 0, "456", "Road 2"));

    var result = addressController.updateAddress(100L, request);

    assertTrue(result.getSuccess());
    assertEquals("Jerry", result.getData().getContactName());
    verify(compatibilityEnricher).enrichAddress(result.getData());
  }

  @Test
  void deleteAddress_shouldFailWhenInternalDeleteFails() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalAddressClient.getAddressById(100L))
        .thenReturn(new InternalAddressClient.AddressSnapshot(100L, 9L, "Tom", 1, "123", "Road 1"));
    when(internalAddressClient.deleteAddress(100L)).thenReturn(false);

    var result = addressController.deleteAddress(100L);

    assertFalse(result.getSuccess());
    assertEquals("Failed to delete address", result.getMessage());
  }
}