package cn.edu.tju.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.edu.tju.order.constant.OrderState;
import cn.edu.tju.order.model.bo.Order;
import cn.edu.tju.order.model.bo.OrderDetailet;
import cn.edu.tju.order.repository.OrderDetailetRepository;
import cn.edu.tju.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class OrderInternalServiceTest {

  @Mock private OrderRepository orderRepository;
  @Mock private OrderDetailetRepository orderDetailetRepository;

  @InjectMocks private OrderInternalService orderInternalService;

  @Test
  void getOrderById_shouldReturnSnapshot_whenFound() {
    Order order = new Order();
    order.setId(8L);
    order.setCustomerId(10L);
    when(orderRepository.findById(8L)).thenReturn(Optional.of(order));

    var result = orderInternalService.getOrderById(8L);

    assertNotNull(result);
    assertEquals(8L, result.getId());
    assertEquals(10L, result.getCustomerId());
  }

  @Test
  void getOrderDetailetsByOrderId_shouldReturnItems() {
    OrderDetailet detail = new OrderDetailet();
    detail.setId(2L);
    detail.setOrderId(8L);
    detail.setFoodId(5L);
    detail.setQuantity(2);
    when(orderDetailetRepository.findAllByOrderId(8L)).thenReturn(List.of(detail));

    var result = orderInternalService.getOrderDetailetsByOrderId(8L);

    assertEquals(1, result.size());
    assertEquals(5L, result.getFirst().getFoodId());
  }

  @Test
  void getOrdersByCustomerId_shouldReturnEmpty_whenNullInput() {
    var result = orderInternalService.getOrdersByCustomerId(null);
    assertTrue(result.isEmpty());
  }

  @Test
  void getOrdersByBusinessId_shouldReturnEmpty_whenNullInput() {
    var result = orderInternalService.getOrdersByBusinessId(null);
    assertTrue(result.isEmpty());
  }

  @Test
  void getOrdersByCustomerIdPage_shouldReturnPagedData() {
    Order order = new Order();
    order.setId(101L);
    order.setCustomerId(10L);
    when(orderRepository.findAllByCustomerId(10L, PageRequest.of(0, 5)))
        .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 5), 7));

    var result = orderInternalService.getOrdersByCustomerId(10L, 1, 5);

    assertEquals(1, result.getOrders().size());
    assertEquals(7, result.getTotal());
    assertEquals(1, result.getPage());
    assertEquals(5, result.getSize());
  }

  @Test
  void getOrdersByBusinessIdPage_shouldReturnPagedData() {
    Order order = new Order();
    order.setId(202L);
    order.setBusinessId(20L);
    when(orderRepository.findAllByBusinessId(20L, PageRequest.of(1, 3)))
        .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(1, 3), 4));

    var result = orderInternalService.getOrdersByBusinessId(20L, 2, 3);

    assertEquals(1, result.getOrders().size());
    assertEquals(4, result.getTotal());
    assertEquals(2, result.getPage());
    assertEquals(3, result.getSize());
  }

  @Test
  void createOrder_shouldSaveOrderAndDetails() {
    Order savedOrder = new Order();
    savedOrder.setId(11L);
    savedOrder.setCustomerId(10L);
    savedOrder.setBusinessId(20L);
    savedOrder.setDeliveryAddressId(30L);
    savedOrder.setOrderTotal(new BigDecimal("42.50"));
    savedOrder.setOrderState(OrderState.PAID);
    savedOrder.setRequestId("req-1");
    when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

    var result =
        orderInternalService.createOrder(
            new OrderInternalService.CreateOrderCommand(
                "req-1",
                10L,
                20L,
                30L,
                new BigDecimal("42.50"),
                null,
                null,
                null,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                List.of(new OrderInternalService.OrderItemCommand(100L, 2))));

    assertEquals(11L, result.getId());
    assertEquals(10L, result.getCustomerId());
  }

  @Test
  void createOrder_shouldReturnExisting_whenRequestIdExists() {
    Order existed = new Order();
    existed.setId(99L);
    existed.setCustomerId(10L);
    existed.setRequestId("req-exists");
    when(orderRepository.findByRequestId("req-exists")).thenReturn(existed);

    var result =
        orderInternalService.createOrder(
            new OrderInternalService.CreateOrderCommand(
                "req-exists",
                10L,
                20L,
                30L,
                new BigDecimal("30.00"),
                OrderState.PAID,
                null,
                null,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                List.of(new OrderInternalService.OrderItemCommand(200L, 1))));

    assertEquals(99L, result.getId());
  }

  @Test
  void cancelPaidOrder_shouldChangeStateToCanceled() {
    Order order = new Order();
    order.setId(7L);
    order.setCustomerId(10L);
    order.setOrderState(OrderState.PAID);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
    when(orderRepository.save(order)).thenReturn(order);

    var result = orderInternalService.cancelPaidOrder(7L, 10L);

    assertEquals(OrderState.CANCELED, result.getOrderState());
  }

  @Test
  void cancelPaidOrder_shouldThrow_whenNotOwner() {
    Order order = new Order();
    order.setId(7L);
    order.setCustomerId(10L);
    order.setOrderState(OrderState.PAID);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

    assertThrows(IllegalStateException.class, () -> orderInternalService.cancelPaidOrder(7L, 11L));
  }

  @Test
  void updateOrderState_shouldUpdate_whenTransitionValid() {
    Order order = new Order();
    order.setId(10L);
    order.setOrderState(OrderState.PAID);
    when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
    when(orderRepository.save(order)).thenReturn(order);

    var result = orderInternalService.updateOrderState(10L, OrderState.COMPLETE);

    assertEquals(OrderState.COMPLETE, result.getOrderState());
  }

  @Test
  void cancelPaidOrder_shouldThrow_whenOrderNotPaid() {
    Order order = new Order();
    order.setId(8L);
    order.setCustomerId(10L);
    order.setOrderState(OrderState.COMPLETE);
    when(orderRepository.findById(8L)).thenReturn(Optional.of(order));

    assertThrows(IllegalStateException.class, () -> orderInternalService.cancelPaidOrder(8L, 10L));
  }

  @Test
  void updateOrderState_shouldThrow_whenTransitionInvalid() {
    Order order = new Order();
    order.setId(11L);
    order.setOrderState(OrderState.COMPLETE);
    when(orderRepository.findById(11L)).thenReturn(Optional.of(order));

    assertThrows(
        IllegalStateException.class,
        () -> orderInternalService.updateOrderState(11L, OrderState.PAID));
  }
}
