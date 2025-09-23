package cn.edu.tju.elm.utils;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.BaseEntity;

import java.util.*;

public class Utils {
    public static <T extends BaseEntity> List<T> checkEntityList(List<T> list) {
        List<T> newList = new ArrayList<>(list.size());
        for (T t : list) {
            if (t.getDeleted() && ! t.getReferred()) continue;
            newList.add(t);
        }
        return newList;
    }

    public static <T extends BaseEntity> T checkEntity(T t) {
        if (t.getDeleted() && !t.getReferred()) return null;
        return t;
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
}
