package cn.edu.tju.merchant.service.impl;

import cn.edu.tju.merchant.model.Merchant;
import cn.edu.tju.merchant.repository.MerchantRepository;
import cn.edu.tju.merchant.service.MerchantInternalService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MerchantInternalServiceImpl implements MerchantInternalService {

    private final MerchantRepository merchantRepository;

    public MerchantInternalServiceImpl(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Override
    public Optional<Merchant> findById(Long id) {
        return merchantRepository.findById(id);
    }

    @Override
    public List<Merchant> findAll() {
        return merchantRepository.findAll();
    }
}
