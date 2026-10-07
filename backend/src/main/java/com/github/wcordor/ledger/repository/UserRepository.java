package com.github.wcordor.ledger.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.github.wcordor.ledger.entity.LedgerUser;

import jakarta.persistence.LockModeType;

@Repository
public interface UserRepository extends JpaRepository<LedgerUser, Long> {

    List<LedgerUser> findByLastName(String lastName);

    @EntityGraph(attributePaths = {"accounts"})
    @Query("SELECT u FROM LedgerUser u WHERE u.id = :id")
    Optional<LedgerUser> findById(@Param("id") Long id);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"accounts"})
    @Query("SELECT u FROM LedgerUser u WHERE u.id = :id")
    Optional<LedgerUser> findWithLockingById(Long id);

    @EntityGraph(attributePaths = {"accounts"})
    @Query("SELECT u FROM LedgerUser u WHERE u.username = :username")
    Optional<LedgerUser> findByUsername(String username);
    
    Optional<LedgerUser> findWithLockingByUsername(String username);
}
     