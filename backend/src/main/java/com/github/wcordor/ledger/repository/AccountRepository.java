package com.github.wcordor.ledger.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.github.wcordor.ledger.entity.Account;

import jakarta.persistence.LockModeType;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findWithLockingById(Long id);

    //List<Account> findByName(String name);
    List<Account> findByUserLastName(String lastName);
    List<Account> findByCurrency(String currency);

    @EntityGraph(attributePaths = {"sent", "received"})
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findById(@Param("id") Long id);
    
    List<Account> findByUser_Id(Long id);
    
    /*@EntityGraph(attributePaths = {"sent", "received"})
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.user.id = :userId")
    Optional<Account> findByIdAndUser_Id(@Param("id") Long accountId, @Param("userId") Long userId);*/

    @EntityGraph(attributePaths = {"sent", "received"})
    @Query("SELECT a FROM Account a WHERE a.name = :name")
    Optional<Account> findByName(@Param("name") String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"sent", "received"})
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.user.id = :userId")
    Optional<Account> findWithLockingByIdAndUser_Id(@Param("id") Long accountId, @Param("userId") Long userId);
}
