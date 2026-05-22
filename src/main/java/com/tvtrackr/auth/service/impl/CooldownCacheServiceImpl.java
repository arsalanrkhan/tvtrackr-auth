package com.tvtrackr.auth.service.impl;

import com.tvtrackr.auth.constants.enums.GlobalConstants;
import com.tvtrackr.auth.service.CooldownCacheService;
import com.tvtrackr.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CooldownCacheServiceImpl implements CooldownCacheService {

  private final RedisService redisService;

  @Override
  public boolean isOnCooldown(String key) {
    return StringUtils.hasText(redisService.getString(GlobalConstants.REDIS_PREFIX + key));
  }

  @Override
  public void setCooldown(String key, Long ttl) {
    redisService.saveString(GlobalConstants.REDIS_PREFIX + key, "1", ttl);
  }
}
