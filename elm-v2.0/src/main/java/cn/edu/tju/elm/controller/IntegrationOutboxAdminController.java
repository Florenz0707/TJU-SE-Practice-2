package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.service.IntegrationOutboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integration/outbox/admin")
@Tag(name = "Outbox管理", description = "Outbox事件可观测与人工补偿管理接口")
public class IntegrationOutboxAdminController {
  private final IntegrationOutboxService integrationOutboxService;

  public IntegrationOutboxAdminController(IntegrationOutboxService integrationOutboxService) {
    this.integrationOutboxService = integrationOutboxService;
  }

  private boolean hasAdminAuthority() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .map(authority -> authority.getAuthority().toUpperCase())
        .anyMatch(name -> "ADMIN".equals(name) || "ROLE_ADMIN".equals(name));
  }

  @GetMapping("/summary")
  @Operation(summary = "Outbox状态总览", description = "查看PENDING/RETRY/SENT/FAILED数量和队列样本")
  public HttpResult<Map<String, Object>> summary() {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "需要管理员权限");
    }
    return HttpResult.success(integrationOutboxService.getStatusSummary());
  }

  @PostMapping("/dispatch-now")
  @Operation(summary = "立即触发Outbox投递", description = "手动触发一轮dispatch，便于联调与恢复")
  public HttpResult<Integer> dispatchNow() {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "需要管理员权限");
    }
    return HttpResult.success(integrationOutboxService.dispatchBatch());
  }

  @PostMapping("/requeue/{eventId}")
  @Operation(summary = "重入队单个失败事件", description = "将FAILED事件重置为RETRY，等待调度重试")
  public HttpResult<Boolean> requeueOne(
      @PathVariable("eventId") Long eventId,
      @RequestBody(required = false) RequeueRequest request) {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "需要管理员权限");
    }
    String reason = request != null ? request.getReason() : null;
    return HttpResult.success(integrationOutboxService.requeueFailedEvent(eventId, reason));
  }

  @PostMapping("/requeue/failed")
  @Operation(summary = "批量重入队失败事件", description = "将FAILED事件批量重置为RETRY（最多20条）")
  public HttpResult<Integer> requeueFailed(@RequestBody(required = false) RequeueRequest request) {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "需要管理员权限");
    }
    String reason = request != null ? request.getReason() : null;
    return HttpResult.success(integrationOutboxService.requeueAllFailedEvents(reason));
  }

  public static class RequeueRequest {
    private String reason;

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }
}
