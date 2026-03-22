package cn.edu.tju.elm.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.elm.constant.OrderState;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.model.BO.Review;
import cn.edu.tju.elm.utils.InternalOrderClient;
import cn.edu.tju.elm.utils.InternalServiceClient;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewApplicationServiceTest {
  @Mock private OrderApplicationService orderApplicationService;
  @Mock private InternalOrderClient internalOrderClient;
  @Mock private IntegrationOutboxService integrationOutboxService;
  @Mock private InternalServiceClient internalServiceClient;
  @Mock private ResponseCompatibilityEnricher compatibilityEnricher;

  @InjectMocks private ReviewApplicationService reviewApplicationService;

  @Test
  void addReview_shouldSucceedAndUpdateOrderState() {
    Long currentUserId = 9L;
    Long orderId = 100L;
    Order order = new Order();
    order.setId(orderId);
    order.setCustomerId(currentUserId);
    order.setOrderState(OrderState.COMPLETE);
    Business business = new Business();
    business.setId(88L);
    order.setBusiness(business);
    when(orderApplicationService.getOrderById(orderId)).thenReturn(order);
    when(internalOrderClient.getReviewByOrderId(orderId)).thenReturn(null);
    when(internalOrderClient.createReview(any()))
        .thenReturn(
            new InternalOrderClient.ReviewSnapshot(
                501L, currentUserId, 88L, orderId, false, 8, "great"));
    when(internalOrderClient.updateOrderState(orderId, OrderState.COMMENTED))
        .thenReturn(
            new InternalOrderClient.OrderSnapshot(
                orderId,
                currentUserId,
                88L,
                7L,
                OrderState.COMMENTED,
                java.math.BigDecimal.TEN,
                null,
                null,
                0,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                null,
                "req-review",
                java.time.LocalDateTime.now()));

    Review review = new Review();
    review.setId(501L);
    review.setStars(8);
    review.setContent("great");
    review.setAnonymous(false);

    var result = reviewApplicationService.addReview(currentUserId, orderId, review);

    assertTrue(result.getSuccess());
    verify(internalOrderClient).createReview(any(InternalOrderClient.CreateReviewCommand.class));
    verify(internalOrderClient).updateOrderState(orderId, OrderState.COMMENTED);
    verify(integrationOutboxService)
        .enqueuePointsReviewSuccess(eq(currentUserId), any(), any(), any(), any());
  }

  @Test
  void addReview_shouldFail_whenOrderNotComplete() {
    Long currentUserId = 9L;
    Long orderId = 100L;
    Order order = new Order();
    order.setId(orderId);
    order.setCustomerId(currentUserId);
    order.setOrderState(OrderState.PAID);
    when(orderApplicationService.getOrderById(orderId)).thenReturn(order);

    Review review = new Review();
    review.setStars(8);
    review.setContent("great");
    review.setAnonymous(false);

    var result = reviewApplicationService.addReview(currentUserId, orderId, review);

    assertFalse(result.getSuccess());
    verify(internalOrderClient, never()).createReview(any());
    verify(internalOrderClient, never()).updateOrderState(any(), any());
  }

  @Test
  void addReview_shouldFail_whenCurrentUserIsNotOrderOwner() {
    Long currentUserId = 9L;
    Long orderId = 100L;
    Order order = new Order();
    order.setId(orderId);
    order.setCustomerId(10L);
    order.setOrderState(OrderState.COMPLETE);
    when(orderApplicationService.getOrderById(orderId)).thenReturn(order);
    when(internalOrderClient.getReviewByOrderId(orderId)).thenReturn(null);

    Review review = new Review();
    review.setStars(8);
    review.setContent("great");
    review.setAnonymous(false);

    var result = reviewApplicationService.addReview(currentUserId, orderId, review);

    assertFalse(result.getSuccess());
    verify(internalOrderClient, never()).createReview(any());
    verify(internalOrderClient, never()).updateOrderState(any(), any());
  }

  @Test
  void deleteReview_shouldSucceedAndRollbackOrderState() {
    Long currentUserId = 9L;
    Long reviewId = 77L;
    Review review = new Review();
    review.setId(reviewId);
    review.setCustomerId(currentUserId);
    when(internalOrderClient.getReviewById(reviewId))
        .thenReturn(
            new InternalOrderClient.ReviewSnapshot(
                reviewId, currentUserId, 88L, 200L, false, 9, "great"));
    when(internalOrderClient.deleteReview(reviewId))
        .thenReturn(
            new InternalOrderClient.ReviewSnapshot(
                reviewId, currentUserId, 88L, 200L, false, 9, "great"));

    var result = reviewApplicationService.deleteReview(currentUserId, false, reviewId);

    assertTrue(result.getSuccess());
    verify(internalServiceClient).notifyReviewDeleted(currentUserId, reviewId.toString());
    verify(internalOrderClient).deleteReview(reviewId);
    verify(internalOrderClient).updateOrderState(200L, OrderState.COMPLETE);
  }
}
