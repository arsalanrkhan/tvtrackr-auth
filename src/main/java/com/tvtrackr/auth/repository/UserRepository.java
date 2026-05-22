package com.tvtrackr.auth.repository;

import com.tvtrackr.auth.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUuid(UUID uuid);

  Optional<User> findByEmail(String email);

  List<User> findAllByEmailVerifiedFalseAndCreatedAtBefore(LocalDateTime cutoff);

  Optional<User> findByUsername(String username);

  Optional<User> findByEmailOrUsername(String email, String username);

  boolean existsByEmail(String email);

  boolean existsByUsername(String username);
}
