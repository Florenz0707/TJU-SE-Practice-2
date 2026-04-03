package cn.edu.tju.points.controller;

import cn.edu.tju.points.model.BO.PointsAccount;
import cn.edu.tju.points.service.PointsInternalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/points")
public class PointsInnerController {

    private final PointsInternalService pointsInternalService;

    public PointsInnerController(PointsInternalService pointsInternalService) {
        this.pointsInternalService = pointsInternalService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PointsAccount> byUser(@PathVariable Long userId) {
        return pointsInternalService.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
