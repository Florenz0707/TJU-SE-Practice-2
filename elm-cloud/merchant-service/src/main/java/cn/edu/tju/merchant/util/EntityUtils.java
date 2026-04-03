package cn.edu.tju.merchant.util;

import cn.edu.tju.merchant.model.*;
import java.util.function.Predicate;
import java.util.List;
import java.util.ArrayList;

public class EntityUtils {
    public static void setNewEntity(BaseEntity entity) {}
    public static void updateEntity(BaseEntity entity) {}
    public static void deleteEntity(BaseEntity entity) { entity.setDeleted(true); }
    public static void substituteEntity(BaseEntity oldEntity, BaseEntity newEntity) {}
    public static <T> Predicate<T> filterEntity() { return e -> true; }
    public static <T> List<T> filterEntityList(List<T> list) { return list == null ? new ArrayList<T>() : list; }
}
