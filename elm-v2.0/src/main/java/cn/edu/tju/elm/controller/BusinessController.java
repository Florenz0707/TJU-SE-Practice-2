package cn.edu.tju.elm.controller;

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

    @GetMapping("")
    public HttpResult<List<Business>> getBusinesses() {
        return null;
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<Business> addBusiness(@RequestBody Business business) {
        Optional<User> adminOptional = userService.getUserWithAuthorities();
        if (adminOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Admin NOT FOUND");
        User admin = adminOptional.get();

        Optional<User> ownerOptional = userRepository.findById(business.getBusinessOwner().getId());
        if (ownerOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "BusinessOwner NOT FOUND");
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

    @GetMapping("/{id}")
    public HttpResult<Business> getBusiness(@PathVariable("id") Long id) {
        Business business = businessService.getBusinessById(id);
        if (business == null || business.getDeleted()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");
        return HttpResult.success(business);
    }

    @PutMapping("/{id}")
    public HttpResult<Business> updateBusiness(@PathVariable("id") Long id, @RequestBody Business business) {
        return null;
    }

    @PatchMapping("/{id}")
    public HttpResult<Business> patchBusiness(@PathVariable("id") Long id, @RequestBody Business business) {
        return null;
    }

    @DeleteMapping("/{id}")
    public HttpResult<Business> deleteBusiness(@PathVariable("id") Long id) {
        return null;
    }
}
