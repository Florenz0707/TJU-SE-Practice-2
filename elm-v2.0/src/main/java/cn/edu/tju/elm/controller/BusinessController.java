package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.repository.UserRepository;
import cn.edu.tju.elm.model.Business;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.core.security.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/businesses")
@Tag(name = "管理店铺", description = "提供对店铺的增删改查功能")
public class BusinessController {
    @Autowired
    private UserService userService;

    @Autowired
    BusinessService businessService;

    @Autowired
    private UserRepository userRepository;

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
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<Business> addBusiness(@RequestBody Business business) {
        Optional<User> adminOptional = userService.getUserWithAuthorities();
        if (adminOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Admin NOT FOUND");
        User admin = adminOptional.get();

        if (business.getBusinessName() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessName CANT BE NULL");
        if (business.getBusinessOwner() == null || business.getBusinessOwner().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessOwner.Id CANT BE NULL");

        Optional<User> ownerOptional = userRepository.findById(business.getBusinessOwner().getId());
        if (ownerOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessOwner NOT FOUND");
        User owner = ownerOptional.get();

        LocalDateTime now = LocalDateTime.now();
        business.setCreateTime(now);
        business.setUpdateTime(now);
        business.setCreator(admin.getId());
        business.setUpdater(admin.getId());
        business.setDeleted(false);
        business.setBusinessOwner(owner);
        if (business.equals(businessService.addBusiness(business))) {
            return HttpResult.success(business);
        }

        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Not Known Error");
    }

    @PutMapping("/{id}")
    public HttpResult<Business> updateBusiness(@PathVariable("id") Long id, @RequestBody Business business) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Business oldBusiness = businessService.getBusinessById(id);
        if (oldBusiness == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
        User oldOwner = oldBusiness.getBusinessOwner();

        if (business.getBusinessName() == null)
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "BusinessName CANT BE NULL");
        if (business.getBusinessOwner() == null || business.getBusinessOwner().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessOwner.Id CANT BE NULL");
        User newOwner = userService.getUserById(business.getBusinessOwner().getId());
        if (newOwner == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessOwner NOT FOUND");

        boolean isAdmin = false;
        boolean isBusiness = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) isAdmin = true;
            if (authority.getName().equals("BUSINESS")) isBusiness = true;
        }
        if (isAdmin || (isBusiness && me.equals(oldOwner) && oldOwner.equals(newOwner))) {
            business.setBusinessOwner(newOwner);
            LocalDateTime now = LocalDateTime.now();
            business.setId(oldBusiness.getId());
            business.setCreateTime(oldBusiness.getCreateTime());
            business.setUpdateTime(now);
            business.setCreator(oldBusiness.getCreator());
            business.setUpdater(me.getId());
            business.setDeleted(false);
            businessService.updateBusiness(business);
            return HttpResult.success(business);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @PatchMapping("/{id}")
    public HttpResult<Business> patchBusiness(@PathVariable("id") Long id, @RequestBody Business business) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Business oldBusiness = businessService.getBusinessById(id);
        if (oldBusiness == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
        User oldOwner = oldBusiness.getBusinessOwner();

        boolean isAdmin = false;
        boolean isBusiness = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) isAdmin = true;
            if (authority.getName().equals("BUSINESS")) isBusiness = true;
        }
        if (isAdmin || (isBusiness && me.equals(oldOwner))) {
            business.setId(oldBusiness.getId());

            if (business.getBusinessOwner() == null) business.setBusinessOwner(oldOwner);
            else if (isBusiness) {
                if (business.getBusinessOwner().getId() == null)
                    return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "IF BusinessOwner NOT NULL THEN BusinessOwner.Id MUST BE NOT NULL");
                if (!oldOwner.getId().equals(business.getBusinessOwner().getId()))
                    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
            }
            if (business.getBusinessAddress() == null) business.setBusinessAddress(oldBusiness.getBusinessAddress());
            if (business.getBusinessExplain() == null) business.setBusinessExplain(oldBusiness.getBusinessExplain());
            if (business.getBusinessImg() == null) business.setBusinessImg(oldBusiness.getBusinessImg());
            if (business.getRemarks() == null) business.setRemarks(oldBusiness.getRemarks());
            if (business.getOrderTypeId() == null) business.setOrderTypeId(oldBusiness.getOrderTypeId());
            if (business.getStartPrice() == null) business.setStartPrice(oldBusiness.getStartPrice());
            if (business.getDeliveryPrice() == null) business.setDeliveryPrice(oldBusiness.getDeliveryPrice());

            LocalDateTime now = LocalDateTime.now();
            business.setCreateTime(oldBusiness.getCreateTime());
            business.setUpdateTime(now);
            business.setCreator(oldBusiness.getCreator());
            business.setUpdater(me.getId());
            business.setDeleted(false);

            businessService.updateBusiness(business);
            return HttpResult.success(business);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @DeleteMapping("/{id}")
    public HttpResult<Business> deleteBusiness(@PathVariable("id") Long id) {
        Business business = businessService.getBusinessById(id);
        if (business == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");

        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        boolean isAdmin = false;
        boolean isBusiness = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) isAdmin = true;
            if (authority.getName().equals("BUSINESS")) isBusiness = true;
        }

        if (isAdmin || (isBusiness && business.getBusinessOwner().equals(me))) {
            LocalDateTime now = LocalDateTime.now();
            business.setUpdateTime(now);
            business.setUpdater(me.getId());
            business.setDeleted(true);
            businessService.updateBusiness(business);
            return HttpResult.success(business);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('BUSINESS')")
    public HttpResult<List<Business>> getMyBusinesses() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        return HttpResult.success(businessService.getBusinessByOwner(me));
    }
}
