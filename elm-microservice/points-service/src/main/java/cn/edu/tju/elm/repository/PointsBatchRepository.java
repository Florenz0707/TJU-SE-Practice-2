package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.PointsBatch;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointsBatchRepository extends JpaRepository<PointsBatch, Long> {
  List<PointsBatch> findByUserId(Long userId);

  @Query(
      "SELECT pb FROM PointsBatch pb WHERE pb.userId = :userId AND pb.availablePoints > 0 "
          + "ORDER BY CASE WHEN pb.expireTime IS NULL THEN 1 ELSE 0 END, pb.expireTime ASC")
  List<PointsBatch> findAvailableBatchesByUserIdOrderByExpireTime(@Param("userId") Long userId);

  List<PointsBatch> findByUserIdAndTempOrderId(Long userId, String tempOrderId);

  @Query(
      "SELECT pb FROM PointsBatch pb WHERE pb.userId = :userId AND pb.tempOrderId = :tempOrderId")
  List<PointsBatch> findFrozenBatchesByUserIdAndTempOrderId(
      @Param("userId") Long userId, @Param("tempOrderId") String tempOrderId);

  @Query(
      "SELECT pb FROM PointsBatch pb WHERE pb.expireTime IS NOT NULL AND pb.expireTime < :now AND"
          + " pb.availablePoints > 0")
  List<PointsBatch> findExpiredBatches(@Param("now") LocalDateTime now);

  @Query(
      "SELECT pb FROM PointsBatch pb WHERE pb.frozenPoints > 0 AND pb.tempOrderId IS NOT NULL AND"
          + " pb.updateTime < :timeout")
  List<PointsBatch> findFrozenBatchesBeforeTime(@Param("timeout") LocalDateTime timeout);
}
