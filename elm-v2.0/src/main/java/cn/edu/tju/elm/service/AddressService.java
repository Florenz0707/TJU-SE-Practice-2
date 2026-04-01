package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BO.DeliveryAddress;
import cn.edu.tju.elm.repository.AddressRepository;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddressService {
  private final AddressRepository addressRepository;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public AddressService(
      AddressRepository addressRepository, ResponseCompatibilityEnricher compatibilityEnricher) {
    this.addressRepository = addressRepository;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  public void addAddress(DeliveryAddress address) {
    addressRepository.save(address);
  }

  public void updateAddress(DeliveryAddress address) {
    addressRepository.save(address);
  }

  public DeliveryAddress getAddressById(Long id) {
    Optional<DeliveryAddress> addressOptional = addressRepository.findById(id);
    DeliveryAddress address = addressOptional.map(EntityUtils::filterEntity).orElse(null);
    compatibilityEnricher.enrichAddress(address);
    return address;
  }

  public List<DeliveryAddress> getAddressesByCustomerId(Long customerId) {
    List<DeliveryAddress> addresses =
        EntityUtils.filterEntityList(addressRepository.findByCustomerId(customerId));
    compatibilityEnricher.enrichAddresses(addresses);
    return addresses;
  }
}
