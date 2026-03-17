package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.constant.ApplicationState;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.BusinessApplication;
import cn.edu.tju.elm.service.BusinessApplicationService;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.EntityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications/business")
@Tag(name = "管理开店申请", description = "提供商家开店申请的提交、审核和查询功能")
public class BusinessApplicationController {
  private final UserService userService;
  private final BusinessService businessService;
  private final BusinessApplicationService businessApplicationService;

  public BusinessApplicationController(
      UserService userService,
      BusinessService businessService,
      BusinessApplicationService businessApplicationService) {
    this.userService = userService;
    this.businessService = businessService;
    this.businessApplicationService = businessApplicationService;
  }

  @PostMapping("")
  @Operation(summary = "提交开店申请", description = "商家用户提交新的开店申请")
  public HttpResult<BusinessApplication> addBusinessApplication(
      @Parameter(description = "开店申请信息", required = true) @RequestBody
          BusinessApplication businessApplication) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (!AuthorityUtils.hasAuthority(me, "BUSINESS"))
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "NOT A MERCHANT YET");

    if (businessApplication == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessApplication CANT BE NULL");

    if (businessApplication.getBusiness() == null
        || businessApplication.getBusiness().getBusinessName() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.BusinessName NOT FOUND");

    Business business = businessApplication.getBusiness();
    EntityUtils.setNewEntity(business);
    business.setDeleted(true);
    business.setBusinessOwner(me);
    businessService.addBusiness(business);

    EntityUtils.setNewEntity(businessApplication);
    businessApplication.setBusiness(business);
    businessApplication.setApplicationState(ApplicationState.UNDISPOSED);
    User admin = userService.getUserWithUsername("admin");
    businessApplication.setHandler(admin);
    businessApplicationService.addApplication(businessApplication);
    return HttpResult.success(businessApplication);
  }

  @GetMapping("")
  @PreAuthorize("hasAuthority('ADMIN')")
  @Operation(summary = "获取所有开店申请", description = "管理员查询所有开店申请列表")
  public HttpResult<List<BusinessApplication>> getBusinessApplications() {
    return HttpResult.success(businessApplicationService.getAllBusinessApplications());
  }

  @GetMapping("/{id}")
  @Operation(summary = "根据ID获取开店申请", description = "查询指定开店申请的详细信息")
  public HttpResult<BusinessApplication> getBusinessApplication(
      @Parameter(description = "申请ID", required = true) @PathVariable Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");

    BusinessApplication businessApplication =
        businessApplicationService.getBusinessApplicationById(id);
    if (businessApplication == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessApplication NOT FOUND");

    if (isAdmin || (isBusiness && me.equals(businessApplication.getBusiness().getBusinessOwner())))
      return HttpResult.success(businessApplication);
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasAuthority('ADMIN')")
  @Operation(summary = "审核开店申请", description = "管理员审核开店申请，通过后店铺自动上线")
  public HttpResult<BusinessApplication> handleBusinessApplication(
      @Parameter(description = "申请ID", required = true) @PathVariable Long id,
      @Parameter(description = "审核结果", required = true) @RequestBody
          BusinessApplication businessApplication) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    BusinessApplication oldApplication = businessApplicationService.getBusinessApplicationById(id);
    if (oldApplication == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessApplication NOT FOUND");

    if (oldApplication.getBusiness() == null
        || oldApplication.getBusiness().getBusinessName() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business.BusinessName NOT FOUND");

    if (!oldApplication.getApplicationState().equals(ApplicationState.UNDISPOSED))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ALREADY DISPOSED");

    if (businessApplication == null || businessApplication.getApplicationState() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ApplicationState CANT BE NULL");

    if (!ApplicationState.isValidApplicationState(oldApplication.getApplicationState()))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ApplicationState NOT VALID");

    if (me.equals(oldApplication.getHandler())) {
      oldApplication.setApplicationState(businessApplication.getApplicationState());
      EntityUtils.updateEntity(oldApplication);
      businessApplicationService.updateBusinessApplication(oldApplication);

      if (oldApplication.getApplicationState().equals(ApplicationState.APPROVED)) {
        Business business = oldApplication.getBusiness();
        business.setDeleted(false);
        business.setUpdateTime(LocalDateTime.now());
        businessService.updateBusiness(business);
      }
      return HttpResult.success(oldApplication);
    }

    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @GetMapping("/my")
  @Operation(summary = "获取我的开店申请", description = "商家查询自己提交的所有开店申请")
  public HttpResult<List<BusinessApplication>> getMyBusinessApplication() {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (!AuthorityUtils.hasAuthority(me, "BUSINESS"))
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "NOT A MERCHANT YET");

    return HttpResult.success(businessApplicationService.getBusinessApplicationsByApplicant(me));
  }
}
