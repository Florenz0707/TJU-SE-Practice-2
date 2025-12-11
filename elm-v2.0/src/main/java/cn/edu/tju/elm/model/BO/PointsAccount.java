package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.utils.EntityUtils;
import jakarta.persistence.*;

@Entity
@Table(name = "points_account")
public class PointsAccount extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Column(name = "frozen_points", nullable = false)
    private Integer frozenPoints = 0;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public static PointsAccount createNewAccount(User user) {
        PointsAccount account = new PointsAccount();
        account.setUser(user);
        account.setTotalPoints(0);
        account.setFrozenPoints(0);
        EntityUtils.setNewEntity(account);
        return account;
    }
}
