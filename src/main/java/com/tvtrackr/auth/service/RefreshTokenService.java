package com.tvtrackr.auth.service;

import com.tvtrackr.auth.entity.RefreshToken;
import com.tvtrackr.auth.entity.User;

public interface RefreshTokenService {

  RefreshToken generateAndSaveRefreshToken(User user);

  RefreshToken find(String token);

  RefreshToken save(RefreshToken refreshToken);

  RefreshToken revoke(RefreshToken refreshToken);

  void revokeAllByUserId(Long userId);
}
