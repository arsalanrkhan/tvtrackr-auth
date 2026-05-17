package com.tvtrackr.auth.service.impl;

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
import com.tvtrackr.auth.service.AuthService;
import com.tvtrackr.auth.service.RefreshTokenService;
import com.tvtrackr.auth.service.TokenService;
import com.tvtrackr.auth.service.UserService;
import com.tvtrackr.auth.validator.RegisterRequestValidator;
import com.tvtrackr.common.error.BusinessException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final RefreshTokenService refreshTokenService;
  private final UserService userService;
  private final RegisterRequestValidator registerRequestValidator;

  @Value("${app.refresh-token.expiry-days}")
  private Long refreshTokenExpiryDays;

  @Override
  @Transactional
  public AuthResponse register(RegisterRequest request) {

    registerRequestValidator.validate(request);

    User user = toUser(request);
    userService.save(user);

    user.getAuthProviders().add(toAuthProvider(user, request.getPassword()));
    userService.save(user);

    String accessToken = tokenService.generateAccessToken(user);
    return new AuthResponse()
        .setUuid(user.getUuid().toString())
        .setEmail(user.getEmail())
        .setAccessToken(accessToken)
        .setUsername(user.getUsername());
  }

  @Override
  public AuthResponse login(LoginRequest request, HttpServletResponse response) {
    User user =
        userService.getUserByUsernameOrEmail(
            request.getEmailOrUsername(), request.getEmailOrUsername());

    UserAuthProvider localAuthProvider = findLocalAuthProviderOrThrow(user);
    verifyPasswordOrThrow(request.getPassword(), localAuthProvider.getPasswordHash());

    String accessToken = tokenService.generateAccessToken(user);
    RefreshToken refreshToken = refreshTokenService.generateAndSaveRefreshToken(user);
    setRefreshTokenCookie(response, refreshToken.getToken());

    return buildAuthResponse(user, accessToken);
  }

  @Override
  public void logout(String refreshTokenStr, HttpServletResponse response) {
    RefreshToken refreshToken = refreshTokenService.find(refreshTokenStr);
    if (refreshToken.isRevoked() || refreshToken.isExpired()) {
      throw new BusinessException(AuthErrors.INVALID_REFRESH_TOKEN);
    }
    refreshToken.setRevoked(true);
    refreshTokenService.save(refreshToken);
    clearRefreshTokenCookie(response);
  }

  @Override
  public AuthResponse refresh(String refreshToken) {
    return null;
  }

  @Override
  public void forgotPassword(ForgotPasswordRequest request) {}

  @Override
  public void resetPassword(ResetPasswordRequest request) {}

  @Override
  public void verifyEmail(String token) {}

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

  private void setRefreshTokenCookie(HttpServletResponse response, String token) {
    Cookie cookie = new Cookie("refreshToken", token);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge((int) (refreshTokenExpiryDays * 24 * 60 * 60));
    response.addCookie(cookie);
  }

  private void clearRefreshTokenCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie("refreshToken", "");
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }

  private AuthResponse buildAuthResponse(User user, String accessToken) {
    return new AuthResponse()
        .setUsername(user.getUsername())
        .setUuid(user.getUuid().toString())
        .setEmail(user.getEmail())
        .setAccessToken(accessToken);
  }
}
