package com.trello.repository;

import com.trello.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	Optional<User> findByUsername(String username);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);
	
	// Pagination support
	Page<User> findAll(Pageable pageable);
	
	@Query("SELECT u FROM User u WHERE u.role = :role ORDER BY u.createdAt DESC")
	Page<User> findByRolePaged(@Param("role") String role, Pageable pageable);
	
	// Count queries optimized
	@Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ADMIN'")
	long countAdmins();
	
	@Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
	long countByRole(@Param("role") String role);
}

