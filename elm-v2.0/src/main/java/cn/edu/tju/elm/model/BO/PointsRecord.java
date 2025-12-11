package cn.edu.tju.elm.model.BO;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.utils.EntityUtils;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "points_record")
public class PointsRecord extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "biz_id", length = 100)
    private String bizId;

    @Column(name = "channel_type", length = 50)
    private String channelType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(LocalDateTime recordTime) {
        this.recordTime = recordTime;
    }

    public static PointsRecord createRecord(User user, String type, Integer points, String bizId,
                                            String channelType, String description) {
        PointsRecord record = new PointsRecord();
        record.setUser(user);
        record.setType(type);
        record.setPoints(points);
        record.setBizId(bizId);
        record.setChannelType(channelType);
        record.setDescription(description);
        record.setRecordTime(LocalDateTime.now());
        EntityUtils.setNewEntity(record);
        return record;
    }
}
