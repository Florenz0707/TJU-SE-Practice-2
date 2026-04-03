package cn.edu.tju.cart.repository;

import cn.edu.tju.cart.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, String> {
    List<Cart> findByUserId(String userId);
    Cart findByUserIdAndBusinessIdAndFoodId(String userId, String businessId, String foodId);
    List<Cart> findByUserIdAndBusinessId(String userId, String businessId);
}
