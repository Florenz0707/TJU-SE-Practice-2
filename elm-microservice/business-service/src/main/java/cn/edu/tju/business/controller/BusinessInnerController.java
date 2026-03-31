package cn.edu.tju.business.controller;

import cn.edu.tju.business.model.vo.BusinessSnapshotVO;
import cn.edu.tju.business.service.BusinessInternalService;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner/business")
@Tag(name = "商家内部接口", description = "订单和食品服务等内部调用的商家查询接口")
public class BusinessInnerController {
  private final BusinessInternalService businessInternalService;

  public BusinessInnerController(BusinessInternalService businessInternalService) {
    this.businessInternalService = businessInternalService;
  }

  @GetMapping("")
  @Operation(summary = "查询全部商家快照", description = "供聚合层回退读取商家列表")
  public HttpResult<List<BusinessSnapshotVO>> getBusinesses() {
    return HttpResult.success(businessInternalService.getBusinessSnapshots());
  }

  @GetMapping("/{businessId}")
  @Operation(summary = "按ID查询商家", description = "查询商家运营快照")
  public HttpResult<BusinessSnapshotVO> getBusinessById(
      @Parameter(description = "商家ID", required = true) @PathVariable("businessId") Long businessId) {
    if (businessId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessId CANT BE NULL");
    }
    BusinessSnapshotVO business = businessInternalService.getBusinessSnapshotById(businessId);
    if (business == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
    }
    return HttpResult.success(business);
  }
}
