package com.tvtrackr.auth.service;

import java.util.Optional;

public interface TokenCacheService {

  Optional<Long> getAndDeletePasswordResetToken(String token);

  Optional<Long> getAndDeleteEmailVerificationToken(String token);

  void savePasswordResetToken(String token, Long userId);

  Optional<Long> getPasswordResetToken(String token);

  void deletePasswordResetToken(String token);

  void saveEmailVerificationToken(String token, Long userId);

  Optional<Long> getEmailVerificationToken(String token);

  void deleteEmailVerificationToken(String token);
}
