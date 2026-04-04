package cn.edu.tju.merchant.controller;
import cn.edu.tju.merchant.service.UserService;

import cn.edu.tju.merchant.util.AuthorityUtils;

import cn.edu.tju.merchant.model.User;

import cn.edu.tju.merchant.model.ApplicationState;


import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;

import cn.edu.tju.merchant.util.JwtUtils;
import cn.edu.tju.merchant.model.MerchantApplication;
import cn.edu.tju.merchant.service.MerchantApplicationService;

import cn.edu.tju.merchant.util.EntityUtils;
import cn.edu.tju.merchant.util.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications/merchant")

public class MerchantApplicationController {
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
    merchantApplication.setHandlerId(admin.getId());
    merchantApplicationService.addApplication(merchantApplication);
    compatibilityEnricher.enrichMerchantApplication(merchantApplication);
    return HttpResult.success(merchantApplication);
  }

  @GetMapping("")
  
  public HttpResult<List<MerchantApplication>> getMerchantApplications() {
    return HttpResult.success(merchantApplicationService.getAllMerchantApplications());
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

    MerchantApplication merchantApplication =
        merchantApplicationService.getMerchantApplicationById(id);
    if (merchantApplication == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "MerchantApplication NOT FOUND");
    if (!merchantApplication.getApplicationState().equals(1))
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "ALREADY DISPOSED");

    if (newMerchantApplication.getApplicationState() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ApplicationState CANT BE NULL");
    merchantApplication.setApplicationState(newMerchantApplication.getApplicationState());
    EntityUtils.updateEntity(merchantApplication);
    merchantApplicationService.updateMerchantApplication(merchantApplication);

    if (merchantApplication.getApplicationState().equals(2)) {
      User applicant = userService.getUserById(merchantApplication.getApplicantId()).orElse(null);
      if (applicant == null)
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Applicant NOT FOUND");
      applicant.setAuthorities(AuthorityUtils.getAuthoritySet("USER BUSINESS"));
      EntityUtils.updateEntity(applicant);
      userService.updateUser(applicant);
    }
    compatibilityEnricher.enrichMerchantApplication(merchantApplication);
    return HttpResult.success(merchantApplication);
  }

  @GetMapping("/my")
  
  public HttpResult<List<MerchantApplication>> getMyMerchantApplication() {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "USER NOT FOUND");
    User me = meOptional.get();

    return HttpResult.success(merchantApplicationService.getMyMerchantApplications(me.getId()));
  }
}
