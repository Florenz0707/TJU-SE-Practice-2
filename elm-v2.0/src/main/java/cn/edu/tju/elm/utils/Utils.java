package cn.edu.tju.elm.utils;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.BaseEntity;
import cn.edu.tju.core.model.User;

import java.time.LocalDateTime;
import java.util.*;

public class Utils {
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

    public static Set<Authority> getAuthoritySet(String authorities) {
        String[] authorityList = authorities.split(" ");
        Set<Authority> authoritySet = new HashSet<>();
        for (String string : authorityList) {
            if (string.equals("ADMIN") || string.equals("BUSINESS") || string.equals("USER")) {
                Authority authority = new Authority();
                authority.setName(string);
                authoritySet.add(authority);
            }
        }
        return authoritySet;
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

    public static boolean hasAuthority(User user, String name) {
        for (Authority authority : user.getAuthorities()) {
            if (authority.getName().equals(name)) return true;
        }
        return false;
    }
}
