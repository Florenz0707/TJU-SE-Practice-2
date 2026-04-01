package cn.edu.tju.wallet.controller;

import cn.edu.tju.wallet.model.Wallet;
import cn.edu.tju.wallet.service.WalletInternalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/wallets")
public class WalletInnerController {

    private final WalletInternalService walletInternalService;

    public WalletInnerController(WalletInternalService walletInternalService) {
        this.walletInternalService = walletInternalService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Wallet> byUser(@PathVariable Long userId) {
        return walletInternalService.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
