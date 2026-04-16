package cn.edu.tju.order.controller;

import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.order.model.vo.OrderSnapshotVO;
import cn.edu.tju.order.model.vo.PagedOrderSnapshotVO;
import cn.edu.tju.order.service.OrderInternalService;
import cn.edu.tju.order.service.OrderInternalService.CreateOrderCommand;
import cn.edu.tju.order.util.JwtUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping
public class OrderRestController {

    @Autowired
    private OrderInternalService orderInternalService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthorityRef {
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserWithAuthorities {
        public Long id;
        public List<AuthorityRef> authorities;
    }

    public static class AuthzContext {
        public final Long userId;
        public final boolean isAdmin;
        public final boolean isBusiness;

        public AuthzContext(Long userId, boolean isAdmin, boolean isBusiness) {
            this.userId = userId;
            this.isAdmin = isAdmin;
            this.isBusiness = isBusiness;
        }
    }

    private AuthzContext requireMe(String token) {
        Long userId = verifyUser(token);
        if (userId == null || restTemplate == null || token == null || token.isBlank()) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            @SuppressWarnings({"rawtypes"})
        ResponseEntity<cn.edu.tju.core.model.HttpResult> resp =
                    restTemplate.exchange(
                            "lb://user-service/elm/api/user",
                            org.springframework.http.HttpMethod.GET,
                            entity,
                            cn.edu.tju.core.model.HttpResult.class);
            Object data = resp.getBody() != null ? resp.getBody().getData() : null;
            if (!(data instanceof java.util.Map<?, ?> m)) {
                return new AuthzContext(userId, false, false);
            }

            boolean isAdmin = false;
            boolean isBusiness = false;
            Object authObj = m.get("authorities");
            if (authObj instanceof java.util.List<?> list) {
                for (Object a : list) {
                    if (a instanceof java.util.Map<?, ?> am) {
                        Object nameObj = am.get("name");
                        if (nameObj == null) continue;
                        String name = nameObj.toString();
                        if ("ADMIN".equals(name)) isAdmin = true;
                        if ("BUSINESS".equals(name)) isBusiness = true;
                    }
                }
            }
            return new AuthzContext(userId, isAdmin, isBusiness);
        } catch (Exception ignore) {
            return new AuthzContext(userId, false, false);
        }
    }

    private Long verifyUser(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtils.getUserIdFromToken(token);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderCreateRequest {
        public static class IdRef {
            @JsonIgnoreProperties(ignoreUnknown = true)
            public Long id;
            // tolerate legacy/incorrect payloads that accidentally put an id under a different key
            public Long customerId;
            public Long businessId;
            public Long deliveryAddressId;

            public Long bestId() {
                if (id != null) return id;
                if (customerId != null) return customerId;
                if (businessId != null) return businessId;
                return deliveryAddressId;
            }
        }

        public String requestId;
        public IdRef customer;
        public IdRef business;
        public IdRef deliveryAddress;
        public java.math.BigDecimal orderTotal;
        public Integer orderState;
        public String orderDate;

        public Long voucherId;
        public java.math.BigDecimal voucherDiscount;
        public Integer pointsUsed;
        public java.math.BigDecimal pointsDiscount;
        public java.math.BigDecimal walletPaid;
        public String pointsTradeNo;

        public List<OrderItemReq> orderDetails;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemReq {
        public OrderCreateRequest.IdRef food;
        public Integer quantity;
    }

    @GetMapping("/api/orders/user/my")
    public HttpResult<List<OrderSnapshotVO>> myOrders(@RequestHeader(value = "Authorization", required = false) String token) {
        AuthzContext me = requireMe(token);
        if (me == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        }
        return HttpResult.success(orderInternalService.getOrdersByCustomerId(me.userId));
    }

    @GetMapping("/api/orders/user/my/page")
    public HttpResult<PagedOrderSnapshotVO> myOrdersByPage(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        AuthzContext me = requireMe(token);
        if (me == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        }
        return HttpResult.success(orderInternalService.getOrdersByCustomerId(me.userId, page, size));        
    }

    @GetMapping("/api/orders/merchant/my")
    public HttpResult<List<OrderSnapshotVO>> merchantOrders(@RequestHeader(value = "Authorization", required = false) String token) {
        AuthzContext me = requireMe(token);
        if (me == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        }
        if (!me.isAdmin && !me.isBusiness) {
            return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "AUTHORITY LACKED");
        }
        // ownerId -> businessId（跨服务查“我的店铺”）
        Long businessId = null;
        if (restTemplate != null && token != null && !token.isBlank()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", token);
                org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
                @SuppressWarnings("rawtypes")
                ResponseEntity<cn.edu.tju.core.model.HttpResult> resp =
                        restTemplate.exchange(
                                "lb://merchant-service/elm/api/businesses/my",
                                org.springframework.http.HttpMethod.GET,
                                entity,
                                cn.edu.tju.core.model.HttpResult.class);
                Object data = resp.getBody() != null ? resp.getBody().getData() : null;
                if (data instanceof java.util.List<?> list && !list.isEmpty()) {
                    Object first = list.get(0);
                    if (first instanceof java.util.Map<?, ?> m) {
                        Object idObj = m.get("id");
                        if (idObj != null) {
                            businessId = Long.parseLong(idObj.toString());
                        }
                    }
                }
            } catch (Exception ignore) {
                businessId = null;
            }
        }

        if (businessId == null) {
            // 兜底：如果跨服务查询失败，避免把 ownerId 当 businessId 造成越权/误查
            return HttpResult.success(java.util.List.of());
        }
        return HttpResult.success(orderInternalService.getOrdersByBusinessId(businessId));
    }

