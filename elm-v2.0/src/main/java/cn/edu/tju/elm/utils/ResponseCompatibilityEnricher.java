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
  private final InternalCatalogClient internalCatalogClient;
  private final InternalAddressClient internalAddressClient;
  private final InternalOrderClient internalOrderClient;

  public ResponseCompatibilityEnricher(
      UserService userService,
      InternalCatalogClient internalCatalogClient,
      InternalAddressClient internalAddressClient,
      InternalOrderClient internalOrderClient) {
    this.userService = userService;
    this.internalCatalogClient = internalCatalogClient;
    this.internalAddressClient = internalAddressClient;
    this.internalOrderClient = internalOrderClient;
  }

  public void enrichBusiness(Business business) {
    if (business == null) return;
    Map<Long, UserSummaryView> userCache = new HashMap<>();
    Map<Long, Business> businessCache = new HashMap<>();
    enrichBusinessWithCache(business, userCache, businessCache);
  }

  public void enrichBusinesses(List<Business> businesses) {
    if (businesses == null || businesses.isEmpty()) return;
    Map<Long, UserSummaryView> userCache = new HashMap<>();
    Map<Long, Business> businessCache = new HashMap<>();
    for (Business business : businesses) {
      if (business != null) {
        enrichBusinessWithCache(business, userCache, businessCache);
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
    Map<Long, UserSummaryView> userCache = new HashMap<>();
    Map<Long, Business> businessCache = new HashMap<>();
    cart.setCustomer(resolveUserSummary(cart.getCustomerId(), userCache));
    enrichBusinessWithCache(cart.getBusiness(), userCache, businessCache);
  }

  public void enrichCarts(List<Cart> carts) {
    if (carts == null || carts.isEmpty()) return;
    Map<Long, UserSummaryView> userCache = new HashMap<>();
    Map<Long, Business> businessCache = new HashMap<>();
    for (Cart cart : carts) {
      if (cart == null) continue;
      cart.setCustomer(resolveUserSummary(cart.getCustomerId(), userCache));
      enrichBusinessWithCache(cart.getBusiness(), userCache, businessCache);
    }
  }

  public void enrichOrder(Order order) {
    if (order == null) return;
    Map<Long, UserSummaryView> userCache = new HashMap<>();
    Map<Long, Business> businessCache = new HashMap<>();
    Map<Long, DeliveryAddress> addressCache = new HashMap<>();
    order.setCustomer(resolveUserSummary(order.getCustomerId(), userCache));
    enrichBusinessWithCache(order.getBusiness(), userCache, businessCache);
    enrichAddressWithCache(order.getDeliveryAddress(), userCache, addressCache);
  }

  public void enrichOrders(List<Order> orders) {
    if (orders == null || orders.isEmpty()) return;
    Map<Long, UserSummaryView> userCache = new HashMap<>();
    Map<Long, Business> businessCache = new HashMap<>();
    Map<Long, DeliveryAddress> addressCache = new HashMap<>();
    for (Order order : orders) {
      if (order == null) continue;
      order.setCustomer(resolveUserSummary(order.getCustomerId(), userCache));
      enrichBusinessWithCache(order.getBusiness(), userCache, businessCache);
      enrichAddressWithCache(order.getDeliveryAddress(), userCache, addressCache);
    }
  }

  public void enrichReview(Review review) {
    if (review == null) return;
    Map<Long, UserSummaryView> userCache = new HashMap<>();
    Map<Long, Business> businessCache = new HashMap<>();
    Map<Long, DeliveryAddress> addressCache = new HashMap<>();
    Map<Long, Order> orderCache = new HashMap<>();
    review.setCustomer(resolveUserSummary(review.getCustomerId(), userCache));
    review.setBusiness(hydrateBusiness(review.getBusiness(), businessCache));
    review.setOrder(hydrateOrder(review.getOrder(), businessCache, addressCache, orderCache));
    enrichBusinessWithCache(review.getBusiness(), userCache, businessCache);
    enrichOrderWithCache(review.getOrder(), userCache, businessCache, addressCache, orderCache);
  }

  public void enrichReviews(List<Review> reviews) {
    if (reviews == null || reviews.isEmpty()) return;
    Map<Long, UserSummaryView> userCache = new HashMap<>();
    Map<Long, Business> businessCache = new HashMap<>();
    Map<Long, DeliveryAddress> addressCache = new HashMap<>();
    Map<Long, Order> orderCache = new HashMap<>();
    for (Review review : reviews) {
      if (review == null) continue;
      review.setCustomer(resolveUserSummary(review.getCustomerId(), userCache));
      review.setBusiness(hydrateBusiness(review.getBusiness(), businessCache));
      review.setOrder(hydrateOrder(review.getOrder(), businessCache, addressCache, orderCache));
      enrichBusinessWithCache(review.getBusiness(), userCache, businessCache);
      enrichOrderWithCache(review.getOrder(), userCache, businessCache, addressCache, orderCache);
    }
  }

  public void enrichBusinessApplication(BusinessApplication app) {
    if (app == null) return;
    Map<Long, UserSummaryView> userCache = new HashMap<>();
    Map<Long, Business> businessCache = new HashMap<>();
    app.setHandler(resolveUserSummary(app.getHandlerId(), userCache));
    enrichBusinessWithCache(app.getBusiness(), userCache, businessCache);
  }

  public void enrichBusinessApplications(List<BusinessApplication> apps) {
    if (apps == null || apps.isEmpty()) return;
    Map<Long, UserSummaryView> userCache = new HashMap<>();
    Map<Long, Business> businessCache = new HashMap<>();
    for (BusinessApplication app : apps) {
      if (app == null) continue;
      app.setHandler(resolveUserSummary(app.getHandlerId(), userCache));
      enrichBusinessWithCache(app.getBusiness(), userCache, businessCache);
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

  private void enrichBusinessWithCache(
      Business business, Map<Long, UserSummaryView> userCache, Map<Long, Business> businessCache) {
    if (business == null) return;
    Business hydrated = hydrateBusiness(business, businessCache);
    if (hydrated != null && hydrated != business) {
      copyBusiness(hydrated, business);
    }
    business.setBusinessOwner(resolveUserSummary(business.getBusinessOwnerId(), userCache));
  }

  private void enrichAddressWithCache(
      DeliveryAddress address,
      Map<Long, UserSummaryView> userCache,
      Map<Long, DeliveryAddress> addressCache) {
    if (address == null) return;
    DeliveryAddress hydrated = hydrateAddress(address, addressCache);
    if (hydrated != null && hydrated != address) {
      copyAddress(hydrated, address);
    }
    address.setCustomer(resolveUserSummary(address.getCustomerId(), userCache));
  }

  private void enrichOrderWithCache(
      Order order,
      Map<Long, UserSummaryView> userCache,
      Map<Long, Business> businessCache,
      Map<Long, DeliveryAddress> addressCache,
      Map<Long, Order> orderCache) {
    if (order == null) return;
    Order hydrated = hydrateOrder(order, businessCache, addressCache, orderCache);
    if (hydrated != null && hydrated != order) {
      copyOrder(hydrated, order);
    }
    order.setCustomer(resolveUserSummary(order.getCustomerId(), userCache));
    enrichBusinessWithCache(order.getBusiness(), userCache, businessCache);
    enrichAddressWithCache(order.getDeliveryAddress(), userCache, addressCache);
  }

  private Business hydrateBusiness(Business business, Map<Long, Business> businessCache) {
    if (business == null || business.getId() == null) return business;
    if (business.getBusinessName() != null && business.getBusinessOwnerId() != null) {
      businessCache.putIfAbsent(business.getId(), business);
      return business;
    }

    Business cached = businessCache.get(business.getId());
    if (cached != null) {
      return cached;
    }

    InternalCatalogClient.BusinessSnapshot snapshot =
        internalCatalogClient.getBusinessSnapshot(business.getId());
    if (snapshot == null) {
      businessCache.put(business.getId(), business);
      return business;
    }

    Business hydrated = new Business();
    hydrated.setId(snapshot.businessId());
    hydrated.setBusinessName(snapshot.businessName());
    hydrated.setBusinessOwnerId(snapshot.businessOwnerId());
    hydrated.setBusinessAddress(snapshot.businessAddress());
    hydrated.setBusinessExplain(snapshot.businessExplain());
    hydrated.setBusinessImg(snapshot.businessImg());
    hydrated.setOrderTypeId(snapshot.orderTypeId());
    hydrated.setStartPrice(snapshot.startPrice());
    hydrated.setDeliveryPrice(snapshot.deliveryPrice());
    hydrated.setRemarks(snapshot.remarks());
    hydrated.setOpenTime(snapshot.openTime());
    hydrated.setCloseTime(snapshot.closeTime());
    hydrated.setDeleted(snapshot.deleted());
    businessCache.put(hydrated.getId(), hydrated);
    return hydrated;
  }

  private DeliveryAddress hydrateAddress(
      DeliveryAddress address, Map<Long, DeliveryAddress> addressCache) {
    if (address == null || address.getId() == null) return address;
    if (address.getAddress() != null && address.getCustomerId() != null) {
      addressCache.putIfAbsent(address.getId(), address);
      return address;
    }

    DeliveryAddress cached = addressCache.get(address.getId());
    if (cached != null) {
      return cached;
    }

    InternalAddressClient.AddressSnapshot snapshot = internalAddressClient.getAddressById(address.getId());
    if (snapshot == null) {
      addressCache.put(address.getId(), address);
      return address;
    }

    DeliveryAddress hydrated = new DeliveryAddress();
    hydrated.setId(snapshot.id());
    hydrated.setCustomerId(snapshot.customerId());
    hydrated.setContactName(snapshot.contactName());
    hydrated.setContactSex(snapshot.contactSex());
    hydrated.setContactTel(snapshot.contactTel());
    hydrated.setAddress(snapshot.address());
    addressCache.put(hydrated.getId(), hydrated);
    return hydrated;
  }

  private Order hydrateOrder(
      Order order,
      Map<Long, Business> businessCache,
      Map<Long, DeliveryAddress> addressCache,
      Map<Long, Order> orderCache) {
    if (order == null || order.getId() == null) return order;
    if (order.getCustomerId() != null && order.getOrderState() != null && order.getBusiness() != null) {
      orderCache.putIfAbsent(order.getId(), order);
      return order;
    }

    Order cached = orderCache.get(order.getId());
    if (cached != null) {
      return cached;
    }

    InternalOrderClient.OrderSnapshot snapshot = internalOrderClient.getOrderById(order.getId());
    if (snapshot == null) {
      orderCache.put(order.getId(), order);
      return order;
    }

    Order hydrated = new Order();
    hydrated.setId(snapshot.id());
    hydrated.setCustomerId(snapshot.customerId());
    hydrated.setOrderState(snapshot.orderState());
    hydrated.setOrderTotal(snapshot.orderTotal());
    hydrated.setVoucherDiscount(snapshot.voucherDiscount());
    hydrated.setPointsUsed(snapshot.pointsUsed());
    hydrated.setPointsDiscount(snapshot.pointsDiscount());
    hydrated.setWalletPaid(snapshot.walletPaid());
    hydrated.setPointsTradeNo(snapshot.pointsTradeNo());
    hydrated.setRequestId(snapshot.requestId());
    hydrated.setOrderDate(snapshot.orderDate());
    if (snapshot.businessId() != null) {
      Business businessRef = new Business();
      businessRef.setId(snapshot.businessId());
      hydrated.setBusiness(hydrateBusiness(businessRef, businessCache));
    }
    if (snapshot.deliveryAddressId() != null) {
      DeliveryAddress addressRef = new DeliveryAddress();
      addressRef.setId(snapshot.deliveryAddressId());
      hydrated.setDeliveryAddress(hydrateAddress(addressRef, addressCache));
    }
    orderCache.put(hydrated.getId(), hydrated);
    return hydrated;
  }

  private void copyBusiness(Business source, Business target) {
    target.setId(source.getId());
    target.setBusinessName(source.getBusinessName());
    target.setBusinessOwnerId(source.getBusinessOwnerId());
    target.setBusinessOwner(source.getBusinessOwner());
    target.setBusinessAddress(source.getBusinessAddress());
    target.setBusinessExplain(source.getBusinessExplain());
    target.setBusinessImg(source.getBusinessImg());
    target.setOrderTypeId(source.getOrderTypeId());
    target.setStartPrice(source.getStartPrice());
    target.setDeliveryPrice(source.getDeliveryPrice());
    target.setRemarks(source.getRemarks());
    target.setOpenTime(source.getOpenTime());
    target.setCloseTime(source.getCloseTime());
    target.setDeleted(source.getDeleted());
  }

  private void copyAddress(DeliveryAddress source, DeliveryAddress target) {
    target.setId(source.getId());
    target.setCustomerId(source.getCustomerId());
    target.setCustomer(source.getCustomer());
    target.setContactName(source.getContactName());
    target.setContactSex(source.getContactSex());
    target.setContactTel(source.getContactTel());
    target.setAddress(source.getAddress());
  }

  private void copyOrder(Order source, Order target) {
    target.setId(source.getId());
    target.setCustomerId(source.getCustomerId());
    target.setCustomer(source.getCustomer());
    target.setBusiness(source.getBusiness());
    target.setOrderDate(source.getOrderDate());
    target.setOrderTotal(source.getOrderTotal());
    target.setDeliveryAddress(source.getDeliveryAddress());
    target.setOrderState(source.getOrderState());
    target.setUsedVoucher(source.getUsedVoucher());
    target.setVoucherDiscount(source.getVoucherDiscount());
    target.setPointsUsed(source.getPointsUsed());
    target.setPointsDiscount(source.getPointsDiscount());
    target.setWalletPaid(source.getWalletPaid());
    target.setRequestId(source.getRequestId());
    target.setPointsTradeNo(source.getPointsTradeNo());
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
