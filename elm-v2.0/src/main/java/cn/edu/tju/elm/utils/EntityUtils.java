package cn.edu.tju.elm.utils;

import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.core.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EntityUtils {
    public static <T extends BaseEntity> List<T> filterEntityList(List<T> entityList) {
        List<T> list = new ArrayList<>(entityList.size());
        for (T entity : entityList) {
            if (!entity.getDeleted()) list.add(entity);
        }
        return list;
    }

    public static <T extends BaseEntity> T filterEntity(T entity) {
        if (entity != null && !entity.getDeleted()) return entity;
        return null;
    }

    public static <T extends BaseEntity> void substituteEntity(T oldEntity, T newEntity, User updater) {
        LocalDateTime now = LocalDateTime.now();
        oldEntity.setDeleted(true);
        oldEntity.setUpdater(updater.getId());
        oldEntity.setUpdateTime(now);

        newEntity.setDeleted(false);
        newEntity.setCreateTime(oldEntity.getCreateTime());
        newEntity.setCreator(oldEntity.getCreator());
        newEntity.setUpdateTime(now);
        newEntity.setUpdater(updater.getId());
    }

    public static <T extends BaseEntity> void setNewEntity(T entity, User creator) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setCreator(creator.getId());
        entity.setUpdater(creator.getId());
        entity.setDeleted(false);
    }

    public static <T extends BaseEntity> void deleteEntity(T entity, User deleter) {
        LocalDateTime now = LocalDateTime.now();
        entity.setUpdateTime(now);
        entity.setUpdater(deleter.getId());
        entity.setDeleted(true);
    }

    public static <T extends BaseEntity> void updateEntity(T entity, User updater) {
        LocalDateTime now = LocalDateTime.now();
        entity.setUpdateTime(now);
        entity.setUpdater(updater.getId());
    }
}
