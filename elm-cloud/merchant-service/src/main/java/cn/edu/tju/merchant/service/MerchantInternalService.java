package cn.edu.tju.merchant.service;

import cn.edu.tju.merchant.model.Merchant;

import java.util.List;
import java.util.Optional;

public interface MerchantInternalService {
    Optional<Merchant> findById(Long id);
    List<Merchant> findAll();
}
