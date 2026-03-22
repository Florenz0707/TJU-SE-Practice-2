package cn.edu.tju.order.service;

import cn.edu.tju.order.model.bo.DeliveryAddress;
import cn.edu.tju.order.model.vo.DeliveryAddressSnapshotVO;
import cn.edu.tju.order.repository.DeliveryAddressRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressInternalService {
  private final DeliveryAddressRepository deliveryAddressRepository;

  public AddressInternalService(DeliveryAddressRepository deliveryAddressRepository) {
    this.deliveryAddressRepository = deliveryAddressRepository;
  }

  @Transactional
  public DeliveryAddressSnapshotVO createAddress(CreateAddressCommand command) {
    if (command == null || command.customerId() == null) {
      throw new IllegalArgumentException("customerId CANT BE NULL");
    }
    DeliveryAddress address = new DeliveryAddress();
    LocalDateTime now = LocalDateTime.now();
    address.setCreateTime(now);
    address.setUpdateTime(now);
    address.setDeleted(false);
    address.setCustomerId(command.customerId());
    address.setContactName(command.contactName());
    address.setContactSex(command.contactSex());
    address.setContactTel(command.contactTel());
    address.setAddress(command.address());
    DeliveryAddress saved = deliveryAddressRepository.save(address);
    return new DeliveryAddressSnapshotVO(saved);
  }

  @Transactional(readOnly = true)
  public DeliveryAddressSnapshotVO getAddressById(Long addressId) {
    if (addressId == null) {
      return null;
    }
    return deliveryAddressRepository
        .findByIdAndDeletedFalse(addressId)
        .map(DeliveryAddressSnapshotVO::new)
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public List<DeliveryAddressSnapshotVO> getAddressesByCustomerId(Long customerId) {
    if (customerId == null) {
      return List.of();
    }
    return deliveryAddressRepository.findAllByCustomerIdAndDeletedFalse(customerId).stream()
        .map(DeliveryAddressSnapshotVO::new)
        .toList();
  }

  @Transactional
  public DeliveryAddressSnapshotVO updateAddress(Long addressId, UpdateAddressCommand command) {
    if (addressId == null || command == null) {
      throw new IllegalArgumentException("addressId/command CANT BE NULL");
    }
    DeliveryAddress address =
        deliveryAddressRepository
            .findByIdAndDeletedFalse(addressId)
            .orElseThrow(() -> new IllegalArgumentException("Address NOT FOUND"));
    if (command.customerId() != null) {
      address.setCustomerId(command.customerId());
    }
    address.setContactName(command.contactName());
    address.setContactSex(command.contactSex());
    address.setContactTel(command.contactTel());
    address.setAddress(command.address());
    address.setUpdateTime(LocalDateTime.now());
    DeliveryAddress saved = deliveryAddressRepository.save(address);
    return new DeliveryAddressSnapshotVO(saved);
  }

  @Transactional
  public boolean deleteAddress(Long addressId) {
    if (addressId == null) {
      return false;
    }
    DeliveryAddress address =
        deliveryAddressRepository
            .findByIdAndDeletedFalse(addressId)
            .orElseThrow(() -> new IllegalArgumentException("Address NOT FOUND"));
    address.setDeleted(true);
    address.setUpdateTime(LocalDateTime.now());
    deliveryAddressRepository.save(address);
    return true;
  }

  public record CreateAddressCommand(
      Long customerId, String contactName, Integer contactSex, String contactTel, String address) {}

  public record UpdateAddressCommand(
      Long customerId, String contactName, Integer contactSex, String contactTel, String address) {}
}
