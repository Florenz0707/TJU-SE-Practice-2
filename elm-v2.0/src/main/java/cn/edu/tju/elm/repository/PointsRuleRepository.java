package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.PointsRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointsRuleRepository extends JpaRepository<PointsRule, Long> {
  @Query(
      "SELECT pr FROM PointsRule pr WHERE pr.channelType = :channelType AND pr.isEnabled ="
          + " :isEnabled AND (pr.deleted IS NULL OR pr.deleted = false)")
  List<PointsRule> findByChannelTypeAndIsEnabled(
      @Param("channelType") String channelType, @Param("isEnabled") Boolean isEnabled);

  @Query(
      "SELECT pr FROM PointsRule pr WHERE pr.isEnabled = :isEnabled AND (pr.deleted IS NULL OR"
          + " pr.deleted = false)")
  List<PointsRule> findByIsEnabled(@Param("isEnabled") Boolean isEnabled);
}
