package com.tvtrackr.auth.service;

import com.tvtrackr.auth.dto.req.*;
import com.tvtrackr.auth.dto.res.AuthResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

  /**
   * Registers a new user account.
   *
   * <p>Validates the request, persists the user and their LOCAL auth provider, issues an
   * access token and refresh token cookie, adds the username to the distributed Bloom filter,
   * and publishes an email verification event to Kafka.
   *
   * @param request  registration payload
   * @param response used to set the {@code refreshToken} HttpOnly cookie
   * @return access token and user details
   */
  AuthResponse register(RegisterRequest request, HttpServletResponse response);

  /**
   * Authenticates a user by email or username and password.
   *
   * <p>Issues a new access token and refresh token cookie on success.
   *
   * @param request  login payload (emailOrUsername, password)
   * @param response used to set the {@code refreshToken} HttpOnly cookie
   * @return access token and user details
   */
  AuthResponse login(LoginRequest request, HttpServletResponse response);

  /**
   * Revokes the given refresh token and clears the refresh token cookie.
   *
   * <p>Uses a pessimistic write lock ({@code SELECT FOR UPDATE}) on the token to prevent
   * race conditions between concurrent logout and refresh requests.
   *
   * @param refreshToken the raw refresh token string extracted from the cookie
   * @param response     used to clear the {@code refreshToken} cookie
   */
  void logout(String refreshToken, HttpServletResponse response);

  /**
   * Issues a new access token and rotates the refresh token.
   *
   * <p>The existing refresh token is revoked and a new one is issued atomically under a
   * pessimistic write lock to prevent duplicate token issuance on concurrent requests.
   *
   * @param refreshToken the raw refresh token string extracted from the cookie
   * @param response     used to set the new {@code refreshToken} cookie
   * @return new access token and user details
   */
  AuthResponse refresh(String refreshToken, HttpServletResponse response);

  /**
   * Initiates a password reset for the given email address.
   *
   * <p>Generates a one-time token, stores it in Redis with a 15-minute TTL, and publishes
   * a password reset email event to Kafka. A 60-second cooldown is enforced per user.
   * Always returns silently if the email is not found to prevent user enumeration attacks.
   *
   * @param request password reset request payload (email)
   */
  void forgotPassword(ForgotPasswordRequest request);

  /**
   * Completes a password reset using a one-time token.
   *
   * <p>The token is consumed atomically via Redis {@code GETDEL} — preventing replay attacks
   * even under concurrent requests. On success, all existing refresh tokens for the user are
   * revoked, forcing re-authentication.
   *
   * @param request reset payload (token, newPassword)
   */
  void resetPassword(ResetPasswordRequest request);

  /**
   * Verifies a user's email address using a one-time token.
   *
   * <p>The token is consumed atomically via Redis {@code GETDEL}. Sets {@code emailVerified=true}
   * on the user and returns a fresh access token with the updated claim so gated features
   * unlock immediately without requiring a new login.
   *
   * @param token one-time email verification token
   * @return fresh access token with {@code emailVerified=true} claim
   */
  AuthResponse verifyEmail(String token);

  /**
   * Resends the email verification email.
   *
   * <p>Silently returns if the email is not found or is already verified, to prevent user
   * enumeration. A 60-second cooldown is enforced per user. Publishes the verification email
   * event to Kafka.
   *
   * @param request resend request payload (email)
   */
  void resendVerificationEmail(ResendVerificationEmailRequest request);

  /**
   * Checks whether a username is available for registration.
   *
   * <p>Uses a two-tier lookup:
   * <ol>
   *   <li>Bloom filter (Redis bitmap, O(1)) — definite negatives bypass the database entirely.</li>
   *   <li>B+ tree index (PostgreSQL, O(log n)) — reached only on probable positives to resolve
   *       false positives definitively.</li>
   * </ol>
   *
   * @param username the username to check
   * @return {@code true} if the username is available, {@code false} if taken
   */
  boolean usernameAvailability(String username);
}
