package com.tvtrackr.auth.scheduler;

import com.tvtrackr.auth.entity.User;
import com.tvtrackr.auth.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class UnverifiedUserCleanupScheduler {

  private final UserRepository userRepository;

  @Value("${app.scheduler.cleanup.after-days}")
  private Long cleanupAfterDays;

  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void cleanupUnverifiedUsers() {

    LocalDateTime cutoff = LocalDateTime.now().minusDays(cleanupAfterDays);
    List<User> users = userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(cutoff);

    if (users.isEmpty()) {
      log.info("[Cleanup] No unverified users to delete");
      return;
    }

    userRepository.deleteAll(users);
    log.info(
        "[Cleanup] Deleted {} unverified user(s) older than {} days",
        users.size(),
        cleanupAfterDays);
  }
}
