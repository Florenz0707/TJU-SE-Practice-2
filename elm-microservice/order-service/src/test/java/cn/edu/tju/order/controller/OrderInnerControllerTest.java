package cn.edu.tju.order.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderInnerControllerTest {

  @Test
  void ping_shouldReturnPong() {
    OrderInnerController controller = new OrderInnerController();

    var result = controller.ping();

    assertTrue(result.getSuccess());
    assertEquals("pong", result.getData());
  }
}
