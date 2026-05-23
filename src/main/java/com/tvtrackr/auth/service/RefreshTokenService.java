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
   * Finds a refresh token by its raw string value using a pessimistic write lock ({@code SELECT FOR
   * UPDATE}).
   *
   * <p>Must be called within an active transaction. Used during refresh and logout flows to prevent
   * race conditions where concurrent requests could consume or revoke the same token
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
   * Deletes a chunk of refresh tokens that are revoked or have expired before the given cutoff.
   *
   * <p>Uses a native chunked delete limited by {@code chunkSize} — call in a loop until 0 is
   * returned. Each call runs in its own transaction ({@code REQUIRES_NEW}) to release row locks
   * between chunks.
   *
   * @param cutoff tokens expiring before this timestamp are eligible for deletion
   * @param chunkSize maximum number of tokens to delete per call
   * @return the number of tokens deleted in this chunk
   */
  int deleteExpiredOrRevoked(LocalDateTime cutoff, int chunkSize);
}
