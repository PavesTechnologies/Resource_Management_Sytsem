package com.security;

import org.springframework.stereotype.Component;

@Component
public class JwtUtils {
//
//    // ⚠️ use same secret as in your authentication flow
//    private final String SECRET_KEY = "replace_with_a_very_strong_secret_key_atleast_32chars";
//
//    private Key getSigningKey() {
//        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
//    }
//
//    public Claims extractAllClaims(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(getSigningKey())
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//    }
//
//    public String extractUserId(String token) {
//        try {
//            Claims claims = extractAllClaims(token);
//            // assuming you store userId in token like: claims.put("userId", user.getId());
//            return claims.get("userId", String.class);
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    public String extractUsername(String token) {
//        try {
//            return extractAllClaims(token).getSubject();
//        } catch (Exception e) {
//            return null;
//        }
//    }
}