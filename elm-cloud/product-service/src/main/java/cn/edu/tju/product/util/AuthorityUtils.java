package cn.edu.tju.product.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AuthorityUtils {

    private AuthorityUtils() {}

    public static Set<String> getAuthoritySet(String authorities) {
        if (authorities == null || authorities.isBlank()) return Collections.emptySet();

        // supports:
        // - "BUSINESS,USER" (comma)
        // - "ROLE_ADMIN ROLE_USER" (space)
        // - "[BUSINESS, USER]" (toString of list)
        String normalized = authorities.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.replace("ROLE_", "");

        String[] parts = normalized.split("[,\\s]+");
        Set<String> set = new HashSet<>();
        Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(set::add);
        return set;
    }

    public static boolean hasAuthority(Set<String> authorities, String authority) {
        return authorities != null && authority != null && authorities.contains(authority);
    }
}
