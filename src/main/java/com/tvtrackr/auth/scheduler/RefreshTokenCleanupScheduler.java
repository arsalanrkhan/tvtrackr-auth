package com.tvtrackr.auth.scheduler;

import com.tvtrackr.auth.service.RefreshTokenService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

  private final RefreshTokenService refreshTokenService;

  @Scheduled(cron = "0 0 1 * * *")
  @SchedulerLock(name = "refreshTokenCleanup", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
  public void cleanupExpiredTokens() {
    long startTime = System.currentTimeMillis();
    log.info("[CleanupToken] Job started at {}", startTime);

    int total = 0, deleted;
    do {
      deleted = refreshTokenService.deleteExpiredOrRevoked(LocalDateTime.now(), 1000);
      total += deleted;
    } while (deleted > 0);

    long endTime = System.currentTimeMillis();
    log.info("[CleanupToken] Deleted {} expired or revoked refresh token(s)", total);
    log.info("[CleanupToken] Job finished at {}. Elapsed time: {}", endTime, endTime - startTime);
  }
}
