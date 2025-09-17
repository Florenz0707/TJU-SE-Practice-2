package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.DeliveryAddress;
import cn.edu.tju.elm.repository.AddressRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Transactional
public class AddressService {
    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public DeliveryAddress addAddress(DeliveryAddress address) {
        return addressRepository.save(address);
    }

    public DeliveryAddress getAddressById(Long id) {
        Optional<DeliveryAddress> addressOptional = addressRepository.findById(id);
        return addressOptional.orElse(null);
    }
}
