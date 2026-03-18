package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.Order;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
  List<Order> findAllByCustomerId(Long id);

  List<Order> findAllByBusinessId(Long businessId);

  Order findByRequestId(String requestId);

  @Query(
      "SELECT o FROM Order o "
          + "LEFT JOIN FETCH o.business "
          + "LEFT JOIN FETCH o.customer "
          + "LEFT JOIN FETCH o.deliveryAddress "
          + "WHERE o.customer.id = :customerId")
  List<Order> findAllByCustomerIdWithDetails(@Param("customerId") Long customerId);

  @Query(
      "SELECT o FROM Order o "
          + "LEFT JOIN FETCH o.business "
          + "LEFT JOIN FETCH o.customer "
          + "LEFT JOIN FETCH o.deliveryAddress "
          + "WHERE o.business.id = :businessId")
  List<Order> findAllByBusinessIdWithDetails(@Param("businessId") Long businessId);

  @Query(
      value = "SELECT o FROM Order o WHERE o.customer.id = :customerId",
      countQuery = "SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId")
  Page<Order> findAllByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

  @Query(
      value = "SELECT o FROM Order o WHERE o.business.id = :businessId",
      countQuery = "SELECT COUNT(o) FROM Order o WHERE o.business.id = :businessId")
  Page<Order> findAllByBusinessId(@Param("businessId") Long businessId, Pageable pageable);
}
