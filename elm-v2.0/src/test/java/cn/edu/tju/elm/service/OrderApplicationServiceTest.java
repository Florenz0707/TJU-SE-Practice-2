package cn.edu.tju.elm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.elm.constant.OrderState;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.DeliveryAddress;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.utils.InternalAccountClient;
import cn.edu.tju.elm.utils.InternalAddressClient;
import cn.edu.tju.elm.utils.InternalCatalogClient;
import cn.edu.tju.elm.utils.InternalOrderClient;
import cn.edu.tju.elm.utils.InternalServiceClient;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
  @Mock private InternalAccountClient internalAccountClient;
    @Mock private InternalAddressClient internalAddressClient;
  @Mock private InternalCatalogClient internalCatalogClient;
  @Mock private InternalOrderClient internalOrderClient;
  @Mock private InternalServiceClient internalServiceClient;
  @Mock private IntegrationOutboxService integrationOutboxService;
  @Mock private ResponseCompatibilityEnricher compatibilityEnricher;

  @InjectMocks private OrderApplicationService orderApplicationService;

  @Test
    void addOrder_shouldReturnExistingOrder_whenRequestIdAlreadyExists() {
        Long userId = 9L;
        Business business = new Business();
        business.setId(1L);

        when(internalOrderClient.getOrderByRequestId("req-duplicate"))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                501L,
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
                                "req-duplicate",
                                LocalDateTime.now()));
        when(businessService.getBusinessById(1L)).thenReturn(business);
        when(internalAddressClient.getAddressById(2L))
                .thenReturn(
                        new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));

        var result = orderApplicationService.addOrder(userId, new Order(), "req-duplicate");

        assertTrue(result.getSuccess());
        assertEquals(501L, result.getData().getId());
        verify(compatibilityEnricher).enrichOrder(result.getData());
        verify(internalCatalogClient, never()).getBusinessSnapshot(any());
        verify(internalOrderClient, never()).createOrder(any());
    }

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
    verify(internalAddressClient, never()).getAddressById(any());
    verify(internalAccountClient, never()).getWalletByUserId(any(), anyBoolean());
  }

  @Test
  void addOrder_shouldFail_whenBusinessUnavailable() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalOrderClient.getOrderByRequestId("req-business-deleted")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null));

    var result = orderApplicationService.addOrder(userId, order, "req-business-deleted");

    assertFalse(result.getSuccess());
    assertEquals("Business NOT AVAILABLE", result.getMessage());
    verify(internalAddressClient, never()).getAddressById(any());
  }

  @Test
  void addOrder_shouldFail_whenDeliveryAddressNotFound() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalOrderClient.getOrderByRequestId("req-address-not-found")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalAddressClient.getAddressById(2L)).thenReturn(null);

    var result = orderApplicationService.addOrder(userId, order, "req-address-not-found");

    assertFalse(result.getSuccess());
    assertEquals("DeliveryAddress NOT FOUND", result.getMessage());
    verify(internalOrderClient, never()).getCartsByBusinessAndCustomerId(any(), any());
  }

  @Test
  void addOrder_shouldFail_whenCartIsEmpty() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalOrderClient.getOrderByRequestId("req-cart-empty")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId)).thenReturn(List.of());

    var result = orderApplicationService.addOrder(userId, order, "req-cart-empty");

    assertFalse(result.getSuccess());
    assertEquals("Customer's Cart IS EMPTY", result.getMessage());
    verify(internalCatalogClient, never()).getFoodSnapshot(any());
  }

  @Test
  void addOrder_shouldFail_whenFoodNotFound() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalOrderClient.getOrderByRequestId("req-food-not-found")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));
    when(internalCatalogClient.getFoodSnapshot(100L)).thenReturn(null);

    var result = orderApplicationService.addOrder(userId, order, "req-food-not-found");

    assertFalse(result.getSuccess());
    assertEquals("Food NOT FOUND", result.getMessage());
    verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
  }

  @Test
  void addOrder_shouldFail_whenFoodUnavailable() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalOrderClient.getOrderByRequestId("req-food-deleted")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, true, new BigDecimal("20"), 10, null));

    var result = orderApplicationService.addOrder(userId, order, "req-food-deleted");

    assertFalse(result.getSuccess());
    assertEquals("Food NOT AVAILABLE", result.getMessage());
    verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
  }

  @Test
  void addOrder_shouldFail_whenCartFoodIdMissing() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalOrderClient.getOrderByRequestId("req-food-id-missing")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, null, userId, 1L, 1)));

    var result = orderApplicationService.addOrder(userId, order, "req-food-id-missing");

    assertFalse(result.getSuccess());
    assertEquals("Food.Id CANT BE NULL", result.getMessage());
    verify(internalCatalogClient, never()).getFoodSnapshot(any());
  }

  @Test
  void addOrder_shouldFail_whenFoodPriceMissing() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalOrderClient.getOrderByRequestId("req-food-price-missing")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, null, 10, null));

    var result = orderApplicationService.addOrder(userId, order, "req-food-price-missing");

    assertFalse(result.getSuccess());
    assertEquals("FoodPrice NOT FOUND", result.getMessage());
    verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
  }

  @Test
  void addOrder_shouldFail_whenBelowBusinessStartPrice() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalOrderClient.getOrderByRequestId("req-below-start-price")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                new BigDecimal("50"),
                BigDecimal.ZERO,
                null,
                null,
                null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("20"), 10, null));

    var result = orderApplicationService.addOrder(userId, order, "req-below-start-price");

    assertFalse(result.getSuccess());
    assertEquals("Order.TotalPrice IS LESS THAN BUSINESS START PRICE", result.getMessage());
    verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
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

    when(internalOrderClient.getOrderByRequestId("req-1")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("30"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));
    when(internalAccountClient.getWalletByUserId(userId, true))
        .thenReturn(new InternalAccountClient.WalletSnapshot(1L, userId, new BigDecimal("10")));

    var result = orderApplicationService.addOrder(userId, order, "req-1");

    assertFalse(result.getSuccess());
    verify(internalAccountClient, never()).debitWallet(any(), any(), any(), any(), any());
    verify(internalOrderClient, never()).createOrder(any());
  }

  @Test
  void addOrder_shouldFail_whenWalletPaidExceedsRemainingTotal() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);
    order.setWalletPaid(new BigDecimal("30"));
    order.setUsedVoucher(new cn.edu.tju.elm.model.BO.PrivateVoucher());
    order.getUsedVoucher().setId(66L);

    when(internalOrderClient.getOrderByRequestId("req-wallet-exceed")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("20"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));
    when(internalAccountClient.getVoucherSnapshot(66L))
        .thenReturn(
            new InternalAccountClient.VoucherSnapshot(
                66L, userId, false, null, new BigDecimal("5"), BigDecimal.ZERO));
    when(internalAccountClient.getWalletByUserId(userId, true))
        .thenReturn(new InternalAccountClient.WalletSnapshot(1L, userId, new BigDecimal("100")));

    var result = orderApplicationService.addOrder(userId, order, "req-wallet-exceed");

    assertFalse(result.getSuccess());
    verify(internalAccountClient, never()).debitWallet(any(), any(), any(), any(), any());
    verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
  }

  @Test
    void addOrder_shouldFail_whenWalletLoadFailed() {
        Long userId = 9L;
        Order order = new Order();
        order.setBusiness(new Business());
        order.getBusiness().setId(1L);
        order.setDeliveryAddress(new DeliveryAddress());
        order.getDeliveryAddress().setId(2L);
        order.setWalletPaid(new BigDecimal("10"));

        when(internalOrderClient.getOrderByRequestId("req-wallet-missing")).thenReturn(null);
        when(internalCatalogClient.getBusinessSnapshot(1L))
                .thenReturn(
                        new InternalCatalogClient.BusinessSnapshot(
                                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
        when(internalCatalogClient.getFoodSnapshot(100L))
                .thenReturn(
                        new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("20"), 10, null));
        when(internalAddressClient.getAddressById(2L))
                .thenReturn(
                        new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
        when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
                .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));
        when(internalAccountClient.getWalletByUserId(userId, true)).thenReturn(null);

        var result = orderApplicationService.addOrder(userId, order, "req-wallet-missing");

        assertFalse(result.getSuccess());
        assertEquals("Failed to load wallet", result.getMessage());
        verify(internalAccountClient, never()).debitWallet(any(), any(), any(), any(), any());
        verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
    }

    @Test
  void addOrder_shouldFail_whenVoucherExpired() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);
    order.setUsedVoucher(new cn.edu.tju.elm.model.BO.PrivateVoucher());
    order.getUsedVoucher().setId(66L);

    when(internalOrderClient.getOrderByRequestId("req-voucher-expired")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("20"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));
    when(internalAccountClient.getVoucherSnapshot(66L))
        .thenReturn(
            new InternalAccountClient.VoucherSnapshot(
                66L,
                userId,
                false,
                LocalDateTime.now().minusDays(1),
                new BigDecimal("5"),
                BigDecimal.ZERO));

    var result = orderApplicationService.addOrder(userId, order, "req-voucher-expired");

    assertFalse(result.getSuccess());
    assertEquals("Voucher has expired", result.getMessage());
    verify(internalAccountClient, never()).redeemVoucher(any(), any(), any(), any());
    verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
  }

  @Test
  void addOrder_shouldFail_whenVoucherThresholdNotMet() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);
    order.setUsedVoucher(new cn.edu.tju.elm.model.BO.PrivateVoucher());
    order.getUsedVoucher().setId(66L);

    when(internalOrderClient.getOrderByRequestId("req-voucher-threshold")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("20"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));
    when(internalAccountClient.getVoucherSnapshot(66L))
        .thenReturn(
            new InternalAccountClient.VoucherSnapshot(
                66L,
                userId,
                false,
                null,
                new BigDecimal("5"),
                new BigDecimal("30")));

    var result = orderApplicationService.addOrder(userId, order, "req-voucher-threshold");

    assertFalse(result.getSuccess());
    assertEquals("Order total does not meet voucher threshold", result.getMessage());
    verify(internalAccountClient, never()).redeemVoucher(any(), any(), any(), any());
    verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
  }

  @Test
  void addOrder_shouldFail_whenVoucherBelongsToAnotherUser() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);
    order.setUsedVoucher(new cn.edu.tju.elm.model.BO.PrivateVoucher());
    order.getUsedVoucher().setId(66L);

    when(internalOrderClient.getOrderByRequestId("req-voucher-owner-mismatch")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("20"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));
    when(internalAccountClient.getVoucherSnapshot(66L))
        .thenReturn(
            new InternalAccountClient.VoucherSnapshot(
                66L, 10L, false, null, new BigDecimal("5"), BigDecimal.ZERO));

    var result = orderApplicationService.addOrder(userId, order, "req-voucher-owner-mismatch");

    assertFalse(result.getSuccess());
    assertEquals("Voucher does not belong to you", result.getMessage());
    verify(internalAccountClient, never()).redeemVoucher(any(), any(), any(), any());
  }

  @Test
  void addOrder_shouldFail_whenRedeemVoucherFailed() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);
    order.setUsedVoucher(new cn.edu.tju.elm.model.BO.PrivateVoucher());
    order.getUsedVoucher().setId(66L);

    when(internalOrderClient.getOrderByRequestId("req-voucher-redeem-fail")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("20"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));
    when(internalAccountClient.getVoucherSnapshot(66L))
        .thenReturn(
            new InternalAccountClient.VoucherSnapshot(
                66L, userId, false, null, new BigDecimal("5"), BigDecimal.ZERO));
    when(internalAccountClient.redeemVoucher(
            eq("req-voucher-redeem-fail:voucher-redeem"), eq(userId), eq(66L), anyString()))
        .thenReturn(false);

    var result = orderApplicationService.addOrder(userId, order, "req-voucher-redeem-fail");

    assertFalse(result.getSuccess());
    assertEquals("Failed to redeem voucher", result.getMessage());
    verify(internalAccountClient, never()).debitWallet(any(), any(), any(), any(), any());
    verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
  }

  @Test
  void addOrder_shouldFail_whenPointsDiscountExceedsRemainingTotal() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);
    order.setPointsUsed(3000);

    when(internalOrderClient.getOrderByRequestId("req-points-exceed")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("20"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));

    var result = orderApplicationService.addOrder(userId, order, "req-points-exceed");

    assertFalse(result.getSuccess());
    assertEquals("Points discount exceeds remaining order total", result.getMessage());
    verify(internalAccountClient, never()).getWalletByUserId(any(), anyBoolean());
    verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
  }

  @Test
  void addOrder_shouldFail_whenBusinessClosed() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalOrderClient.getOrderByRequestId("req-business-closed")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                LocalTime.of(23, 59),
                LocalTime.of(23, 59)));

    var result = orderApplicationService.addOrder(userId, order, "req-business-closed");

    assertFalse(result.getSuccess());
    assertEquals("商家未营业", result.getMessage());
    verify(internalAddressClient, never()).getAddressById(any());
    verify(internalOrderClient, never()).getCartsByBusinessAndCustomerId(any(), any());
  }

  @Test
  void addOrder_shouldFail_whenAddressBelongsToAnotherUser() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalOrderClient.getOrderByRequestId("req-address-owner-mismatch")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, 10L, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));

    var result = orderApplicationService.addOrder(userId, order, "req-address-owner-mismatch");

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY LACKED", result.getMessage());
    verify(internalCatalogClient, never()).getFoodSnapshot(any());
    verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
  }

  @Test
  void addOrder_shouldFail_whenFoodDoesNotBelongToBusiness() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalOrderClient.getOrderByRequestId("req-food-business-mismatch")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 1)));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 2L, false, new BigDecimal("20"), 10, null));

    var result = orderApplicationService.addOrder(userId, order, "req-food-business-mismatch");

    assertFalse(result.getSuccess());
    assertEquals("Food DOES NOT BELONG TO Business", result.getMessage());
    verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
    verify(internalOrderClient, never()).createOrder(any());
  }

  @Test
  void addOrder_shouldFail_whenFoodStockInsufficient() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalOrderClient.getOrderByRequestId("req-stock-insufficient")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(11L, 100L, userId, 1L, 3)));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("20"), 2, null));

    var result = orderApplicationService.addOrder(userId, order, "req-stock-insufficient");

    assertFalse(result.getSuccess());
    assertEquals("商品 商品#100 库存不足", result.getMessage());
    verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
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

    when(internalOrderClient.getOrderByRequestId("req-reserve-fail")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("30"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(12L, 100L, userId, 1L, 1)));
    when(internalCatalogClient.reserveStock(any(), any(), any())).thenReturn(false);

    var result = orderApplicationService.addOrder(userId, order, "req-reserve-fail");

    assertFalse(result.getSuccess());
    verify(internalOrderClient, never()).createOrder(any());
    verify(internalOrderClient, never()).deleteCart(any());
  }

  @Test
  void addOrder_shouldFail_whenReserveStockFailed_shouldRollbackWalletAndVoucher() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);
    order.setWalletPaid(new BigDecimal("10"));
    order.setUsedVoucher(new cn.edu.tju.elm.model.BO.PrivateVoucher());
    order.getUsedVoucher().setId(66L);

    when(internalOrderClient.getOrderByRequestId("req-reserve-fail-rollback")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("30"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(12L, 100L, userId, 1L, 1)));
    when(internalAccountClient.getVoucherSnapshot(66L))
        .thenReturn(
            new InternalAccountClient.VoucherSnapshot(
                66L, userId, false, null, new BigDecimal("5"), BigDecimal.ZERO));
    when(internalAccountClient.getWalletByUserId(userId, true))
        .thenReturn(new InternalAccountClient.WalletSnapshot(1L, userId, new BigDecimal("20")));
    when(internalAccountClient.redeemVoucher(any(), eq(userId), eq(66L), any())).thenReturn(true);
    when(internalAccountClient.debitWallet(any(), eq(userId), eq(new BigDecimal("10")), any(), any()))
        .thenReturn(true);
    when(internalCatalogClient.reserveStock(any(), any(), any())).thenReturn(false);

    var result = orderApplicationService.addOrder(userId, order, "req-reserve-fail-rollback");

    assertFalse(result.getSuccess());
    assertEquals("Failed to reserve stock", result.getMessage());
    verify(internalAccountClient)
        .refundWallet(
            eq("req-reserve-fail-rollback:wallet-refund"),
            eq(userId),
            eq(new BigDecimal("10")),
            any(),
            eq("stock reserve failed rollback"));
    verify(internalAccountClient)
        .rollbackVoucher(
            eq("req-reserve-fail-rollback:voucher-rollback"),
            eq(userId),
            eq(66L),
            any(),
            eq("stock reserve failed rollback"));
    verify(internalOrderClient, never()).createOrder(any());
  }

  @Test
  void addOrder_shouldFail_whenWalletDebitFailed_shouldRollbackVoucher() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);
    order.setWalletPaid(new BigDecimal("10"));
    order.setUsedVoucher(new cn.edu.tju.elm.model.BO.PrivateVoucher());
    order.getUsedVoucher().setId(66L);

    when(internalOrderClient.getOrderByRequestId("req-wallet-debit-fail")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("30"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(12L, 100L, userId, 1L, 1)));
    when(internalAccountClient.getVoucherSnapshot(66L))
        .thenReturn(
            new InternalAccountClient.VoucherSnapshot(
                66L, userId, false, null, new BigDecimal("5"), BigDecimal.ZERO));
    when(internalAccountClient.getWalletByUserId(userId, true))
        .thenReturn(new InternalAccountClient.WalletSnapshot(1L, userId, new BigDecimal("20")));
    when(internalAccountClient.redeemVoucher(
            eq("req-wallet-debit-fail:voucher-redeem"), eq(userId), eq(66L), anyString()))
        .thenReturn(true);
    when(internalAccountClient.debitWallet(
            eq("req-wallet-debit-fail:wallet-debit"),
            eq(userId),
            eq(new BigDecimal("10")),
            any(),
            eq("order wallet payment")))
        .thenReturn(false);

    var result = orderApplicationService.addOrder(userId, order, "req-wallet-debit-fail");

    assertFalse(result.getSuccess());
    verify(internalAccountClient)
        .rollbackVoucher(
            eq("req-wallet-debit-fail:voucher-rollback"),
            eq(userId),
            eq(66L),
            any(),
            eq("wallet debit failed"));
    verify(internalCatalogClient, never()).reserveStock(any(), any(), any());
    verify(internalOrderClient, never()).createOrder(any());
  }

  @Test
  void addOrder_shouldSucceed_whenCartCleanupFailedAfterCreate() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalOrderClient.getOrderByRequestId("req-create-ok")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("30"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(13L, 100L, userId, 1L, 1)));
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
    doThrow(new RuntimeException("cleanup failed")).when(internalOrderClient).deleteCart(13L);

    var result = orderApplicationService.addOrder(userId, order, "req-create-ok");

    assertTrue(result.getSuccess());
    verify(internalOrderClient).createOrder(any());
    verify(internalCatalogClient, never())
        .releaseStock(eq("req-create-ok:stock-reserve-rollback"), any(), any());
  }

  @Test
  void addOrder_shouldFail_whenPersistOrderThrows_shouldRollbackAll() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);
    order.setWalletPaid(new BigDecimal("10"));
    order.setUsedVoucher(new cn.edu.tju.elm.model.BO.PrivateVoucher());
    order.getUsedVoucher().setId(66L);

    when(internalOrderClient.getOrderByRequestId("req-persist-fail")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("30"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(12L, 100L, userId, 1L, 1)));
    when(internalAccountClient.getVoucherSnapshot(66L))
        .thenReturn(
            new InternalAccountClient.VoucherSnapshot(
                66L, userId, false, null, new BigDecimal("5"), BigDecimal.ZERO));
    when(internalAccountClient.getWalletByUserId(userId, true))
        .thenReturn(new InternalAccountClient.WalletSnapshot(1L, userId, new BigDecimal("20")));
    when(internalAccountClient.redeemVoucher(any(), eq(userId), eq(66L), any())).thenReturn(true);
    when(internalAccountClient.debitWallet(
            any(), eq(userId), eq(new BigDecimal("10")), any(), any()))
        .thenReturn(true);
    when(internalCatalogClient.reserveStock(any(), any(), any())).thenReturn(true);
    doThrow(new RuntimeException("persist failed")).when(internalOrderClient).createOrder(any());

    var result = orderApplicationService.addOrder(userId, order, "req-persist-fail");

    assertFalse(result.getSuccess());
    verify(internalAccountClient)
        .refundWallet(
            eq("req-persist-fail:wallet-refund"),
            eq(userId),
            eq(new BigDecimal("10")),
            any(),
            eq("order persist failed"));
    verify(internalAccountClient)
        .rollbackVoucher(
            eq("req-persist-fail:voucher-rollback"),
            eq(userId),
            eq(66L),
            any(),
            eq("order persist failed"));
    verify(internalCatalogClient)
        .releaseStock(eq("req-persist-fail:stock-reserve-rollback"), any(), any());
  }

  @Test
  void addOrder_shouldFail_whenPersistOrderThrowsAfterPointsDeduct_shouldRefundDeductedPoints() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);
    order.setPointsUsed(500);

    when(internalOrderClient.getOrderByRequestId("req-persist-points-fail")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("30"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(12L, 100L, userId, 1L, 1)));
    when(internalCatalogClient.reserveStock(any(), any(), any())).thenReturn(true);
    when(internalServiceClient.freezePoints(eq(userId), eq(500), anyString()))
        .thenReturn(Map.of("frozen", true));
    when(internalServiceClient.deductPoints(eq(userId), anyString(), anyString())).thenReturn(true);
    doThrow(new RuntimeException("persist failed")).when(internalOrderClient).createOrder(any());

    var result = orderApplicationService.addOrder(userId, order, "req-persist-points-fail");

    assertFalse(result.getSuccess());
    assertEquals("Failed to persist order", result.getMessage());
    verify(internalServiceClient)
        .refundDeductedPoints(eq(userId), argThat(value -> value != null && value.startsWith("ORDER_")), eq("订单创建失败返还积分"));
    verify(internalCatalogClient)
        .releaseStock(eq("req-persist-points-fail:stock-reserve-rollback"), any(), any());
  }

  @Test
  void addOrder_shouldGenerateRequestIdWhenBlank() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);

    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("30"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(13L, 100L, userId, 1L, 1)));
    when(internalCatalogClient.reserveStock(any(), any(), any())).thenReturn(true);
    when(internalOrderClient.createOrder(
            argThat(
                command ->
                    command != null
                        && command.requestId() != null
                        && command.requestId().startsWith("order-create-"))))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                2001L,
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
                "generated",
                LocalDateTime.now()));

    var result = orderApplicationService.addOrder(userId, order, " ");

    assertTrue(result.getSuccess());
    assertTrue(order.getRequestId().startsWith("order-create-"));
    verify(internalOrderClient).createOrder(any());
  }

  @Test
  void addOrder_shouldFail_whenDeductPointsFails_shouldRollbackWalletVoucherAndStock() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);
    order.setWalletPaid(new BigDecimal("10"));
    order.setPointsUsed(500);
    order.setUsedVoucher(new cn.edu.tju.elm.model.BO.PrivateVoucher());
    order.getUsedVoucher().setId(66L);

    when(internalOrderClient.getOrderByRequestId("req-points-fail")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("30"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(12L, 100L, userId, 1L, 1)));
    when(internalAccountClient.getVoucherSnapshot(66L))
        .thenReturn(
            new InternalAccountClient.VoucherSnapshot(
                66L, userId, false, null, new BigDecimal("5"), BigDecimal.ZERO));
    when(internalAccountClient.getWalletByUserId(userId, true))
        .thenReturn(new InternalAccountClient.WalletSnapshot(1L, userId, new BigDecimal("20")));
    when(internalAccountClient.redeemVoucher(any(), eq(userId), eq(66L), any())).thenReturn(true);
    when(internalAccountClient.debitWallet(
            eq("req-points-fail:wallet-debit"),
            eq(userId),
            eq(new BigDecimal("10")),
            any(),
            eq("order wallet payment")))
        .thenReturn(true);
    when(internalCatalogClient.reserveStock(any(), any(), any())).thenReturn(true);
    when(internalServiceClient.freezePoints(eq(userId), eq(500), anyString()))
        .thenReturn(Map.of("frozen", true));
    when(internalServiceClient.deductPoints(eq(userId), anyString(), anyString())).thenReturn(false);

    var result = orderApplicationService.addOrder(userId, order, "req-points-fail");

    assertFalse(result.getSuccess());
    verify(internalAccountClient)
        .refundWallet(
            eq("req-points-fail:wallet-refund"),
            eq(userId),
            eq(new BigDecimal("10")),
            any(),
            eq("points deduct failed rollback"));
    verify(internalAccountClient)
        .rollbackVoucher(
            eq("req-points-fail:voucher-rollback"),
            eq(userId),
            eq(66L),
            any(),
            eq("points deduct failed rollback"));
    verify(internalCatalogClient)
        .releaseStock(eq("req-points-fail:stock-reserve-rollback"), any(), any());
    verify(internalOrderClient, never()).createOrder(any());
  }

  @Test
  void addOrder_shouldFail_whenFreezePointsReturnsNull_shouldRollbackWalletVoucherAndStock() {
    Long userId = 9L;
    Order order = new Order();
    order.setBusiness(new Business());
    order.getBusiness().setId(1L);
    order.setDeliveryAddress(new DeliveryAddress());
    order.getDeliveryAddress().setId(2L);
    order.setWalletPaid(new BigDecimal("10"));
    order.setPointsUsed(500);
    order.setUsedVoucher(new cn.edu.tju.elm.model.BO.PrivateVoucher());
    order.getUsedVoucher().setId(66L);

    when(internalOrderClient.getOrderByRequestId("req-freeze-points-fail")).thenReturn(null);
    when(internalCatalogClient.getBusinessSnapshot(1L))
        .thenReturn(
            new InternalCatalogClient.BusinessSnapshot(
                1L, null, null, null, null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));
    when(internalCatalogClient.getFoodSnapshot(100L))
        .thenReturn(
            new InternalCatalogClient.FoodSnapshot(100L, null, null, null, 1L, false, new BigDecimal("30"), 10, null));
    when(internalAddressClient.getAddressById(2L))
        .thenReturn(
            new InternalAddressClient.AddressSnapshot(2L, userId, "n", 1, "18800000000", "addr"));
    when(internalOrderClient.getCartsByBusinessAndCustomerId(1L, userId))
        .thenReturn(List.of(new InternalOrderClient.CartSnapshot(12L, 100L, userId, 1L, 1)));
    when(internalAccountClient.getVoucherSnapshot(66L))
        .thenReturn(
            new InternalAccountClient.VoucherSnapshot(
                66L, userId, false, null, new BigDecimal("5"), BigDecimal.ZERO));
    when(internalAccountClient.getWalletByUserId(userId, true))
        .thenReturn(new InternalAccountClient.WalletSnapshot(1L, userId, new BigDecimal("20")));
    when(internalAccountClient.redeemVoucher(any(), eq(userId), eq(66L), any())).thenReturn(true);
    when(internalAccountClient.debitWallet(
            eq("req-freeze-points-fail:wallet-debit"),
            eq(userId),
            eq(new BigDecimal("10")),
            any(),
            eq("order wallet payment")))
        .thenReturn(true);
    when(internalCatalogClient.reserveStock(any(), any(), any())).thenReturn(true);
    when(internalServiceClient.freezePoints(eq(userId), eq(500), anyString())).thenReturn(null);

    var result = orderApplicationService.addOrder(userId, order, "req-freeze-points-fail");

    assertFalse(result.getSuccess());
    assertEquals("Failed to deduct points: Failed to freeze points", result.getMessage());
    verify(internalAccountClient)
        .refundWallet(
            eq("req-freeze-points-fail:wallet-refund"),
            eq(userId),
            eq(new BigDecimal("10")),
            any(),
            eq("points deduct failed rollback"));
    verify(internalAccountClient)
        .rollbackVoucher(
            eq("req-freeze-points-fail:voucher-rollback"),
            eq(userId),
            eq(66L),
            any(),
            eq("points deduct failed rollback"));
    verify(internalCatalogClient)
        .releaseStock(eq("req-freeze-points-fail:stock-reserve-rollback"), any(), any());
    verify(internalServiceClient, never()).deductPoints(eq(userId), anyString(), anyString());
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
    void cancelOrder_shouldFail_whenOrderNotFound() {
        when(internalOrderClient.getOrderById(404L)).thenReturn(null);

        var result = orderApplicationService.cancelOrder(9L, 404L);

        assertFalse(result.getSuccess());
        assertEquals("Order NOT FOUND", result.getMessage());
        verify(internalAccountClient, never()).refundWallet(any(), any(), any(), any(), any());
        verify(internalOrderClient, never()).cancelOrder(any(), any());
    }

    @Test
    void cancelOrder_shouldFail_whenOrderBelongsToAnotherUser() {
        when(internalOrderClient.getOrderById(126L))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                126L,
                                99L,
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
                                "req-cancel-forbidden",
                                LocalDateTime.now()));

        var result = orderApplicationService.cancelOrder(9L, 126L);

        assertFalse(result.getSuccess());
        assertEquals("AUTHORITY LACKED", result.getMessage());
        verify(internalAccountClient, never()).refundWallet(any(), any(), any(), any(), any());
        verify(internalOrderClient, never()).cancelOrder(any(), any());
    }

    @Test
    void cancelOrder_shouldFail_whenOrderStateIsNotPaid() {
        Long userId = 9L;
        when(internalOrderClient.getOrderById(127L))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                127L,
                                userId,
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
                                "req-cancel-invalid-state",
                                LocalDateTime.now()));

        var result = orderApplicationService.cancelOrder(userId, 127L);

        assertFalse(result.getSuccess());
        assertEquals("只能取消已支付订单", result.getMessage());
        verify(internalCatalogClient, never()).releaseStock(any(), any(), any());
        verify(internalOrderClient, never()).cancelOrder(any(), any());
    }

    @Test
    void cancelOrder_shouldRollbackFrozenPoints_whenRefundPointsReturnsFalse() {
        Long userId = 9L;
        when(internalOrderClient.getOrderById(128L))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                128L,
                                userId,
                                1L,
                                2L,
                                OrderState.PAID,
                                new BigDecimal("30"),
                                null,
                                null,
                                300,
                                new BigDecimal("3"),
                                BigDecimal.ZERO,
                                "trade-128",
                                "req-cancel-points",
                                LocalDateTime.now()));
        when(internalServiceClient.refundDeductedPoints(userId, "trade-128", "订单取消返还积分"))
                .thenReturn(false);
        when(internalOrderClient.getOrderDetailsByOrderId(128L)).thenReturn(Collections.emptyList());
        when(internalCatalogClient.releaseStock(any(), any(), any())).thenReturn(true);
        when(internalOrderClient.cancelOrder(128L, userId))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                128L,
                                userId,
                                1L,
                                2L,
                                OrderState.CANCELED,
                                new BigDecimal("30"),
                                null,
                                null,
                                300,
                                new BigDecimal("3"),
                                BigDecimal.ZERO,
                                "trade-128",
                                "req-cancel-points",
                                LocalDateTime.now()));

        var result = orderApplicationService.cancelOrder(userId, 128L);

        assertTrue(result.getSuccess());
        verify(internalServiceClient).rollbackPoints(userId, "trade-128", "订单取消");
    }

  @Test
  void cancelOrder_shouldUseOrderIdFallback_whenPointsTradeNoMissing() {
    Long userId = 9L;
    when(internalOrderClient.getOrderById(129L))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                129L,
                userId,
                1L,
                2L,
                OrderState.PAID,
                new BigDecimal("30"),
                null,
                null,
                300,
                new BigDecimal("3"),
                BigDecimal.ZERO,
                null,
                "req-cancel-points-fallback",
                LocalDateTime.now()));
    when(internalServiceClient.refundDeductedPoints(userId, "ORDER_129", "订单取消返还积分"))
        .thenReturn(false);
    when(internalOrderClient.getOrderDetailsByOrderId(129L)).thenReturn(Collections.emptyList());
    when(internalCatalogClient.releaseStock(any(), any(), any())).thenReturn(true);
    when(internalOrderClient.cancelOrder(129L, userId))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                129L,
                userId,
                1L,
                2L,
                OrderState.CANCELED,
                new BigDecimal("30"),
                null,
                null,
                300,
                new BigDecimal("3"),
                BigDecimal.ZERO,
                null,
                "req-cancel-points-fallback",
                LocalDateTime.now()));

    var result = orderApplicationService.cancelOrder(userId, 129L);

    assertTrue(result.getSuccess());
    verify(internalServiceClient).refundDeductedPoints(userId, "ORDER_129", "订单取消返还积分");
    verify(internalServiceClient).rollbackPoints(userId, "ORDER_129", "订单取消");
  }

  @Test
  void cancelOrder_shouldFail_whenWalletRefundReturnsFalse() {
    Long userId = 9L;
    when(internalOrderClient.getOrderById(130L))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                130L,
                userId,
                1L,
                2L,
                OrderState.PAID,
                new BigDecimal("30"),
                null,
                null,
                0,
                BigDecimal.ZERO,
                new BigDecimal("15"),
                null,
                "req-cancel-wallet-false",
                LocalDateTime.now()));
    when(internalAccountClient.refundWallet(any(), eq(userId), eq(new BigDecimal("15")), any(), any()))
        .thenReturn(false);

    var result = orderApplicationService.cancelOrder(userId, 130L);

    assertFalse(result.getSuccess());
    assertEquals("取消订单失败: 钱包退款失败", result.getMessage());
    verify(internalCatalogClient, never()).releaseStock(any(), any(), any());
    verify(internalOrderClient, never()).cancelOrder(130L, userId);
  }

    @Test
  void cancelOrder_shouldFail_whenWalletRefundThrowsException() {
    Long userId = 9L;
    when(internalOrderClient.getOrderById(223L))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                223L,
                userId,
                1L,
                2L,
                OrderState.PAID,
                new BigDecimal("30"),
                null,
                null,
                0,
                BigDecimal.ZERO,
                new BigDecimal("15"),
                null,
                "req-cancel-ex",
                java.time.LocalDateTime.now()));
    doThrow(new RuntimeException("wallet timeout"))
        .when(internalAccountClient)
        .refundWallet(any(), eq(userId), eq(new BigDecimal("15")), any(), any());

    var result = orderApplicationService.cancelOrder(userId, 223L);

    assertFalse(result.getSuccess());
    verify(internalOrderClient, never()).cancelOrder(223L, userId);
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
    void cancelOrder_shouldFail_whenVoucherRollbackFailed() {
        Long userId = 9L;
        when(internalOrderClient.getOrderById(125L))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                125L,
                                userId,
                                1L,
                                2L,
                                OrderState.PAID,
                                new BigDecimal("30"),
                                66L,
                                new BigDecimal("5"),
                                0,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                null,
                                "req-cancel-3",
                                LocalDateTime.now()));
        when(internalAccountClient.rollbackVoucher(any(), eq(userId), eq(66L), any(), any()))
                .thenReturn(false);

        var result = orderApplicationService.cancelOrder(userId, 125L);

        assertFalse(result.getSuccess());
        assertEquals("取消订单失败: 优惠券回滚失败", result.getMessage());
        verify(internalCatalogClient, never()).releaseStock(any(), any(), any());
        verify(internalOrderClient, never()).cancelOrder(125L, userId);
    }

    @Test
    void cancelOrder_shouldFail_whenRemoteCancelReturnsNull() {
        Long userId = 9L;
        when(internalOrderClient.getOrderById(131L))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                131L,
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
                                "req-cancel-null",
                                LocalDateTime.now()));
        when(internalOrderClient.getOrderDetailsByOrderId(131L)).thenReturn(Collections.emptyList());
        when(internalCatalogClient.releaseStock(any(), any(), any())).thenReturn(true);
        when(internalOrderClient.cancelOrder(131L, userId)).thenReturn(null);

        var result = orderApplicationService.cancelOrder(userId, 131L);

        assertFalse(result.getSuccess());
        assertEquals("取消订单失败: 订单状态更新失败", result.getMessage());
        verify(compatibilityEnricher, never()).enrichOrder(any(Order.class));
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
    verify(integrationOutboxService)
        .enqueuePointsOrderSuccess(eq(100L), eq("200"), eq(30D), any(), eq("订单完成"));
  }

  @Test
  void updateOrderStatus_shouldNotifyOutboxWithNetAmountAfterDiscounts() {
    Long userId = 9L;
    Order request = new Order();
    request.setId(205L);
    request.setOrderState(OrderState.COMPLETE);

    when(internalOrderClient.getOrderById(205L))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                205L,
                userId,
                1L,
                2L,
                OrderState.DELIVERY,
                new BigDecimal("30"),
                66L,
                new BigDecimal("5"),
                300,
                new BigDecimal("3"),
                BigDecimal.ZERO,
                null,
                "req-state-net-amount",
                LocalDateTime.now()));
    when(internalOrderClient.updateOrderState(205L, OrderState.COMPLETE))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                205L,
                userId,
                1L,
                2L,
                OrderState.COMPLETE,
                new BigDecimal("30"),
                66L,
                new BigDecimal("5"),
                300,
                new BigDecimal("3"),
                BigDecimal.ZERO,
                null,
                "req-state-net-amount",
                LocalDateTime.now()));

    var result = orderApplicationService.updateOrderStatus(userId, false, false, request);

    assertTrue(result.getSuccess());
    verify(integrationOutboxService)
        .enqueuePointsOrderSuccess(eq(userId), eq("205"), eq(22D), any(), eq("订单完成"));
  }

  @Test
  void updateOrderStatus_shouldClampOutboxAmountToZeroWhenDiscountsExceedTotal() {
    Long userId = 9L;
    Order request = new Order();
    request.setId(206L);
    request.setOrderState(OrderState.COMPLETE);

    when(internalOrderClient.getOrderById(206L))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                206L,
                userId,
                1L,
                2L,
                OrderState.DELIVERY,
                new BigDecimal("5"),
                66L,
                new BigDecimal("10"),
                300,
                new BigDecimal("1"),
                BigDecimal.ZERO,
                null,
                "req-state-zero-amount",
                LocalDateTime.now()));
    when(internalOrderClient.updateOrderState(206L, OrderState.COMPLETE))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                206L,
                userId,
                1L,
                2L,
                OrderState.COMPLETE,
                new BigDecimal("5"),
                66L,
                new BigDecimal("10"),
                300,
                new BigDecimal("1"),
                BigDecimal.ZERO,
                null,
                "req-state-zero-amount",
                LocalDateTime.now()));

    var result = orderApplicationService.updateOrderStatus(userId, false, false, request);

    assertTrue(result.getSuccess());
    verify(integrationOutboxService)
        .enqueuePointsOrderSuccess(eq(userId), eq("206"), eq(0D), any(), eq("订单完成"));
  }

  @Test
    void updateOrderStatus_shouldUpdateWhenAdmin() {
        Order request = new Order();
        request.setId(201L);
        request.setOrderState(OrderState.ACCEPTED);

        when(internalOrderClient.getOrderById(201L))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                201L,
                                88L,
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
                                "req-state-admin",
                                LocalDateTime.now()));
        when(internalOrderClient.updateOrderState(201L, OrderState.ACCEPTED))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                201L,
                                88L,
                                1L,
                                2L,
                                OrderState.ACCEPTED,
                                new BigDecimal("30"),
                                null,
                                null,
                                0,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                null,
                                "req-state-admin",
                                LocalDateTime.now()));

        var result = orderApplicationService.updateOrderStatus(9L, true, false, request);

        assertTrue(result.getSuccess());
        verify(internalOrderClient).updateOrderState(201L, OrderState.ACCEPTED);
    }

    @Test
    void updateOrderStatus_shouldUpdateWhenCustomerOwnsOrder() {
        Long userId = 9L;
        Order request = new Order();
        request.setId(202L);
        request.setOrderState(OrderState.ACCEPTED);

        when(internalOrderClient.getOrderById(202L))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                202L,
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
                                "req-state-customer",
                                LocalDateTime.now()));
        when(businessService.getBusinessById(1L)).thenReturn(null);
        when(internalOrderClient.updateOrderState(202L, OrderState.ACCEPTED))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                202L,
                                userId,
                                1L,
                                2L,
                                OrderState.ACCEPTED,
                                new BigDecimal("30"),
                                null,
                                null,
                                0,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                null,
                                "req-state-customer",
                                LocalDateTime.now()));

        var result = orderApplicationService.updateOrderStatus(userId, false, false, request);

        assertTrue(result.getSuccess());
        verify(internalOrderClient).updateOrderState(202L, OrderState.ACCEPTED);
    }

    @Test
    void updateOrderStatus_shouldFail_whenOrderIsNull() {
        var result = orderApplicationService.updateOrderStatus(9L, false, false, null);

        assertFalse(result.getSuccess());
        assertEquals("Order CANT BE NULL", result.getMessage());
        verify(internalOrderClient, never()).getOrderById(any());
    }

    @Test
    void updateOrderStatus_shouldFail_whenOrderIdMissing() {
        Order request = new Order();
        request.setOrderState(OrderState.ACCEPTED);

        var result = orderApplicationService.updateOrderStatus(9L, false, false, request);

        assertFalse(result.getSuccess());
        assertEquals("Order.Id CANT BE NULL", result.getMessage());
        verify(internalOrderClient, never()).getOrderById(any());
    }

    @Test
    void updateOrderStatus_shouldFail_whenOrderNotFound() {
        Order request = new Order();
        request.setId(301L);
        request.setOrderState(OrderState.ACCEPTED);

        when(internalOrderClient.getOrderById(301L)).thenReturn(null);

        var result = orderApplicationService.updateOrderStatus(9L, false, false, request);

        assertFalse(result.getSuccess());
        assertEquals("Order NOT FOUND", result.getMessage());
        verify(internalOrderClient, never()).updateOrderState(any(), any());
    }

    @Test
    void updateOrderStatus_shouldFail_whenOrderStateMissing() {
        Long userId = 9L;
        Order request = new Order();
        request.setId(302L);

        when(internalOrderClient.getOrderById(302L))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                302L,
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
                                "req-state-missing",
                                LocalDateTime.now()));

        var result = orderApplicationService.updateOrderStatus(userId, false, false, request);

        assertFalse(result.getSuccess());
        assertEquals("Order.OrderState CANT BE NULL", result.getMessage());
        verify(internalOrderClient, never()).updateOrderState(any(), any());
    }

    @Test
    void updateOrderStatus_shouldFail_whenOrderStateInvalid() {
        Long userId = 9L;
        Order request = new Order();
        request.setId(305L);
        request.setOrderState(99);

        when(internalOrderClient.getOrderById(305L))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                305L,
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
                                "req-state-invalid",
                                LocalDateTime.now()));

        var result = orderApplicationService.updateOrderStatus(userId, false, false, request);

        assertFalse(result.getSuccess());
        assertEquals("OrderState NOT VALID", result.getMessage());
        verify(internalOrderClient, never()).updateOrderState(any(), any());
    }

    @Test
    void updateOrderStatus_shouldFail_whenUserHasNoAuthority() {
        Long userId = 9L;
        Order request = new Order();
        request.setId(303L);
        request.setOrderState(OrderState.ACCEPTED);

        Business business = new Business();
        business.setId(1L);
        business.setBusinessOwnerId(1000L);

        when(internalOrderClient.getOrderById(303L))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                303L,
                                2000L,
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
                                "req-state-forbidden",
                                LocalDateTime.now()));
        when(businessService.getBusinessById(1L)).thenReturn(business);

        var result = orderApplicationService.updateOrderStatus(userId, false, true, request);

        assertFalse(result.getSuccess());
        assertEquals("AUTHORITY LACKED", result.getMessage());
        verify(internalOrderClient, never()).updateOrderState(any(), any());
    }

    @Test
    void updateOrderStatus_shouldNotNotifyOutbox_whenAlreadyComplete() {
        Long userId = 9L;
        Order request = new Order();
        request.setId(306L);
        request.setOrderState(OrderState.COMPLETE);

        when(internalOrderClient.getOrderById(306L))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                306L,
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
                                "req-state-already-complete",
                                LocalDateTime.now()));

        var result = orderApplicationService.updateOrderStatus(userId, false, false, request);

        assertFalse(result.getSuccess());
        assertEquals("非法的状态转换", result.getMessage());
        verify(integrationOutboxService, never())
                .enqueuePointsOrderSuccess(any(), any(), any(), any(), any());
    }

    @Test
    void updateOrderStatus_shouldStillSucceed_whenOutboxThrows() {
        Long userId = 9L;
        Order request = new Order();
        request.setId(304L);
        request.setOrderState(OrderState.COMPLETE);

        when(internalOrderClient.getOrderById(304L))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                304L,
                                userId,
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
                                "req-state-outbox",
                                LocalDateTime.now()));
        when(internalOrderClient.updateOrderState(304L, OrderState.COMPLETE))
                .thenReturn(
                        new InternalOrderClient.OrderSnapshot(
                                304L,
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
                                "req-state-outbox",
                                LocalDateTime.now()));
        doThrow(new RuntimeException("outbox down"))
                .when(integrationOutboxService)
                .enqueuePointsOrderSuccess(eq(userId), eq("304"), eq(30D), any(), eq("订单完成"));

        var result = orderApplicationService.updateOrderStatus(userId, false, false, request);

        assertTrue(result.getSuccess());
        verify(compatibilityEnricher).enrichOrder(any(Order.class));
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

  @Test
  void updateOrderStatus_shouldFail_whenRemoteUpdateReturnsNull() {
    Long userId = 9L;
    Order request = new Order();
    request.setId(301L);
    request.setOrderState(OrderState.DELIVERY);

    when(internalOrderClient.getOrderById(301L))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                301L,
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
                "req-state-3",
                java.time.LocalDateTime.now()));
    when(internalOrderClient.updateOrderState(301L, OrderState.DELIVERY)).thenReturn(null);

    var result = orderApplicationService.updateOrderStatus(userId, false, false, request);

    assertFalse(result.getSuccess());
    verify(internalOrderClient).updateOrderState(301L, OrderState.DELIVERY);
  }
}
