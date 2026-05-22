package com.tvtrackr.auth.service;

import java.util.Optional;

public interface TokenCacheService {

  Optional<Long> getAndDeletePasswordResetToken(String token);

  Optional<Long> getAndDeleteEmailVerificationToken(String token);

  void savePasswordResetToken(String token, Long userId);

  void saveEmailVerificationToken(String token, Long userId);
}
