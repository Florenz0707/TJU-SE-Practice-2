package cn.edu.tju.points.service;

import cn.edu.tju.points.model.BO.PointsAccount;
import cn.edu.tju.points.exception.PointsException;

import java.util.Optional;

public interface PointsInternalService {
    Optional<PointsAccount> findByUserId(Long userId);
    
    Integer notifyOrderSuccess(Long userId, String bizId, Double amount, String eventTime, String extraInfo) throws PointsException;
    
    Integer notifyLoginSuccess(Long userId, String eventTime) throws PointsException;
    
    Integer notifyRegisterSuccess(Long userId, String eventTime) throws PointsException;
    
    Integer notifyReviewSuccess(Long userId, String bizId, Integer amount, String eventTime, String extraInfo) throws PointsException;
    
    boolean notifyReviewDeleted(Long userId, String reviewId) throws PointsException;
}
