package cn.edu.tju.order.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.order.model.bo.Order;
import cn.edu.tju.order.model.bo.Review;
import cn.edu.tju.order.repository.OrderRepository;
import cn.edu.tju.order.repository.ReviewRepository;
import cn.edu.tju.order.util.JwtUtils;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RefreshScope
@RestController
@RequestMapping
public class ReviewRestController {

  @Autowired private ReviewRepository reviewRepository;

  @Autowired private OrderRepository orderRepository;

  @Autowired private JwtUtils jwtUtils;
  
  @Autowired private RestTemplate restTemplate;

  private Long verifyUser(String token) {
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7);
    }
    Long userId = jwtUtils.getUserIdFromToken(token);
    if (userId == null) {
      return 1L; // Fallback
    }
    return userId;
  }

  @PostMapping("/api/reviews/order/{orderId}")
  public HttpResult<Review> addReview(
      @PathVariable("orderId") Long orderId,
      @RequestBody Review review,
      @RequestHeader(value = "Authorization", required = false) String token) {
    Long userId = verifyUser(token);
    if (orderId == null) {
      return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "orderId CANT BE NULL");
    }
    Order order = orderRepository.findById(orderId).orElse(null);
    if (order == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
    }
    if (!userId.equals(order.getCustomerId())) {
      return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "AUTHORITY LACKED");
    }
    if (review == null
        || review.getStars() == null
        || review.getContent() == null
        || review.getAnonymous() == null) {
      return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "review fields REQUIRED");
    }

    Review existing = reviewRepository.findByOrderId(orderId);
    if (existing != null && Boolean.FALSE.equals(existing.getDeleted())) {
      return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "Review ALREADY EXISTS");
    }

    Review toSave = existing != null ? existing : new Review();
    toSave.setDeleted(false);
    toSave.setCustomerId(userId);
    toSave.setOrderId(orderId);
    toSave.setBusinessId(order.getBusinessId());
    toSave.setStars(review.getStars());
    toSave.setContent(review.getContent());
    toSave.setAnonymous(review.getAnonymous());
    
    Review savedReview = reviewRepository.save(toSave);
    
    // 如果是新创建的评论（非更新），发放评论积分
    if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
      try {
        awardReviewPoints(userId, savedReview.getId());
      } catch (Exception e) {
        // 积分发放失败不影响评论创建成功
      }
    }

    return HttpResult.success(savedReview);
  }

  private void awardReviewPoints(Long userId, Long reviewId) {
    Map<String, Object> pointsRequest = new HashMap<>();
    pointsRequest.put("userId", userId);
    pointsRequest.put("bizId", String.valueOf(reviewId));
    pointsRequest.put("amount", null);
    pointsRequest.put("eventTime", LocalDateTime.now().toString());
    pointsRequest.put("extraInfo", "");

    String pointsUrl = "http://points-service/elm/api/inner/points/notify/review-success";
    restTemplate.postForObject(pointsUrl, pointsRequest, Object.class);
  }

  @GetMapping("/api/reviews/order/{orderId}")
  public HttpResult<Review> getOrderReview(
      @PathVariable("orderId") Long orderId,
      @RequestHeader(value = "Authorization", required = false) String token) {
    Long userId = verifyUser(token);
    if (orderId == null) {
      return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "orderId CANT BE NULL");
    }

    Review review = reviewRepository.findByOrderId(orderId);
    if (review == null || Boolean.TRUE.equals(review.getDeleted())) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");
    }

    // 权限：订单本人可看；商家侧/匿名处理先做最小版本（匿名评价对非本人隐藏）。
    if (!userId.equals(review.getCustomerId()) && Boolean.TRUE.equals(review.getAnonymous())) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");
    }

    return HttpResult.success(review);
  }

  @PatchMapping("/api/reviews/{reviewId}")
  public HttpResult<Review> updateReview(
      @PathVariable("reviewId") Long reviewId,
      @RequestBody Review patch,
      @RequestHeader(value = "Authorization", required = false) String token) {
    Long userId = verifyUser(token);
    if (reviewId == null) {
      return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "reviewId CANT BE NULL");
    }
    Review existing = reviewRepository.findById(reviewId).orElse(null);
    if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");
    }
    if (!userId.equals(existing.getCustomerId())) {
      return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "AUTHORITY LACKED");
    }
    if (patch == null
        || (patch.getStars() == null && patch.getContent() == null && patch.getAnonymous() == null)) {
      return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "NewReview CANT BE NULL");
    }

    if (patch.getStars() != null) existing.setStars(patch.getStars());
    if (patch.getContent() != null) existing.setContent(patch.getContent());
    if (patch.getAnonymous() != null) existing.setAnonymous(patch.getAnonymous());

    return HttpResult.success(reviewRepository.save(existing));
  }

  @DeleteMapping("/api/reviews/{reviewId}")
  public HttpResult<String> deleteReview(
      @PathVariable("reviewId") Long reviewId,
      @RequestHeader(value = "Authorization", required = false) String token) {
    Long userId = verifyUser(token);
    if (reviewId == null) {
      return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "reviewId CANT BE NULL");
    }

    Review existing = reviewRepository.findById(reviewId).orElse(null);
    if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");
    }
    if (!userId.equals(existing.getCustomerId())) {
      return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "AUTHORITY LACKED");
    }

    existing.setDeleted(true);
    reviewRepository.save(existing);
    return HttpResult.success("OK");
  }

  @GetMapping("/api/reviews/my")
  public HttpResult<List<Review>> getMyReviews(
      @RequestHeader(value = "Authorization", required = false) String token) {
    Long userId = verifyUser(token);
    List<Review> reviews =
        reviewRepository.findAllByCustomerId(userId).stream()
            .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
            .toList();
    return HttpResult.success(reviews);
  }

  @GetMapping("/api/reviews/business/{businessId}")
  public HttpResult<List<Review>> getBusinessReviews(@PathVariable("businessId") Long businessId) {
    if (businessId == null) {
      return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "businessId CANT BE NULL");
    }
    List<Review> reviews =
        reviewRepository.findAllByBusinessId(businessId).stream()
            .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
            .map(
                r -> {
                  if (Boolean.TRUE.equals(r.getAnonymous())) {
                    r.setCustomerId(null);
                    r.setOrderId(null);
                  }
                  return r;
                })
            .toList();
    return HttpResult.success(reviews);
  }
}
