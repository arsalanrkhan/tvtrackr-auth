package com.tvtrackr.auth.service;

import java.util.Optional;

public interface TokenCacheService {

  /**
   * Atomically retrieves and deletes the user ID associated with a password reset token.
   *
   * <p>Uses Redis {@code GETDEL} — the delete is the gate, not cleanup. This ensures the token
   * can only be consumed once even under concurrent requests, preventing replay attacks.
   *
   * @param token the one-time password reset token
   * @return the associated user ID, or empty if the token does not exist or has expired
   */
  Optional<Long> getAndDeletePasswordResetToken(String token);

  /**
   * Atomically retrieves and deletes the user ID associated with an email verification token.
   *
   * <p>Uses Redis {@code GETDEL} — the delete is the gate, not cleanup. This ensures the token
   * can only be consumed once even under concurrent requests, preventing replay attacks.
   *
   * @param token the one-time email verification token
   * @return the associated user ID, or empty if the token does not exist or has expired
   */
  Optional<Long> getAndDeleteEmailVerificationToken(String token);

  /**
   * Stores a password reset token in Redis mapped to the given user ID.
   *
   * <p>Key format: {@code passwordReset:{token}}. TTL: 15 minutes.
   *
   * @param token  the one-time password reset token
   * @param userId the internal user ID to associate with the token
   */
  void savePasswordResetToken(String token, Long userId);

  /**
   * Stores an email verification token in Redis mapped to the given user ID.
   *
   * <p>Key format: {@code emailVerification:{token}}. TTL: 15 minutes.
   *
   * @param token  the one-time email verification token
   * @param userId the internal user ID to associate with the token
   */
  void saveEmailVerificationToken(String token, Long userId);
}
