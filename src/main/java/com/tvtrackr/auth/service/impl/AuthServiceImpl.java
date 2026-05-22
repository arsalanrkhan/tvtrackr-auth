package com.tvtrackr.auth.service.impl;

import static com.tvtrackr.auth.util.ApplicationUtil.isValidToken;

import com.tvtrackr.auth.constants.enums.AuthProvider;
import com.tvtrackr.auth.dto.req.ForgotPasswordRequest;
import com.tvtrackr.auth.dto.req.LoginRequest;
import com.tvtrackr.auth.dto.req.RegisterRequest;
import com.tvtrackr.auth.dto.req.ResetPasswordRequest;
import com.tvtrackr.auth.dto.res.AuthResponse;
import com.tvtrackr.auth.entity.RefreshToken;
import com.tvtrackr.auth.entity.User;
import com.tvtrackr.auth.entity.UserAuthProvider;
import com.tvtrackr.auth.exception.AuthErrors;
import com.tvtrackr.auth.service.*;
import com.tvtrackr.auth.validator.RegisterRequestValidator;
import com.tvtrackr.common.error.BusinessException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final RefreshTokenService refreshTokenService;
  private final UserService userService;
  private final UserAuthProviderService userAuthProviderService;
  private final TokenCacheService tokenCacheService;
  private final RegisterRequestValidator registerRequestValidator;

  @Value("${app.refresh-token.expiry-days}")
  private Long refreshTokenExpiryDays;

  @Override
  @Transactional
  public AuthResponse register(RegisterRequest request) {
    log.info("[Register User] Request received for email {}", request.getEmail());

    registerRequestValidator.validate(request);

    User user = toUser(request);
    userService.save(user);

    user.getAuthProviders().add(toAuthProvider(user, request.getPassword()));
    userService.save(user);

    String accessToken = tokenService.generateAccessToken(user);
    log.info("[Register User] Successful for email {}", request.getEmail());
    return new AuthResponse()
        .setUuid(user.getUuid().toString())
        .setEmail(user.getEmail())
        .setAccessToken(accessToken)
        .setUsername(user.getUsername());
  }

  @Override
  public AuthResponse login(LoginRequest request, HttpServletResponse response) {
    log.info("[Login] Request for identifier={}", request.getEmailOrUsername());
    User user =
        userService.getUserByUsernameOrEmail(
            request.getEmailOrUsername(), request.getEmailOrUsername());

    UserAuthProvider localAuthProvider = findLocalAuthProviderOrThrow(user);
    verifyPasswordOrThrow(request.getPassword(), localAuthProvider.getPasswordHash());

    String accessToken = tokenService.generateAccessToken(user);
    RefreshToken refreshToken = refreshTokenService.generateAndSaveRefreshToken(user);
    setRefreshTokenCookie(response, refreshToken.getToken());
    log.info(
        "[Login] Successful for uuid={} identifier={}",
        user.getUuid(),
        request.getEmailOrUsername());
    return buildAuthResponse(user, accessToken);
  }

  @Override
  @Transactional
  public void logout(String refreshTokenStr, HttpServletResponse response) {
    RefreshToken refreshToken = refreshTokenService.find(refreshTokenStr);
    if (!isValidToken(refreshToken)) {
      throw new BusinessException(AuthErrors.INVALID_TOKEN);
    }
    refreshTokenService.revoke(refreshToken);
    clearRefreshTokenCookie(response);
  }

  @Override
  @Transactional
  public AuthResponse refresh(String refreshTokenStr, HttpServletResponse response) {
    // Fetching and validating refresh token
    RefreshToken refreshToken = refreshTokenService.findForUpdate(refreshTokenStr);
    if (!isValidToken(refreshToken)) {
      throw new BusinessException(AuthErrors.INVALID_TOKEN);
    }

    // Revoking old refresh token & generating new refresh and access tokens
    User user = refreshToken.getUser();
    refreshTokenService.revoke(refreshToken);

    RefreshToken newRefreshToken = refreshTokenService.generateAndSaveRefreshToken(user);
    setRefreshTokenCookie(response, newRefreshToken.getToken());

    return buildAuthResponse(user, tokenService.generateAccessToken(user));
  }

  @Override
  public void forgotPassword(ForgotPasswordRequest request) {
    log.info("[Password Reset] Received request for email: {}", request.getEmail());
    User user;
    try {
      user = userService.getUserByEmail(request.getEmail());
    } catch (BusinessException be) {
      // Do not throw an error if user doesn't exist to avoid enumeration attack
      log.debug(
          "[Password Reset] Email {} not found. Returning empty response", request.getEmail());
      return;
    }
    String token = tokenService.generateRefreshToken();
    tokenCacheService.savePasswordResetToken(token, user.getId());

    // TODO: Send email to the user with password reset link
    log.info("[Password Reset] Password reset email sent to {}", request.getEmail());

    log.debug(
        "[Password Reset] Password reset email sent to {}, token={}", request.getEmail(), token);
  }

  @Override
  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    log.debug("[Password Reset] Received token={}", request.getToken());
    Long userId =
        tokenCacheService
            .getPasswordResetToken(request.getToken())
            .orElseThrow(() -> new BusinessException(AuthErrors.INVALID_TOKEN));
    User user = userService.getUserById(userId);
    UserAuthProvider authProvider =
        user.getAuthProviders().stream()
            .filter(a -> AuthProvider.LOCAL.equals(a.getProvider()))
            .findFirst()
            .orElseThrow(() -> new BusinessException(AuthErrors.PASSWORD_RESET_NOT_SUPPORTED));

    String passHash = passwordEncoder.encode(request.getNewPassword());
    authProvider.setPasswordHash(passHash);
    userAuthProviderService.save(authProvider);

    tokenCacheService.deletePasswordResetToken(request.getToken());
    refreshTokenService.revokeAllByUserId(user.getId());
    log.info(
        "[Password Reset] Successfully reset password for user={} email={}",
        user.getUsername(),
        user.getEmail());
  }

  @Override
  @Transactional
  public void verifyEmail(String token) {
    Long userId =
        tokenCacheService
            .getEmailVerificationToken(token)
            .orElseThrow(() -> new BusinessException(AuthErrors.INVALID_TOKEN));
    User user = userService.getUserById(userId);
    user.setEmailVerified(true);
    userService.save(user);

    tokenCacheService.deleteEmailVerificationToken(token);
  }

  private User toUser(RegisterRequest request) {
    User user = new User();
    return user.setEmail(request.getEmail())
        .setUsername(request.getUsername())
        .setDisplayName(request.getDisplayName());
  }

  private UserAuthProvider toAuthProvider(User user, String password) {
    return new UserAuthProvider()
        .setProvider(AuthProvider.LOCAL)
        .setUser(user)
        .setPasswordHash(passwordEncoder.encode(password));
  }

  private UserAuthProvider findLocalAuthProviderOrThrow(User user) {
    return user.getAuthProviders().stream()
        .filter(ap -> AuthProvider.LOCAL.equals(ap.getProvider()))
        .findFirst()
        .orElseThrow(() -> new BusinessException(AuthErrors.INVALID_CREDENTIALS));
  }

  private void verifyPasswordOrThrow(String rawPassword, String hashedPassword) {
    if (!passwordEncoder.matches(rawPassword, hashedPassword)) {
      throw new BusinessException(AuthErrors.INVALID_CREDENTIALS);
    }
  }

  private Cookie buildCookie(String value, int maxAge) {
    Cookie cookie = new Cookie("refreshToken", value);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(maxAge);
    return cookie;
  }

  private void setRefreshTokenCookie(HttpServletResponse response, String token) {
    response.addCookie(buildCookie(token, (int) (refreshTokenExpiryDays * 24 * 60 * 60)));
  }

  private void clearRefreshTokenCookie(HttpServletResponse response) {
    response.addCookie(buildCookie("", 0));
  }

  private AuthResponse buildAuthResponse(User user, String accessToken) {
    return new AuthResponse()
        .setUsername(user.getUsername())
        .setUuid(user.getUuid().toString())
        .setEmail(user.getEmail())
        .setAccessToken(accessToken);
  }
}
