package com.tvtrackr.auth.bloomfilter.service.impl;

import static java.nio.charset.StandardCharsets.*;

import com.google.common.hash.Hashing;
import com.tvtrackr.auth.bloomfilter.service.UsernameBFService;
import com.tvtrackr.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Distributed Bloom filter for username availability checks, backed by Redis bitmaps. Uses double
 * hashing (MurmurHash3 with two seeds) to derive k bit offsets per username, stored via
 * SETBIT/GETBIT on a shared Redis key. This keeps the filter distributed across all auth-service
 * instances, unlike an in-memory solution (e.g. Guava's BloomFilter).
 *
 * <p>Upstash Redis (free tier) does not support native RedisBloom commands (BF.ADD/BF.EXISTS), so
 * this implementation builds equivalent behaviour on top of standard Redis bitmap primitives.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsernameBFServiceImpl implements UsernameBFService {

  private static final String BLOOM_KEY = "bloom:usernames";
  private static final String SEEDED_KEY = "bloom:usernames:seeded";

  private final RedisService redisService;

  @Value("${bloom-filter.bit-size}")
  private long bitSize;

  @Value("${bloom-filter.hash-count}")
  private int hashCount;

  @Override
  public void add(String username) {
    for (long offset : getOffsets(username.toLowerCase())) {
      redisService.setBit(BLOOM_KEY, offset);
    }
  }

  @Override
  public boolean mightExist(String username) {
    for (long offset : getOffsets(username.toLowerCase())) {
      if (!redisService.getBit(BLOOM_KEY, offset)) return false;
    }
    return true;
  }

  @Override
  public boolean isSeeded() {
    return redisService.exists(SEEDED_KEY);
  }

  @Override
  public void markSeeded() {
    redisService.saveStringPermanent(SEEDED_KEY, "1");
  }

  @Override
  public void clear() {
    log.warn("[UsernameBFService] Clearing BloomFilter");
    redisService.delete(BLOOM_KEY);
    redisService.delete(SEEDED_KEY);
    log.warn("[UsernameBFService] Cleared BloomFilter");
  }

  private long[] getOffsets(String normalized) {
    long hash1 = Hashing.murmur3_128(0).hashString(normalized, UTF_8).asLong();
    long hash2 = Hashing.murmur3_128(1).hashString(normalized, UTF_8).asLong();
    long[] offsets = new long[hashCount];
    for (int i = 0; i < hashCount; i++) {
      offsets[i] = Math.abs((hash1 + (long) i * hash2) % bitSize);
    }
    return offsets;
  }
}
