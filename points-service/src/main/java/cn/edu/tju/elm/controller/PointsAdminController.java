package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.elm.exception.PointsException;
import cn.edu.tju.elm.model.BO.PointsRule;
import cn.edu.tju.elm.model.VO.PointsRuleVO;
import cn.edu.tju.elm.service.PointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points/admin/rules")
@Tag(name = "积分规则管理", description = "管理员对积分规则的增删改查")
public class PointsAdminController {
  private final PointsService pointsService;

  public PointsAdminController(PointsService pointsService) {
    this.pointsService = pointsService;
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

  @PostMapping("")
  @Operation(summary = "新增积分规则", description = "创建新的积分发放/消耗规则")
  public HttpResult<PointsRuleVO> createPointsRule(@RequestBody PointsRule rule) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
    }
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "需要管理员权限");
    }

    try {
      rule.setCreator(currentUserId);
      rule.setUpdater(currentUserId);
      PointsRule created = pointsService.createPointsRule(rule);
      return HttpResult.success(new PointsRuleVO(created));
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("")
  @Operation(summary = "获取规则列表", description = "查看当前系统配置的所有规则")
  public HttpResult<List<PointsRuleVO>> getAllPointsRules() {
    try {
      List<PointsRule> rules = pointsService.getAllPointsRules();
      List<PointsRuleVO> ruleVOs =
          rules.stream().map(PointsRuleVO::new).collect(Collectors.toList());
      return HttpResult.success(ruleVOs);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PutMapping("/{id}")
  @Operation(summary = "修改积分规则", description = "更新规则（如调整双十一双倍积分）")
  public HttpResult<PointsRuleVO> updatePointsRule(
      @PathVariable("id") Long id, @RequestBody PointsRule rule) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
    }
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "需要管理员权限");
    }

    try {
      rule.setUpdater(currentUserId);
      PointsRule updated = pointsService.updatePointsRule(id, rule);
      return HttpResult.success(new PointsRuleVO(updated));
    } catch (PointsException e) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, e.getMessage());
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "删除积分规则", description = "删除系统中已有的积分规则")
  public HttpResult<String> deletePointsRule(@PathVariable("id") Long id) {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
    }
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "需要管理员权限");
    }

    try {
      pointsService.deletePointsRule(id);
      return HttpResult.success("删除成功");
    } catch (PointsException e) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, e.getMessage());
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }
}
