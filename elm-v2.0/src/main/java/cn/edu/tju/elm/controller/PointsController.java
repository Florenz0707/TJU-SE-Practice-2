package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.exception.PointsException;
import cn.edu.tju.elm.model.BO.PointsAccount;
import cn.edu.tju.elm.model.BO.PointsRecord;
import cn.edu.tju.elm.model.VO.PointsAccountVO;
import cn.edu.tju.elm.model.VO.PointsRecordVO;
import cn.edu.tju.elm.service.PointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/points")
@Tag(name = "积分管理", description = "用户积分账户和积分明细查询")
public class PointsController {
    private final UserService userService;
    private final PointsService pointsService;

    public PointsController(UserService userService, PointsService pointsService) {
        this.userService = userService;
        this.pointsService = pointsService;
    }

    @GetMapping("/account/my")
    @Operation(summary = "获取我的积分账户信息", description = "查询当前登录用户的积分余额，返回当前用户的总积分、冻结积分等信息。后端需从 Token 解析 userId")
    public HttpResult<PointsAccountVO> getMyPointsAccount() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
        }
        User me = meOptional.get();

        try {
            PointsAccount account = pointsService.getPointsAccount(me.getId());
            if (account == null) {
                return HttpResult.failure(ResultCodeEnum.NOT_FOUND, PointsException.ACCOUNT_NOT_FOUND);
            }
            return HttpResult.success(new PointsAccountVO(account));
        } catch (PointsException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/record/my")
    @Operation(summary = "分页查询积分明细", description = "分页展示积分获取和消费的记录")
    public HttpResult<Map<String, Object>> getMyPointsRecords(
            @RequestParam("page") Integer page,
            @RequestParam("size") Integer size,
            @RequestParam(value = "type", required = false) String type) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
        }
        User me = meOptional.get();

        try {
            Page<PointsRecord> records = pointsService.getPointsRecords(me.getId(), page, size, type);
            List<PointsRecordVO> recordVOs = records.getContent().stream()
                    .map(PointsRecordVO::new)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", recordVOs);
            result.put("total", records.getTotalElements());
            
            return HttpResult.success(result);
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }
}
