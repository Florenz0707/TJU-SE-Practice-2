package cn.edu.tju.points.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.points.exception.PointsException;
import cn.edu.tju.points.model.BO.PointsAccount;
import cn.edu.tju.points.model.BO.PointsRecord;
import cn.edu.tju.points.model.VO.PointsAccountVO;
import cn.edu.tju.points.model.VO.PointsRecordVO;
import cn.edu.tju.points.service.PointsService;
import cn.edu.tju.points.util.JwtUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
public class PointsController {
  private final PointsService pointsService;
  private final JwtUtils jwtUtils;

  public PointsController(PointsService pointsService, JwtUtils jwtUtils) {
    this.pointsService = pointsService;
    this.jwtUtils = jwtUtils;
  }

  @GetMapping("/account/my")
  public HttpResult<PointsAccountVO> getMyPointsAccount(@RequestHeader(value = "Authorization", required = false) String token) {
    System.out.println("=== PointsController.getMyPointsAccount() ===");
    System.out.println("Token received: " + (token != null ? token.substring(0, Math.min(50, token.length())) + "..." : "null"));
    Long currentUserId = jwtUtils.getUserIdFromToken(token);
    System.out.println("Parsed userId: " + currentUserId);
    
    if (currentUserId == null) {
      System.out.println("Returning error: 用户未登录");
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
    }

    try {
      System.out.println("Fetching account for userId: " + currentUserId);
      PointsAccount account = pointsService.getPointsAccount(currentUserId);
      if (account == null) {
        System.out.println("Returning error: ACCOUNT_NOT_FOUND");
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ACCOUNT_NOT_FOUND");
      }
      PointsAccountVO vo = new PointsAccountVO(account);
      System.out.println("Returning VO: " + vo);
      return HttpResult.success(vo);
    } catch (PointsException e) {
      System.err.println("PointsException caught: " + e.getMessage());
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/record/my")
  public HttpResult<Map<String, Object>> getMyPointsRecords(
      @RequestHeader(value = "Authorization", required = false) String token,
      @RequestParam("page") Integer page,
      @RequestParam("size") Integer size,
      @RequestParam(value = "type", required = false) String type) {
    Long currentUserId = jwtUtils.getUserIdFromToken(token);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
    }

    try {
      Page<PointsRecord> records = pointsService.getPointsRecords(currentUserId, page, size, type);
      List<PointsRecordVO> recordVOs =
          records.getContent().stream().map(PointsRecordVO::new).collect(Collectors.toList());

      Map<String, Object> result = new HashMap<>();
      result.put("records", recordVOs);
      result.put("total", records.getTotalElements());

      return HttpResult.success(result);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }
}
