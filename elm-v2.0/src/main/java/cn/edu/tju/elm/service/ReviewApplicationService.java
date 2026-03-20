package cn.edu.tju.elm.service;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.constant.OrderState;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.model.BO.Review;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewApplicationService {
  private static final Logger log = LoggerFactory.getLogger(ReviewApplicationService.class);

  private final ReviewService reviewService;
  private final OrderService orderService;
  private final IntegrationOutboxService integrationOutboxService;
  private final PointsService pointsService;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public ReviewApplicationService(
      ReviewService reviewService,
      OrderService orderService,
      IntegrationOutboxService integrationOutboxService,
      PointsService pointsService,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.reviewService = reviewService;
    this.orderService = orderService;
    this.integrationOutboxService = integrationOutboxService;
    this.pointsService = pointsService;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  @Transactional
  public HttpResult<Review> addReview(Long currentUserId, Long orderId, Review review) {
    Order order = orderService.getOrderById(orderId);
    if (order == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
    if (!order.getOrderState().equals(OrderState.COMPLETE))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Order.OrderState ERROR");

    Review existingReview = reviewService.getReviewByOrderId(orderId);
    if (existingReview != null) return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "订单已评价");

    Long customerId = order.getCustomerId();

    if (review == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review CANT BE NULL");
    if (review.getStars() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review.Stars CANT BE NULL");
    if (0 > review.getStars() || 10 < review.getStars())
      return HttpResult.failure(
          ResultCodeEnum.SERVER_ERROR, "Review.Stars MUST BE BETWEEN 0 AND 10");
    if (review.getContent() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review.Content CANT BE NULL");
    if (review.getAnonymous() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review.Anonymous CANT BE NULL");

    if (!currentUserId.equals(customerId))
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");

    EntityUtils.setNewEntity(review);
    review.setOrder(order);
    review.setBusiness(order.getBusiness());
    review.setCustomerId(customerId);
    reviewService.addReview(review);

    order.setOrderState(OrderState.COMMENTED);
    EntityUtils.updateEntity(order);
    orderService.updateOrder(order);

    try {
      integrationOutboxService.enqueuePointsReviewSuccess(
          customerId,
          review.getId().toString(),
          null,
          review.getCreateTime() != null ? review.getCreateTime().toString() : null,
          "用户发表评价");
    } catch (Exception e) {
      log.error("Failed to notify review success for points: {}", e.getMessage());
    }

    compatibilityEnricher.enrichReview(review);
    return HttpResult.success(review);
  }

  @Transactional
  public HttpResult<String> deleteReview(Long currentUserId, boolean isAdmin, Long reviewId) {
    Review review = reviewService.getReviewById(reviewId);
    if (review == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");

    if (!(isAdmin || currentUserId.equals(review.getCustomerId()))) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    try {
      pointsService.notifyReviewDeleted(review.getCustomerId(), reviewId.toString());
    } catch (Exception e) {
      log.error("Failed to refund points for deleted review: {}", e.getMessage());
    }

    EntityUtils.deleteEntity(review);
    reviewService.updateReview(review);

    Order order = review.getOrder();
    order.setOrderState(OrderState.COMPLETE);
    EntityUtils.updateEntity(order);
    orderService.updateOrder(order);
    return HttpResult.success("Delete review successfully.");
  }
}
