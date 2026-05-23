package com.tvtrackr.auth.bloomfilter;

import com.tvtrackr.auth.bloomfilter.service.UsernameBFService;
import com.tvtrackr.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsernameBFSeeder implements ApplicationRunner {

  private final UsernameBFService usernameBFService;
  private final UserRepository userRepository;

  /**
   * Seeds the Bloom filter from all existing usernames on startup.
   *
   * <p>Uses {@code isSeeded()}/{@code markSeeded()} — two separate Redis calls with no atomicity
   * between them. In theory, two instances starting simultaneously could both see {@code isSeeded()
   * = false} and seed concurrently, resulting in redundant parallel DB streaming queries. In
   * practice this is not a concern: instances are spun up sequentially by the load balancer, and
   * each new instance finds the guard already set by the first. The scheduler path is separately
   * protected by ShedLock. If a truly atomic claim ever becomes necessary, replace the two calls
   * with a single {@code SET NX} via {@code saveStringPermanentIfAbsent}.
   */
  @Override
  @Transactional(readOnly = true)
  public void run(ApplicationArguments args) {
    if (usernameBFService.isSeeded()) {
      log.info("[Username BloomFilter] Already seeded, skipping.");
      return;
    }
    seed();
  }

  @Transactional(readOnly = true)
  public void seed() {
    log.info("[Username BloomFilter] Seeding from DB...");
    userRepository.streamAllUsernames().forEach(usernameBFService::add);
    usernameBFService.markSeeded();
    log.info("[Username BloomFilter] Seeding complete.");
  }
}
