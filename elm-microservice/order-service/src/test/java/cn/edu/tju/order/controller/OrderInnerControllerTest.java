package cn.edu.tju.order.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import cn.edu.tju.order.constant.OrderState;
import cn.edu.tju.order.model.bo.Order;
import cn.edu.tju.order.model.vo.OrderSnapshotVO;
import cn.edu.tju.order.service.OrderInternalService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderInnerControllerTest {

  @Mock private OrderInternalService orderInternalService;

  @InjectMocks private OrderInnerController orderInnerController;

  @Test
  void ping_shouldReturnPong() {
    var result = orderInnerController.ping();
    assertTrue(result.getSuccess());
    assertEquals("pong", result.getData());
  }

  @Test
  void getOrderById_shouldReturnFailure_whenMissing() {
    when(orderInternalService.getOrderById(999L)).thenReturn(null);

    var result = orderInnerController.getOrderById(999L);

    assertFalse(result.getSuccess());
  }

  @Test
  void getOrdersByCustomerId_shouldReturnData() {
    Order order = new Order();
    order.setId(1L);
    order.setCustomerId(10L);
    when(orderInternalService.getOrdersByCustomerId(10L))
        .thenReturn(List.of(new OrderSnapshotVO(order)));

    var result = orderInnerController.getOrdersByCustomerId(10L);

    assertTrue(result.getSuccess());
    assertEquals(1, result.getData().size());
  }

  @Test
  void createOrder_shouldReturnData_whenServiceSuccess() {
    Order order = new Order();
    order.setId(12L);
    order.setCustomerId(10L);
    order.setOrderState(OrderState.PAID);
    when(orderInternalService.createOrder(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new OrderSnapshotVO(order));
    OrderInnerController.CreateOrderRequest request = new OrderInnerController.CreateOrderRequest();
    request.setRequestId("req-create");
    request.setCustomerId(10L);
    request.setBusinessId(20L);
    request.setDeliveryAddressId(30L);
    request.setOrderTotal(new BigDecimal("15.00"));
    OrderInnerController.OrderItemRequest item = new OrderInnerController.OrderItemRequest();
    item.setFoodId(1L);
    item.setQuantity(2);
    request.setItems(List.of(item));

    var result = orderInnerController.createOrder(request);

    assertTrue(result.getSuccess());
    assertEquals(12L, result.getData().getId());
  }

  @Test
  void createOrder_shouldReturnFailure_whenItemsEmpty() {
    OrderInnerController.CreateOrderRequest request = new OrderInnerController.CreateOrderRequest();
    request.setRequestId("req-empty");
    request.setItems(List.of());

    var result = orderInnerController.createOrder(request);

    assertFalse(result.getSuccess());
  }

  @Test
  void cancelOrder_shouldReturnFailure_whenOperatorMissing() {
    var result =
        orderInnerController.cancelOrder(1L, new OrderInnerController.CancelOrderRequest());
    assertFalse(result.getSuccess());
  }
}
