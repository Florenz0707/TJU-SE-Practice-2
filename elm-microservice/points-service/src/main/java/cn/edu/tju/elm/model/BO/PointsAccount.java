package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.elm.utils.EntityUtils;
import jakarta.persistence.*;

@Entity
@Table(name = "points_account")
public class PointsAccount extends BaseEntity {
  @Column(name = "user_id", nullable = false, unique = true)
  private Long userId;

  @Column(name = "total_points", nullable = false)
  private Integer totalPoints = 0;

  @Column(name = "frozen_points", nullable = false)
  private Integer frozenPoints = 0;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Integer getTotalPoints() {
    return totalPoints;
  }

  public void setTotalPoints(Integer totalPoints) {
    this.totalPoints = totalPoints;
  }

  public Integer getFrozenPoints() {
    return frozenPoints;
  }

  public void setFrozenPoints(Integer frozenPoints) {
    this.frozenPoints = frozenPoints;
  }

  public Integer getAvailablePoints() {
    return totalPoints - frozenPoints;
  }

  public void addPoints(Integer points) {
    if (points != null && points > 0) {
      this.totalPoints += points;
    }
  }

  public void freezePoints(Integer points) {
    if (points != null && points > 0) {
      this.frozenPoints += points;
    }
  }

  public void unfreezePoints(Integer points) {
    if (points != null && points > 0 && this.frozenPoints >= points) {
      this.frozenPoints -= points;
    }
  }

  public void deductPoints(Integer points) {
    if (points != null && points > 0) {
      if (this.frozenPoints >= points) {
        this.frozenPoints -= points;
      }
      this.totalPoints -= points;
    }
  }

  public void expirePoints(Integer points) {
    if (points != null && points > 0) {
      this.totalPoints -= points;
    }
  }

  public static PointsAccount createNewAccount(Long userId) {
    PointsAccount account = new PointsAccount();
    account.setUserId(userId);
    account.setTotalPoints(0);
    account.setFrozenPoints(0);
    EntityUtils.setNewEntity(account);
    return account;
  }
}
