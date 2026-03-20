package cn.edu.tju.elm.service;

import cn.edu.tju.elm.constant.OrderState;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.repository.OrderRepository;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

  private final OrderRepository orderRepository;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public OrderService(
      OrderRepository orderRepository, ResponseCompatibilityEnricher compatibilityEnricher) {
    this.orderRepository = orderRepository;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  public void addOrder(Order order) {
    orderRepository.save(order);
  }

  public Order getOrderById(Long id) {
    Optional<Order> orderOptional = orderRepository.findById(id);
    Order order = orderOptional.map(EntityUtils::filterEntity).orElse(null);
    compatibilityEnricher.enrichOrder(order);
    return order;
  }

  public List<Order> getOrdersByCustomerId(Long id) {
    List<Order> orders =
        EntityUtils.filterEntityList(orderRepository.findAllByCustomerIdWithDetails(id));
    compatibilityEnricher.enrichOrders(orders);
    return orders;
  }

  public Page<Order> getOrdersByCustomerId(Long id, Pageable pageable) {
    Page<Order> page =
        orderRepository.findAllByCustomerId(id, pageable).map(EntityUtils::filterEntity);
    compatibilityEnricher.enrichOrders(page.getContent());
    return page;
  }

  public void updateOrder(Order order) {
    orderRepository.save(order);
  }

  public List<Order> getOrdersByBusinessId(Long businessId) {
    List<Order> orders =
        EntityUtils.filterEntityList(orderRepository.findAllByBusinessIdWithDetails(businessId));
    compatibilityEnricher.enrichOrders(orders);
    return orders;
  }

  public Page<Order> getOrdersByBusinessId(Long businessId, Pageable pageable) {
    Page<Order> page =
        orderRepository.findAllByBusinessId(businessId, pageable).map(EntityUtils::filterEntity);
    compatibilityEnricher.enrichOrders(page.getContent());
    return page;
  }

  public Order getOrderByRequestId(String requestId) {
    Order order = orderRepository.findByRequestId(requestId);
    Order filtered = order != null ? EntityUtils.filterEntity(order) : null;
    compatibilityEnricher.enrichOrder(filtered);
    return filtered;
  }

  public boolean isValidStateTransition(Integer from, Integer to) {
    if (from.equals(OrderState.CANCELED) || from.equals(OrderState.COMMENTED)) {
      return false;
    }
    if (to.equals(OrderState.CANCELED)) {
      return from.equals(OrderState.PAID);
    }
    return to > from;
  }
}
