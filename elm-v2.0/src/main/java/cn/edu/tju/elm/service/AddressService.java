package cn.edu.tju.elm.service;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.model.DeliveryAddress;
import cn.edu.tju.elm.repository.AddressRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AddressService {
    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public HttpResult<DeliveryAddress> addAddress(DeliveryAddress address) {
        addressRepository.save(address);
        return HttpResult.success(address);
    }
}
