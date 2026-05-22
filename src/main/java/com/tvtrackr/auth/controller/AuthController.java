package com.tvtrackr.auth.controller;

import com.tvtrackr.auth.dto.req.*;
import com.tvtrackr.auth.dto.res.AuthResponse;
import com.tvtrackr.auth.dto.res.MessageResponse;
import com.tvtrackr.auth.dto.res.UsernameAvailabilityResponse;
import com.tvtrackr.auth.exception.AuthErrors;
import com.tvtrackr.auth.service.AuthService;
import com.tvtrackr.common.error.BusinessException;
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

@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
@Validated
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> registerUser(@RequestBody @Valid RegisterRequest request) {
    return ResponseEntity.ok(authService.register(request));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(
      @RequestBody @Valid LoginRequest request, HttpServletResponse response) {
    return ResponseEntity.ok(authService.login(request, response));
  }

  @PostMapping("/logout")
  public ResponseEntity<MessageResponse> logout(
      HttpServletRequest request, HttpServletResponse response) {
    authService.logout(extractRefreshToken(request), response);
    return ResponseEntity.ok(new MessageResponse().setMessage("Logout successful"));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      HttpServletRequest request, HttpServletResponse response) {
    return ResponseEntity.ok(authService.refresh(extractRefreshToken(request), response));
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<MessageResponse> forgotPassword(
      @RequestBody @Valid ForgotPasswordRequest request) {
    authService.forgotPassword(request);
    return ResponseEntity.ok(new MessageResponse().setMessage("Password reset email sent."));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<MessageResponse> resetPassword(
      @RequestBody @Valid ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ResponseEntity.ok(new MessageResponse().setMessage("Password reset"));
  }

  @GetMapping("/verify-email")
  public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
    authService.verifyEmail(token);
    return ResponseEntity.ok(new MessageResponse().setMessage("Email verified"));
  }

  @PostMapping("/resend-verification-email")
  public ResponseEntity<MessageResponse> resendVerificationEmail(
      @RequestBody @Valid ResendVerificationEmailRequest request) {
    authService.resendVerificationEmail(request);
    return ResponseEntity.ok(new MessageResponse().setMessage("Email sent"));
  }

  @GetMapping("/usernames/{username}/availability")
  public ResponseEntity<UsernameAvailabilityResponse> usernameAvailability(
      @PathVariable @Size(min = 3, message = "Username must be at least 3 characters")
          String username) {
    boolean available = authService.usernameAvailability(username);
    return ResponseEntity.ok(new UsernameAvailabilityResponse().setAvailable(available));
  }

  private String extractRefreshToken(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, "refreshToken");
    if (cookie == null) {
      throw new BusinessException(AuthErrors.INVALID_TOKEN);
    }
    return cookie.getValue();
  }
}
