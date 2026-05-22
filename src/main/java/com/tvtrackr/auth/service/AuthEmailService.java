package com.tvtrackr.auth.service;

public interface AuthEmailService {

  void sendPasswordResetEmail(String to, String displayName, String token);

  void sendEmailVerificationEmail(String to, String displayName, String token);
}
