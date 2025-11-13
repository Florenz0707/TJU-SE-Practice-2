package cn.edu.tju.elm.utils;

import cn.edu.tju.core.model.BaseEntity;

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

    public static <T extends BaseEntity> void substituteEntity(T oldEntity, T newEntity) {
        LocalDateTime now = LocalDateTime.now();
        oldEntity.setDeleted(true);
        oldEntity.setUpdateTime(now);

        newEntity.setDeleted(false);
        newEntity.setCreateTime(oldEntity.getCreateTime());
        newEntity.setUpdateTime(now);
    }

    public static <T extends BaseEntity> void setNewEntity(T entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setDeleted(false);
    }

    public static <T extends BaseEntity> void deleteEntity(T entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setUpdateTime(now);
        entity.setDeleted(true);
    }

    public static <T extends BaseEntity> void updateEntity(T entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setUpdateTime(now);
    }
}
