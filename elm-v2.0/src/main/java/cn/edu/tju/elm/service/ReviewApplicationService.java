package cn.edu.tju.elm.service;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.constant.OrderState;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.model.BO.Review;
import cn.edu.tju.elm.utils.InternalOrderClient;
import cn.edu.tju.elm.utils.InternalServiceClient;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewApplicationService {
  private static final Logger log = LoggerFactory.getLogger(ReviewApplicationService.class);

  private final OrderApplicationService orderApplicationService;
  private final InternalOrderClient internalOrderClient;
  private final IntegrationOutboxService integrationOutboxService;
  private final InternalServiceClient internalServiceClient;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public ReviewApplicationService(
      OrderApplicationService orderApplicationService,
      InternalOrderClient internalOrderClient,
      IntegrationOutboxService integrationOutboxService,
      InternalServiceClient internalServiceClient,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.orderApplicationService = orderApplicationService;
    this.internalOrderClient = internalOrderClient;
    this.integrationOutboxService = integrationOutboxService;
    this.internalServiceClient = internalServiceClient;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  @Transactional
  public HttpResult<Review> addReview(Long currentUserId, Long orderId, Review review) {
    Order order = orderApplicationService.getOrderById(orderId);
    if (order == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
    if (!order.getOrderState().equals(OrderState.COMPLETE))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Order.OrderState ERROR");

    InternalOrderClient.ReviewSnapshot existingReview =
        internalOrderClient.getReviewByOrderId(orderId);
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

    InternalOrderClient.ReviewSnapshot createdReview =
        internalOrderClient.createReview(
            new InternalOrderClient.CreateReviewCommand(
                customerId,
                order.getBusiness().getId(),
                orderId,
                review.getAnonymous(),
                review.getStars(),
                review.getContent()));
    if (createdReview == null) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to create review");
    }

    InternalOrderClient.OrderSnapshot updatedSnapshot =
        internalOrderClient.updateOrderState(order.getId(), OrderState.COMMENTED);
    if (updatedSnapshot == null) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to update order state");
    }

    try {
      integrationOutboxService.enqueuePointsReviewSuccess(
          customerId, createdReview.id().toString(), null, null, "用户发表评价");
    } catch (Exception e) {
      log.error("Failed to notify review success for points: {}", e.getMessage());
    }

    Review created = toReview(createdReview);
    compatibilityEnricher.enrichReview(created);
    return HttpResult.success(created);
  }

  @Transactional
  public HttpResult<String> deleteReview(Long currentUserId, boolean isAdmin, Long reviewId) {
    InternalOrderClient.ReviewSnapshot reviewSnapshot = internalOrderClient.getReviewById(reviewId);
    if (reviewSnapshot == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");

    if (!(isAdmin || currentUserId.equals(reviewSnapshot.customerId()))) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    try {
      internalServiceClient.notifyReviewDeleted(reviewSnapshot.customerId(), reviewId.toString());
    } catch (Exception e) {
      log.error("Failed to refund points for deleted review: {}", e.getMessage());
    }

    InternalOrderClient.ReviewSnapshot deleted = internalOrderClient.deleteReview(reviewId);
    if (deleted == null) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to delete review");
    }

    if (deleted.orderId() != null) {
      InternalOrderClient.OrderSnapshot rollbackSnapshot =
          internalOrderClient.updateOrderState(deleted.orderId(), OrderState.COMPLETE);
      if (rollbackSnapshot == null) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to rollback order state");
      }
    }
    return HttpResult.success("Delete review successfully.");
  }

  private Review toReview(InternalOrderClient.ReviewSnapshot snapshot) {
    Review review = new Review();
    review.setId(snapshot.id());
    review.setCustomerId(snapshot.customerId());
    Business business = new Business();
    business.setId(snapshot.businessId());
    review.setBusiness(business);
    Order order = new Order();
    order.setId(snapshot.orderId());
    review.setOrder(order);
    review.setAnonymous(snapshot.anonymous());
    review.setStars(snapshot.stars());
    review.setContent(snapshot.content());
    return review;
  }
}
