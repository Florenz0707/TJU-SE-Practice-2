package cn.edu.tju.elm.model.VO;

import cn.edu.tju.elm.model.BO.PointsRecord;

import java.time.LocalDateTime;

public class PointsRecordVO {
    private Long id;
    private Long userId;
    private String type;
    private Integer points;
    private String bizId;
    private String channelType;
    private String description;
    private LocalDateTime recordTime;

    public PointsRecordVO() {
    }

    public PointsRecordVO(PointsRecord record) {
        if (record != null) {
            this.id = record.getId();
            this.userId = record.getUser() != null ? record.getUser().getId() : null;
            this.type = record.getType();
            this.points = record.getPoints();
            this.bizId = record.getBizId();
            this.channelType = record.getChannelType();
            this.description = record.getDescription();
            this.recordTime = record.getRecordTime();
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
}
