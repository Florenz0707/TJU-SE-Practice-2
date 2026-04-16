package cn.edu.tju.merchant.controller;
import cn.edu.tju.merchant.service.UserService;

import cn.edu.tju.merchant.util.AuthorityUtils;

import cn.edu.tju.merchant.model.User;


import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
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
    List<Business> businesses = businessService.getOpenBusinesses();
    compatibilityEnricher.enrichBusinesses(businesses);
    return HttpResult.success(businesses);
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
      // Hard guard: each merchant(user) can only have ONE active store.
      // This prevents accidental duplicates when frontend mistakenly calls POST instead of PUT.
      try {
        List<Business> existing = businessService.getBusinessByOwnerId(owner.getId());
        if (existing != null) {
          for (Business b : existing) {
            if (b != null && (b.getDeleted() == null || !b.getDeleted())) {
              return HttpResult.failure(
                  ResultCodeEnum.SERVER_ERROR,
                  "ALREADY HAS ACTIVE STORE (use update endpoint instead)");
            }
          }
        }
      } catch (Exception ignore) {
        // best-effort guard
      }

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
      // IMPORTANT: Update should NOT create a new row.
      // Keep the same id and only mutate fields on the existing entity.
      EntityUtils.updateEntity(oldBusiness);
      oldBusiness.setBusinessName(business.getBusinessName());
      oldBusiness.setBusinessAddress(business.getBusinessAddress());
      oldBusiness.setBusinessExplain(business.getBusinessExplain());
      oldBusiness.setBusinessImg(business.getBusinessImg());
      oldBusiness.setRemarks(business.getRemarks());
      oldBusiness.setOrderTypeId(business.getOrderTypeId());
      oldBusiness.setStartPrice(business.getStartPrice());
      oldBusiness.setDeliveryPrice(business.getDeliveryPrice());
      oldBusiness.setBusinessOwnerId(oldOwnerId);

      businessService.updateBusiness(oldBusiness);
      compatibilityEnricher.enrichBusiness(oldBusiness);
      return HttpResult.success(oldBusiness);
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
      // Patch should also update the same row, not clone/insert.
      EntityUtils.updateEntity(oldBusiness);
      if (business.getBusinessName() != null) oldBusiness.setBusinessName(business.getBusinessName());
      if (business.getBusinessAddress() != null) oldBusiness.setBusinessAddress(business.getBusinessAddress());
      if (business.getBusinessExplain() != null) oldBusiness.setBusinessExplain(business.getBusinessExplain());
      if (business.getBusinessImg() != null) oldBusiness.setBusinessImg(business.getBusinessImg());
      if (business.getRemarks() != null) oldBusiness.setRemarks(business.getRemarks());
      if (business.getOrderTypeId() != null) oldBusiness.setOrderTypeId(business.getOrderTypeId());
      if (business.getStartPrice() != null) oldBusiness.setStartPrice(business.getStartPrice());
      if (business.getDeliveryPrice() != null) oldBusiness.setDeliveryPrice(business.getDeliveryPrice());
      oldBusiness.setBusinessOwnerId(oldOwnerId);

      businessService.updateBusiness(oldBusiness);
      compatibilityEnricher.enrichBusiness(oldBusiness);
      return HttpResult.success(oldBusiness);
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

    if (AuthorityUtils.hasAuthority(me, "BUSINESS")) {
      List<Business> businesses = businessService.getBusinessByOwnerId(me.getId());
      compatibilityEnricher.enrichBusinesses(businesses);
      return HttpResult.success(businesses);
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }
}
