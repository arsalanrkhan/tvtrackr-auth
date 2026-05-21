package com.tvtrackr.auth.service.impl;

import static com.tvtrackr.auth.exception.AuthErrors.INVALID_REFRESH_TOKEN;

import com.tvtrackr.auth.entity.RefreshToken;
import com.tvtrackr.auth.entity.User;
import com.tvtrackr.auth.repository.RefreshTokenRepository;
import com.tvtrackr.auth.service.RefreshTokenService;
import com.tvtrackr.auth.service.TokenService;
import com.tvtrackr.common.error.BusinessException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final TokenService tokenService;

  @Value("${app.refresh-token.expiry-days}")
  private Long refreshTokenExpiryDays;

  @Override
  public RefreshToken generateAndSaveRefreshToken(User user) {
    String token = tokenService.generateRefreshToken();
    RefreshToken refreshToken =
        new RefreshToken()
            .setToken(token)
            .setUser(user)
            .setExpiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays));
    return refreshTokenRepository.save(refreshToken);
  }

  @Override
  public RefreshToken find(String tokenStr) {
    return refreshTokenRepository
        .findByToken(tokenStr)
        .orElseThrow(() -> new BusinessException(INVALID_REFRESH_TOKEN));
  }

  @Override
  public RefreshToken save(RefreshToken refreshToken) {
    return refreshTokenRepository.save(refreshToken);
  }

  @Override
  public RefreshToken revoke(RefreshToken refreshToken) {
    refreshToken.setRevoked(true);
    return refreshTokenRepository.save(refreshToken);
  }
}
