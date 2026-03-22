package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
}
