package cn.edu.tju.elm.utils;

import cn.edu.tju.core.model.BaseEntity;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static <T extends BaseEntity> List<T> removeDeleted(List<T> list) {
        List<T> newList = new ArrayList<>(list.size());
        for (T t : list) {
            if (!t.getDeleted()) newList.add(t);
        }
        return newList;
    }
}
