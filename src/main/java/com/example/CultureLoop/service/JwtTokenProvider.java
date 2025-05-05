package com.example.CultureLoop.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long EXPIRATION = 1000 * 60 * 60 * 24; // 1일

    public String createToken(String uid, String email) {
        Claims claims = Jwts.claims().setSubject(uid);
        claims.put("email", email);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String uid = claims.getSubject();
        String email = (String) claims.get("email");

        UserDetails user = User.builder()
                .username(email)
                .password("") // 비밀번호는 사용 안 함
                .authorities("ROLE_USER")
                .build();

        return new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());
    }
}

