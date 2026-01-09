package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.exception.PointsException;
import cn.edu.tju.elm.service.PointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inner/points")
@Tag(name = "积分内部接口", description = "系统内部调用的积分接口")
public class PointsInnerController {
    private final PointsService pointsService;

    public PointsInnerController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @PostMapping("/notify/order-success")
    @Operation(summary = "订单完成（发放积分）", description = "订单系统调用此接口。积分系统内部会查找 channelType=ORDER 的规则，计算积分，创建 PointsRecord 和 PointsBatch")
    public HttpResult<Integer> notifyOrderSuccess(@RequestBody OrderSuccessRequest request) {
        try {
            Integer points = pointsService.notifyOrderSuccess(
                    request.getUserId().longValue(),
                    request.getBizId(),
                    request.getAmount(),
                    request.getEventTime(),
                    request.getExtraInfo()
            );
            return HttpResult.success(points);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "积分发放失败：" + e.getMessage());
        }
    }

    @PostMapping("/notify/review-success")
    @Operation(summary = "评价完成发放积分", description = "评价发布后调用，发放积分")
    public HttpResult<Integer> notifyReviewSuccess(@RequestBody ReviewSuccessRequest request) {
        try {
            Integer points = pointsService.notifyReviewSuccess(
                    request.getUserId().longValue(),
                    request.getBizId(),
                    request.getAmount(),
                    request.getEventTime(),
                    request.getExtraInfo()
            );
            return HttpResult.success(points);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "积分发放失败：" + e.getMessage());
        }
    }

    @PostMapping("/trade/freeze")
    @Operation(summary = "积分冻结（积分抵扣）", description = "用户在确认订单页面选择积分抵扣时调用。系统需按有效期优先原则（FIFO）锁定即将过期的积分")
    public HttpResult<Map<String, Object>> freezePoints(@RequestBody FreezePointsRequest request) {
        try {
            Map<String, Object> result = pointsService.freezePoints(
                    request.getUserId().longValue(),
                    request.getPoints(),
                    request.getTempOrderId()
            );
            return HttpResult.success(result);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "积分冻结失败：" + e.getMessage());
        }
    }

    @PostMapping("/trade/deduct")
    @Operation(summary = "积分扣除", description = "支付成功后调用，将冻结状态转为实际扣除，并生成消费流水记录。")
    public HttpResult<Boolean> deductPoints(@RequestBody DeductPointsRequest request) {
        try {
            boolean success = pointsService.deductPoints(
                    request.getUserId().longValue(),
                    request.getTempOrderId(),
                    request.getFinalOrderId()
            );
            return HttpResult.success(success);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "积分扣除失败：" + e.getMessage());
        }
    }

    @PostMapping("/trade/rollback")
    @Operation(summary = "积分解冻/回滚", description = "用户未支付/取消订单，或支付失败时调用。将冻结的积分释放回账户。")
    public HttpResult<Boolean> rollbackPoints(@RequestBody RollbackPointsRequest request) {
        try {
            boolean success = pointsService.rollbackPoints(
                    request.getUserId().longValue(),
                    request.getTempOrderId(),
                    request.getReason()
            );
            return HttpResult.success(success);
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "积分回滚失败：" + e.getMessage());
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
}