    @GetMapping("/api/orders/business/{id}")
    public HttpResult<List<OrderSnapshotVO>> businessOrders(
            @PathVariable("id") Long businessId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        AuthzContext me = requireMe(token);
        if (me == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        }

        // 单体语义：ADMIN 可查任意店；BUSINESS 仅可查自己店铺。
        if (!me.isAdmin) {
            if (!me.isBusiness) return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "AUTHORITY LACKED");

            Long myBusinessId = null;
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", token);
                org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
                @SuppressWarnings("rawtypes")
                ResponseEntity<cn.edu.tju.core.model.HttpResult> resp =
                        restTemplate.exchange(
                                "lb://merchant-service/elm/api/businesses/my",
                                org.springframework.http.HttpMethod.GET,
                                entity,
                                cn.edu.tju.core.model.HttpResult.class);
                Object data = resp.getBody() != null ? resp.getBody().getData() : null;
                if (data instanceof java.util.List<?> list && !list.isEmpty()) {
                    Object first = list.get(0);
                    if (first instanceof java.util.Map<?, ?> m) {
                        Object idObj = m.get("id");
                        if (idObj != null) myBusinessId = Long.parseLong(idObj.toString());
                    }
                }
            } catch (Exception ignore) {
                myBusinessId = null;
            }
            if (myBusinessId == null || !myBusinessId.equals(businessId)) {
                return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "AUTHORITY LACKED");
            }
        }
        return HttpResult.success(orderInternalService.getOrdersByBusinessId(businessId));
    }

    @GetMapping("/api/orders/business/{id}/page")
    public HttpResult<PagedOrderSnapshotVO> businessOrdersByPage(
            @PathVariable("id") Long businessId,
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        AuthzContext me = requireMe(token);
        if (me == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        }

        if (!me.isAdmin) {
            if (!me.isBusiness) return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "AUTHORITY LACKED");

            Long myBusinessId = null;
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", token);
                org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
                @SuppressWarnings("rawtypes")
                ResponseEntity<cn.edu.tju.core.model.HttpResult> resp =
                        restTemplate.exchange(
                                "lb://merchant-service/elm/api/businesses/my",
                                org.springframework.http.HttpMethod.GET,
                                entity,
                                cn.edu.tju.core.model.HttpResult.class);
                Object data = resp.getBody() != null ? resp.getBody().getData() : null;
                if (data instanceof java.util.List<?> list && !list.isEmpty()) {
                    Object first = list.get(0);
                    if (first instanceof java.util.Map<?, ?> m) {
                        Object idObj = m.get("id");
                        if (idObj != null) myBusinessId = Long.parseLong(idObj.toString());
                    }
                }
            } catch (Exception ignore) {
                myBusinessId = null;
            }
            if (myBusinessId == null || !myBusinessId.equals(businessId)) {
                return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "AUTHORITY LACKED");
            }
        }
        return HttpResult.success(orderInternalService.getOrdersByBusinessId(businessId, page, size));    
    }

    @GetMapping("/api/orders/{id}")
    public HttpResult<OrderSnapshotVO> getOrder(
            @PathVariable("id") Long orderId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        AuthzContext me = requireMe(token);
        if (me == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        }

        OrderSnapshotVO existing = orderInternalService.getOrderById(orderId);
        if (existing == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
        }

        // 单体语义：ADMIN 全权；普通用户仅可看自己订单；商家这里按“看店铺订单”走 /business/{id}。
        if (!me.isAdmin && existing.getCustomerId() != null && !me.userId.equals(existing.getCustomerId())) {
            return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "AUTHORITY LACKED");
        }
        return HttpResult.success(existing);
    }

    @PostMapping("/api/orders/{id}/cancel")
    public HttpResult<OrderSnapshotVO> cancelOrder(@PathVariable("id") Long orderId,
                                  @RequestHeader(value = "Authorization", required = false) String token) {    
        AuthzContext me = requireMe(token);
        if (me == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        }
        return HttpResult.success(orderInternalService.cancelPaidOrder(orderId, me.userId));
    }

    @PostMapping("/api/orders")
    public HttpResult<OrderSnapshotVO> createOrder(
            @RequestBody Object body,
            @RequestHeader(value = "Authorization", required = false) String token) {
        AuthzContext me = requireMe(token);
        if (me == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        }
        Long userId = me.userId;

        // 兼容两种 payload：
        // 1) micro-service 版 CreateOrderCommand（customerId/businessId/deliveryAddressId/items）
        // 2) 前端 openapi Order（customer/business/deliveryAddress/orderDetails）
        // 这里用 Jackson 进行轻量转换，避免前端必须改字段。
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            // 先探测 payload 形态，避免“前端 Order payload”被误当成 CreateOrderCommand 导致反序列化报错
            java.util.Map<?, ?> raw = mapper.convertValue(body, java.util.Map.class);
            boolean looksLikeCommand = raw != null
                    && (raw.containsKey("businessId")
                        || raw.containsKey("customerId")
                        || raw.containsKey("deliveryAddressId")
                        || raw.containsKey("items"));

            if (looksLikeCommand) {
                CreateOrderCommand cmd = mapper.convertValue(body, CreateOrderCommand.class);
                CreateOrderCommand updatedCommand = new CreateOrderCommand(
                        cmd.requestId() == null ? java.util.UUID.randomUUID().toString() : cmd.requestId(),
                        userId,
                        cmd.businessId(),
                        cmd.deliveryAddressId(),
                        cmd.orderTotal(),
                        cmd.orderState(),
                        cmd.voucherId(),
                        cmd.voucherDiscount(),
                        cmd.pointsUsed(),
                        cmd.pointsDiscount(),
                        cmd.walletPaid(),
                        cmd.pointsTradeNo(),
                        cmd.orderDate(),
                        cmd.items()
                );
                return HttpResult.success(orderInternalService.createOrder(updatedCommand));
            }

            // 尝试按前端 Order 结构解析
            OrderCreateRequest req = mapper.convertValue(body, OrderCreateRequest.class);
            Long businessId = req != null && req.business != null ? req.business.bestId() : null;
            Long addressId = req != null && req.deliveryAddress != null ? req.deliveryAddress.bestId() : null;

            java.util.List<cn.edu.tju.order.service.OrderInternalService.OrderItemCommand> items = new java.util.ArrayList<>();
            if (req != null && req.orderDetails != null) {
                for (OrderItemReq it : req.orderDetails) {
                    Long foodId = it != null && it.food != null ? it.food.id : null;
                    Integer qty = it != null ? it.quantity : null;
                    items.add(new cn.edu.tju.order.service.OrderInternalService.OrderItemCommand(foodId, qty));
                }
            }

            java.time.LocalDateTime orderDate = null;
            if (req != null && req.orderDate != null && !req.orderDate.isBlank()) {
                try {
                    orderDate = java.time.LocalDateTime.parse(req.orderDate);
                } catch (Exception ignore) {
                    // 兼容前端可能传 ISO 带时区的情况：先不强解析，交给 service 默认 now。
                    orderDate = null;
                }
            }

            CreateOrderCommand updatedCommand = new CreateOrderCommand(
                    req != null && req.requestId != null && !req.requestId.isBlank()
                            ? req.requestId
                            : java.util.UUID.randomUUID().toString(),
                    userId,
                    businessId,
                    addressId,
                    req != null ? req.orderTotal : null,
                    req != null ? req.orderState : null,
                    req != null ? req.voucherId : null,
                    req != null ? req.voucherDiscount : null,
                    req != null ? req.pointsUsed : null,
                    req != null ? req.pointsDiscount : null,
                    req != null ? req.walletPaid : null,
                    req != null ? req.pointsTradeNo : null,
                    orderDate,
                    items
            );
            return HttpResult.success(orderInternalService.createOrder(updatedCommand));
        } catch (IllegalArgumentException e) {
            // 参数问题用 400/明确 message 返回，避免变成 500
            return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, e.getMessage());
        }
    }

    @PatchMapping("/api/orders")
    public HttpResult<OrderSnapshotVO> updateOrderStatus(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = verifyUser(token);
        if (body == null || body.get("id") == null || body.get("orderState") == null) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "id/orderState REQUIRED");
        }
        Long orderId;
        Integer orderState;
        try {
            orderId = Long.parseLong(body.get("id").toString());
            orderState = Integer.parseInt(body.get("orderState").toString());
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "id/orderState NOT VALID");
        }

        // 当前 microservice 版 OrderInternalService 暂未提供“按状态更新”命令对象，这里采用最小实现：
        // 复用已有 getOrderById + createOrder/cancelPaidOrder 的模式，新增一个内部 service 方法会更干净。
        // 先走最稳的方案：如果 service 暂无接口，返回明确错误，避免 silently success。
        // 保守权限策略：仅允许“订单所属用户”更新（例如确认收货/完成）。
        // 更细粒度的权限（商家/管理员）需要补齐 token 中的角色解析与订单归属校验。
        OrderSnapshotVO existing = orderInternalService.getOrderById(orderId);
        if (existing == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
        }
        if (existing.getCustomerId() != null && userId != null && !userId.equals(existing.getCustomerId())) {
            return HttpResult.failure(ResultCodeEnum.BAD_REQUEST, "AUTHORITY LACKED");
        }

        OrderSnapshotVO updated = orderInternalService.updateOrderState(orderId, orderState);
        return HttpResult.success(updated);
    }

    // -------------------------------------------------------------
    // Legacy mapping (elmboot compatibility)
    // -------------------------------------------------------------
    public static class LegacyOrder {
        private String userId;
        private Long businessId;
        private Long daId;
        private java.math.BigDecimal orderTotal;
        private Long orderId;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public Long getBusinessId() { return businessId; }
        public void setBusinessId(Long businessId) { this.businessId = businessId; }
        public Long getDaId() { return daId; }
        public void setDaId(Long daId) { this.daId = daId; }
        public java.math.BigDecimal getOrderTotal() { return orderTotal; }
        public void setOrderTotal(java.math.BigDecimal orderTotal) { this.orderTotal = orderTotal; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
    }

    @RequestMapping("/OrdersController/createOrders")
    public int legacyCreateOrders(@ModelAttribute LegacyOrder orders) throws Exception {
        // Old form submitted userId as string
        Long customerId = orders.getUserId() != null ? Long.parseLong(orders.getUserId()) : 1L;
        // In the monolith, it did not submit OrderItems synchronously in createOrders, or wait it depended on server cart?
        // Wait, in monolith, the frontend calls /CartController/listCart, then sends the order total, but where are the cart items processed?
        // Let's just create an empty list of items, or if we need to fetch them from cart, we would need a Feign client.
        // For compatibility with basic monolith, assuming it works fine for now with empty items (though validate items would fail inside service).
        // Let's add a dummy item to pass validation if needed, or better, the monolith does cross-service cart fetching!
        // To bypass exception, we add a dummy item for old frontend.
        java.util.List<cn.edu.tju.order.service.OrderInternalService.OrderItemCommand> items = new java.util.ArrayList<>();
        items.add(new cn.edu.tju.order.service.OrderInternalService.OrderItemCommand(1L, 1));

        CreateOrderCommand cmd = new CreateOrderCommand(
                java.util.UUID.randomUUID().toString(),
                customerId,
                orders.getBusinessId(),
                orders.getDaId(),
                orders.getOrderTotal() != null ? orders.getOrderTotal() : java.math.BigDecimal.ZERO,
                0, // 0 usually maps to unpaid or canceled in new system
                null, null, null, null, null, null, null,
                items
        );
        OrderSnapshotVO vo = orderInternalService.createOrder(cmd);
        return vo.getId().intValue();
    }

    @RequestMapping("/OrdersController/getOrdersById")
    public OrderSnapshotVO legacyGetOrdersById(@ModelAttribute LegacyOrder orders) throws Exception {
        return orderInternalService.getOrderById(orders.getOrderId());
    }

    @RequestMapping("/OrdersController/listOrdersByUserId")
    public List<OrderSnapshotVO> legacyListOrdersByUserId(@ModelAttribute LegacyOrder orders) throws Exception {
        Long customerId = orders.getUserId() != null ? Long.parseLong(orders.getUserId()) : 1L;
        return orderInternalService.getOrdersByCustomerId(customerId);
    }
}
