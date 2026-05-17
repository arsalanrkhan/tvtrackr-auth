package com.tvtrackr.auth.service.impl;

import com.tvtrackr.auth.config.JwtProperties;
import com.tvtrackr.auth.entity.User;
import com.tvtrackr.auth.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

  private final JwtProperties jwtProperties;
  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  public String generateAccessToken(User user) {
    return Jwts.builder()
        .subject(user.getUuid().toString())
        .claim("email", user.getEmail())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiryMs()))
        .signWith(jwtProperties.getRsaPrivateKey())
        .compact();
  }

  @Override
  public String generateRefreshToken() {
    byte[] bytes = new byte[64];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  @Override
  public boolean validateAccessToken(String token) {
    try {
      extractClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public Claims extractClaims(String token) {
    return Jwts.parser()
        .verifyWith(jwtProperties.getRsaPublicKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  @Override
  public UUID extractUserUuid(String token) {
    return UUID.fromString(extractClaims(token).getSubject());
  }
}
