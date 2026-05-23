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
