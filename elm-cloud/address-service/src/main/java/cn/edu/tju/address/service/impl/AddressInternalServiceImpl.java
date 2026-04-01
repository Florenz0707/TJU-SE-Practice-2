package cn.edu.tju.address.service.impl;

import cn.edu.tju.address.model.DeliveryAddress;
import cn.edu.tju.address.repository.DeliveryAddressRepository;
import cn.edu.tju.address.service.AddressInternalService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressInternalServiceImpl implements AddressInternalService {

    private final DeliveryAddressRepository repository;

    public AddressInternalServiceImpl(DeliveryAddressRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<DeliveryAddress> findByUserId(Long userId) {
        return repository.findByCustomerId(userId);
    }
}
