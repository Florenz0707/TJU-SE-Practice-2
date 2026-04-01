package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.model.BO.Review;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.OrderApplicationService;
import cn.edu.tju.elm.service.ReviewApplicationService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.InternalOrderClient;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {
  @Mock private BusinessService businessService;
  @Mock private OrderApplicationService orderApplicationService;
  @Mock private UserService userService;
  @Mock private ReviewApplicationService reviewApplicationService;
  @Mock private InternalOrderClient internalOrderClient;
  @Mock private ResponseCompatibilityEnricher compatibilityEnricher;

  @InjectMocks private ReviewController reviewController;

  @Test
  void addReview_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = reviewController.addReview(3L, new Review());

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY NOT FOUND", result.getMessage());
    verify(reviewApplicationService, never()).addReview(any(), any(), any());
  }

  @Test
  void addReview_shouldDelegateWhenAuthorized() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));

    Review review = new Review();
    review.setId(1L);
    when(reviewApplicationService.addReview(9L, 3L, review))
        .thenReturn(cn.edu.tju.core.model.HttpResult.success(review));

    var result = reviewController.addReview(3L, review);

    assertTrue(result.getSuccess());
    verify(reviewApplicationService).addReview(9L, 3L, review);
  }

  @Test
  void getReviewsByUserId_shouldReadFromInternalOrderClient() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalOrderClient.getReviewsByCustomerId(9L))
        .thenReturn(
            List.of(new InternalOrderClient.ReviewSnapshot(1L, 9L, 2L, 3L, false, 8, "ok")));

    var result = reviewController.getReviewsByUserId();

    assertTrue(result.getSuccess());
    verify(internalOrderClient).getReviewsByCustomerId(9L);
    verify(compatibilityEnricher).enrichReviews(any());
  }

  @Test
  void getReviewsByUserId_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = reviewController.getReviewsByUserId();

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY NOT FOUND", result.getMessage());
    verify(internalOrderClient, never()).getReviewsByCustomerId(any());
  }

  @Test
  void updateReview_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = reviewController.updateReview(1L, new Review());

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY NOT FOUND", result.getMessage());
    verify(internalOrderClient, never()).getReviewById(1L);
  }

  @Test
  void updateReview_shouldFailWhenOldReviewNotFound() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalOrderClient.getReviewById(1L)).thenReturn(null);

    var result = reviewController.updateReview(1L, new Review());

    assertFalse(result.getSuccess());
    assertEquals("OldReview NOT FOUND", result.getMessage());
    verify(internalOrderClient, never()).updateReview(any(), any());
  }

  @Test
  void updateReview_shouldFailWhenNotOwner() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalOrderClient.getReviewById(1L))
        .thenReturn(new InternalOrderClient.ReviewSnapshot(1L, 10L, 2L, 3L, false, 8, "ok"));

    Review update = new Review();
    update.setContent("new content");

    var result = reviewController.updateReview(1L, update);

    assertFalse(result.getSuccess());
    verify(internalOrderClient, never()).updateReview(any(), any());
  }

  @Test
  void updateReview_shouldUpdateWhenOwner() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalOrderClient.getReviewById(1L))
        .thenReturn(new InternalOrderClient.ReviewSnapshot(1L, 9L, 2L, 3L, false, 8, "old"));
    when(internalOrderClient.updateReview(any(), any()))
        .thenReturn(new InternalOrderClient.ReviewSnapshot(1L, 9L, 2L, 3L, false, 10, "new"));

    Review update = new Review();
    update.setStars(10);
    update.setContent("new");

    var result = reviewController.updateReview(1L, update);

    assertTrue(result.getSuccess());
    verify(internalOrderClient).updateReview(any(), any());
    verify(compatibilityEnricher).enrichReview(any(Review.class));
  }

  @Test
  void updateReview_shouldFailWhenInternalUpdateReturnsNull() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalOrderClient.getReviewById(1L))
        .thenReturn(new InternalOrderClient.ReviewSnapshot(1L, 9L, 2L, 3L, false, 8, "old"));
    when(internalOrderClient.updateReview(any(), any())).thenReturn(null);

    Review update = new Review();
    update.setContent("new");

    var result = reviewController.updateReview(1L, update);

    assertFalse(result.getSuccess());
    assertEquals("Failed to update review", result.getMessage());
  }

  @Test
  void updateReview_shouldFailWhenPayloadEmpty() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(internalOrderClient.getReviewById(1L))
        .thenReturn(new InternalOrderClient.ReviewSnapshot(1L, 9L, 2L, 3L, false, 8, "old"));

    var result = reviewController.updateReview(1L, new Review());

    assertFalse(result.getSuccess());
    verify(internalOrderClient, never()).updateReview(any(), any());
  }

  @Test
  void getReviewByOrderId_shouldFailWhenReviewNotFound() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Order order = new Order();
    order.setId(3L);
    when(orderApplicationService.getOrderById(3L)).thenReturn(order);
    when(internalOrderClient.getReviewByOrderId(3L)).thenReturn(null);

    var result = reviewController.getReviewByOrderId(3L);

    assertFalse(result.getSuccess());
  }

  @Test
  void getReviewByOrderId_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = reviewController.getReviewByOrderId(3L);

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY NOT FOUND", result.getMessage());
    verify(orderApplicationService, never()).getOrderById(3L);
  }

  @Test
  void getReviewByOrderId_shouldFailWhenOrderNotFound() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(orderApplicationService.getOrderById(3L)).thenReturn(null);

    var result = reviewController.getReviewByOrderId(3L);

    assertFalse(result.getSuccess());
    assertEquals("Order NOT FOUND", result.getMessage());
    verify(internalOrderClient, never()).getReviewByOrderId(3L);
  }

  @Test
  void getReviewByOrderId_shouldReturnReviewToOwner() {
    User me = new User();
    me.setId(10L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Order order = new Order();
    order.setId(3L);
    when(orderApplicationService.getOrderById(3L)).thenReturn(order);
    when(internalOrderClient.getReviewByOrderId(3L))
        .thenReturn(new InternalOrderClient.ReviewSnapshot(1L, 10L, 2L, 3L, true, 8, "ok"));

    var result = reviewController.getReviewByOrderId(3L);

    assertTrue(result.getSuccess());
    verify(compatibilityEnricher).enrichReview(any(Review.class));
  }

  @Test
  void getReviewByOrderId_shouldFailWhenBusinessNotFound() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Order order = new Order();
    order.setId(3L);
    when(orderApplicationService.getOrderById(3L)).thenReturn(order);
    when(internalOrderClient.getReviewByOrderId(3L))
        .thenReturn(new InternalOrderClient.ReviewSnapshot(1L, 10L, 2L, 3L, false, 8, "ok"));
    when(businessService.getBusinessById(2L)).thenReturn(null);

    var result = reviewController.getReviewByOrderId(3L);

    assertFalse(result.getSuccess());
    assertEquals("Business NOT FOUND", result.getMessage());
  }

  @Test
  void getReviewByOrderId_shouldFailWhenNonBusinessNonOwnerUser() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("USER"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Order order = new Order();
    order.setId(3L);
    when(orderApplicationService.getOrderById(3L)).thenReturn(order);
    when(internalOrderClient.getReviewByOrderId(3L))
        .thenReturn(new InternalOrderClient.ReviewSnapshot(1L, 10L, 2L, 3L, false, 8, "ok"));

    var result = reviewController.getReviewByOrderId(3L);

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY LACKED", result.getMessage());
  }

  @Test
  void getReviewByOrderId_shouldHideAnonymousReviewFromBusinessOwner() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Order order = new Order();
    order.setId(3L);
    when(orderApplicationService.getOrderById(3L)).thenReturn(order);
    when(internalOrderClient.getReviewByOrderId(3L))
        .thenReturn(new InternalOrderClient.ReviewSnapshot(1L, 10L, 2L, 3L, true, 8, "ok"));
    Business business = new Business();
    business.setId(2L);
    business.setBusinessOwnerId(9L);
    when(businessService.getBusinessById(2L)).thenReturn(business);

    var result = reviewController.getReviewByOrderId(3L);

    assertFalse(result.getSuccess());
  }

  @Test
  void getReviewByOrderId_shouldReturnNonAnonymousReviewToBusinessOwner() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Order order = new Order();
    order.setId(3L);
    when(orderApplicationService.getOrderById(3L)).thenReturn(order);
    when(internalOrderClient.getReviewByOrderId(3L))
        .thenReturn(new InternalOrderClient.ReviewSnapshot(1L, 10L, 2L, 3L, false, 8, "ok"));
    Business business = new Business();
    business.setId(2L);
    business.setBusinessOwnerId(9L);
    when(businessService.getBusinessById(2L)).thenReturn(business);

    var result = reviewController.getReviewByOrderId(3L);

    assertTrue(result.getSuccess());
  }

  @Test
  void getReviewByOrderId_shouldFailWhenBusinessUserIsNotOwner() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("BUSINESS"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    Order order = new Order();
    order.setId(3L);
    when(orderApplicationService.getOrderById(3L)).thenReturn(order);
    when(internalOrderClient.getReviewByOrderId(3L))
        .thenReturn(new InternalOrderClient.ReviewSnapshot(1L, 10L, 2L, 3L, false, 8, "ok"));
    Business business = new Business();
    business.setId(2L);
    business.setBusinessOwnerId(11L);
    when(businessService.getBusinessById(2L)).thenReturn(business);

    var result = reviewController.getReviewByOrderId(3L);

    assertFalse(result.getSuccess());
  }

  @Test
  void getReviewsByBusinessId_shouldHideAnonymousFields() {
    Business business = new Business();
    business.setId(2L);
    when(businessService.getBusinessById(2L)).thenReturn(business);
    when(internalOrderClient.getReviewsByBusinessId(2L))
        .thenReturn(
            List.of(
                new InternalOrderClient.ReviewSnapshot(1L, 10L, 2L, 3L, true, 8, "anon"),
                new InternalOrderClient.ReviewSnapshot(2L, 11L, 2L, 4L, false, 9, "named")));

    var result = reviewController.getReviewsByBusinessId(2L);

    assertTrue(result.getSuccess());
    assertNotNull(result.getData());
    Review anonymousReview = result.getData().get(0);
    Review normalReview = result.getData().get(1);
    assertNull(anonymousReview.getCustomerId());
    assertNull(anonymousReview.getOrder());
    assertNotNull(normalReview.getCustomerId());
    assertNotNull(normalReview.getOrder());
    verify(compatibilityEnricher).enrichReviews(any());
  }

  @Test
  void getReviewsByBusinessId_shouldFailWhenBusinessNotFound() {
    when(businessService.getBusinessById(2L)).thenReturn(null);

    var result = reviewController.getReviewsByBusinessId(2L);

    assertFalse(result.getSuccess());
    assertEquals("Business NOT FOUND", result.getMessage());
    verify(internalOrderClient, never()).getReviewsByBusinessId(2L);
  }

  @Test
  void deleteReview_shouldFailWhenNoAuthority() {
    when(userService.getUserWithAuthorities()).thenReturn(Optional.empty());

    var result = reviewController.deleteReview(1L);

    assertFalse(result.getSuccess());
    assertEquals("AUTHORITY NOT FOUND", result.getMessage());
    verify(reviewApplicationService, never()).deleteReview(anyLong(), anyBoolean(), anyLong());
  }

  @Test
  void deleteReview_shouldPassAdminFlag() {
    User me = new User();
    me.setId(9L);
    me.setAuthorities(AuthorityUtils.getAuthoritySet("ADMIN"));
    when(userService.getUserWithAuthorities()).thenReturn(Optional.of(me));
    when(reviewApplicationService.deleteReview(9L, true, 1L))
        .thenReturn(cn.edu.tju.core.model.HttpResult.success("ok"));

    var result = reviewController.deleteReview(1L);

    assertTrue(result.getSuccess());
    verify(reviewApplicationService).deleteReview(9L, true, 1L);
  }
}
