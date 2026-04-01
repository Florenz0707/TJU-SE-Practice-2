package cn.edu.tju.merchant.controller;
import cn.edu.tju.merchant.service.UserService;

import cn.edu.tju.merchant.util.AuthorityUtils;

import cn.edu.tju.merchant.model.User;

import cn.edu.tju.merchant.model.ApplicationState;


import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;

import cn.edu.tju.merchant.util.JwtUtils;
import cn.edu.tju.merchant.model.Business;
import cn.edu.tju.merchant.model.BusinessApplication;
import cn.edu.tju.merchant.service.BusinessApplicationService;
import cn.edu.tju.merchant.service.BusinessService;

import cn.edu.tju.merchant.util.EntityUtils;
import cn.edu.tju.merchant.util.ResponseCompatibilityEnricher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications/business")

public class BusinessApplicationController {
    private boolean isValidApplicationState(Integer state) { return state == 1 || state == 2; }

  private final UserService userService;
  private final BusinessService businessService;
  private final BusinessApplicationService businessApplicationService;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public BusinessApplicationController(
      UserService userService,
      BusinessService businessService,
      BusinessApplicationService businessApplicationService,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.userService = userService;
    this.businessService = businessService;
    this.businessApplicationService = businessApplicationService;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  @PostMapping("")
  
  public HttpResult<BusinessApplication> addBusinessApplication(
       @RequestBody
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
    business.setBusinessOwnerId(me.getId());
    businessService.addBusiness(business);

    EntityUtils.setNewEntity(businessApplication);
    businessApplication.setBusiness(business);
    businessApplication.setApplicationState(1);
    User admin = userService.getUserWithUsername("admin").orElse(null);
    businessApplication.setHandlerId(admin.getId());
    businessApplicationService.addApplication(businessApplication);
    compatibilityEnricher.enrichBusinessApplication(businessApplication);
    return HttpResult.success(businessApplication);
  }

  @GetMapping("")
  
  public HttpResult<List<BusinessApplication>> getBusinessApplications() {
    return HttpResult.success(businessApplicationService.getAllBusinessApplications());
  }

  @GetMapping("/{id}")
  
  public HttpResult<BusinessApplication> getBusinessApplication(
       @PathVariable Long id) {
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

    if (isAdmin
        || (isBusiness
            && me.getId().equals(businessApplication.getBusiness().getBusinessOwnerId())))
      return HttpResult.success(businessApplication);
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @PatchMapping("/{id}")
  
  public HttpResult<BusinessApplication> handleBusinessApplication(
       @PathVariable Long id,
       @RequestBody
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

    if (!oldApplication.getApplicationState().equals(1))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ALREADY DISPOSED");

    if (businessApplication == null || businessApplication.getApplicationState() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ApplicationState CANT BE NULL");

    if (!isValidApplicationState(oldApplication.getApplicationState()))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ApplicationState NOT VALID");

    if (me.getId().equals(oldApplication.getHandlerId())) {
      oldApplication.setApplicationState(businessApplication.getApplicationState());
      EntityUtils.updateEntity(oldApplication);
      businessApplicationService.updateBusinessApplication(oldApplication);

      if (oldApplication.getApplicationState().equals(2D)) {
        Business business = oldApplication.getBusiness();
        business.setDeleted(false);
        business.setUpdateTime(LocalDateTime.now());
        businessService.updateBusiness(business);
      }
      compatibilityEnricher.enrichBusinessApplication(oldApplication);
      return HttpResult.success(oldApplication);
    }

    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @GetMapping("/my")
  
  public HttpResult<List<BusinessApplication>> getMyBusinessApplication() {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (!AuthorityUtils.hasAuthority(me, "BUSINESS"))
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "NOT A MERCHANT YET");

    return HttpResult.success(
        businessApplicationService.getBusinessApplicationsByApplicantId(me.getId()));
  }
}
