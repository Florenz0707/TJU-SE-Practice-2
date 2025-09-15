package cn.edu.tju.elm.controller;

import cn.edu.tju.elm.model.Business;
import cn.edu.tju.core.model.HttpResult;
//import cn.edu.tju.elb.service.BusinessService;
import cn.edu.tju.core.security.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/businesses")
@Tag(name="管理店铺", description = "提供对店铺的增删改查功能")
public class BusinessController {
    @Autowired
    private UserService userService;

//    @Autowired
//    BusinessService businessService;

    @GetMapping("")
    public HttpResult<List<Business>> getBusinesses(){
        return null;
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<Business> addBusiness(@RequestBody Business business){
        return null;
    }

    @GetMapping("/{id}")
    public HttpResult<Business> getBusiness(@PathVariable("id") Long id){
        return null;
    }

    @PutMapping("/{id}")
    public HttpResult<Business> updateBusiness(@PathVariable("id") Long id, @RequestBody Business business){
        return null;
    }

    @PatchMapping("/{id}")
    public HttpResult<Business> patchBusiness(@PathVariable("id") Long id, @RequestBody Business business){
        return null;
    }

    @DeleteMapping("/{id}")
    public HttpResult<Business> deleteBusiness(@PathVariable("id") Long id){
        return null;
    }
}
