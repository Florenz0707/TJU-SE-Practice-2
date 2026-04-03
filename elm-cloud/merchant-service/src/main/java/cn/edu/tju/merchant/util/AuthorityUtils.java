package cn.edu.tju.merchant.util;

import cn.edu.tju.merchant.model.User;
import java.util.Set;
import java.util.HashSet;

public class AuthorityUtils {
    public static boolean userHasAuthority(User user, String authority) {
        if (user == null || user.getAuthorities() == null || authority == null) return false;
        for (String auth : user.getAuthorities()) {
            if (auth.equalsIgnoreCase(authority) || 
                auth.equalsIgnoreCase("ROLE_" + authority) || 
                ("ROLE_" + auth).equalsIgnoreCase(authority)) {
                return true;
            }
        }
        return false;
    }
    public static boolean hasAuthority(User user, String authority) {
        return userHasAuthority(user, authority);
    }
    public static Set<String> getAuthoritySet(String authority) {
        Set<String> set = new HashSet<>();
        if (authority != null) {
            for (String auth : authority.split(" ")) {
                set.add(auth.trim());
            }
        }
        return set;
    }
}