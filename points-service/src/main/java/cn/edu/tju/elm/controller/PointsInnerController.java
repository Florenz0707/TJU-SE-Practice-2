package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.exception.PointsException;
import cn.edu.tju.elm.service.PointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner/points")
@Tag(name = "积分内部接口", description = "系统内部调用的积分接口，用于订单和评价积分发放")
public class PointsInnerController {
  private final PointsService pointsService;

  public PointsInnerController(PointsService pointsService) {
    this.pointsService = pointsService;
  }

  @PostMapping("/notify/order-success")
  @Operation(summary = "订单完成发放积分", description = "订单系统调用此接口，根据订单金额发放积分")
  public HttpResult<Integer> notifyOrderSuccess(
      @Parameter(description = "订单成功请求信息", required = true) @RequestBody
          OrderSuccessRequest request) {
    try {
      Integer points =
          pointsService.notifyOrderSuccess(
              request.getUserId().longValue(),
              request.getBizId(),
              request.getAmount(),
              request.getEventTime(),
              request.getExtraInfo());
      return HttpResult.success(points);
    } catch (PointsException e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "积分发放失败：" + e.getMessage());
    }
  }

  @PostMapping("/notify/review-success")
  @Operation(summary = "评价完成发放积分", description = "评价发布后调用，发放评价积分")
  public HttpResult<Integer> notifyReviewSuccess(
      @Parameter(description = "评价成功请求信息", required = true) @RequestBody
          ReviewSuccessRequest request) {
    try {
      Integer points =
          pointsService.notifyReviewSuccess(
              request.getUserId().longValue(),
              request.getBizId(),
              request.getAmount(),
              request.getEventTime(),
              request.getExtraInfo());
      return HttpResult.success(points);
    } catch (PointsException e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "积分发放失败：" + e.getMessage());
    }
  }

  @PostMapping("/trade/freeze")
  @Operation(summary = "积分冻结", description = "用户选择积分抵扣时调用，按有效期优先原则冻结积分")
  public HttpResult<Map<String, Object>> freezePoints(
      @Parameter(description = "冻结积分请求信息", required = true) @RequestBody
          FreezePointsRequest request) {
    try {
      Map<String, Object> result =
          pointsService.freezePoints(
              request.getUserId().longValue(), request.getPoints(), request.getTempOrderId());
      return HttpResult.success(result);
    } catch (PointsException e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "积分冻结失败：" + e.getMessage());
    }
  }

  @PostMapping("/trade/deduct")
  @Operation(summary = "积分扣除", description = "支付成功后调用，将冻结积分转为实际扣除")
  public HttpResult<Boolean> deductPoints(
      @Parameter(description = "扣除积分请求信息", required = true) @RequestBody
          DeductPointsRequest request) {
    try {
      boolean success =
          pointsService.deductPoints(
              request.getUserId().longValue(), request.getTempOrderId(), request.getFinalOrderId());
      return HttpResult.success(success);
    } catch (PointsException e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "积分扣除失败：" + e.getMessage());
    }
  }

  @PostMapping("/trade/rollback")
  @Operation(summary = "积分解冻回滚", description = "订单取消或支付失败时调用，释放冻结的积分")
  public HttpResult<Boolean> rollbackPoints(
      @Parameter(description = "回滚积分请求信息", required = true) @RequestBody
          RollbackPointsRequest request) {
    try {
      boolean success =
          pointsService.rollbackPoints(
              request.getUserId().longValue(), request.getTempOrderId(), request.getReason());
      return HttpResult.success(success);
    } catch (PointsException e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "积分回滚失败：" + e.getMessage());
    }
  }

  @PostMapping("/trade/refund")
  @Operation(summary = "返还已扣减积分", description = "订单取消后调用，返还已扣减的积分")
  public HttpResult<Boolean> refundDeductedPoints(
      @Parameter(description = "返还积分请求信息", required = true) @RequestBody
          RefundDeductedPointsRequest request) {
    try {
      boolean success =
          pointsService.refundDeductedPoints(
              request.getUserId().longValue(), request.getOrderBizId(), request.getReason());
      return HttpResult.success(success);
    } catch (PointsException e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "积分返还失败：" + e.getMessage());
    }
  }

  @PostMapping("/notify/review-deleted")
  @Operation(summary = "评价删除扣减积分", description = "评价删除后调用，回收对应评价积分")
  public HttpResult<Boolean> notifyReviewDeleted(
      @Parameter(description = "评价删除请求信息", required = true) @RequestBody
          ReviewDeletedRequest request) {
    try {
      boolean success =
          pointsService.notifyReviewDeleted(request.getUserId().longValue(), request.getReviewId());
      return HttpResult.success(success);
    } catch (PointsException e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "评价积分回收失败：" + e.getMessage());
    }
  }

  // Request DTOs
  public static class OrderSuccessRequest {
    private Integer userId;
    private String bizId;
    private Double amount;
    private String eventTime;
    private String extraInfo;

    public Integer getUserId() {
      return userId;
    }

    public void setUserId(Integer userId) {
      this.userId = userId;
    }

    public String getBizId() {
      return bizId;
    }

    public void setBizId(String bizId) {
      this.bizId = bizId;
    }

    public Double getAmount() {
      return amount;
    }

    public void setAmount(Double amount) {
      this.amount = amount;
    }

    public String getEventTime() {
      return eventTime;
    }

    public void setEventTime(String eventTime) {
      this.eventTime = eventTime;
    }

    public String getExtraInfo() {
      return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
      this.extraInfo = extraInfo;
    }
  }

  public static class ReviewSuccessRequest {
    private Integer userId;
    private String bizId;
    private Integer amount;
    private String eventTime;
    private String extraInfo;

    public Integer getUserId() {
      return userId;
    }

    public void setUserId(Integer userId) {
      this.userId = userId;
    }

    public String getBizId() {
      return bizId;
    }

    public void setBizId(String bizId) {
      this.bizId = bizId;
    }

    public Integer getAmount() {
      return amount;
    }

    public void setAmount(Integer amount) {
      this.amount = amount;
    }

    public String getEventTime() {
      return eventTime;
    }

    public void setEventTime(String eventTime) {
      this.eventTime = eventTime;
    }

    public String getExtraInfo() {
      return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
      this.extraInfo = extraInfo;
    }
  }

  public static class FreezePointsRequest {
    private Integer userId;
    private Integer points;
    private String tempOrderId;

    public Integer getUserId() {
      return userId;
    }

    public void setUserId(Integer userId) {
      this.userId = userId;
    }

    public Integer getPoints() {
      return points;
    }

    public void setPoints(Integer points) {
      this.points = points;
    }

    public String getTempOrderId() {
      return tempOrderId;
    }

    public void setTempOrderId(String tempOrderId) {
      this.tempOrderId = tempOrderId;
    }
  }

  public static class DeductPointsRequest {
    private Integer userId;
    private String tempOrderId;
    private String finalOrderId;

    public Integer getUserId() {
      return userId;
    }

    public void setUserId(Integer userId) {
      this.userId = userId;
    }

    public String getTempOrderId() {
      return tempOrderId;
    }

    public void setTempOrderId(String tempOrderId) {
      this.tempOrderId = tempOrderId;
    }

    public String getFinalOrderId() {
      return finalOrderId;
    }

    public void setFinalOrderId(String finalOrderId) {
      this.finalOrderId = finalOrderId;
    }
  }

  public static class RollbackPointsRequest {
    private Integer userId;
    private String tempOrderId;
    private String reason;

    public Integer getUserId() {
      return userId;
    }

    public void setUserId(Integer userId) {
      this.userId = userId;
    }

    public String getTempOrderId() {
      return tempOrderId;
    }

    public void setTempOrderId(String tempOrderId) {
      this.tempOrderId = tempOrderId;
    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }

  public static class RefundDeductedPointsRequest {
    private Integer userId;
    private String orderBizId;
    private String reason;

    public Integer getUserId() {
      return userId;
    }

    public void setUserId(Integer userId) {
      this.userId = userId;
    }

    public String getOrderBizId() {
      return orderBizId;
    }

    public void setOrderBizId(String orderBizId) {
      this.orderBizId = orderBizId;
    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }

  public static class ReviewDeletedRequest {
    private Integer userId;
    private String reviewId;

    public Integer getUserId() {
      return userId;
    }

    public void setUserId(Integer userId) {
      this.userId = userId;
    }

    public String getReviewId() {
      return reviewId;
    }

    public void setReviewId(String reviewId) {
      this.reviewId = reviewId;
    }
  }
}
