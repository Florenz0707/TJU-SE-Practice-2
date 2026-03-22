package cn.edu.tju.elm.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.elm.constant.OrderState;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Cart;
import cn.edu.tju.elm.model.BO.DeliveryAddress;
import cn.edu.tju.elm.model.BO.Food;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.utils.InternalAccountClient;
import cn.edu.tju.elm.utils.InternalCatalogClient;
import cn.edu.tju.elm.utils.InternalOrderClient;
import cn.edu.tju.elm.utils.InternalServiceClient;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {
  @Mock private BusinessService businessService;
  @Mock private AddressService addressService;
  @Mock private CartItemService cartItemService;
  @Mock private InternalAccountClient internalAccountClient;
  @Mock private InternalCatalogClient internalCatalogClient;
  @Mock private InternalOrderClient internalOrderClient;
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

    when(internalOrderClient.getOrderByRequestId("req-not-found")).thenReturn(null);
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

    when(internalOrderClient.getOrderByRequestId("req-1")).thenReturn(null);
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
    verify(internalOrderClient, never()).createOrder(any());
  }

  @Test
  void addOrder_shouldFail_whenReserveStockFailed() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    DeliveryAddress address = new DeliveryAddress();
    address.setId(2L);
    address.setCustomerId(userId);
    Food food = new Food();
    food.setId(100L);
    food.setFoodName("rice");
    Cart cart = new Cart();
    cart.setFood(food);
    cart.setQuantity(1);

    when(internalOrderClient.getOrderByRequestId("req-reserve-fail")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, 1L, false, new BigDecimal("30"), 10));
    when(addressService.getAddressById(2L)).thenReturn(address);
    when(cartItemService.getCart(1L, userId)).thenReturn(List.of(cart));
    when(internalCatalogClient.reserveStock(any(), any(), any())).thenReturn(false);

    var result = orderApplicationService.addOrder(userId, order, "req-reserve-fail");

    assertFalse(result.getSuccess());
    verify(internalOrderClient, never()).createOrder(any());
    verify(cartItemService, never()).deleteCart(any());
  }

  @Test
  void addOrder_shouldSucceed_whenCartCleanupFailedAfterCreate() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    DeliveryAddress address = new DeliveryAddress();
    address.setId(2L);
    address.setCustomerId(userId);
    Food food = new Food();
    food.setId(100L);
    food.setFoodName("rice");
    Cart cart = new Cart();
    cart.setFood(food);
    cart.setQuantity(1);

    when(internalOrderClient.getOrderByRequestId("req-create-ok")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, 1L, false, new BigDecimal("30"), 10));
    when(addressService.getAddressById(2L)).thenReturn(address);
    when(cartItemService.getCart(1L, userId)).thenReturn(List.of(cart));
    when(internalCatalogClient.reserveStock(any(), any(), any())).thenReturn(true);
    when(internalOrderClient.createOrder(any()))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                1000L,
                userId,
                1L,
                2L,
                OrderState.PAID,
                new BigDecimal("30"),
                null,
                null,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                "req-create-ok",
                java.time.LocalDateTime.now()));
    doThrow(new RuntimeException("cleanup failed"))
        .when(cartItemService)
        .deleteCart(any(Cart.class));

    var result = orderApplicationService.addOrder(userId, order, "req-create-ok");

    assertTrue(result.getSuccess());
    verify(internalOrderClient).createOrder(any());
    verify(internalCatalogClient, never())
        .releaseStock(eq("req-create-ok:stock-reserve-rollback"), any(), any());
  }

  @Test
  void cancelOrder_shouldCallAccountRollbackAndRefund() {
    Long userId = 9L;
    when(internalOrderClient.getOrderById(123L))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                123L,
                userId,
                1L,
                2L,
                OrderState.PAID,
                new BigDecimal("30"),
                66L,
                new BigDecimal("5"),
                0,
                BigDecimal.ZERO,
                new BigDecimal("15"),
                null,
                "req-cancel",
                java.time.LocalDateTime.now()));
    when(internalOrderClient.getOrderDetailsByOrderId(123L)).thenReturn(Collections.emptyList());
    when(internalAccountClient.refundWallet(
            any(), eq(userId), eq(new BigDecimal("15")), any(), any()))
        .thenReturn(true);
    when(internalAccountClient.rollbackVoucher(any(), eq(userId), eq(66L), any(), any()))
        .thenReturn(true);
    when(internalCatalogClient.releaseStock(any(), any(), any())).thenReturn(true);
    when(internalOrderClient.cancelOrder(123L, userId))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                123L,
                userId,
                1L,
                2L,
                OrderState.CANCELED,
                new BigDecimal("30"),
                66L,
                new BigDecimal("5"),
                0,
                BigDecimal.ZERO,
                new BigDecimal("15"),
                null,
                "req-cancel",
                java.time.LocalDateTime.now()));

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
    verify(internalCatalogClient)
        .releaseStock(
            eq("order-cancel-123-stock-release"), eq("ORDER_123"), eq(Collections.emptyList()));
    verify(internalOrderClient).cancelOrder(123L, userId);
  }

  @Test
  void cancelOrder_shouldFail_whenReleaseStockFailed() {
    Long userId = 9L;
    when(internalOrderClient.getOrderById(124L))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                124L,
                userId,
                1L,
                2L,
                OrderState.PAID,
                new BigDecimal("30"),
                null,
                null,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                "req-cancel-2",
                java.time.LocalDateTime.now()));
    when(internalOrderClient.getOrderDetailsByOrderId(124L)).thenReturn(Collections.emptyList());
    when(internalCatalogClient.releaseStock(any(), any(), any())).thenReturn(false);

    var result = orderApplicationService.cancelOrder(userId, 124L);

    assertFalse(result.getSuccess());
    verify(internalOrderClient, never()).cancelOrder(124L, userId);
  }

  @Test
  void getOrdersByCustomerIdPage_shouldReturnPagedResult() {
    Long userId = 9L;
    when(internalOrderClient.getOrdersByCustomerId(userId, 1, 5))
        .thenReturn(
            new InternalOrderClient.PagedOrderSnapshot(
                List.of(
                    new InternalOrderClient.OrderSnapshot(
                        1L,
                        userId,
                        2L,
                        3L,
                        OrderState.PAID,
                        new BigDecimal("20"),
                        null,
                        null,
                        0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        "req-page-1",
                        java.time.LocalDateTime.now())),
                9L,
                1,
                5));

    Map<String, Object> result = orderApplicationService.getOrdersByCustomerId(userId, 1, 5);

    assertTrue(result.containsKey("records"));
    assertTrue(result.containsKey("total"));
    assertTrue(result.get("records") instanceof List);
    assertTrue(((List<?>) result.get("records")).size() == 1);
    assertTrue(result.get("total").equals(9L));
  }

  @Test
  void updateOrderStatus_shouldUpdateByOwner() {
    Long userId = 9L;
    Order request = new Order();
    request.setId(200L);
    request.setOrderState(OrderState.COMPLETE);

    Business business = new Business();
    business.setId(1L);
    business.setBusinessOwnerId(userId);

    when(internalOrderClient.getOrderById(200L))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                200L,
                100L,
                1L,
                2L,
                OrderState.DELIVERY,
                new BigDecimal("30"),
                null,
                null,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                "req-state",
                java.time.LocalDateTime.now()));
    when(businessService.getBusinessById(1L)).thenReturn(business);
    when(internalOrderClient.updateOrderState(200L, OrderState.COMPLETE))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                200L,
                100L,
                1L,
                2L,
                OrderState.COMPLETE,
                new BigDecimal("30"),
                null,
                null,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                "req-state",
                java.time.LocalDateTime.now()));

    var result = orderApplicationService.updateOrderStatus(userId, false, true, request);

    assertTrue(result.getSuccess());
    verify(internalOrderClient).updateOrderState(200L, OrderState.COMPLETE);
  }

  @Test
  void updateOrderStatus_shouldFail_whenTransitionInvalid() {
    Long userId = 9L;
    Order request = new Order();
    request.setId(201L);
    request.setOrderState(OrderState.PAID);
    when(internalOrderClient.getOrderById(201L))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                201L,
                userId,
                1L,
                2L,
                OrderState.COMPLETE,
                new BigDecimal("30"),
                null,
                null,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                "req-state-2",
                java.time.LocalDateTime.now()));

    var result = orderApplicationService.updateOrderStatus(userId, false, false, request);

    assertFalse(result.getSuccess());
    verify(internalOrderClient, never()).updateOrderState(any(), any());
  }
}
