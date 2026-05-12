package com.legalai.repository;

import com.legalai.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    Optional<User> findByNameAndPhoneAndDeletedAtIsNull(String name, String phone);

    boolean existsByEmailAndDeletedAtIsNull(String email);
}
