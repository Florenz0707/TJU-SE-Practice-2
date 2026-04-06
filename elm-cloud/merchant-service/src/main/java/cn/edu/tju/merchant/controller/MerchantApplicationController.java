package cn.edu.tju.merchant.controller;
import cn.edu.tju.merchant.service.UserService;

import cn.edu.tju.merchant.util.AuthorityUtils;

import cn.edu.tju.merchant.model.User;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.merchant.model.MerchantApplication;
import cn.edu.tju.merchant.service.MerchantApplicationService;

import cn.edu.tju.merchant.util.EntityUtils;
import cn.edu.tju.merchant.util.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications/merchant")

public class MerchantApplicationController {
  private static final Logger log = LoggerFactory.getLogger(MerchantApplicationController.class);

  private final UserService userService;
  private final MerchantApplicationService merchantApplicationService;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public MerchantApplicationController(
      UserService userService,
      MerchantApplicationService merchantApplicationService,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.userService = userService;
    this.merchantApplicationService = merchantApplicationService;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  @PostMapping("")
  
  public HttpResult<MerchantApplication> addMerchantApplication(
       @RequestBody
          MerchantApplication merchantApplication) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
    if (isBusiness) return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ALREADY MERCHANT");

    if (merchantApplication == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "MerchantApplication CANT BE NULL");

    EntityUtils.setNewEntity(merchantApplication);
    merchantApplication.setApplicationState(1);
    merchantApplication.setApplicantId(me.getId());
    User admin = userService.getUserWithUsername("admin").orElse(null);
    if (admin == null || admin.getId() == null)
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ADMIN USER NOT FOUND");
    merchantApplication.setHandlerId(admin.getId());
    merchantApplicationService.addApplication(merchantApplication);
    compatibilityEnricher.enrichMerchantApplication(merchantApplication);
    return HttpResult.success(merchantApplication);
  }

  @GetMapping("")
  
  public HttpResult<List<MerchantApplication>> getMerchantApplications() {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();
    if (!AuthorityUtils.hasAuthority(me, "ADMIN"))
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    List<MerchantApplication> apps = merchantApplicationService.getAllMerchantApplications();
    compatibilityEnricher.enrichMerchantApplications(apps);
    return HttpResult.success(apps);
  }

  @GetMapping("/{id}")
  
  public HttpResult<MerchantApplication> getMerchantApplication(
       @PathVariable Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");
    User me = meOptional.get();

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");

    MerchantApplication merchantApplication =
        merchantApplicationService.getMerchantApplicationById(id);
    if (merchantApplication == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "MerchantApplication NOT FOUND");

    if (isAdmin || (isBusiness && me.getId().equals(merchantApplication.getApplicantId())))
      return HttpResult.success(merchantApplication);

    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @PatchMapping("/{id}")
  
  public HttpResult<MerchantApplication> updateMerchantApplication(
       @PathVariable Long id,
       @RequestBody
          MerchantApplication newMerchantApplication) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();
    if (!AuthorityUtils.hasAuthority(me, "ADMIN"))
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");

    MerchantApplication merchantApplication =
        merchantApplicationService.getMerchantApplicationById(id);
    if (merchantApplication == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "MerchantApplication NOT FOUND");
    if (!merchantApplication.getApplicationState().equals(1))
      return HttpResult.failure(
          ResultCodeEnum.SERVER_ERROR,
          "ALREADY DISPOSED: currentState=" + merchantApplication.getApplicationState());

    if (newMerchantApplication.getApplicationState() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ApplicationState CANT BE NULL");
    merchantApplication.setApplicationState(newMerchantApplication.getApplicationState());
    EntityUtils.updateEntity(merchantApplication);
    merchantApplicationService.updateMerchantApplication(merchantApplication);

  log.info(
    "MerchantApplication audited: id={}, applicantId={}, handlerId={}, oldState=1, newState={}",
    merchantApplication.getId(),
    merchantApplication.getApplicantId(),
    merchantApplication.getHandlerId(),
    merchantApplication.getApplicationState());

    if (merchantApplication.getApplicationState().equals(2)) {
      Long applicantId = merchantApplication.getApplicantId();
      if (applicantId == null)
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ApplicantId NOT FOUND");
      User applicant = new User();
      applicant.setId(applicantId);
      applicant.setAuthorities(AuthorityUtils.getAuthoritySet("USER BUSINESS"));
      boolean ok = userService.updateUser(applicant);
      if (!ok) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "UPDATE USER AUTHORITIES FAILED");
      }
    }
    compatibilityEnricher.enrichMerchantApplication(merchantApplication);
    return HttpResult.success(merchantApplication);
  }

  @GetMapping("/my")
  
  public HttpResult<List<MerchantApplication>> getMyMerchantApplication() {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");
    List<MerchantApplication> apps = merchantApplicationService.getMyMerchantApplications(meOptional.get().getId());
    compatibilityEnricher.enrichMerchantApplications(apps);
    return HttpResult.success(apps);
  }
}
