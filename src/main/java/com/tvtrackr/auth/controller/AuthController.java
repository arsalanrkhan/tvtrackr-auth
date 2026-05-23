package com.tvtrackr.auth.controller;

import com.tvtrackr.auth.dto.req.*;
import com.tvtrackr.auth.dto.res.AuthResponse;
import com.tvtrackr.auth.dto.res.MessageResponse;
import com.tvtrackr.auth.dto.res.UsernameAvailabilityResponse;
import com.tvtrackr.auth.exception.AuthErrors;
import com.tvtrackr.auth.service.AuthService;
import com.tvtrackr.common.error.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;

/**
 * REST controller for all authentication and account management endpoints.
 *
 * <p>Base path: {@code /api/auth/v1}
 *
 * <p>All endpoints are public (no JWT required). Downstream services receive the authenticated
 * user's identity via the {@code X-User-Id} header forwarded by the API gateway after token
 * validation.
 */
@Tag(
    name = "Authentication",
    description = "User registration, login, token management, and account verification endpoints.")
@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
@Validated
public class AuthController {

  private final AuthService authService;

  /**
   * Registers a new user account.
   *
   * <p>On success: persists the user, issues a short-lived access token in the response body, sets
   * a long-lived refresh token as an {@code HttpOnly} cookie, adds the username to the distributed
   * Bloom filter, and publishes an email verification event to Kafka.
   *
   * @param request registration payload (email, username, displayName, password)
   * @param response used to set the {@code refreshToken} cookie
   * @return {@link AuthResponse} containing the access token and user details
   */
  @Operation(
      summary = "Register a new user",
      description =
          "Creates a new user account, issues an access token and refresh token cookie, "
              + "and publishes an email verification event.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Registration successful"),
    @ApiResponse(responseCode = "400", description = "Validation failed"),
    @ApiResponse(responseCode = "409", description = "Email or username already exists")
  })
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> registerUser(
      @RequestBody @Valid RegisterRequest request, HttpServletResponse response) {
    return ResponseEntity.ok(authService.register(request, response));
  }

  /**
   * Authenticates an existing user.
   *
   * <p>Accepts either an email address or username as the identifier. On success, issues a
   * short-lived access token in the response body and sets a long-lived refresh token as an {@code
   * HttpOnly} cookie.
   *
   * @param request login payload (emailOrUsername, password)
   * @param response used to set the {@code refreshToken} cookie
   * @return {@link AuthResponse} containing the access token and user details
   */
  @Operation(
      summary = "Log in",
      description =
          "Authenticates a user by email or username and password. "
              + "Returns an access token and sets a refresh token cookie.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Login successful"),
    @ApiResponse(responseCode = "400", description = "Validation failed"),
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
  })
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(
      @RequestBody @Valid LoginRequest request, HttpServletResponse response) {
    return ResponseEntity.ok(authService.login(request, response));
  }

  /**
   * Logs out the current user.
   *
   * <p>Revokes the refresh token extracted from the {@code refreshToken} cookie and clears the
   * cookie from the response. The access token remains valid until it naturally expires —
   * revocation is enforced at the gateway level via token expiry.
   *
   * @param request used to extract the {@code refreshToken} cookie
   * @param response used to clear the {@code refreshToken} cookie
   * @return confirmation message
   */
  @Operation(
      summary = "Log out",
      description =
          "Revokes the refresh token from the cookie and clears it. "
              + "The access token expires naturally after 15 minutes.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Logout successful"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid refresh token cookie")
  })
  @PostMapping("/logout")
  public ResponseEntity<MessageResponse> logout(
      HttpServletRequest request, HttpServletResponse response) {
    authService.logout(extractRefreshToken(request), response);
    return ResponseEntity.ok(new MessageResponse().setMessage("Logout successful"));
  }

  /**
   * Issues a new access token using a valid refresh token.
   *
   * <p>Implements refresh token rotation — the existing refresh token is revoked and a new one is
   * issued. A pessimistic write lock prevents duplicate token issuance on concurrent refresh
   * requests for the same token.
   *
   * @param request used to extract the {@code refreshToken} cookie
   * @param response used to set the new {@code refreshToken} cookie
   * @return {@link AuthResponse} containing the new access token and user details
   */
  @Operation(
      summary = "Refresh access token",
      description =
          "Issues a new access token using the refresh token cookie. "
              + "Implements refresh token rotation — old token is revoked, new one is issued.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
    @ApiResponse(responseCode = "401", description = "Missing, expired, or revoked refresh token")
  })
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      HttpServletRequest request, HttpServletResponse response) {
    return ResponseEntity.ok(authService.refresh(extractRefreshToken(request), response));
  }

  /**
   * Initiates a password reset flow for the given email address.
   *
   * <p>If the email exists, publishes a password reset email event to Kafka. A 60-second cooldown
   * is enforced per user to prevent abuse. Always returns {@code 200 OK} regardless of whether the
   * email exists to prevent user enumeration attacks.
   *
   * @param request password reset request payload (email)
   * @return confirmation message (always returned, even if email not found)
   */
  @Operation(
      summary = "Initiate password reset",
      description =
          "Publishes a password reset email event if the email exists. "
              + "Always returns 200 OK to prevent user enumeration. "
              + "A 60-second cooldown is enforced per user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Response sent (regardless of email existence)"),
    @ApiResponse(responseCode = "429", description = "Cooldown active — too many requests")
  })
  @PostMapping("/forgot-password")
  public ResponseEntity<MessageResponse> forgotPassword(
      @RequestBody @Valid ForgotPasswordRequest request) {
    authService.forgotPassword(request);
    return ResponseEntity.ok(new MessageResponse().setMessage("Password reset email sent."));
  }

  /**
   * Completes the password reset flow using a one-time token.
   *
   * <p>The token is consumed atomically via Redis {@code GETDEL} — the delete is the gate,
   * preventing replay attacks even under concurrent requests. On success, all existing refresh
   * tokens for the user are revoked.
   *
   * @param request reset payload (token, newPassword)
   * @return confirmation message
   */
  @Operation(
      summary = "Complete password reset",
      description =
          "Resets the user's password using a one-time token. "
              + "Token is consumed atomically — replay attacks are prevented. "
              + "All existing refresh tokens are revoked on success.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Password reset successfully"),
    @ApiResponse(responseCode = "400", description = "Validation failed"),
    @ApiResponse(responseCode = "401", description = "Invalid or expired token")
  })
  @PostMapping("/reset-password")
  public ResponseEntity<MessageResponse> resetPassword(
      @RequestBody @Valid ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ResponseEntity.ok(new MessageResponse().setMessage("Password reset"));
  }

  /**
   * Verifies a user's email address using a one-time token.
   *
   * <p>The token is consumed atomically via Redis {@code GETDEL}. On success, the user's {@code
   * emailVerified} flag is set to {@code true} and a fresh access token is returned with the
   * updated {@code emailVerified=true} claim, so gated features unlock immediately without
   * requiring a new login.
   *
   * @param token one-time email verification token (sent via email link)
   * @return {@link AuthResponse} containing a fresh access token with {@code emailVerified=true}
   */
  @Operation(
      summary = "Verify email address",
      description =
          "Verifies the user's email using a one-time token. "
              + "Returns a fresh access token with emailVerified=true so gated features "
              + "unlock immediately without requiring a new login.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Email verified successfully"),
    @ApiResponse(responseCode = "401", description = "Invalid or expired token")
  })
  @GetMapping("/verify-email")
  public ResponseEntity<AuthResponse> verifyEmail(
      @Parameter(description = "One-time email verification token", required = true) @RequestParam
          String token) {
    return ResponseEntity.ok(authService.verifyEmail(token));
  }

  /**
   * Resends the email verification email to the given address.
   *
   * <p>Silently returns {@code 200 OK} if the email is not found or already verified, to prevent
   * user enumeration. A 60-second cooldown is enforced per user to prevent abuse. Publishes the
   * verification email event to Kafka.
   *
   * @param request resend request payload (email)
   * @return confirmation message
   */
  @Operation(
      summary = "Resend verification email",
      description =
          "Publishes a new email verification event. "
              + "Silently returns 200 OK if the email is not found or already verified. "
              + "A 60-second cooldown is enforced per user.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Email sent (or silently skipped)"),
    @ApiResponse(responseCode = "429", description = "Cooldown active — too many requests")
  })
  @PostMapping("/resend-verification-email")
  public ResponseEntity<MessageResponse> resendVerificationEmail(
      @RequestBody @Valid ResendVerificationEmailRequest request) {
    authService.resendVerificationEmail(request);
    return ResponseEntity.ok(new MessageResponse().setMessage("Email sent"));
  }

  /**
   * Checks whether a username is available for registration.
   *
   * <p>Uses a two-tier lookup for efficiency at scale:
   *
   * <ol>
   *   <li>Tier 1 — distributed Bloom filter (Redis bitmap, O(1)): if the filter returns a definite
   *       negative, the username is available and the database is not consulted.
   *   <li>Tier 2 — B+ tree index (PostgreSQL, O(log n)): reached only on a probable positive from
   *       the Bloom filter, to resolve false positives definitively.
   * </ol>
   *
   * @param username the username to check (minimum 3 characters)
   * @return {@link UsernameAvailabilityResponse} indicating whether the username is available
   */
  @Operation(
      summary = "Check username availability",
      description =
          "Checks if a username is available using a two-tier lookup: "
              + "a distributed Bloom filter (O(1), no DB hit on definite negatives) "
              + "followed by a B+ tree index lookup (O(log n)) to resolve false positives.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Availability check successful"),
    @ApiResponse(responseCode = "400", description = "Username too short (minimum 3 characters)")
  })
  @GetMapping("/usernames/{username}/availability")
  public ResponseEntity<UsernameAvailabilityResponse> usernameAvailability(
      @Parameter(description = "Username to check (minimum 3 characters)", required = true)
          @PathVariable
          @Size(min = 3, max = 40, message = "Username must be at betwee 3 and 40 characters")
          String username) {
    boolean available = authService.usernameAvailability(username);
    return ResponseEntity.ok(new UsernameAvailabilityResponse().setAvailable(available));
  }

  /**
   * Extracts the refresh token value from the {@code refreshToken} HttpOnly cookie.
   *
   * @param request the incoming HTTP request
   * @return the refresh token string
   * @throws BusinessException ({@code INVALID_TOKEN}) if the cookie is absent
   */
  private String extractRefreshToken(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, "refreshToken");
    if (cookie == null) {
      throw new BusinessException(AuthErrors.INVALID_TOKEN);
    }
    return cookie.getValue();
  }
}
