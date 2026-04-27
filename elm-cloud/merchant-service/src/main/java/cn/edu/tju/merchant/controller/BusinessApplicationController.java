package cn.edu.tju.merchant.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import cn.edu.tju.merchant.service.UserService;
import cn.edu.tju.merchant.util.AuthorityUtils;
import cn.edu.tju.merchant.model.User;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RefreshScope
@RestController
@RequestMapping("/api/applications/business")

public class BusinessApplicationController {
  private static final Logger log = LoggerFactory.getLogger(BusinessApplicationController.class);
  private boolean isValidApplicationState(Integer state) { return state != null && (state == 1 || state == 2); }

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

    boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
    log.info(
        "addBusinessApplication: uid={}, username={}, authorities={}, isBusiness={}",
        me.getId(),
        me.getUsername(),
        me.getAuthorities(),
        isBusiness);

    // Business application (open a store) requires the user to already be a merchant (BUSINESS).
    if (!isBusiness) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "NOT A MERCHANT YET");
    }

    // Hard guard: if the merchant already has an active store, they shouldn't apply again.
    List<Business> myBusinesses = businessService.getBusinessByOwnerId(me.getId());
    if (myBusinesses != null) {
      for (Business b : myBusinesses) {
        if (b != null && (b.getDeleted() == null || !b.getDeleted())) {
          return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "ALREADY HAS STORE");
        }
      }
    }

    // If there is already a pending application, forbid duplicate submissions.
    try {
      List<BusinessApplication> myApps =
          businessApplicationService.getBusinessApplicationsByApplicantId(me.getId());
      if (myApps != null) {
        for (BusinessApplication a : myApps) {
          if (a != null && Integer.valueOf(1).equals(a.getApplicationState())) {
            return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "APPLICATION ALREADY SUBMITTED");
          }
        }
      }
    } catch (Exception ignore) {
      // best-effort
    }

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
    businessApplication.setApplicantId(me.getId());
    User admin = userService.getUserWithUsername("admin").orElse(null);
    if (admin == null || admin.getId() == null) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ADMIN USER NOT FOUND");
    }
    businessApplication.setHandlerId(admin.getId());
    businessApplicationService.addApplication(businessApplication);
    compatibilityEnricher.enrichBusinessApplication(businessApplication);
    return HttpResult.success(businessApplication);
  }

  @GetMapping("")
  
  public HttpResult<List<BusinessApplication>> getBusinessApplications() {
    List<BusinessApplication> apps = businessApplicationService.getAllBusinessApplications();
    compatibilityEnricher.enrichBusinessApplications(apps);
    return HttpResult.success(apps);
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
            && me.getId().equals(businessApplication.getBusiness().getBusinessOwnerId()))) {
      compatibilityEnricher.enrichBusinessApplication(businessApplication);
      return HttpResult.success(businessApplication);
    }
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

    if (!isValidApplicationState(businessApplication.getApplicationState()))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ApplicationState NOT VALID");

    if (me.getId().equals(oldApplication.getHandlerId())) {
      oldApplication.setApplicationState(businessApplication.getApplicationState());
      EntityUtils.updateEntity(oldApplication);
      businessApplicationService.updateBusinessApplication(oldApplication);

      if (oldApplication.getApplicationState().equals(2)) {
        Business business = oldApplication.getBusiness();
        business.setDeleted(false);
        business.setUpdateTime(LocalDateTime.now());
        businessService.updateBusiness(business);

        // After approval, upgrade the applicant's authority to BUSINESS.
        // The applicant is the business owner recorded in the application business.
        try {
          Long applicantId = business.getBusinessOwnerId();
          if (applicantId != null) {
            User applicant = new User();
            applicant.setId(applicantId);
            java.util.Set<String> auths = new java.util.HashSet<>();
            auths.add("USER");
            auths.add("BUSINESS");
            applicant.setAuthorities(auths);
            boolean ok = userService.updateUser(applicant);
            if (!ok) {
              return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "UPDATE USER AUTHORITIES FAILED");
            }
          }
        } catch (Exception ignore) {
          // updateUser already logs details; keep approval flow resilient
        }
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

    // Viewing my applications should be allowed as long as the user is a merchant.
    if (!AuthorityUtils.hasAuthority(me, "BUSINESS")) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "NOT A MERCHANT YET");
    }

  List<BusinessApplication> apps =
    businessApplicationService.getBusinessApplicationsByApplicantId(me.getId());
  compatibilityEnricher.enrichBusinessApplications(apps);
  return HttpResult.success(apps);
  }
}
