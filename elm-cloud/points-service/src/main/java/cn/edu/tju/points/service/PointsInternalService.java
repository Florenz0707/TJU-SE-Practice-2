package cn.edu.tju.points.service;

import cn.edu.tju.points.model.PointsAccount;

import java.util.Optional;

public interface PointsInternalService {
    Optional<PointsAccount> findByUserId(Long userId);
}
