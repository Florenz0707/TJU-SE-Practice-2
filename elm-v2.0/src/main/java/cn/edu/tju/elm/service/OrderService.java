package cn.edu.tju.elm.service;

import cn.edu.tju.elm.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import cn.edu.tju.elm.model.Order;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order addOrder(Order order) {
        return orderRepository.save(order);
    }
}
