package cn.edu.tju.elm.model.VO;

import cn.edu.tju.elm.model.BO.PointsRule;

public class PointsRuleVO {
    private Long id;
    private String channelType;
    private Double ratio;
    private Integer expireDays;
    private String description;
    private Boolean isEnabled;
    private Long creator;
    private Long updater;

    public PointsRuleVO() {
    }

    public PointsRuleVO(PointsRule rule) {
        if (rule != null) {
            this.id = rule.getId();
            this.channelType = rule.getChannelType();
            this.ratio = rule.getRatio();
            this.expireDays = rule.getExpireDays();
            this.description = rule.getDescription();
            this.isEnabled = rule.getIsEnabled();
            this.creator = rule.getCreator();
            this.updater = rule.getUpdater();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public Double getRatio() {
        return ratio;
    }

    public void setRatio(Double ratio) {
        this.ratio = ratio;
    }

    public Integer getExpireDays() {
        return expireDays;
    }

    public void setExpireDays(Integer expireDays) {
        this.expireDays = expireDays;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public Long getCreator() {
        return creator;
    }

    public void setCreator(Long creator) {
        this.creator = creator;
    }

    public Long getUpdater() {
        return updater;
    }

    public void setUpdater(Long updater) {
        this.updater = updater;
    }
}
