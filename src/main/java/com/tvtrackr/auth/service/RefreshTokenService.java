package com.tvtrackr.auth.service;

import com.tvtrackr.auth.entity.RefreshToken;
import com.tvtrackr.auth.entity.User;
import java.time.LocalDateTime;

public interface RefreshTokenService {

  /**
   * Generates a new refresh token, persists it, and associates it with the given user.
   *
   * @param user the user to generate the token for
   * @return the persisted {@link RefreshToken}
   */
  RefreshToken generateAndSaveRefreshToken(User user);

  /**
   * Finds a refresh token by its raw string value.
   *
   * @param token the raw refresh token string
   * @return the matching {@link RefreshToken}
   */
  RefreshToken find(String token);

  /**
   * Finds a refresh token by its raw string value using a pessimistic write lock
   * ({@code SELECT FOR UPDATE}).
   *
   * <p>Must be called within an active transaction. Used during refresh and logout flows to
   * prevent race conditions where concurrent requests could consume or revoke the same token
   * simultaneously.
   *
   * @param tokenStr the raw refresh token string
   * @return the locked {@link RefreshToken}
   */
  RefreshToken findForUpdate(String tokenStr);

  /**
   * Persists the given refresh token.
   *
   * @param refreshToken the token to save
   * @return the saved {@link RefreshToken}
   */
  RefreshToken save(RefreshToken refreshToken);

  /**
   * Marks the given refresh token as revoked.
   *
   * @param refreshToken the token to revoke
   * @return the updated {@link RefreshToken}
   */
  RefreshToken revoke(RefreshToken refreshToken);

  /**
   * Revokes all refresh tokens belonging to the given user.
   *
   * <p>Used after a successful password reset to force re-authentication across all sessions.
   *
   * @param userId the internal user ID
   */
  void revokeAllByUserId(Long userId);

  /**
   * Bulk deletes all refresh tokens that are revoked or have expired before the given cutoff.
   *
   * <p>Uses a bulk {@code @Modifying @Query} delete — tokens are not loaded into memory.
   * Called by the scheduled cleanup job.
   *
   * @param cutoff tokens expiring before this timestamp are eligible for deletion
   * @return the number of tokens deleted
   */
  int deleteExpiredOrRevoked(LocalDateTime cutoff);
}
