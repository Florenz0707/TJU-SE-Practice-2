package cn.edu.tju.order.repository;

import cn.edu.tju.order.model.bo.DeliveryAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {
  Optional<DeliveryAddress> findByIdAndDeletedFalse(Long id);

  List<DeliveryAddress> findAllByCustomerIdAndDeletedFalse(Long customerId);
}
