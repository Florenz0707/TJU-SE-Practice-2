package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.EntityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/businesses")
@Tag(name = "管理店铺", description = "提供对店铺的增删改查功能")
public class BusinessController {
    private final UserService userService;
    private final BusinessService businessService;

    public BusinessController(UserService userService, BusinessService businessService) {
        this.userService = userService;
        this.businessService = businessService;
    }

    @GetMapping("/{id}")
    public HttpResult<Business> getBusiness(@PathVariable("id") Long id) {
        Business business = businessService.getBusinessById(id);
        if (business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
        return HttpResult.success(business);
    }


    @GetMapping("")
    public HttpResult<List<Business>> getBusinesses() {
        return HttpResult.success(businessService.getBusinesses());
    }

    @PostMapping("")
    public HttpResult<Business> addBusiness(@RequestBody Business business) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (business == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business CANT BE NULL");

        if (business.getBusinessName() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessName CANT BE NULL");
        if (business.getBusinessOwner() == null || business.getBusinessOwner().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessOwner.Id CANT BE NULL");

        User owner = userService.getUserById(business.getBusinessOwner().getId());
        if (owner == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessOwner NOT FOUND");

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");

        if (isAdmin || (isBusiness && me.equals(owner))) {
            business.setBusinessOwner(owner);
            EntityUtils.setNewEntity(business, me);
            businessService.addBusiness(business);

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
        User oldOwner = oldBusiness.getBusinessOwner();

        if (business == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business CANT BE NULL");
        if (business.getBusinessName() == null)
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "BusinessName CANT BE NULL");

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
        if (isAdmin || (isBusiness && me.equals(oldOwner))) {
            business.setId(null);
            EntityUtils.substituteEntity(oldBusiness, business, me);
            business.setBusinessOwner(oldOwner);
            businessService.updateBusiness(oldBusiness);
            businessService.updateBusiness(business);

            return HttpResult.success(business);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @PatchMapping("/{id}")
    public HttpResult<Business> patchBusiness(
            @PathVariable("id") Long id,
            @RequestBody Business business) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Business oldBusiness = businessService.getBusinessById(id);
        if (oldBusiness == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
        User oldOwner = oldBusiness.getBusinessOwner();

        if (business == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business CANT BE NULL");

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
        if (isAdmin || (isBusiness && me.equals(oldOwner))) {
            business.setBusinessOwner(oldOwner);
            if (business.getBusinessName() == null)
                business.setBusinessName(oldBusiness.getBusinessName());
            if (business.getBusinessAddress() == null)
                business.setBusinessAddress(oldBusiness.getBusinessAddress());
            if (business.getBusinessExplain() == null)
                business.setBusinessExplain(oldBusiness.getBusinessExplain());
            if (business.getBusinessImg() == null)
                business.setBusinessImg(oldBusiness.getBusinessImg());
            if (business.getRemarks() == null)
                business.setRemarks(oldBusiness.getRemarks());
            if (business.getOrderTypeId() == null)
                business.setOrderTypeId(oldBusiness.getOrderTypeId());
            if (business.getStartPrice() == null)
                business.setStartPrice(oldBusiness.getStartPrice());
            if (business.getDeliveryPrice() == null)
                business.setDeliveryPrice(oldBusiness.getDeliveryPrice());

            business.setId(null);
            EntityUtils.substituteEntity(oldBusiness, business, me);
            businessService.updateBusiness(oldBusiness);
            businessService.updateBusiness(business);
            return HttpResult.success(business);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @DeleteMapping("/{id}")
    public HttpResult<String> deleteBusiness(@PathVariable("id") Long id) {
        Business business = businessService.getBusinessById(id);
        if (business == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");

        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        boolean isBusiness = AuthorityUtils.hasAuthority(me, "BUSINESS");
        if (isAdmin || (isBusiness && business.getBusinessOwner().equals(me))) {
            EntityUtils.deleteEntity(business, me);
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
            return HttpResult.success(businessService.getBusinessByOwner(me));
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
}
