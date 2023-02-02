package com.group.jjabflix.config.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final String BEARER_TYPE = "Bearer";
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24L; // 24시간

    private final Key key;
    private final Logger LOGGER = LoggerFactory.getLogger(JwtProvider.class);


    public JwtProvider(@Value("${jwt.secret.key}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 유저 정보를 가지고 AccessToken 을 생성하는 메서드
    public TokenInfoResponse generateToken (Authentication authentication) {
        // 권한 가져오기 (ROLE)
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();

        // Access Token 생성
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName()) // payload.sub: 토큰의 주인을 판별하는 식별자. 여기서는 email
                .claim(AUTHORITIES_KEY, authorities) // auth: 직접 작성한 claim. 여기서는 ROLE 정보
                .setExpiration(accessTokenExpiresIn) // payload.exp: 토큰이 만료되는 시간
                .signWith(key, SignatureAlgorithm.HS256) // header.alg: 서명 시 사용하는 알고리즘
                .compact();

        return TokenInfoResponse.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .accessTokenExpirationTime(ACCESS_TOKEN_EXPIRE_TIME)
                .build();
    }

    // JWT 를 복호화하여 토큰에 들어있는 정보를 꺼내는 메서드
    public Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // UserDetails 객체를 만들어서 Authentication 리턴
        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // 토큰 정보를 검증하는 메서드
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            LOGGER.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            LOGGER.info("만료된 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            LOGGER.info("지원되지 않는 토큰입니다.");
        } catch (IllegalArgumentException e) {
            LOGGER.info("JWT claims string is empty.");
        }
        return false;
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    // access 토큰 남은 시간 확인
    public Long getExpiration(String accessToken) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(accessToken)
                .getBody()
                .getExpiration();

        Long now = new Date().getTime();
        return (expiration.getTime() - now);
    }

}
