package cn.edu.tju.elm.utils;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.User;

import java.util.HashSet;
import java.util.Set;

public class AuthorityUtils {
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

    public static boolean hasAuthority(User user, String name) {
        for (Authority authority : user.getAuthorities()) {
            if (authority.getName().equals(name)) return true;
        }
        return false;
    }
}
