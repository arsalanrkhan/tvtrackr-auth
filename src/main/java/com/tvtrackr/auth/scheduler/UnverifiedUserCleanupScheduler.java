package com.tvtrackr.auth.scheduler;

import com.tvtrackr.auth.bloomfilter.UsernameBFSeeder;
import com.tvtrackr.auth.bloomfilter.service.UsernameBFService;
import com.tvtrackr.auth.service.UserService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UnverifiedUserCleanupScheduler {

  private final UserService userService;
  private final UsernameBFService usernameBFService;
  private final UsernameBFSeeder usernameBFSeeder;

  @Value("${app.scheduler.cleanup.after-days}")
  private Long cleanupAfterDays;

  @Scheduled(cron = "0 0 0 * * *")
  @SchedulerLock(name = "unverifiedUserCleanup", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
  public void cleanupUnverifiedUsers() {

    long startTime = System.currentTimeMillis();
    log.info("[Cleanup] Job started at {}", startTime);

    LocalDateTime cutoff = LocalDateTime.now().minusDays(cleanupAfterDays);
    int total = 0, deleted;
    do {
      deleted = userService.deleteUnverifiedUsersWithCutoffInChunk(cutoff, 1000);
      total += deleted;
    } while (deleted > 0);

    log.info("[Cleanup] Deleted {} unverified user(s) older than {} days", total, cleanupAfterDays);

    // Note: there is a brief window between clear() and seed() completing where
    // the filter is empty. During this time, mightExist() returns false for all
    // usernames, causing usernameAvailability() to skip the DB check and return
    // available. The DB unique constraint on registration remains the safety net.
    // When moving to large scale, we can add a blue/green filter strategy.
    usernameBFService.clear();
    usernameBFSeeder.seed();

    long endTime = System.currentTimeMillis();
    log.info("[Cleanup] Job finished at {}. Elapsed time: {}", endTime, endTime - startTime);
  }
}
