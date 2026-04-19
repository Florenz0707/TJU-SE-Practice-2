package cn.edu.tju.points.model.VO;

import cn.edu.tju.points.model.BO.PointsAccount;

public class PointsAccountVO {
  private Long id;
  private Long userId;
  private Integer totalPoints;
  private Integer frozenPoints;
  private Integer availablePoints;

  public PointsAccountVO() {}

  public PointsAccountVO(PointsAccount account) {
    if (account != null) {
      this.id = account.getId();
      this.userId = account.getUserId();
      this.totalPoints = account.getTotalPoints();
      this.frozenPoints = account.getFrozenPoints();
      this.availablePoints = account.getAvailablePoints();
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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
    return availablePoints;
  }

  public void setAvailablePoints(Integer availablePoints) {
    this.availablePoints = availablePoints;
  }

  @Override
  public String toString() {
    return "PointsAccountVO{" +
        "id=" + id +
        ", userId=" + userId +
        ", totalPoints=" + totalPoints +
        ", frozenPoints=" + frozenPoints +
        ", availablePoints=" + availablePoints +
        '}';
  }
}
