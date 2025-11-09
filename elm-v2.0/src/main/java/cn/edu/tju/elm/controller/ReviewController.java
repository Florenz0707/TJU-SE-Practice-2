package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.Business;
import cn.edu.tju.elm.model.Order;
import cn.edu.tju.elm.model.OrderState;
import cn.edu.tju.elm.model.Review;
import cn.edu.tju.elm.service.BusinessService;
import cn.edu.tju.elm.service.OrderService;
import cn.edu.tju.elm.service.ReviewService;
import cn.edu.tju.elm.utils.Utils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    private final BusinessService businessService;
    private final OrderService orderService;
    private final UserService userService;

    public ReviewController(ReviewService reviewService, BusinessService businessService, OrderService orderService, UserService userService) {
        this.reviewService = reviewService;
        this.businessService = businessService;
        this.orderService = orderService;
        this.userService = userService;
    }

    @PostMapping("/order/{orderId}")
    public HttpResult<Review> addReview(@PathVariable Long orderId, @RequestBody Review review) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Order order = orderService.getOrderById(orderId);
        if (order == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
        if (!order.getOrderState().equals(OrderState.COMPLETE))
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Order.OrderState ERROR");
        Business business = order.getBusiness();
        User customer = order.getCustomer();

        if (review == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review CANT BE NULL");
        if (review.getStars() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review.Stars CANT BE NULL");
        if (0 > review.getStars() || 10 < review.getStars())
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Review.Stars MUST BE BETWEEN 0 AND 10");
        if (review.getContent() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review.Content CANT BE NULL");
        if (review.getAnonymous() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review.Anonymous CANT BE NULL");

        if (me.equals(customer)) {
            Utils.setNewEntity(review, me);
            review.setOrder(order);
            review.setBusiness(business);
            review.setCustomer(customer);
            reviewService.addReview(review);

            order.setOrderState(OrderState.COMMENTED);
            order.setUpdater(me.getId());
            order.setUpdateTime(review.getUpdateTime());
            orderService.updateOrder(order);
            return HttpResult.success(review);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @PatchMapping("/{reviewId}")
    public HttpResult<Review> updateReview(@PathVariable Long reviewId, @RequestBody Review review) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Review oldReview = reviewService.getReviewById(reviewId);
        if (oldReview == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "OldReview NOT FOUND");
        if (review == null ||
                (review.getStars() == null && review.getContent() == null && review.getAnonymous() == null))
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "NewReview CANT BE NULL");

        if (me.equals(oldReview.getCustomer())) {
            // 只修改stars, content, anonymous
            if (review.getStars() != null)
                oldReview.setStars(review.getStars());
            if (review.getContent() != null)
                oldReview.setContent(review.getContent());
            if (review.getAnonymous() != null)
                oldReview.setAnonymous(review.getAnonymous());

            LocalDateTime now = LocalDateTime.now();
            review.setUpdateTime(now);
            review.setUpdater(me.getId());
            reviewService.updateReview(review);
            return HttpResult.success(review);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @DeleteMapping("/{reviewId}")
    public HttpResult<String> deleteReview(@PathVariable Long reviewId) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Review review = reviewService.getReviewById(reviewId);
        if (review == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");

        boolean isAdmin = Utils.hasAuthority(me, "ADMIN");
        if (isAdmin || me.equals(review.getCustomer())) {
            Utils.deleteEntity(review, me);
            reviewService.updateReview(review);

            Order order = review.getOrder();
            order.setOrderState(OrderState.COMPLETE);
            order.setUpdateTime(review.getUpdateTime());
            order.setUpdater(me.getId());
            orderService.updateOrder(order);
            return HttpResult.success("Delete review successfully.");
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @GetMapping("/my")
    public HttpResult<List<Review>> getReviewsByUserId() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        return HttpResult.success(reviewService.getReviewsByUserId(me.getId()));
    }

    @GetMapping("/order/{orderId}")
    public HttpResult<Review> getReviewByOrderId(@PathVariable Long orderId) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        Order order = orderService.getOrderById(orderId);
        if (order == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Order NOT FOUND");
        Review review = reviewService.getReviewByOrderId(orderId);
        if (review == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");

        // 如果是用户想看自己的订单的评价，鉴权完毕之后就返回评价
        if (me.equals(review.getCustomer()))
            return HttpResult.success(review);

        // 如果是商家想看订单评价，鉴权之后，如果用户设置了匿名评价就不展示，否则返回评价
        boolean isBusiness = Utils.hasAuthority(me, "BUSINESS");
        if (isBusiness && me.equals(review.getBusiness().getBusinessOwner())) {
            if (review.getAnonymous())
                return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Review NOT FOUND");
            return HttpResult.success(review);
        }

        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @GetMapping("/business/{businessId}")
    public HttpResult<List<Review>> getReviewsByBusinessId(@PathVariable Long businessId) {
        Business business = businessService.getBusinessById(businessId);
        if (business == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Business NOT FOUND");

        List<Review> reviewList = reviewService.getReviewsByBusinessId(businessId);
        // 作匿名处理
        for (Review review : reviewList) {
            if (review.getAnonymous()) {
                review.setCustomer(null);
                review.setOrder(null);
                review.setCreator(null);
                review.setUpdater(null);
            }
        }
        return HttpResult.success(reviewList);
    }
}
