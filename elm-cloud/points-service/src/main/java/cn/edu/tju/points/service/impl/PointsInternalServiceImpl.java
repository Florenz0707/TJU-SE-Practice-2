package cn.edu.tju.points.service.impl;

import cn.edu.tju.points.model.BO.PointsAccount;
import cn.edu.tju.points.repository.PointsRepository;
import cn.edu.tju.points.service.PointsInternalService;
import cn.edu.tju.points.service.PointsService;
import cn.edu.tju.points.exception.PointsException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PointsInternalServiceImpl implements PointsInternalService {

    private final PointsRepository repository;
    private final PointsService pointsService;

    public PointsInternalServiceImpl(PointsRepository repository, PointsService pointsService) {
        this.repository = repository;
        this.pointsService = pointsService;
    }

    @Override
    public Optional<PointsAccount> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public Integer notifyOrderSuccess(Long userId, String bizId, Double amount, String eventTime, String extraInfo) throws PointsException {
        return pointsService.notifyOrderSuccess(userId, bizId, amount, eventTime, extraInfo);
    }

    @Override
    public Integer notifyLoginSuccess(Long userId, String eventTime) throws PointsException {
        return pointsService.notifyLoginSuccess(userId, eventTime);
    }

    @Override
    public Integer notifyRegisterSuccess(Long userId, String eventTime) throws PointsException {
        return pointsService.notifyRegisterSuccess(userId, eventTime);
    }

    @Override
    public Integer notifyReviewSuccess(Long userId, String bizId, Integer amount, String eventTime, String extraInfo) throws PointsException {
        return pointsService.notifyReviewSuccess(userId, bizId, amount, eventTime, extraInfo);
    }

    @Override
    public boolean notifyReviewDeleted(Long userId, String reviewId) throws PointsException {
        return pointsService.notifyReviewDeleted(userId, reviewId);
    }
}
