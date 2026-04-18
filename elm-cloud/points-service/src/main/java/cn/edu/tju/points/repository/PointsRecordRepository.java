package cn.edu.tju.points.repository;

import cn.edu.tju.points.model.BO.PointsRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointsRecordRepository extends JpaRepository<PointsRecord, Long> {
  Page<PointsRecord> findByUserId(Long userId, Pageable pageable);

  Page<PointsRecord> findByUserIdAndType(Long userId, String type, Pageable pageable);

  List<PointsRecord> findByUserId(Long userId);

  Optional<PointsRecord> findByBizId(String bizId);

  Optional<PointsRecord> findTopByUserIdAndTypeAndBizIdOrderByRecordTimeDesc(
      Long userId, String type, String bizId);

  @Query("SELECT COUNT(pr) > 0 FROM PointsRecord pr WHERE pr.userId = :userId AND pr.type = 'EARN' AND pr.channelType = 'LOGIN' AND pr.recordTime >= :startTime")
  boolean existsTodayLoginRecord(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);
}
