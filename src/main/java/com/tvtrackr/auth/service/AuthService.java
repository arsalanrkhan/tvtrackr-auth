package com.tvtrackr.auth.service;

import com.tvtrackr.auth.dto.req.*;
import com.tvtrackr.auth.dto.res.AuthResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

  AuthResponse register(RegisterRequest request, HttpServletResponse response);

  AuthResponse login(LoginRequest request, HttpServletResponse response);

  void logout(String refreshToken, HttpServletResponse response);

  AuthResponse refresh(String refreshToken, HttpServletResponse response);

  void forgotPassword(ForgotPasswordRequest request);

  void resetPassword(ResetPasswordRequest request);

  AuthResponse verifyEmail(String token);

  void resendVerificationEmail(ResendVerificationEmailRequest request);

  boolean usernameAvailability(String username);
}
