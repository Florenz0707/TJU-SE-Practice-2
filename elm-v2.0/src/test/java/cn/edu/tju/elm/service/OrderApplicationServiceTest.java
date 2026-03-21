package cn.edu.tju.elm.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.elm.constant.OrderState;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Cart;
import cn.edu.tju.elm.model.BO.DeliveryAddress;
import cn.edu.tju.elm.model.BO.Food;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.model.BO.PrivateVoucher;
import cn.edu.tju.elm.utils.InternalAccountClient;
import cn.edu.tju.elm.utils.InternalCatalogClient;
import cn.edu.tju.elm.utils.InternalServiceClient;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {
  @Mock private OrderService orderService;
  @Mock private FoodService foodService;
  @Mock private AddressService addressService;
  @Mock private CartItemService cartItemService;
  @Mock private OrderDetailetService orderDetailetService;
  @Mock private InternalAccountClient internalAccountClient;
  @Mock private InternalCatalogClient internalCatalogClient;
  @Mock private InternalServiceClient internalServiceClient;
  @Mock private IntegrationOutboxService integrationOutboxService;
  @Mock private ResponseCompatibilityEnricher compatibilityEnricher;

  @InjectMocks private OrderApplicationService orderApplicationService;

  @Test
  void addOrder_shouldFail_whenRemoteBusinessNotFound() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(orderService.getOrderByRequestId("req-not-found")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L)).thenReturn(null);

    var result = orderApplicationService.addOrder(userId, order, "req-not-found");

    assertFalse(result.getSuccess());
    verify(addressService, never()).getAddressById(any());
    verify(internalAccountClient, never()).getWalletByUserId(any(), anyBoolean());
  }

  @Test
  void addOrder_shouldFail_whenRemoteWalletBalanceInsufficient() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);
    order.setWalletPaid(new BigDecimal("20"));

    Business business = new Business();
    business.setId(1L);
    business.setDeliveryPrice(BigDecimal.ZERO);
    DeliveryAddress address = new DeliveryAddress();
    address.setId(2L);
    address.setCustomerId(userId);
    Food food = new Food();
    food.setId(100L);
    food.setFoodName("rice");
    food.setFoodPrice(new BigDecimal("30"));
    food.setStock(10);
    Cart cart = new Cart();
    cart.setFood(food);
    cart.setQuantity(1);

    when(orderService.getOrderByRequestId("req-1")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, 1L, false, new BigDecimal("30"), 10));
    when(addressService.getAddressById(2L)).thenReturn(address);
    when(cartItemService.getCart(1L, userId)).thenReturn(List.of(cart));
    when(internalAccountClient.getWalletByUserId(userId, true))
        .thenReturn(new InternalAccountClient.WalletSnapshot(1L, userId, new BigDecimal("10")));

    var result = orderApplicationService.addOrder(userId, order, "req-1");

    assertFalse(result.getSuccess());
    verify(internalAccountClient, never()).debitWallet(any(), any(), any(), any(), any());
  }

  @Test
  void cancelOrder_shouldCallAccountRollbackAndRefund() {
    Long userId = 9L;
    Order order = new Order();
    order.setId(123L);
    order.setCustomerId(userId);
    order.setOrderState(OrderState.PAID);
    order.setWalletPaid(new BigDecimal("15"));
    order.setPointsUsed(0);
    PrivateVoucher usedVoucher = new PrivateVoucher();
    usedVoucher.setId(66L);
    order.setUsedVoucher(usedVoucher);

    when(orderService.getOrderById(123L)).thenReturn(order);
    when(orderDetailetService.getOrderDetailetsByOrderId(123L)).thenReturn(Collections.emptyList());
    when(internalAccountClient.refundWallet(
            any(), eq(userId), eq(new BigDecimal("15")), any(), any()))
        .thenReturn(true);
    when(internalAccountClient.rollbackVoucher(any(), eq(userId), eq(66L), any(), any()))
        .thenReturn(true);

    var result = orderApplicationService.cancelOrder(userId, 123L);

    assertTrue(result.getSuccess());
    verify(internalAccountClient)
        .refundWallet(
            eq("order-cancel-123-wallet-refund"),
            eq(userId),
            eq(new BigDecimal("15")),
            eq("ORDER_123"),
            eq("order cancel refund"));
    verify(internalAccountClient)
        .rollbackVoucher(
            eq("order-cancel-123-voucher-rollback"),
            eq(userId),
            eq(66L),
            eq("ORDER_123"),
            eq("order cancel rollback voucher"));
    verify(orderService).updateOrder(order);
  }
}
