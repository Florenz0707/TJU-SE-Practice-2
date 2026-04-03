package cn.edu.tju.merchant.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Key;

@Component
public class JwtUtils {

    @Value("${jwt.base64-secret:ZmQ0ZGI5NjQ0MDQwY2I4MjMxY2Y3ZmI3MjdhN2ZmMjNhODViOTg1ZGE0NTBjMGM4NDA5NzYxMjdjOWMwYWRmZTBlZjlhNGY3ZTg4Y2U3YTE1ODVkZDU5Y2Y3OGYwZWE1NzUzNWQ2YjFjZDc0NGMxZWU2MmQ3MjY1NzJmNTE0MzI=}")
    private String base64Secret;

    public String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    public Claims getClaims(String token) {
        if(token == null || token.isEmpty()) return null;
        try {
            byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
            Key key = Keys.hmacShaKeyFor(keyBytes);
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch(Exception e) { 
            return null; 
        }
    }

    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        if (claims != null && claims.get("uid") != null) {
            return Long.valueOf(claims.get("uid").toString());
        }
        if (claims != null && claims.get("userId") != null) {
            return Long.valueOf(claims.get("userId").toString());
        }
        if (claims != null && claims.get("id") != null) {
            return Long.valueOf(claims.get("id").toString());
        }
        return null;
    }

    public String getUsername(String token) {
        Claims claims = getClaims(token);
        if (claims != null) {
            return claims.getSubject();
        }
        return null;
    }

    public String getAuthorities(String token) {
        Claims claims = getClaims(token);
        if (claims != null && claims.get("auth") != null) {
            return claims.get("auth").toString();
        }
        return "";
    }
}
