package com.tvtrackr.auth.service;

import com.tvtrackr.auth.dto.req.*;
import com.tvtrackr.auth.dto.res.AuthResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

  AuthResponse register(RegisterRequest request);

  AuthResponse login(LoginRequest request, HttpServletResponse response);

  void logout(String refreshToken, HttpServletResponse response);

  AuthResponse refresh(String refreshToken, HttpServletResponse response);

  void forgotPassword(ForgotPasswordRequest request);

  void resetPassword(ResetPasswordRequest request);

  void verifyEmail(String token);

  void resendVerificationEmail(ResendVerificationEmailRequest request);
}
