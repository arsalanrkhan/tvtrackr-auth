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
    if (!isValidToken(refreshToken)) {
      throw new BusinessException(AuthErrors.INVALID_REFRESH_TOKEN);
    }
    refreshTokenService.revoke(refreshToken);
    clearRefreshTokenCookie(response);
  }

  @Override
  @Transactional
  public AuthResponse refresh(String refreshTokenStr, HttpServletResponse response) {
    // Fetching and validating refresh token
    RefreshToken refreshToken = refreshTokenService.find(refreshTokenStr);
    if (!isValidToken(refreshToken)) {
      throw new BusinessException(AuthErrors.INVALID_REFRESH_TOKEN);
    }

    // Revoking old refresh token & generating new refresh and access tokens
    User user = refreshToken.getUser();
    refreshTokenService.revoke(refreshToken);

    RefreshToken newRefreshToken = refreshTokenService.generateAndSaveRefreshToken(user);
    setRefreshTokenCookie(response, newRefreshToken.getToken());

    return buildAuthResponse(user, tokenService.generateAccessToken(user));
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
