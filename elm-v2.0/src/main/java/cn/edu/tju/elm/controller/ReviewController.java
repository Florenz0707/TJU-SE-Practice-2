package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "管理评价", description = "提供对订单评价的增删改查功能")
public class ReviewController {
  private final BusinessService businessService;
  private final OrderApplicationService orderApplicationService;
  private final UserService userService;
  private final ReviewApplicationService reviewApplicationService;
  private final InternalOrderClient internalOrderClient;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public ReviewController(
      BusinessService businessService,
      OrderApplicationService orderApplicationService,
      UserService userService,
      ReviewApplicationService reviewApplicationService,
      InternalOrderClient internalOrderClient,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.businessService = businessService;
    this.orderApplicationService = orderApplicationService;
    this.userService = userService;
    this.reviewApplicationService = reviewApplicationService;
    this.internalOrderClient = internalOrderClient;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  @PostMapping("/order/{orderId}")
  @Operation(summary = "添加订单评价", description = "顾客对已完成订单进行评价，评价后自动发放积分")
  public HttpResult<Review> addReview(
      @Parameter(description = "订单ID", required = true) @PathVariable Long orderId,
      @Parameter(description = "评价信息", required = true) @RequestBody Review review) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    return reviewApplicationService.addReview(meOptional.get().getId(), orderId, review);
  }

  @PatchMapping("/{reviewId}")
  @Operation(summary = "更新评价", description = "修改已发表的评价内容")
  public HttpResult<Review> updateReview(
      @Parameter(description = "评价ID", required = true) @PathVariable Long reviewId,
      @Parameter(description = "要更新的评价字段", required = true) @RequestBody Review review) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    InternalOrderClient.ReviewSnapshot oldReviewSnapshot =
        internalOrderClient.getReviewById(reviewId);
    Review oldReview = oldReviewSnapshot == null ? null : toReview(oldReviewSnapshot);
    if (oldReview == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "OldReview NOT FOUND");
    if (review == null
        || (review.getStars() == null
            && review.getContent() == null
            && review.getAnonymous() == null))
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "NewReview CANT BE NULL");

    if (me.getId().equals(oldReview.getCustomerId())) {
      InternalOrderClient.ReviewSnapshot updatedSnapshot =
          internalOrderClient.updateReview(
              reviewId,
              new InternalOrderClient.UpdateReviewCommand(
                  review.getStars(), review.getContent(), review.getAnonymous()));
      if (updatedSnapshot == null) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to update review");
      }
      Review updated = toReview(updatedSnapshot);
      compatibilityEnricher.enrichReview(updated);
      return HttpResult.success(updated);
    }

    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @DeleteMapping("/{reviewId}")
  @Operation(summary = "删除评价", description = "软删除指定评价，订单状态恢复为已完成")
  public HttpResult<String> deleteReview(
      @Parameter(description = "评价ID", required = true) @PathVariable Long reviewId) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();
    return reviewApplicationService.deleteReview(
        me.getId(), AuthorityUtils.hasAuthority(me, "ADMIN"), reviewId);
  }

  @GetMapping("/my")
  @Operation(summary = "获取我的评价", description = "获取当前用户发表的所有评价")
  public HttpResult<List<Review>> getReviewsByUserId() {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();
    List<Review> reviews =
        internalOrderClient.getReviewsByCustomerId(me.getId()).stream()
            .map(this::toReview)
            .toList();
    compatibilityEnricher.enrichReviews(reviews);
    return HttpResult.success(reviews);
  }

  @GetMapping("/order/{orderId}")
  @Operation(summary = "根据订单ID获取评价", description = "查询指定订单的评价，匿名评价对商家不可见")
  public HttpResult<Review> getReviewByOrderId(
      @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    Order order = orderApplicationService.getOrderById(orderId);
    if (order == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
    InternalOrderClient.ReviewSnapshot reviewSnapshot =
        internalOrderClient.getReviewByOrderId(orderId);
    Review review = reviewSnapshot == null ? null : toReview(reviewSnapshot);
    if (review == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");
    compatibilityEnricher.enrichReview(review);

    if (me.getId().equals(review.getCustomerId())) return HttpResult.success(review);

    boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
    if (isBusiness && review.getBusiness() != null && review.getBusiness().getId() != null) {
      Business reviewBusiness = businessService.getBusinessById(review.getBusiness().getId());
      if (reviewBusiness == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
      }
      if (!me.getId().equals(reviewBusiness.getBusinessOwnerId())) {
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
      }
      if (review.getAnonymous())
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");
      return HttpResult.success(review);
    }

    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @GetMapping("/business/{businessId}")
  @Operation(summary = "根据店铺ID获取评价列表", description = "查询指定店铺的所有评价，匿名评价会隐藏用户信息")
  public HttpResult<List<Review>> getReviewsByBusinessId(
      @Parameter(description = "店铺ID", required = true) @PathVariable Long businessId) {
    Business business = businessService.getBusinessById(businessId);
    if (business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");

    List<Review> reviewList =
        internalOrderClient.getReviewsByBusinessId(businessId).stream()
            .map(this::toReview)
            .toList();
    compatibilityEnricher.enrichReviews(reviewList);
    for (Review review : reviewList) {
      if (review.getAnonymous()) {
        review.setCustomerId(null);
        review.setOrder(null);
      }
    }
    return HttpResult.success(reviewList);
  }

  private Review toReview(InternalOrderClient.ReviewSnapshot snapshot) {
    Review review = new Review();
    review.setId(snapshot.id());
    review.setCustomerId(snapshot.customerId());
    review.setAnonymous(snapshot.anonymous());
    review.setStars(snapshot.stars());
    review.setContent(snapshot.content());
    Business business = new Business();
    business.setId(snapshot.businessId());
    review.setBusiness(business);
    Order order = new Order();
    order.setId(snapshot.orderId());
    review.setOrder(order);
    return review;
  }
}
