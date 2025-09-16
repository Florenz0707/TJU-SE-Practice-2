package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<DeliveryAddress, Integer> {
}
