package cn.edu.tju.order.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.order.model.vo.ReviewSnapshotVO;
import cn.edu.tju.order.service.ReviewInternalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner/order/review")
@Tag(name = "评价内部接口", description = "order-service 评价域内部接口")
public class ReviewInnerController {
  private final ReviewInternalService reviewInternalService;

  public ReviewInnerController(ReviewInternalService reviewInternalService) {
    this.reviewInternalService = reviewInternalService;
  }

  @PostMapping("")
  @Operation(summary = "创建评价", description = "新增订单评价")
  public HttpResult<ReviewSnapshotVO> createReview(@RequestBody CreateReviewRequest request) {
    if (request == null
        || request.getCustomerId() == null
        || request.getBusinessId() == null
        || request.getOrderId() == null
        || request.getAnonymous() == null
        || request.getStars() == null
        || request.getContent() == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review required fields CANT BE NULL");
    }
    try {
      ReviewSnapshotVO created =
          reviewInternalService.createReview(
              new ReviewInternalService.CreateReviewCommand(
                  request.getCustomerId(),
                  request.getBusinessId(),
                  request.getOrderId(),
                  request.getAnonymous(),
                  request.getStars(),
                  request.getContent()));
      return HttpResult.success(created);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PatchMapping("/{reviewId}")
  @Operation(summary = "更新评价", description = "更新评价字段")
  public HttpResult<ReviewSnapshotVO> updateReview(
      @Parameter(description = "评价ID", required = true) @PathVariable("reviewId") Long reviewId,
      @RequestBody UpdateReviewRequest request) {
    if (request == null
        || (request.getStars() == null
            && request.getContent() == null
            && request.getAnonymous() == null)) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review fields CANT BE NULL");
    }
    try {
      ReviewSnapshotVO updated =
          reviewInternalService.updateReview(
              reviewId,
              new ReviewInternalService.UpdateReviewCommand(
                  request.getStars(), request.getContent(), request.getAnonymous()));
      return HttpResult.success(updated);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/{reviewId}")
  @Operation(summary = "按评价ID查询", description = "查询指定评价")
  public HttpResult<ReviewSnapshotVO> getReviewById(
      @Parameter(description = "评价ID", required = true) @PathVariable("reviewId") Long reviewId) {
    ReviewSnapshotVO review = reviewInternalService.getReviewById(reviewId);
    if (review == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");
    }
    return HttpResult.success(review);
  }

  @GetMapping("/order/{orderId}")
  @Operation(summary = "按订单ID查询评价", description = "查询订单对应评价")
  public HttpResult<ReviewSnapshotVO> getReviewByOrderId(
      @Parameter(description = "订单ID", required = true) @PathVariable("orderId") Long orderId) {
    ReviewSnapshotVO review = reviewInternalService.getReviewByOrderId(orderId);
    if (review == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");
    }
    return HttpResult.success(review);
  }

  @GetMapping("/customer/{customerId}")
  @Operation(summary = "按用户ID查询评价列表", description = "查询用户评价列表")
  public HttpResult<List<ReviewSnapshotVO>> getReviewsByCustomerId(
      @Parameter(description = "用户ID", required = true) @PathVariable("customerId")
          Long customerId) {
    return HttpResult.success(reviewInternalService.getReviewsByCustomerId(customerId));
  }

  @GetMapping("/business/{businessId}")
  @Operation(summary = "按商家ID查询评价列表", description = "查询商家评价列表")
  public HttpResult<List<ReviewSnapshotVO>> getReviewsByBusinessId(
      @Parameter(description = "商家ID", required = true) @PathVariable("businessId")
          Long businessId) {
    return HttpResult.success(reviewInternalService.getReviewsByBusinessId(businessId));
  }

  @DeleteMapping("/{reviewId}")
  @Operation(summary = "删除评价", description = "软删除评价")
  public HttpResult<ReviewSnapshotVO> deleteReview(
      @Parameter(description = "评价ID", required = true) @PathVariable("reviewId") Long reviewId) {
    try {
      ReviewSnapshotVO deleted = reviewInternalService.deleteReview(reviewId);
      if (deleted == null) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");
      }
      return HttpResult.success(deleted);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  public static class CreateReviewRequest {
    private Long customerId;
    private Long businessId;
    private Long orderId;
    private Boolean anonymous;
    private Integer stars;
    private String content;

    public Long getCustomerId() {
      return customerId;
    }

    public void setCustomerId(Long customerId) {
      this.customerId = customerId;
    }

    public Long getBusinessId() {
      return businessId;
    }

    public void setBusinessId(Long businessId) {
      this.businessId = businessId;
    }

    public Long getOrderId() {
      return orderId;
    }

    public void setOrderId(Long orderId) {
      this.orderId = orderId;
    }

    public Boolean getAnonymous() {
      return anonymous;
    }

    public void setAnonymous(Boolean anonymous) {
      this.anonymous = anonymous;
    }

    public Integer getStars() {
      return stars;
    }

    public void setStars(Integer stars) {
      this.stars = stars;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }
  }

  public static class UpdateReviewRequest {
    private Integer stars;
    private String content;
    private Boolean anonymous;

    public Integer getStars() {
      return stars;
    }

    public void setStars(Integer stars) {
      this.stars = stars;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }

    public Boolean getAnonymous() {
      return anonymous;
    }

    public void setAnonymous(Boolean anonymous) {
      this.anonymous = anonymous;
    }
  }
}
