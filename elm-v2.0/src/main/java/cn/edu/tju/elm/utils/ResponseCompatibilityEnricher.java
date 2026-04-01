package cn.edu.tju.elm.utils;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.Business;
import cn.edu.tju.elm.model.BO.BusinessApplication;
import cn.edu.tju.elm.model.BO.Cart;
import cn.edu.tju.elm.model.BO.DeliveryAddress;
import cn.edu.tju.elm.model.BO.MerchantApplication;
import cn.edu.tju.elm.model.BO.Order;
import cn.edu.tju.elm.model.BO.Review;
import cn.edu.tju.elm.model.VO.UserSummaryView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ResponseCompatibilityEnricher {
  private final UserService userService;

  public ResponseCompatibilityEnricher(UserService userService) {
    this.userService = userService;
  }

  public void enrichBusiness(Business business) {
    if (business == null) return;
    business.setBusinessOwner(resolveUserSummary(business.getBusinessOwnerId(), new HashMap<>()));
  }

  public void enrichBusinesses(List<Business> businesses) {
    if (businesses == null || businesses.isEmpty()) return;
    Map<Long, UserSummaryView> cache = new HashMap<>();
    for (Business business : businesses) {
      if (business != null) {
        business.setBusinessOwner(resolveUserSummary(business.getBusinessOwnerId(), cache));
      }
    }
  }

  public void enrichAddress(DeliveryAddress address) {
    if (address == null) return;
    address.setCustomer(resolveUserSummary(address.getCustomerId(), new HashMap<>()));
  }

  public void enrichAddresses(List<DeliveryAddress> addresses) {
    if (addresses == null || addresses.isEmpty()) return;
    Map<Long, UserSummaryView> cache = new HashMap<>();
    for (DeliveryAddress address : addresses) {
      if (address != null) {
        address.setCustomer(resolveUserSummary(address.getCustomerId(), cache));
      }
    }
  }

  public void enrichCart(Cart cart) {
    if (cart == null) return;
    Map<Long, UserSummaryView> cache = new HashMap<>();
    cart.setCustomer(resolveUserSummary(cart.getCustomerId(), cache));
    enrichBusinessWithCache(cart.getBusiness(), cache);
  }

  public void enrichCarts(List<Cart> carts) {
    if (carts == null || carts.isEmpty()) return;
    Map<Long, UserSummaryView> cache = new HashMap<>();
    for (Cart cart : carts) {
      if (cart == null) continue;
      cart.setCustomer(resolveUserSummary(cart.getCustomerId(), cache));
      enrichBusinessWithCache(cart.getBusiness(), cache);
    }
  }

  public void enrichOrder(Order order) {
    if (order == null) return;
    Map<Long, UserSummaryView> cache = new HashMap<>();
    order.setCustomer(resolveUserSummary(order.getCustomerId(), cache));
    enrichBusinessWithCache(order.getBusiness(), cache);
    enrichAddressWithCache(order.getDeliveryAddress(), cache);
  }

  public void enrichOrders(List<Order> orders) {
    if (orders == null || orders.isEmpty()) return;
    Map<Long, UserSummaryView> cache = new HashMap<>();
    for (Order order : orders) {
      if (order == null) continue;
      order.setCustomer(resolveUserSummary(order.getCustomerId(), cache));
      enrichBusinessWithCache(order.getBusiness(), cache);
      enrichAddressWithCache(order.getDeliveryAddress(), cache);
    }
  }

  public void enrichReview(Review review) {
    if (review == null) return;
    Map<Long, UserSummaryView> cache = new HashMap<>();
    review.setCustomer(resolveUserSummary(review.getCustomerId(), cache));
    enrichBusinessWithCache(review.getBusiness(), cache);
    enrichOrderWithCache(review.getOrder(), cache);
  }

  public void enrichReviews(List<Review> reviews) {
    if (reviews == null || reviews.isEmpty()) return;
    Map<Long, UserSummaryView> cache = new HashMap<>();
    for (Review review : reviews) {
      if (review == null) continue;
      review.setCustomer(resolveUserSummary(review.getCustomerId(), cache));
      enrichBusinessWithCache(review.getBusiness(), cache);
      enrichOrderWithCache(review.getOrder(), cache);
    }
  }

  public void enrichBusinessApplication(BusinessApplication app) {
    if (app == null) return;
    Map<Long, UserSummaryView> cache = new HashMap<>();
    app.setHandler(resolveUserSummary(app.getHandlerId(), cache));
    enrichBusinessWithCache(app.getBusiness(), cache);
  }

  public void enrichBusinessApplications(List<BusinessApplication> apps) {
    if (apps == null || apps.isEmpty()) return;
    Map<Long, UserSummaryView> cache = new HashMap<>();
    for (BusinessApplication app : apps) {
      if (app == null) continue;
      app.setHandler(resolveUserSummary(app.getHandlerId(), cache));
      enrichBusinessWithCache(app.getBusiness(), cache);
    }
  }

  public void enrichMerchantApplication(MerchantApplication app) {
    if (app == null) return;
    Map<Long, UserSummaryView> cache = new HashMap<>();
    app.setApplicant(resolveUserSummary(app.getApplicantId(), cache));
    app.setHandler(resolveUserSummary(app.getHandlerId(), cache));
  }

  public void enrichMerchantApplications(List<MerchantApplication> apps) {
    if (apps == null || apps.isEmpty()) return;
    Map<Long, UserSummaryView> cache = new HashMap<>();
    for (MerchantApplication app : apps) {
      if (app == null) continue;
      app.setApplicant(resolveUserSummary(app.getApplicantId(), cache));
      app.setHandler(resolveUserSummary(app.getHandlerId(), cache));
    }
  }

  private void enrichBusinessWithCache(Business business, Map<Long, UserSummaryView> cache) {
    if (business == null) return;
    business.setBusinessOwner(resolveUserSummary(business.getBusinessOwnerId(), cache));
  }

  private void enrichAddressWithCache(DeliveryAddress address, Map<Long, UserSummaryView> cache) {
    if (address == null) return;
    address.setCustomer(resolveUserSummary(address.getCustomerId(), cache));
  }

  private void enrichOrderWithCache(Order order, Map<Long, UserSummaryView> cache) {
    if (order == null) return;
    order.setCustomer(resolveUserSummary(order.getCustomerId(), cache));
    enrichBusinessWithCache(order.getBusiness(), cache);
    enrichAddressWithCache(order.getDeliveryAddress(), cache);
  }

  private UserSummaryView resolveUserSummary(Long userId, Map<Long, UserSummaryView> cache) {
    if (userId == null) return null;
    UserSummaryView cached = cache.get(userId);
    if (cached != null) return cached;
    User user = userService.getUserById(userId);
    UserSummaryView summary =
        user == null
            ? new UserSummaryView(userId, null)
            : new UserSummaryView(user.getId(), user.getUsername());
    cache.put(userId, summary);
    return summary;
  }
}
