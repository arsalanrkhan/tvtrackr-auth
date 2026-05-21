package com.tvtrackr.auth.repository;

import com.tvtrackr.auth.entity.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByToken(String token);

  List<RefreshToken> findAllByUserId(Long userId);

  List<RefreshToken> findAllByUserIdAndRevokedFalse(Long userId);
}
