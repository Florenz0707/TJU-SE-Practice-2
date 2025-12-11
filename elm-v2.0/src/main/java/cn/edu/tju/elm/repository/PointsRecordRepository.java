package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.PointsRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointsRecordRepository extends JpaRepository<PointsRecord, Long> {
    Page<PointsRecord> findByUserId(Long userId, Pageable pageable);

    Page<PointsRecord> findByUserIdAndType(Long userId, String type, Pageable pageable);

    List<PointsRecord> findByUserId(Long userId);
}
