package cn.edu.tju.points.service.impl;

import cn.edu.tju.points.model.BO.PointsAccount;
import cn.edu.tju.points.repository.PointsRepository;
import cn.edu.tju.points.service.PointsInternalService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PointsInternalServiceImpl implements PointsInternalService {

    private final PointsRepository repository;

    public PointsInternalServiceImpl(PointsRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PointsAccount> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }
}
