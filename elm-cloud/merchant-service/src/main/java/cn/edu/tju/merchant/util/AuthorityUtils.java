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
            // token claim examples we need to support:
            // - "BUSINESS,USER" (comma separated)
            // - "ROLE_ADMIN ROLE_USER" (space separated)
            // Support both comma and whitespace.
            for (String auth : authority.split("[\\s,]+")) {
                if (auth == null) continue;
                String a = auth.trim();
                if (!a.isEmpty()) set.add(a);
            }
        }
        return set;
    }
}