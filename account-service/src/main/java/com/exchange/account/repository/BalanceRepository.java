package com.exchange.account.repository;

import com.exchange.account.entity.Balance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BalanceRepository extends JpaRepository<Balance, Long> {

    List<Balance> findByUserId(String userId);

    Optional<Balance> findByUserIdAndAsset(String userId, String asset);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Balance b WHERE b.userId = :userId AND b.asset = :asset")
    Optional<Balance> findByUserIdAndAssetForUpdate(
            @Param("userId") String userId,
            @Param("asset") String asset);
}
