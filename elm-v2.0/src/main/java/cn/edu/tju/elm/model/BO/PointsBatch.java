package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.utils.EntityUtils;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "points_batch")
public class PointsBatch extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "frozen_points", nullable = false)
    private Integer frozenPoints = 0;

    @Column(name = "available_points", nullable = false)
    private Integer availablePoints;

    @ManyToOne
    @JoinColumn(name = "record_id", nullable = false)
    private PointsRecord record;

    @Column(name = "temp_order_id", length = 100)
    private String tempOrderId;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
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

    public PointsRecord getRecord() {
        return record;
    }

    public void setRecord(PointsRecord record) {
        this.record = record;
    }

    public String getTempOrderId() {
        return tempOrderId;
    }

    public void setTempOrderId(String tempOrderId) {
        this.tempOrderId = tempOrderId;
    }

    public void freezePoints(Integer points) {
        if (points != null && points > 0 && this.availablePoints >= points) {
            this.frozenPoints += points;
            this.availablePoints -= points;
        }
    }

    public void unfreezePoints(Integer points) {
        if (points != null && points > 0 && this.frozenPoints >= points) {
            this.frozenPoints -= points;
            this.availablePoints += points;
        }
    }

    public void deductPoints(Integer points) {
        if (points != null && points > 0 && this.frozenPoints >= points) {
            this.frozenPoints -= points;
            this.points -= points;
        }
    }

    public static PointsBatch createBatch(User user, Integer points, LocalDateTime expireTime, PointsRecord record) {
        PointsBatch batch = new PointsBatch();
        batch.setUser(user);
        batch.setPoints(points);
        batch.setExpireTime(expireTime);
        batch.setRecord(record);
        batch.setFrozenPoints(0);
        batch.setAvailablePoints(points);
        EntityUtils.setNewEntity(batch);
        return batch;
    }
}
