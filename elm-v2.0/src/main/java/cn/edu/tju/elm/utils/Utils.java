package cn.edu.tju.elm.utils;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.BaseEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {
    public static <T extends BaseEntity> List<T> removeDeleted(List<T> list) {
        List<T> newList = new ArrayList<>(list.size());
        for (T t : list) {
            if (!t.getDeleted()) newList.add(t);
        }
        return newList;
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
