package cn.edu.tju.points.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.points.model.BO.PointsAccount;
import cn.edu.tju.points.service.PointsInternalService;
import cn.edu.tju.points.exception.PointsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inner/points")
public class PointsInnerController {

    private final PointsInternalService pointsInternalService;

    public PointsInnerController(PointsInternalService pointsInternalService) {
        this.pointsInternalService = pointsInternalService;
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
}
