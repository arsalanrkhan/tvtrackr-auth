package com.tvtrackr.auth.service;

import com.tvtrackr.auth.entity.RefreshToken;
import com.tvtrackr.auth.entity.User;
import java.time.LocalDateTime;

public interface RefreshTokenService {

  RefreshToken generateAndSaveRefreshToken(User user);

  RefreshToken find(String token);

  RefreshToken findForUpdate(String tokenStr);

  RefreshToken save(RefreshToken refreshToken);

  RefreshToken revoke(RefreshToken refreshToken);

  void revokeAllByUserId(Long userId);

  int deleteExpiredOrRevoked(LocalDateTime cutoff);
}
