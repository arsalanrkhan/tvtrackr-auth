package com.tvtrackr.auth.service.impl;

import com.tvtrackr.auth.service.CooldownCacheService;
import com.tvtrackr.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CooldownCacheServiceImpl implements CooldownCacheService {

  private final RedisService redisService;

  @Override
  public boolean trySetCooldown(String key, long ttlMs) {
    return redisService.saveStringIfAbsent(key, "1", ttlMs);
  }
}
