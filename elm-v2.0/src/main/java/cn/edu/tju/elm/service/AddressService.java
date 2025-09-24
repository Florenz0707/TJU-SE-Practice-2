package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.DeliveryAddress;
import cn.edu.tju.elm.repository.AddressRepository;
import cn.edu.tju.elm.utils.Utils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AddressService {
    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public void addAddress(DeliveryAddress address) {
        addressRepository.save(address);
    }

    public void updateAddress(DeliveryAddress address) {
        addressRepository.save(address);
    }

    public DeliveryAddress getAddressById(Long id) {
        Optional<DeliveryAddress> addressOptional = addressRepository.findById(id);
        return addressOptional.map(Utils::filterEntity).orElse(null);
    }

    public List<DeliveryAddress> getAddressesByCustomerId(Long customerId) {
        return Utils.filterEntityList(addressRepository.findByCustomerId(customerId));
    }
}
