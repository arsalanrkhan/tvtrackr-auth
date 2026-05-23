package com.tvtrackr.auth.repository;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;

import com.tvtrackr.auth.entity.User;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUuid(UUID uuid);

  Optional<User> findByEmail(String email);

  Optional<User> findByUsername(String username);

  @Query(
      """
    SELECT u FROM User u
    LEFT JOIN FETCH u.authProviders
    WHERE u.email = :email OR LOWER(u.username) = LOWER(:username)
    """)
  Optional<User> findByEmailOrUsernameCaseInsensitive(
      @Param("email") String email, @Param("username") String username);

  boolean existsByEmail(String email);

  @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.username) = LOWER(:username)")
  boolean existsByUsernameCaseInsensitive(@Param("username") String username);

  @Modifying
  @Query("DELETE FROM User u WHERE u.emailVerified = false AND u.createdAt < :cutoff")
  int deleteAllUnverifiedBefore(@Param("cutoff") LocalDateTime cutoff);

  @QueryHints({
    @QueryHint(name = HINT_FETCH_SIZE, value = "1000"),
    @QueryHint(name = "org.hibernate.readOnly", value = "true")
  })
  @Query("SELECT u.username FROM User u")
  Stream<String> streamAllUsernames();
}
