package cn.edu.tju.elm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.DeliveryAddress;
import cn.edu.tju.elm.model.BO.MerchantApplication;
import cn.edu.tju.elm.model.BO.Order;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResponseCompatibilityEnricherTest {

  @Mock private UserService userService;

  private ResponseCompatibilityEnricher compatibilityEnricher;

  @BeforeEach
  void setUp() {
    compatibilityEnricher = new ResponseCompatibilityEnricher(userService);
  }

  @Test
  void enrichOrderShouldFillCustomerBusinessOwnerAndAddressCustomer() {
    when(userService.getUserById(1L)).thenReturn(user(1L, "customer-1"));
    when(userService.getUserById(2L)).thenReturn(user(2L, "merchant-2"));
    when(userService.getUserById(3L)).thenReturn(user(3L, "receiver-3"));

    Business business = new Business();
    business.setBusinessOwnerId(2L);

    DeliveryAddress address = new DeliveryAddress();
    address.setCustomerId(3L);

    Order order = new Order();
    order.setCustomerId(1L);
    order.setBusiness(business);
    order.setDeliveryAddress(address);

    compatibilityEnricher.enrichOrder(order);

    assertThat(order.getCustomer().getId()).isEqualTo(1L);
    assertThat(order.getCustomer().getUsername()).isEqualTo("customer-1");
    assertThat(order.getBusiness().getBusinessOwner().getId()).isEqualTo(2L);
    assertThat(order.getBusiness().getBusinessOwner().getUsername()).isEqualTo("merchant-2");
    assertThat(order.getDeliveryAddress().getCustomer().getId()).isEqualTo(3L);
    assertThat(order.getDeliveryAddress().getCustomer().getUsername()).isEqualTo("receiver-3");
  }

  @Test
  void enrichMerchantApplicationsShouldReuseLookupCacheForSameUserId() {
    when(userService.getUserById(11L)).thenReturn(user(11L, "applicant-11"));
    when(userService.getUserById(12L)).thenReturn(user(12L, "handler-12"));

    MerchantApplication app1 = new MerchantApplication();
    app1.setApplicantId(11L);
    app1.setHandlerId(12L);

    MerchantApplication app2 = new MerchantApplication();
    app2.setApplicantId(11L);
    app2.setHandlerId(12L);

    compatibilityEnricher.enrichMerchantApplications(List.of(app1, app2));

    assertThat(app1.getApplicant().getUsername()).isEqualTo("applicant-11");
    assertThat(app1.getHandler().getUsername()).isEqualTo("handler-12");
    assertThat(app2.getApplicant().getUsername()).isEqualTo("applicant-11");
    assertThat(app2.getHandler().getUsername()).isEqualTo("handler-12");

    verify(userService, times(1)).getUserById(11L);
    verify(userService, times(1)).getUserById(12L);
  }

  @Test
  void enrichBusinessShouldKeepIdWhenUserNotFound() {
    when(userService.getUserById(77L)).thenReturn(null);

    Business business = new Business();
    business.setBusinessOwnerId(77L);

    compatibilityEnricher.enrichBusiness(business);

    assertThat(business.getBusinessOwner()).isNotNull();
    assertThat(business.getBusinessOwner().getId()).isEqualTo(77L);
    assertThat(business.getBusinessOwner().getUsername()).isNull();
  }

  private User user(Long id, String username) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    return user;
  }
}
