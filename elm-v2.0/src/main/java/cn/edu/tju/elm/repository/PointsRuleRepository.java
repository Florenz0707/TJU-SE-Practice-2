package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.PointsRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointsRuleRepository extends JpaRepository<PointsRule, Long> {
    List<PointsRule> findByChannelTypeAndIsEnabled(String channelType, Boolean isEnabled);

    List<PointsRule> findByIsEnabled(Boolean isEnabled);
}
