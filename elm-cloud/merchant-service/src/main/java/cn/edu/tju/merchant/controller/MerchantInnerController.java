package cn.edu.tju.merchant.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import cn.edu.tju.merchant.service.UserService;

import cn.edu.tju.merchant.util.AuthorityUtils;

import cn.edu.tju.merchant.model.User;


import cn.edu.tju.merchant.model.Merchant;
import cn.edu.tju.merchant.service.MerchantInternalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RefreshScope
@RestController
@RequestMapping("/internal/merchants")
public class MerchantInnerController {

    private final MerchantInternalService merchantInternalService;

    public MerchantInnerController(MerchantInternalService merchantInternalService) {
        this.merchantInternalService = merchantInternalService;
    }

    @GetMapping
    public ResponseEntity<List<Merchant>> all() {
        return ResponseEntity.ok(merchantInternalService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Merchant> get(@PathVariable Long id) {
        return merchantInternalService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
