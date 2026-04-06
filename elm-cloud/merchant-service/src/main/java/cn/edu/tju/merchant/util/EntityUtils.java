package cn.edu.tju.merchant.util;

import cn.edu.tju.merchant.model.*;
import java.time.LocalDateTime;
import java.util.function.Predicate;
import java.util.List;
import java.util.ArrayList;

public class EntityUtils {
    public static void setNewEntity(BaseEntity entity) {
        if (entity == null) return;
        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(now);
        }
        entity.setUpdateTime(now);
        if (entity.getDeleted() == null) {
            entity.setDeleted(false);
        }
    }

    public static void updateEntity(BaseEntity entity) {
        if (entity == null) return;
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getDeleted() == null) {
            entity.setDeleted(false);
        }
    }

    public static void deleteEntity(BaseEntity entity) {
        if (entity == null) return;
        entity.setDeleted(true);
        entity.setUpdateTime(LocalDateTime.now());
    }
    public static void substituteEntity(BaseEntity oldEntity, BaseEntity newEntity) {}
    /**
     * 兼容软删除：仅保留 deleted != true 的记录（含 deleted=null）。
     */
    public static <T> Predicate<T> filterEntity() {
        return e -> {
            if (e instanceof BaseEntity be) {
                return be.getDeleted() == null || !be.getDeleted();
            }
            return true;
        };
    }

    public static <T> List<T> filterEntityList(List<T> list) {
        if (list == null) return new ArrayList<>();
        List<T> ret = new ArrayList<>(list.size());
        for (T e : list) {
            if (filterEntity().test(e)) {
                ret.add(e);
            }
        }
        return ret;
    }
}
