package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.constant.OrderState;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.model.BO.Review;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.OrderService;
import cn.edu.tju.elm.service.PointsService;
import cn.edu.tju.elm.service.ReviewService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.InternalServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "管理评价", description = "提供对订单评价的增删改查功能")
public class ReviewController {
  private static final Logger log = LoggerFactory.getLogger(ReviewController.class);
  private final ReviewService reviewService;
  private final BusinessService businessService;
  private final OrderService orderService;
  private final UserService userService;
  private final InternalServiceClient internalServiceClient;
  private final PointsService pointsService;

  public ReviewController(
      ReviewService reviewService,
      BusinessService businessService,
      OrderService orderService,
      UserService userService,
      InternalServiceClient internalServiceClient,
      PointsService pointsService) {
    this.reviewService = reviewService;
    this.businessService = businessService;
    this.orderService = orderService;
    this.userService = userService;
    this.internalServiceClient = internalServiceClient;
    this.pointsService = pointsService;
  }

  @PostMapping("/order/{orderId}")
  @Operation(summary = "添加订单评价", description = "顾客对已完成订单进行评价，评价后自动发放积分")
  public HttpResult<Review> addReview(
      @Parameter(description = "订单ID", required = true) @PathVariable Long orderId,
      @Parameter(description = "评价信息", required = true) @RequestBody Review review) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    Order order = orderService.getOrderById(orderId);
    if (order == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
    if (!order.getOrderState().equals(OrderState.COMPLETE))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Order.OrderState ERROR");

    Review existingReview = reviewService.getReviewByOrderId(orderId);
    if (existingReview != null) return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "订单已评价");

    Business business = order.getBusiness();
    User customer = order.getCustomer();

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

    if (me.equals(customer)) {
      EntityUtils.setNewEntity(review);
      review.setOrder(order);
      review.setBusiness(business);
      review.setCustomer(customer);
      reviewService.addReview(review);

      order.setOrderState(OrderState.COMMENTED);
      EntityUtils.updateEntity(order);
      orderService.updateOrder(order);

      // 发放评价积分
      try {
        internalServiceClient.notifyReviewSuccess(
            customer.getId(),
            review.getId().toString(),
            null, // amount为null时，使用规则中的默认值
            review.getCreateTime() != null ? review.getCreateTime().toString() : null,
            "用户发表评价");
      } catch (Exception e) {
        log.error("Failed to notify review success for points: {}", e.getMessage());
        // 不影响评价创建
      }

      return HttpResult.success(review);
    }

    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
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

    Review oldReview = reviewService.getReviewById(reviewId);
    if (oldReview == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "OldReview NOT FOUND");
    if (review == null
        || (review.getStars() == null
            && review.getContent() == null
            && review.getAnonymous() == null))
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "NewReview CANT BE NULL");

    if (me.equals(oldReview.getCustomer())) {
      // 只修改stars, content, anonymous
      if (review.getStars() != null) oldReview.setStars(review.getStars());
      if (review.getContent() != null) oldReview.setContent(review.getContent());
      if (review.getAnonymous() != null) oldReview.setAnonymous(review.getAnonymous());

      EntityUtils.updateEntity(oldReview);
      reviewService.updateReview(oldReview);
      return HttpResult.success(oldReview);
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

    Review review = reviewService.getReviewById(reviewId);
    if (review == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    if (isAdmin || me.equals(review.getCustomer())) {
      // 返还评价积分
      try {
        pointsService.notifyReviewDeleted(review.getCustomer().getId(), reviewId.toString());
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

    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @GetMapping("/my")
  @Operation(summary = "获取我的评价", description = "获取当前用户发表的所有评价")
  public HttpResult<List<Review>> getReviewsByUserId() {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    return HttpResult.success(reviewService.getReviewsByUserId(me.getId()));
  }

  @GetMapping("/order/{orderId}")
  @Operation(summary = "根据订单ID获取评价", description = "查询指定订单的评价，匿名评价对商家不可见")
  public HttpResult<Review> getReviewByOrderId(
      @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    Order order = orderService.getOrderById(orderId);
    if (order == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
    Review review = reviewService.getReviewByOrderId(orderId);
    if (review == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");

    // 如果是用户想看自己的订单的评价，鉴权完毕之后就返回评价
    if (me.equals(review.getCustomer())) return HttpResult.success(review);

    // 如果是商家想看订单评价，鉴权之后，如果用户设置了匿名评价就不展示，否则返回评价
    boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
    if (isBusiness && me.equals(review.getBusiness().getBusinessOwner())) {
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

    List<Review> reviewList = reviewService.getReviewsByBusinessId(businessId);
    // 作匿名处理
    for (Review review : reviewList) {
      if (review.getAnonymous()) {
        review.setCustomer(null);
        review.setOrder(null);
      }
    }
    return HttpResult.success(reviewList);
  }
}
