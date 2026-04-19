package cn.edu.tju.points.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.points.model.BO.PointsAccount;
import cn.edu.tju.points.service.PointsInternalService;
import cn.edu.tju.points.service.PointsService;
import cn.edu.tju.points.exception.PointsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inner/points")
public class PointsInnerController {

    private final PointsInternalService pointsInternalService;
    private final PointsService pointsService;

    public PointsInnerController(PointsInternalService pointsInternalService, PointsService pointsService) {
        this.pointsInternalService = pointsInternalService;
        this.pointsService = pointsService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PointsAccount> byUser(@PathVariable Long userId) {
        return pointsInternalService.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/notify/order-success")
    public HttpResult<Integer> notifyOrderSuccess(@RequestBody NotifyOrderSuccessRequest request) {
        try {
            Integer points = pointsInternalService.notifyOrderSuccess(
                    request.getUserId(),
                    request.getBizId(),
                    request.getAmount(),
                    request.getEventTime(),
                    request.getExtraInfo()
            );
            return HttpResult.success(points);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/notify/login-success")
    public HttpResult<Integer> notifyLoginSuccess(@RequestBody NotifyLoginSuccessRequest request) {
        try {
            Integer points = pointsInternalService.notifyLoginSuccess(
                    request.getUserId(),
                    request.getEventTime()
            );
            return HttpResult.success(points);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/notify/register-success")
    public HttpResult<Integer> notifyRegisterSuccess(@RequestBody NotifyRegisterSuccessRequest request) {
        try {
            Integer points = pointsInternalService.notifyRegisterSuccess(
                    request.getUserId(),
                    request.getEventTime()
            );
            return HttpResult.success(points);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/notify/review-success")
    public HttpResult<Integer> notifyReviewSuccess(@RequestBody NotifyReviewSuccessRequest request) {
        try {
            Integer points = pointsInternalService.notifyReviewSuccess(
                    request.getUserId(),
                    request.getBizId(),
                    request.getAmount(),
                    request.getEventTime(),
                    request.getExtraInfo()
            );
            return HttpResult.success(points);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/notify/review-deleted")
    public HttpResult<Boolean> notifyReviewDeleted(@RequestBody NotifyReviewDeletedRequest request) {
        try {
            boolean success = pointsInternalService.notifyReviewDeleted(
                    request.getUserId(),
                    request.getReviewId()
            );
            return HttpResult.success(success);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    public static class NotifyOrderSuccessRequest {
        private Long userId;
        private String bizId;
        private Double amount;
        private String eventTime;
        private String extraInfo;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
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

    public static class NotifyLoginSuccessRequest {
        private Long userId;
        private String eventTime;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getEventTime() {
            return eventTime;
        }

        public void setEventTime(String eventTime) {
            this.eventTime = eventTime;
        }
    }

    public static class NotifyRegisterSuccessRequest {
        private Long userId;
        private String eventTime;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getEventTime() {
            return eventTime;
        }

        public void setEventTime(String eventTime) {
            this.eventTime = eventTime;
        }
    }

    public static class NotifyReviewSuccessRequest {
        private Long userId;
        private String bizId;
        private Integer amount;
        private String eventTime;
        private String extraInfo;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
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

    public static class NotifyReviewDeletedRequest {
        private Long userId;
        private String reviewId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getReviewId() {
            return reviewId;
        }

        public void setReviewId(String reviewId) {
            this.reviewId = reviewId;
        }
    }

    @PostMapping("/freeze")
    public HttpResult<Map<String, Object>> freezePoints(@RequestBody FreezePointsRequest request) {
        try {
            Map<String, Object> result = pointsService.freezePoints(
                    request.getUserId(),
                    request.getPoints(),
                    request.getTempOrderId()
            );
            return HttpResult.success(result);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/deduct")
    public HttpResult<Boolean> deductPoints(@RequestBody DeductPointsRequest request) {
        try {
            boolean success = pointsService.deductPoints(
                    request.getUserId(),
                    request.getTempOrderId(),
                    request.getFinalOrderId()
            );
            return HttpResult.success(success);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/rollback")
    public HttpResult<Boolean> rollbackPoints(@RequestBody RollbackPointsRequest request) {
        try {
            boolean success = pointsService.rollbackPoints(
                    request.getUserId(),
                    request.getTempOrderId(),
                    request.getReason()
            );
            return HttpResult.success(success);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/refund-deducted")
    public HttpResult<Boolean> refundDeductedPoints(@RequestBody RefundDeductedPointsRequest request) {
        try {
            boolean success = pointsService.refundDeductedPoints(
                    request.getUserId(),
                    request.getOrderBizId(),
                    request.getReason()
            );
            return HttpResult.success(success);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    public static class FreezePointsRequest {
        private Long userId;
        private Integer points;
        private String tempOrderId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
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
        private Long userId;
        private String tempOrderId;
        private String finalOrderId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
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
        private Long userId;
        private String tempOrderId;
        private String reason;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
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
        private Long userId;
        private String orderBizId;
        private String reason;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
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
}
