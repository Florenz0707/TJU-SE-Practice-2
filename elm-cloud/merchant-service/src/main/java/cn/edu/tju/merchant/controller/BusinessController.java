package cn.edu.tju.merchant.controller;
import cn.edu.tju.merchant.service.UserService;

import cn.edu.tju.merchant.util.AuthorityUtils;

import cn.edu.tju.merchant.model.User;


import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;

import cn.edu.tju.merchant.util.JwtUtils;
import cn.edu.tju.merchant.model.Business;
import cn.edu.tju.merchant.service.BusinessService;

import cn.edu.tju.merchant.util.EntityUtils;
import cn.edu.tju.merchant.util.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/businesses")

public class BusinessController {
  private final UserService userService;
  private final BusinessService businessService;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public BusinessController(
      UserService userService,
      BusinessService businessService,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.userService = userService;
    this.businessService = businessService;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  @GetMapping("/{id}")
  
  public HttpResult<Business> getBusiness(
       @PathVariable("id") Long id) {
    Business business = businessService.getBusinessById(id);
    if (business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
    return HttpResult.success(business);
  }

  @GetMapping("")
  
  public HttpResult<List<Business>> getBusinesses() {
    return HttpResult.success(businessService.getBusinesses());
  }

  @PostMapping("")
  
  public HttpResult<Business> addBusiness(
       @RequestBody Business business) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (business == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business CANT BE NULL");

    if (business.getBusinessName() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessName CANT BE NULL");
    if (business.getBusinessOwnerId() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessOwner.Id CANT BE NULL");

    User owner = userService.getUserById(business.getBusinessOwnerId()).orElse(null);
    if (owner == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessOwner NOT FOUND");

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");

    if (isAdmin || (isBusiness && me.equals(owner))) {
      business.setBusinessOwnerId(owner.getId());
      EntityUtils.setNewEntity(business);
      businessService.addBusiness(business);
      compatibilityEnricher.enrichBusiness(business);

      return HttpResult.success(business);
    }
    return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "AUTHORITY LACKED");
  }

  @PutMapping("/{id}")
  
  public HttpResult<Business> updateBusiness(
       @PathVariable("id") Long id,
       @RequestBody Business business) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    Business oldBusiness = businessService.getBusinessById(id);
    if (oldBusiness == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
    Long oldOwnerId = oldBusiness.getBusinessOwnerId();

    if (business == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business CANT BE NULL");
    if (business.getBusinessName() == null)
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "BusinessName CANT BE NULL");

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
    if (isAdmin || (isBusiness && me.getId().equals(oldOwnerId))) {
      business.setId(null);
      EntityUtils.substituteEntity(oldBusiness, business);
      business.setBusinessOwnerId(oldOwnerId);
      businessService.updateBusiness(oldBusiness);
      businessService.updateBusiness(business);
      compatibilityEnricher.enrichBusiness(business);

      return HttpResult.success(business);
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @PatchMapping("/{id}")
  
  public HttpResult<Business> patchBusiness(
       @PathVariable("id") Long id,
       @RequestBody Business business) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    Business oldBusiness = businessService.getBusinessById(id);
    if (oldBusiness == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
    Long oldOwnerId = oldBusiness.getBusinessOwnerId();

    if (business == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business CANT BE NULL");

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
    if (isAdmin || (isBusiness && me.getId().equals(oldOwnerId))) {
      business.setBusinessOwnerId(oldOwnerId);
      if (business.getBusinessName() == null)
        business.setBusinessName(oldBusiness.getBusinessName());
      if (business.getBusinessAddress() == null)
        business.setBusinessAddress(oldBusiness.getBusinessAddress());
      if (business.getBusinessExplain() == null)
        business.setBusinessExplain(oldBusiness.getBusinessExplain());
      if (business.getBusinessImg() == null) business.setBusinessImg(oldBusiness.getBusinessImg());
      if (business.getRemarks() == null) business.setRemarks(oldBusiness.getRemarks());
      if (business.getOrderTypeId() == null) business.setOrderTypeId(oldBusiness.getOrderTypeId());
      if (business.getStartPrice() == null) business.setStartPrice(oldBusiness.getStartPrice());
      if (business.getDeliveryPrice() == null)
        business.setDeliveryPrice(oldBusiness.getDeliveryPrice());

      business.setId(null);
      EntityUtils.substituteEntity(oldBusiness, business);
      businessService.updateBusiness(oldBusiness);
      businessService.updateBusiness(business);
      compatibilityEnricher.enrichBusiness(business);
      return HttpResult.success(business);
    }

    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @DeleteMapping("/{id}")
  
  public HttpResult<String> deleteBusiness(
       @PathVariable("id") Long id) {
    Business business = businessService.getBusinessById(id);
    if (business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");

    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
    if (isAdmin || (isBusiness && business.getBusinessOwnerId().equals(me.getId()))) {
      EntityUtils.deleteEntity(business);
      businessService.updateBusiness(business);
      return HttpResult.success("Delete business successfully.");
    }

    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @GetMapping("/my")
  
  public HttpResult<List<Business>> getMyBusinesses() {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (AuthorityUtils.hasAuthority(me, "BUSINESS"))
      return HttpResult.success(businessService.getBusinessByOwnerId(me.getId()));
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }
}
