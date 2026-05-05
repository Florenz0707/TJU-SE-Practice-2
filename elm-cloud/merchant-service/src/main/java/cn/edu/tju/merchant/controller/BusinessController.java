package cn.edu.tju.merchant.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import cn.edu.tju.merchant.service.UserService;
import cn.edu.tju.merchant.service.BusinessApplicationService;

import cn.edu.tju.merchant.util.AuthorityUtils;

import cn.edu.tju.merchant.model.User;
import cn.edu.tju.merchant.model.BusinessApplication;


import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.merchant.model.Business;
import cn.edu.tju.merchant.service.BusinessService;

import cn.edu.tju.merchant.util.EntityUtils;
import cn.edu.tju.merchant.util.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;

@RefreshScope
@RestController
@RequestMapping("/api/businesses")

public class BusinessController {
  private final UserService userService;
  private final BusinessService businessService;
  private final BusinessApplicationService businessApplicationService;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public BusinessController(
      UserService userService,
      BusinessService businessService,
      BusinessApplicationService businessApplicationService,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.userService = userService;
    this.businessService = businessService;
    this.businessApplicationService = businessApplicationService;
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
    
    // 兼容旧格式：从 businessOwner 对象中提取 id
    if (business.getBusinessOwnerId() == null && business.getBusinessOwner() != null) {
      business.setBusinessOwnerId(business.getBusinessOwner().getId());
    }
    
    if (business.getBusinessOwnerId() == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessOwner.Id CANT BE NULL");
    
    Long finalOwnerId = business.getBusinessOwnerId();

    Optional<User> ownerOptional = userService.getUserById(business.getBusinessOwnerId());
    User owner = ownerOptional.orElse(null);
    if (owner == null) {
      // 如果无法从 user-service 获取用户，创建一个 fallback 用户
      owner = new User();
      owner.setId(finalOwnerId);
      owner.setUsername("user" + finalOwnerId);
    }

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");

    if (isAdmin || (isBusiness && me.equals(owner))) {
      // 检查同一商家是否已经有相同名称的店铺
      if (businessService.hasBusinessWithSameName(finalOwnerId, business.getBusinessName())) {
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR,
            "ALREADY HAS BUSINESS WITH SAME NAME");
      }
      
      business.setBusinessOwnerId(finalOwnerId);
      EntityUtils.setNewEntity(business);
      businessService.addBusiness(business);
      compatibilityEnricher.enrichBusiness(business);

      // 如果是 admin 直接创建店铺，自动创建一个已批准的 BusinessApplication
      if (isAdmin) {
        try {
          BusinessApplication application = new BusinessApplication();
          EntityUtils.setNewEntity(application);
          application.setBusiness(business);
          application.setApplicationState(2); // 2 = 已批准
          application.setApplicantId(finalOwnerId);
          application.setHandlerId(me.getId());
          businessApplicationService.addApplication(application);
        } catch (Exception e) {
          // 即使创建 application 失败，也不要阻止店铺的创建
        }
      }

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
      // 检查更新后的店铺名称是否与该商家的其他店铺重复（排除当前正在更新的店铺）
      if (!oldBusiness.getBusinessName().equals(business.getBusinessName()) && 
          businessService.hasBusinessWithSameName(oldOwnerId, business.getBusinessName())) {
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR,
            "ALREADY HAS BUSINESS WITH SAME NAME");
      }
      
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
      // 如果要更新店铺名称，检查是否与该商家的其他店铺重复（排除当前正在更新的店铺）
      if (business.getBusinessName() != null && 
          !oldBusiness.getBusinessName().equals(business.getBusinessName()) && 
          businessService.hasBusinessWithSameName(oldOwnerId, business.getBusinessName())) {
        return HttpResult.failure(
            ResultCodeEnum.SERVER_ERROR,
            "ALREADY HAS BUSINESS WITH SAME NAME");
      }
      
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

    if (AuthorityUtils.hasAuthority(me, "BUSINESS") || AuthorityUtils.hasAuthority(me, "ADMIN")) {
      List<Business> businesses = businessService.getBusinessByOwnerId(me.getId());
      compatibilityEnricher.enrichBusinesses(businesses);
      return HttpResult.success(businesses);
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }
}
