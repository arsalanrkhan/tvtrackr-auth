package com.tvtrackr.auth.service.impl;

import com.tvtrackr.auth.constants.enums.GlobalConstants;
import com.tvtrackr.auth.service.TokenCacheService;
import com.tvtrackr.common.redis.service.RedisService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TokenCacheServiceImpl implements TokenCacheService {

  private final RedisService redisService;

  private static final String PASSWORD_RESET_PREFIX =
      GlobalConstants.REDIS_PREFIX + "passwordReset:";
  private static final String EMAIL_VERIFY_PREFIX =
      GlobalConstants.REDIS_PREFIX + "emailVerification:";

  @Value("${redis.cache.password-reset.ttl}")
  private Long passwordResetTtl;

  @Value("${redis.cache.email-verification.ttl}")
  private Long emailVerificationTtl;

  @Override
  public void savePasswordResetToken(String token, Long userId) {
    redisService.saveString(
        PASSWORD_RESET_PREFIX + token, String.valueOf(userId), passwordResetTtl);
  }

  @Override
  public Optional<Long> getPasswordResetToken(String token) {
    String val = redisService.getString(PASSWORD_RESET_PREFIX + token);
    return Optional.ofNullable(StringUtils.hasText(val) ? Long.valueOf(val) : null);
  }

  @Override
  public void deletePasswordResetToken(String token) {
    redisService.delete(PASSWORD_RESET_PREFIX + token);
  }

  @Override
  public void saveEmailVerificationToken(String token, Long userId) {
    redisService.saveString(
        EMAIL_VERIFY_PREFIX + token, String.valueOf(userId), emailVerificationTtl);
  }

  @Override
  public Optional<Long> getEmailVerificationToken(String token) {
    String val = redisService.getString(EMAIL_VERIFY_PREFIX + token);
    return Optional.ofNullable(StringUtils.hasText(val) ? Long.valueOf(val) : null);
  }

  @Override
  public void deleteEmailVerificationToken(String token) {
    redisService.delete(EMAIL_VERIFY_PREFIX + token);
  }
}
