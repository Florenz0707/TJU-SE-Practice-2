package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.Review;
import cn.edu.tju.elm.repository.ReviewRepository;
import cn.edu.tju.elm.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public void addReview(Review review) {
        reviewRepository.save(review);
    }

    public void updateReview(Review review) {
        reviewRepository.save(review);
    }

    public Review getReviewById(Long id) {
        Optional<Review> reviewOptional = reviewRepository.findById(id);
        return reviewOptional.map(Utils::filterEntity).orElse(null);
    }

    public Review getReviewByOrderId(Long orderId) {
        Optional<Review> reviewOptional = reviewRepository.findByOrderId(orderId);
        return reviewOptional.map(Utils::filterEntity).orElse(null);
    }

    public List<Review> getReviewsByBusinessId(Long businessId) {
        return Utils.filterEntityList(reviewRepository.findAllByBusinessId(businessId));
    }

    public List<Review> getReviewsByUserId(Long userId) {
        return Utils.filterEntityList(reviewRepository.findAllByCustomerId(userId));
    }
}
