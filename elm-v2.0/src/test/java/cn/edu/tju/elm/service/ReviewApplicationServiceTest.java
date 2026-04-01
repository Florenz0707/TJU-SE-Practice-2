package cn.edu.tju.elm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
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
  void addReview_shouldFail_whenOrderNotFound() {
    when(orderApplicationService.getOrderById(100L)).thenReturn(null);

    var result = reviewApplicationService.addReview(9L, 100L, new Review());

    assertFalse(result.getSuccess());
    assertEquals("Order NOT FOUND", result.getMessage());
    verify(internalOrderClient, never()).createReview(any());
  }

  @Test
  void addReview_shouldFail_whenReviewPayloadIsNull() {
    Long currentUserId = 9L;
    Long orderId = 100L;
    Order order = new Order();
    order.setId(orderId);
    order.setCustomerId(currentUserId);
    order.setOrderState(OrderState.COMPLETE);
    when(orderApplicationService.getOrderById(orderId)).thenReturn(order);
    when(internalOrderClient.getReviewByOrderId(orderId)).thenReturn(null);

    var result = reviewApplicationService.addReview(currentUserId, orderId, null);

    assertFalse(result.getSuccess());
    assertEquals("Review CANT BE NULL", result.getMessage());
    verify(internalOrderClient, never()).createReview(any());
  }

  @Test
  void addReview_shouldFail_whenStarsMissing() {
    Long currentUserId = 9L;
    Long orderId = 100L;
    Order order = new Order();
    order.setId(orderId);
    order.setCustomerId(currentUserId);
    order.setOrderState(OrderState.COMPLETE);
    when(orderApplicationService.getOrderById(orderId)).thenReturn(order);
    when(internalOrderClient.getReviewByOrderId(orderId)).thenReturn(null);

    Review review = new Review();
    review.setContent("great");
    review.setAnonymous(false);

    var result = reviewApplicationService.addReview(currentUserId, orderId, review);

    assertFalse(result.getSuccess());
    assertEquals("Review.Stars CANT BE NULL", result.getMessage());
  }

  @Test
  void addReview_shouldFail_whenCreateReviewReturnsNull() {
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
    when(internalOrderClient.createReview(any())).thenReturn(null);

    Review review = new Review();
    review.setStars(8);
    review.setContent("great");
    review.setAnonymous(false);

    var result = reviewApplicationService.addReview(currentUserId, orderId, review);

    assertFalse(result.getSuccess());
    assertEquals("Failed to create review", result.getMessage());
    verify(internalOrderClient, never()).updateOrderState(any(), any());
  }

  @Test
  void addReview_shouldFail_whenUpdateOrderStateReturnsNull() {
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
    when(internalOrderClient.updateOrderState(orderId, OrderState.COMMENTED)).thenReturn(null);

    Review review = new Review();
    review.setStars(8);
    review.setContent("great");
    review.setAnonymous(false);

    var result = reviewApplicationService.addReview(currentUserId, orderId, review);

    assertFalse(result.getSuccess());
    assertEquals("Failed to update order state", result.getMessage());
  }

  @Test
  void addReview_shouldStillSucceed_whenOutboxThrows() {
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
                "req-review-outbox",
                java.time.LocalDateTime.now()));
    doThrow(new RuntimeException("outbox down"))
        .when(integrationOutboxService)
        .enqueuePointsReviewSuccess(eq(currentUserId), eq("501"), any(), any(), eq("用户发表评价"));

    Review review = new Review();
    review.setStars(8);
    review.setContent("great");
    review.setAnonymous(false);

    var result = reviewApplicationService.addReview(currentUserId, orderId, review);

    assertTrue(result.getSuccess());
    verify(compatibilityEnricher).enrichReview(any(Review.class));
  }

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
  void addReview_shouldFail_whenOrderAlreadyReviewed() {
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
    when(internalOrderClient.getReviewByOrderId(orderId))
        .thenReturn(
            new InternalOrderClient.ReviewSnapshot(
                501L, currentUserId, 88L, orderId, false, 8, "great"));

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
    when(internalOrderClient.updateOrderState(200L, OrderState.COMPLETE))
      .thenReturn(
        new InternalOrderClient.OrderSnapshot(
          200L,
          currentUserId,
          88L,
          7L,
          OrderState.COMPLETE,
          java.math.BigDecimal.TEN,
          null,
          null,
          0,
          java.math.BigDecimal.ZERO,
          java.math.BigDecimal.ZERO,
          null,
          "req-review-delete-success",
          java.time.LocalDateTime.now()));

    var result = reviewApplicationService.deleteReview(currentUserId, false, reviewId);

    assertTrue(result.getSuccess());
    verify(internalServiceClient).notifyReviewDeleted(currentUserId, reviewId.toString());
    verify(internalOrderClient).deleteReview(reviewId);
    verify(internalOrderClient).updateOrderState(200L, OrderState.COMPLETE);
  }

  @Test
  void deleteReview_shouldFail_whenNotOwnerAndNotAdmin() {
    Long currentUserId = 9L;
    Long reviewId = 77L;
    when(internalOrderClient.getReviewById(reviewId))
        .thenReturn(
            new InternalOrderClient.ReviewSnapshot(reviewId, 10L, 88L, 200L, false, 9, "great"));

    var result = reviewApplicationService.deleteReview(currentUserId, false, reviewId);

    assertFalse(result.getSuccess());
    verify(internalOrderClient, never()).deleteReview(reviewId);
    verify(internalOrderClient, never()).updateOrderState(any(), any());
  }

    @Test
    void deleteReview_shouldStillDelete_whenNotifyReviewDeletedThrows() {
    Long currentUserId = 9L;
    Long reviewId = 78L;
    when(internalOrderClient.getReviewById(reviewId))
      .thenReturn(
        new InternalOrderClient.ReviewSnapshot(
          reviewId, currentUserId, 88L, 201L, false, 9, "great"));
    doThrow(new RuntimeException("points timeout"))
      .when(internalServiceClient)
      .notifyReviewDeleted(currentUserId, reviewId.toString());
    when(internalOrderClient.deleteReview(reviewId))
      .thenReturn(
        new InternalOrderClient.ReviewSnapshot(
          reviewId, currentUserId, 88L, 201L, false, 9, "great"));
    when(internalOrderClient.updateOrderState(201L, OrderState.COMPLETE))
      .thenReturn(
        new InternalOrderClient.OrderSnapshot(
          201L,
          currentUserId,
          88L,
          7L,
          OrderState.COMPLETE,
          java.math.BigDecimal.TEN,
          null,
          null,
          0,
          java.math.BigDecimal.ZERO,
          java.math.BigDecimal.ZERO,
          null,
          "req-review-delete",
          java.time.LocalDateTime.now()));

    var result = reviewApplicationService.deleteReview(currentUserId, false, reviewId);

    assertTrue(result.getSuccess());
    verify(internalOrderClient).deleteReview(reviewId);
    verify(internalOrderClient).updateOrderState(201L, OrderState.COMPLETE);
    }

    @Test
    void deleteReview_shouldFail_whenDeleteReviewReturnsNull() {
    Long currentUserId = 9L;
    Long reviewId = 79L;
    when(internalOrderClient.getReviewById(reviewId))
      .thenReturn(
        new InternalOrderClient.ReviewSnapshot(
          reviewId, currentUserId, 88L, 202L, false, 9, "great"));
    when(internalOrderClient.deleteReview(reviewId)).thenReturn(null);

    var result = reviewApplicationService.deleteReview(currentUserId, false, reviewId);

    assertFalse(result.getSuccess());
    assertEquals("Failed to delete review", result.getMessage());
    verify(internalOrderClient, never()).updateOrderState(202L, OrderState.COMPLETE);
    }

    @Test
    void deleteReview_shouldFail_whenRollbackOrderStateReturnsNull() {
    Long currentUserId = 9L;
    Long reviewId = 80L;
    when(internalOrderClient.getReviewById(reviewId))
      .thenReturn(
        new InternalOrderClient.ReviewSnapshot(
          reviewId, currentUserId, 88L, 203L, false, 9, "great"));
    when(internalOrderClient.deleteReview(reviewId))
      .thenReturn(
        new InternalOrderClient.ReviewSnapshot(
          reviewId, currentUserId, 88L, 203L, false, 9, "great"));
    when(internalOrderClient.updateOrderState(203L, OrderState.COMPLETE)).thenReturn(null);

    var result = reviewApplicationService.deleteReview(currentUserId, false, reviewId);

    assertFalse(result.getSuccess());
    assertEquals("Failed to rollback order state", result.getMessage());
    }

    @Test
    void deleteReview_shouldAllowAdminDeletingOthersReview() {
    Long reviewId = 81L;
    when(internalOrderClient.getReviewById(reviewId))
      .thenReturn(
        new InternalOrderClient.ReviewSnapshot(
          reviewId, 10L, 88L, 204L, false, 9, "great"));
    when(internalOrderClient.deleteReview(reviewId))
      .thenReturn(
        new InternalOrderClient.ReviewSnapshot(
          reviewId, 10L, 88L, 204L, false, 9, "great"));
    when(internalOrderClient.updateOrderState(204L, OrderState.COMPLETE))
      .thenReturn(
        new InternalOrderClient.OrderSnapshot(
          204L,
          10L,
          88L,
          7L,
          OrderState.COMPLETE,
          java.math.BigDecimal.TEN,
          null,
          null,
          0,
          java.math.BigDecimal.ZERO,
          java.math.BigDecimal.ZERO,
          null,
          "req-review-admin-delete",
          java.time.LocalDateTime.now()));

    var result = reviewApplicationService.deleteReview(9L, true, reviewId);

    assertTrue(result.getSuccess());
    verify(internalServiceClient).notifyReviewDeleted(10L, reviewId.toString());
    verify(internalOrderClient).deleteReview(reviewId);
    }
}
