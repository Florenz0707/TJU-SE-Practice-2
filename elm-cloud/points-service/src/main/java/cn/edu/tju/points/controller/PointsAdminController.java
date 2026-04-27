package cn.edu.tju.points.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.points.exception.PointsException;
import cn.edu.tju.points.model.BO.PointsRule;
import cn.edu.tju.points.model.VO.PointsRuleVO;
import cn.edu.tju.points.service.PointsService;
import cn.edu.tju.points.util.JwtUtils;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

@RefreshScope
@RestController
@RequestMapping("/api/points/admin/rules")
public class PointsAdminController {
  private final PointsService pointsService;
  private final JwtUtils jwtUtils;

  public PointsAdminController(PointsService pointsService, JwtUtils jwtUtils) {
    this.pointsService = pointsService;
    this.jwtUtils = jwtUtils;
  }

  @PostMapping("")
  public HttpResult<PointsRuleVO> createPointsRule(@RequestHeader(value = "Authorization", required = false) String token, @RequestBody PointsRule rule) {
    Long currentUserId = jwtUtils.getUserIdFromToken(token);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
    }
    if (!jwtUtils.hasAdminAuthority(token)) {
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
  public HttpResult<PointsRuleVO> updatePointsRule(
      @RequestHeader(value = "Authorization", required = false) String token,
      @PathVariable("id") Long id, @RequestBody PointsRule rule) {
    Long currentUserId = jwtUtils.getUserIdFromToken(token);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
    }
    if (!jwtUtils.hasAdminAuthority(token)) {
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
  public HttpResult<String> deletePointsRule(@RequestHeader(value = "Authorization", required = false) String token, @PathVariable("id") Long id) {
    Long currentUserId = jwtUtils.getUserIdFromToken(token);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
    }
    if (!jwtUtils.hasAdminAuthority(token)) {
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
