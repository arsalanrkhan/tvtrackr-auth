package com.tvtrackr.auth.service.impl;

import com.tvtrackr.auth.service.AuthEmailService;
import com.tvtrackr.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEmailServiceImpl implements AuthEmailService {

  // Template names
  private static final String TEMPLATE_PASSWORD_RESET = "email/password-reset";
  private static final String TEMPLATE_EMAIL_VERIFICATION = "email/email-verification";

  // Subjects
  private static final String SUBJECT_PASSWORD_RESET = "Reset your TVTrackr password";
  private static final String SUBJECT_EMAIL_VERIFICATION = "Verify your TVTrackr email address";

  // Template variables
  private static final String VAR_DISPLAY_NAME = "displayName";
  private static final String VAR_RESET_LINK = "resetLink";
  private static final String VAR_VERIFICATION_LINK = "verificationLink";

  // URL paths
  private static final String PATH_RESET_PASSWORD = "/api/auth/v1/reset-password";
  private static final String PATH_VERIFY_EMAIL = "/api/auth/v1/verify-email";

  // Query params
  private static final String PARAM_TOKEN = "?token=";

  private final EmailService emailService;
  private final SpringTemplateEngine templateEngine;

  @Value("${app.frontend.base-url}")
  private String frontendBaseUrl;

  @Override
  public void sendPasswordResetEmail(String to, String displayName, String token) {
    String resetLink = buildLink(PATH_RESET_PASSWORD, token);

    Context context = new Context();
    context.setVariable(VAR_DISPLAY_NAME, displayName);
    context.setVariable(VAR_RESET_LINK, resetLink);

    String html = templateEngine.process(TEMPLATE_PASSWORD_RESET, context);
    emailService.send(to, SUBJECT_PASSWORD_RESET, html);
    log.info("[AuthEmail] Password reset email sent to {}", to);
  }

  @Override
  public void sendEmailVerificationEmail(String to, String displayName, String token) {
    String verificationLink = buildLink(PATH_VERIFY_EMAIL, token);

    Context context = new Context();
    context.setVariable(VAR_DISPLAY_NAME, displayName);
    context.setVariable(VAR_VERIFICATION_LINK, verificationLink);

    String html = templateEngine.process(TEMPLATE_EMAIL_VERIFICATION, context);
    emailService.send(to, SUBJECT_EMAIL_VERIFICATION, html);
    log.info("[AuthEmail] Verification email sent to {}", to);
  }

  private String buildLink(String path, String token) {
    return frontendBaseUrl + path + PARAM_TOKEN + token;
  }
}
