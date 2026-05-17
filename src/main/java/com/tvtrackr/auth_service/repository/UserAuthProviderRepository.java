package com.tvtrackr.auth_service.repository;

import com.tvtrackr.auth_service.constants.enums.AuthProvider;
import com.tvtrackr.auth_service.entity.UserAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {

    List<UserAuthProvider> findAllByUserId(Long userId);

    Optional<UserAuthProvider> findByUserIdAndProvider(Long userId, AuthProvider provider);

    boolean existsByUserIdAndProvider(Long userId, AuthProvider provider);
}
