package com.tvtrackr.auth.service;

import com.tvtrackr.auth.entity.User;
import io.jsonwebtoken.Claims;

import java.util.UUID;

public interface TokenService {

    String generateAccessToken(User user);

    String generateRefreshToken();

    boolean validateAccessToken(String token);

    Claims extractClaims(String token);

    UUID extractUserUuid(String token);
}
