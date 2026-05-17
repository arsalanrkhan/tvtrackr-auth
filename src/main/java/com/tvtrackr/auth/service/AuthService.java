package com.tvtrackr.auth.service;

import com.tvtrackr.auth.dto.req.ForgotPasswordRequest;
import com.tvtrackr.auth.dto.req.LoginRequest;
import com.tvtrackr.auth.dto.req.RegisterRequest;
import com.tvtrackr.auth.dto.req.ResetPasswordRequest;
import com.tvtrackr.auth.dto.res.AuthResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

  AuthResponse register(RegisterRequest request);

  AuthResponse login(LoginRequest request, HttpServletResponse response);

  void logout(String refreshToken, HttpServletResponse response);

  AuthResponse refresh(String refreshToken);

  void forgotPassword(ForgotPasswordRequest request);

  void resetPassword(ResetPasswordRequest request);

  void verifyEmail(String token);
}
